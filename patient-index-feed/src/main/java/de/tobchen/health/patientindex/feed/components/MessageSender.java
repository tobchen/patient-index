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
import org.springframework.transaction.annotation.Transactional;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.HapiContext;
import ca.uhn.hl7v2.app.Connection;
import ca.uhn.hl7v2.llp.LLPException;
import ca.uhn.hl7v2.model.AbstractMessage;
import ca.uhn.hl7v2.model.DataTypeException;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.model.v231.datatype.CX;
import ca.uhn.hl7v2.model.v231.message.ADT_A01;
import ca.uhn.hl7v2.model.v231.message.ADT_A39;
import ca.uhn.hl7v2.model.v231.segment.EVN;
import ca.uhn.hl7v2.model.v231.segment.MSH;
import ca.uhn.hl7v2.model.v231.segment.PID;
import de.tobchen.health.patientindex.feed.model.repositories.MessageRepository;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;

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
    
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Nullable
    private Connection connection;

    public MessageSender(OpenTelemetry openTelemetry, HapiContext context, MessageRepository repository,
        @Value("${patient-index-feed.hl7v2.server.host}") String serverHost,
        @Value("${patient-index-feed.hl7v2.server.port}") int serverPort,
        @Value("${patient-index-feed.hl7v2.pid.oid}") String pidOid,
        @Value("${patient-index-feed.hl7v2.sender.application.oid}") String sendingAppOid,
        @Value("${patient-index-feed.hl7v2.sender.facility.oid}") String sendingFacOid,
        @Value("${patient-index-feed.hl7v2.receiver.application.oid}") String receivingAppOid,
        @Value("${patient-index-feed.hl7v2.receiver.facility.oid}") String receivingFacOid)
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

        // TODO Queue all pending messages
    }

    public void queue(Long messageId)
    {
        executor.submit(new MessageTask(messageId));
    }

    private synchronized void send(Long messageId)
    {
        var span = tracer.spanBuilder("MessageSender.send").startSpan();

        try (var scope = span.makeCurrent())
        {
            logger.debug("Trying to send message {}", messageId);

            var rawMessage = getRawMessage(messageId);
            if (rawMessage != null && !rawMessage.wasSent())
            {
                var hl7v2Message = createHl7Message(rawMessage);
                if (hl7v2Message != null)
                {
                    while (true)
                    {
                        var response = sendMessage(hl7v2Message);
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

                    // TODO Update message status
                }
                else
                {
                    // TODO Set message status to error
                }
            }
            else
            {
                logger.info("Not sending message {}", messageId);
            }
        }
        finally
        {
            span.end();
        }
    }

    @Transactional(readOnly = true)
    private @Nullable RawMessage getRawMessage(Long id)
    {
        var entity = repository.findById(id).orElse(null);

        return entity != null ? new RawMessage(entity.getId(), entity.getPatientId(), entity.getPatientUpdatedAt(),
            entity.getLinkedPatientId(), entity.getRecordedAt(), entity.getSentAt() != null) : null;
    }

    private @Nullable AbstractMessage createHl7Message(RawMessage rawMessage)
    {
        // TODO Set rawMessage id as hl7v2 message id
        AbstractMessage result;

        try
        {
            var linkedPatientId = rawMessage.linkedPatientId;
            if (linkedPatientId == null)
            {
                var message = new ADT_A01();                
                setSegment(message.getMSH(), "A01", rawMessage.id());
                setSegment(message.getEVN(), rawMessage.recordedAt(), rawMessage.patientUpdatedAt());
                setSegment(message.getPID(), rawMessage.patientId());
                message.getPV1().getPatientClass().setValue("N");

                result = message;
            }
            else
            {
                var message = new ADT_A39();
                setSegment(message.getMSH(), "A40", rawMessage.id());
                setSegment(message.getEVN(), rawMessage.recordedAt(), rawMessage.patientUpdatedAt());
                var patientGroup = message.getPIDPD1MRGPV1();
                setSegment(patientGroup.getPID(), rawMessage.linkedPatientId());
                setPidCx(patientGroup.getMRG().getPriorPatientIdentifierList(0), rawMessage.patientId());

                result = message;
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
            pid.getPatientName(0).getFamilyLastName().getFamilyName().setValue(" ");
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
        private Long messageId;

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
                send(messageId);
            }
        }
    }

    private record RawMessage(Long id, String patientId, Instant patientUpdatedAt,
        @Nullable String linkedPatientId, Instant recordedAt, boolean wasSent) { }
}
