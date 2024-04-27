package de.tobchen.health.patientindex.feed.services;

import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.DataTypeException;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.model.v231.datatype.CX;
import ca.uhn.hl7v2.model.v231.message.ADT_A01;
import ca.uhn.hl7v2.model.v231.message.ADT_A39;
import ca.uhn.hl7v2.model.v231.segment.EVN;
import ca.uhn.hl7v2.model.v231.segment.MSH;
import ca.uhn.hl7v2.model.v231.segment.PID;

@Service
public class Hl7MessageCreator
{
    private final Logger logger = LoggerFactory.getLogger(Hl7MessageCreator.class);

    private final String pidOid;
    private final String sendingAppOid;
    private final String sendingFacOid;
    private final String receivingAppOid;
    private final String receivingFacOid;

    public Hl7MessageCreator(
        @Value("${patient-index.pid-oid}") String pidOid,
        @Value("${patient-index.feed.sender.application-oid}") String sendingAppOid,
        @Value("${patient-index.feed.sender.facility-oid}") String sendingFacOid,
        @Value("${patient-index.feed.receiver.application-oid}") String receivingAppOid,
        @Value("${patient-index.feed.receiver.facility-oid}") String receivingFacOid)
    {
        this.pidOid = pidOid;
        this.sendingAppOid = sendingAppOid;
        this.sendingFacOid = sendingFacOid;
        this.receivingAppOid = receivingAppOid;
        this.receivingFacOid = receivingFacOid;
    }

    private @Nullable Message create(String msgId, Date msgDt, Date eventDt, String pid, String mrgId)
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
            pidAa.getUniversalID().setValue(pidOid);
            pidAa.getUniversalIDType().setValue("ISO");
        }
    }
}
