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
import ca.uhn.hl7v2.model.AbstractMessage;
import ca.uhn.hl7v2.model.DataTypeException;
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

    private final String pidOid;
    private final String sendingAppOid;
    private final String sendingFacOid;
    private final String receivingAppOid;
    private final String receivingFacOid;
    
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public MessageSender(OpenTelemetry openTelemetry, HapiContext context, MessageRepository repository,
        @Value("${patient-index-feed.hl7v2.pid.oid}") String pidOid,
        @Value("${patient-index-feed.hl7v2.sender.application.oid}") String sendingAppOid,
        @Value("${patient-index-feed.hl7v2.sender.facility.oid}") String sendingFacOid,
        @Value("${patient-index-feed.hl7v2.receiver.application.oid}") String receivingAppOid,
        @Value("${patient-index-feed.hl7v2.receiver.facility.oid}") String receivingFacOid)
    {
        this.tracer = openTelemetry.getTracer(MessageSender.class.getName());

        this.context = context;

        this.repository = repository;

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
            var rawMessage = getRawMessage(messageId);
            if (rawMessage != null && !rawMessage.wasSent())
            {
                var hl7v2Message = createHl7Message(rawMessage);
                if (hl7v2Message != null)
                {
                    // TODO Send message
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

        return entity != null ? new RawMessage(entity.getPatientId(), entity.getPatientUpdatedAt(),
            entity.getLinkedPatientId(), entity.getRecordedAt(), entity.getSentAt() != null) : null;
    }

    private @Nullable AbstractMessage createHl7Message(RawMessage rawMessage)
    {
        // TODO Get processing id from settings
        // TODO Set rawMessage id as hl7v2 message id
        AbstractMessage result;

        try
        {
            var linkedPatientId = rawMessage.linkedPatientId;
            if (linkedPatientId == null)
            {
                var message = new ADT_A01();
                message.initQuickstart("ADT", "A01", "P");
                
                setSegment(message.getMSH());
                setSegment(message.getEVN(), rawMessage.recordedAt(), rawMessage.patientUpdatedAt());
                setSegment(message.getPID(), rawMessage.patientId());
                message.getPV1().getPatientClass().setValue("N");

                result = message;
            }
            else
            {
                var message = new ADT_A39();
                message.initQuickstart("ADT", "A40", "P");

                setSegment(message.getMSH());
                setSegment(message.getEVN(), rawMessage.recordedAt(), rawMessage.patientUpdatedAt());
                var patientGroup = message.getPIDPD1MRGPV1();
                setSegment(patientGroup.getPID(), rawMessage.linkedPatientId());
                setPidCx(patientGroup.getMRG().getPriorPatientIdentifierList(0), rawMessage.patientId());

                result = message;
            }
        }
        catch (HL7Exception | IOException e)
        {
            logger.error("Cannot create HL7v2 message", e);
            result = null;
        }

        return result;
    }

    private void setSegment(@Nullable MSH msh) throws DataTypeException
    {
        if (msh != null)
        {
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

    private record RawMessage(String patientId, Instant patientUpdatedAt,
        @Nullable String linkedPatientId, Instant recordedAt, boolean wasSent) { }
}
