package de.tobchen.health.patientindex.feed.components;

import java.io.IOException;
import java.time.Instant;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.HapiContext;
import ca.uhn.hl7v2.app.Connection;
import ca.uhn.hl7v2.llp.LLPException;
import ca.uhn.hl7v2.model.DataTypeException;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.model.v231.datatype.CX;
import ca.uhn.hl7v2.model.v231.message.ADT_A01;
import ca.uhn.hl7v2.model.v231.message.ADT_A39;
import ca.uhn.hl7v2.model.v231.segment.EVN;
import ca.uhn.hl7v2.model.v231.segment.MSH;
import ca.uhn.hl7v2.model.v231.segment.PID;
import ca.uhn.hl7v2.parser.PipeParser;
import de.tobchen.health.patientindex.feed.model.entities.MessageEntity;
import de.tobchen.health.patientindex.feed.model.enums.MessageStatus;
import de.tobchen.health.patientindex.feed.model.repositories.MessageRepository;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;

@Component
public class MessageSender
{
    private final Logger logger = LoggerFactory.getLogger(MessageSender.class);

    private final Tracer tracer;

    private final HapiContext context;

    private final MessageRepository repository;

    private final String serverHost;
    private final int serverPort;

    private final String pidOid;
    private final String sendingAppOid;
    private final String sendingFacOid;
    private final String receivingAppOid;
    private final String receivingFacOid;

    // TODO Set timeout from config
    private final long retryTimeout = 15000;
    
    private final ExecutorService executor;

    private final PipeParser parser = new PipeParser();

    @Nullable
    private Connection connection;

    public MessageSender(OpenTelemetry openTelemetry, HapiContext context, MessageRepository repository,
        @Value("${patient-index.feed.receiver.host}") String serverHost,
        @Value("${patient-index.feed.receiver.port}") int serverPort,
        @Value("${patient-index.pid-oid}") String pidOid,
        @Value("${patient-index.feed.sender.application-oid}") String sendingAppOid,
        @Value("${patient-index.feed.sender.facility-oid}") String sendingFacOid,
        @Value("${patient-index.feed.receiver.application-oid}") String receivingAppOid,
        @Value("${patient-index.feed.receiver.facility-oid}") String receivingFacOid)
    {
        this.tracer = openTelemetry.getTracer(MessageSender.class.getName());

        this.context = context;

        this.repository = repository;

        this.serverHost = serverHost;
        this.serverPort = serverPort;

        this.pidOid = pidOid;
        this.sendingAppOid = sendingAppOid;
        this.sendingFacOid = sendingFacOid;
        this.receivingAppOid = receivingAppOid;
        this.receivingFacOid = receivingFacOid;

        executor = Context.taskWrapping(Executors.newSingleThreadExecutor());

        for (var msgId : repository.findByStatusOrderByPatientUpdatedAtAsc(MessageStatus.QUEUED))
        {
            queue(msgId.getId());
        }
    }

    public void queue(Long messageId)
    {
        executor.submit(new MessageTask(messageId));
    }

    private synchronized void handleMessage(Long messageId)
    {
        var span = tracer.spanBuilder("MessageSender.handleMessage").setNoParent().startSpan();

        try (var scope = span.makeCurrent())
        {
            logger.debug("Trying to send message {}", messageId);

            var entity = repository.findById(messageId).orElse(null);

            if (entity != null && entity.getStatus() == MessageStatus.QUEUED)
            {
                var hl7v2Message = createHl7Message(entity);
                if (hl7v2Message != null)
                {
                    try
                    {
                        span.setAttribute("mllp.message", parser.encode(hl7v2Message).replace("\r", "\n"));
                    }
                    catch (HL7Exception e)
                    {
                        logger.error("Cannot encode message");
                    }

                    Message response;
                    long attemptCount = 0;

                    while (true)
                    {
                        ++attemptCount;

                        response = sendMessage(hl7v2Message);
                        if (response != null)
                        {
                            break;
                        }
                        else
                        {
                            logger.info("Waiting for {} milliseconds", retryTimeout);
                            try
                            {
                                Thread.sleep(retryTimeout);
                            }
                            catch (InterruptedException e)
                            {
                                logger.warn("Retry timeout interrupted", e);
                            }
                        }
                    }

                    entity.setStatus(MessageStatus.SENT);
                    repository.save(entity);

                    span.setAttribute("mllp.attempts", attemptCount);
                    try
                    {
                        span.setAttribute("mllp.response", parser.encode(response).replace("\r", "\n"));
                    }
                    catch (HL7Exception e)
                    {
                        logger.error("Cannot encode response");
                    }
                }
                else
                {
                    entity.setStatus(MessageStatus.FAILED);
                    repository.save(entity);

                    span.setStatus(StatusCode.ERROR, "Failed to create HL7v2 message");
                }
            }
            else
            {
                logger.info("Skipping message {}", messageId);
            }
        }
        finally
        {
            span.end();
        }
    }

    private @Nullable Message createHl7Message(MessageEntity msgEntity)
    {
        Message result;

        try
        {
            var linkedPatientId = msgEntity.getLinkedPatientId();
            if (linkedPatientId == null)
            {
                var hl7Msg = new ADT_A01();                
                setSegment(hl7Msg.getMSH(), "A01", msgEntity.getId());
                setSegment(hl7Msg.getEVN(), msgEntity.getRecordedAt(), msgEntity.getPatientUpdatedAt());
                setSegment(hl7Msg.getPID(), msgEntity.getPatientId());
                hl7Msg.getPV1().getPatientClass().setValue("N");

                result = hl7Msg;
            }
            else
            {
                var hl7Msg = new ADT_A39();
                setSegment(hl7Msg.getMSH(), "A40", msgEntity.getId());
                setSegment(hl7Msg.getEVN(), msgEntity.getRecordedAt(), msgEntity.getPatientUpdatedAt());
                var patientGroup = hl7Msg.getPIDPD1MRGPV1();
                setSegment(patientGroup.getPID(), linkedPatientId);
                setPidCx(patientGroup.getMRG().getPriorPatientIdentifierList(0), msgEntity.getPatientId());

                result = hl7Msg;
            }
        }
        catch (HL7Exception e)
        {
            logger.error("Cannot create HL7v2 message", e);
            result = null;
        }

        return result;
    }

    private void setSegment(@Nullable MSH msh, String triggerEvent, Long messageId) throws DataTypeException
    {
        if (msh != null)
        {
            msh.getFieldSeparator().setValue("|");
            msh.getEncodingCharacters().setValue("^~\\&");

            var sendingApp = msh.getSendingApplication();
            sendingApp.getUniversalID().setValue(sendingAppOid);
            sendingApp.getUniversalIDType().setValue("ISO");
            var sendingFac = msh.getSendingFacility();
            sendingFac.getUniversalID().setValue(sendingFacOid);
            sendingFac.getUniversalIDType().setValue("ISO");
            var receivingApp = msh.getReceivingApplication();
            receivingApp.getUniversalID().setValue(receivingAppOid);
            receivingApp.getUniversalIDType().setValue("ISO");
            var receivingFac = msh.getReceivingFacility();
            receivingFac.getUniversalID().setValue(receivingFacOid);
            receivingFac.getUniversalIDType().setValue("ISO");

            msh.getDateTimeOfMessage().getTimeOfAnEvent().setValue(new Date());

            var messageType = msh.getMessageType();
            messageType.getMessageType().setValue("ADT");
            messageType.getTriggerEvent().setValue(triggerEvent);

            msh.getMessageControlID().setValue(messageId.toString());

            // TODO Get processing id from settings
            msh.getProcessingID().getProcessingID().setValue("P");

            msh.getVersionID().getVersionID().setValue(msh.getMessage().getVersion());
        }
    }

    private static void setSegment(@Nullable EVN evn, Instant recordedAt, Instant occuredAt) throws DataTypeException
    {
        if (evn != null)
        {
            evn.getRecordedDateTime().getTimeOfAnEvent().setValue(Date.from(recordedAt));
            evn.getEventOccurred().getTimeOfAnEvent().setValue(Date.from(occuredAt));
        }
    }

    private void setSegment(@Nullable PID pid, String patientId) throws DataTypeException
    {
        if (pid != null)
        {
            setPidCx(pid.getPatientIdentifierList(0), patientId);
            pid.getPatientName(0).getFamilyLastName().getFamilyName().setValue("_");
        }
    }

    private void setPidCx(@Nullable CX pidCx, String patientId) throws DataTypeException
    {
        if (pidCx != null)
        {
            pidCx.getID().setValue(patientId);
            var pidAa = pidCx.getAssigningAuthority();
            pidAa.getUniversalID().setValue(pidOid);
            pidAa.getUniversalIDType().setValue("ISO");
        }
    }

    private @Nullable Message sendMessage(Message message)
    {
        Message response = null;

        var connection = this.connection;

        if (connection == null || !connection.isOpen())
        {
            this.connection = connection = null;

            try
            {
                this.connection = connection = context.newClient(serverHost, serverPort, false);
            }
            catch (HL7Exception e)
            {
                logger.error("Cannot connect", e);
            }
        }

        if (connection != null)
        {
            var initiator = connection.getInitiator();
            try
            {
                response = initiator.sendAndReceive(message);
            }
            catch (HL7Exception | LLPException | IOException e)
            {
                logger.error("Cannot send message or receive response", e);
            }
        }

        return response;
    }

    private class MessageTask implements Runnable
    {
        private final Long messageId;

        public MessageTask(Long messageId)
        {
            this.messageId = messageId;
        }

        @Override
        public void run()
        {
            var messageId = this.messageId;
            if (messageId != null)
            {
                handleMessage(messageId);
            }
        }
    }
}
