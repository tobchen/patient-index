package de.tobchen.health.patientindex.feed.transformers;

import java.util.Date;

import org.hl7.fhir.r5.model.Patient;
import org.hl7.fhir.r5.model.Patient.LinkType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.transformer.AbstractTransformer;
import org.springframework.lang.Nullable;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.DataTypeException;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.model.v231.datatype.CX;
import ca.uhn.hl7v2.model.v231.message.ADT_A01;
import ca.uhn.hl7v2.model.v231.message.ADT_A39;
import ca.uhn.hl7v2.model.v231.segment.EVN;
import ca.uhn.hl7v2.model.v231.segment.MSH;
import ca.uhn.hl7v2.model.v231.segment.PID;
import de.tobchen.health.patientindex.commons.configurations.PatientIndexConfig;

public class PatientToHl7v2AdtTransformer extends AbstractTransformer
{
    private final Logger logger = LoggerFactory.getLogger(PatientToHl7v2AdtTransformer.class);

    private final PatientIndexConfig config;

    public PatientToHl7v2AdtTransformer(PatientIndexConfig config)
    {
        this.config = config;
    }

    @Override
    protected Object doTransform(org.springframework.messaging.Message<?> message) {
        var headers = message.getHeaders();

        logger.debug("Transforming message with headers {}", headers);

        var msgId = headers.get("amqp_messageId", String.class);
        if (msgId == null)
        {
            msgId = headers.getId().toString();
        }

        var msgDt = headers.get("amqp_timestamp", Date.class);
        if (msgDt == null)
        {
            msgDt = new Date(headers.getTimestamp());
        }

        var patient = (Patient) message.getPayload();

        var eventDt = patient.getMeta().getLastUpdated();
        if (eventDt == null)
        {
            eventDt = msgDt;
        }

        var pid = patient.getIdPart();

        String mrgId = null;
        for (var link : patient.getLink())
        {
            if (LinkType.REPLACEDBY.equals(link.getType()))
            {
                var idType = link.getOther().getReferenceElement();
                if ("Patient".equals(idType.getResourceType()))
                {
                    var id = idType.getIdPart();
                    if (id != null)
                    {
                        mrgId = pid;
                        pid = id;
                        break;
                    }
                }
            }
        }

        return createHl7(msgId, msgDt, eventDt, pid, mrgId);
    }

    private @Nullable Message createHl7(String msgId, Date msgDt, Date eventDt, String pid, String mrgId)
    {
        Message result;

        try
        {
            if (mrgId == null)
            {
                var hl7Msg = new ADT_A01();                
                setSegment(hl7Msg.getMSH(), "A01", msgId);
                setSegment(hl7Msg.getEVN(), msgDt, eventDt);
                setSegment(hl7Msg.getPID(), pid);
                hl7Msg.getPV1().getPatientClass().setValue("N");

                result = hl7Msg;
            }
            else
            {
                var hl7Msg = new ADT_A39();
                setSegment(hl7Msg.getMSH(), "A40", msgId);
                setSegment(hl7Msg.getEVN(), msgDt, eventDt);
                var patientGroup = hl7Msg.getPIDPD1MRGPV1();
                setSegment(patientGroup.getPID(), pid);
                setPidCx(patientGroup.getMRG().getPriorPatientIdentifierList(0), mrgId);

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

    private void setSegment(@Nullable MSH msh, String triggerEvent, String messageId)
        throws DataTypeException
    {
        if (msh != null)
        {
            msh.getFieldSeparator().setValue("|");
            msh.getEncodingCharacters().setValue("^~\\&");

            var sendingApp = msh.getSendingApplication();
            sendingApp.getNamespaceID().setValue(config.feed().sender().application().namespace());
            sendingApp.getUniversalID().setValue(config.feed().sender().application().oid());
            sendingApp.getUniversalIDType().setValue("ISO");
            var sendingFac = msh.getSendingFacility();
            sendingFac.getNamespaceID().setValue(config.feed().sender().facility().namespace());
            sendingFac.getUniversalID().setValue(config.feed().sender().facility().oid());
            sendingFac.getUniversalIDType().setValue("ISO");
            var receivingApp = msh.getReceivingApplication();
            receivingApp.getNamespaceID().setValue(config.feed().receiver().application().namespace());
            receivingApp.getUniversalID().setValue(config.feed().receiver().application().oid());
            receivingApp.getUniversalIDType().setValue("ISO");
            var receivingFac = msh.getReceivingFacility();
            receivingFac.getNamespaceID().setValue(config.feed().receiver().facility().namespace());
            receivingFac.getUniversalID().setValue(config.feed().receiver().facility().oid());
            receivingFac.getUniversalIDType().setValue("ISO");

            msh.getDateTimeOfMessage().getTimeOfAnEvent().setValue(new Date());

            var messageType = msh.getMessageType();
            messageType.getMessageType().setValue("ADT");
            messageType.getTriggerEvent().setValue(triggerEvent);

            msh.getMessageControlID().setValue(messageId.toString());

            msh.getProcessingID().getProcessingID().setValue(config.feed().processingMode());

            msh.getVersionID().getVersionID().setValue(msh.getMessage().getVersion());

            msh.getCharacterSet(0).setValue("UNICODE UTF-8");
        }
    }

    private static void setSegment(@Nullable EVN evn, Date recordedAt, Date occuredAt)
        throws DataTypeException
    {
        if (evn != null)
        {
            evn.getRecordedDateTime().getTimeOfAnEvent().setValue(recordedAt);
            evn.getEventOccurred().getTimeOfAnEvent().setValue(occuredAt);
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
            pidAa.getNamespaceID().setValue(config.pid().namespace());
            pidAa.getUniversalID().setValue(config.pid().oid());
            pidAa.getUniversalIDType().setValue("ISO");
        }
    }
}
