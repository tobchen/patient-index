package de.tobchen.health.patientindex.ihe.components;

import java.sql.Timestamp;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

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
import ca.uhn.hl7v2.parser.Parser;
import de.tobchen.health.patientindex.configurations.GeneralConfig;
import de.tobchen.health.patientindex.events.PatientEvent;
import de.tobchen.health.patientindex.ihe.configurations.IheConfig;
import de.tobchen.health.patientindex.ihe.configurations.PatientIdentityFeedConfig;
import de.tobchen.health.patientindex.ihe.model.enums.MessageStatus;
import de.tobchen.health.patientindex.ihe.services.PatientIdentityFeedMessageService;

@Component
public class PatientIdentityFeedDispatcher
{
    private final Logger logger = LoggerFactory.getLogger(PatientIdentityFeedDispatcher.class);

    private final GeneralConfig generalConfig;
    private final IheConfig iheConfig;
    private final PatientIdentityFeedConfig feedConfig;

    private final PatientIdentityFeedMessageService service;

    // TODO Graceful shutdown
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private final Parser hl7Parser;

    public PatientIdentityFeedDispatcher(GeneralConfig generalConfig,
        IheConfig iheConfig, PatientIdentityFeedConfig feedConfig,
        PatientIdentityFeedMessageService service,
        HapiContext hapiContext)
    {
        this.generalConfig = generalConfig;
        this.iheConfig = iheConfig;
        this.feedConfig = feedConfig;

        this.service = service;

        this.hl7Parser = hapiContext.getPipeParser();
    }

    @EventListener
    public void onPatientEvent(PatientEvent event) {
        var type = event.getType();
        if (type == PatientEvent.Type.CREATE || type == PatientEvent.Type.MERGE)
        {
            try {
                String message = createMessage(event);

                var id = service.create(message);

                executor.submit(new SendMessageTask(id));
            } catch (IllegalArgumentException | HL7Exception e) {
                logger.error("Cannot queue message!", e);
            }
        }
    }

    synchronized private void sendMessage(Long id)
    {
        var optional = service.read(id);
        if (optional.isPresent())
        {
            var record = optional.get();

            if (record.status() == MessageStatus.QUEUED)
            {
                // TODO Actually send

                service.updateToSent(id, null);
            }
        }
    }

    private String createMessage(PatientEvent event) throws IllegalArgumentException, HL7Exception
    {
        var eventType = event.getType();

        AbstractMessage message;
        if (eventType == PatientEvent.Type.CREATE)
        {
            var admitMessage = new ADT_A01();
            setMsh(admitMessage.getMSH(), "A01");
            setEvn(admitMessage.getEVN(), new Timestamp(event.getTimestamp()));
            setPid(admitMessage.getPID(), event.getId());
            admitMessage.getPV1().getPatientClass().setValue("N");
            message = admitMessage;
        }
        else if (eventType == PatientEvent.Type.MERGE)
        {
            var mergeMessage = new ADT_A39();
            setMsh(mergeMessage.getMSH(), "A40");
            setEvn(mergeMessage.getEVN(), new Timestamp(event.getTimestamp()));
            var pidMrg = mergeMessage.getPIDPD1MRGPV1();
            setPid(pidMrg.getPID(), event.getId());
            setPatientCx(pidMrg.getMRG().getMrg1_PriorPatientIdentifierList(0), event.getOtherId());;
            message = mergeMessage;
        }
        else
        {
            throw new IllegalArgumentException("Unknown patient event type");
        }

        return hl7Parser.encode(message);
    }

    private void setMsh(MSH msh, String type) throws DataTypeException
    {
        msh.getFieldSeparator().setValue("|");
        msh.getEncodingCharacters().setValue("^~\\&");
        msh.getDateTimeOfMessage().getTimeOfAnEvent().setValue(new Date());;
        var msh9 = msh.getMessageType();
        msh9.getMessageType().setValue("ADT");
        msh9.getTriggerEvent().setValue(type);
        if (type == "A01")
        {
            msh9.getMessageStructure().setValue("ADT_A01");
        }
        else if (type == "A40")
        {
            msh9.getMessageStructure().setValue("ADT_A39");
        }
        msh.getMessageControlID().setValue(UUID.randomUUID().toString());
        msh.getProcessingID().getProcessingID().setValue(
            generalConfig.productionMode() != null
                && generalConfig.productionMode().booleanValue() ? "P" : "T");
        msh.getVersionID().getVersionID().setValue("2.3.1");
    }

    private void setEvn(EVN evn, Date date) throws DataTypeException
    {
        evn.getRecordedDateTime().getTimeOfAnEvent().setValue(date);
    }

    private void setPid(PID pid, String identifier) throws DataTypeException
    {
        setPatientCx(pid.getPatientIdentifierList(0), identifier);
        // TODO Set patient name to " " (one space character)
    }

    private void setPatientCx(CX cx, String identifier) throws DataTypeException
    {
        cx.getID().setValue(identifier);
        if (iheConfig.pidOid() != null)
        {
            cx.getAssigningAuthority().getUniversalID().setValue(iheConfig.pidOid());
            cx.getAssigningAuthority().getUniversalIDType().setValue("ISO");
        }
        cx.getIdentifierTypeCode().setValue("PI");
    }

    private class SendMessageTask implements Runnable
    {
        private final Long id;

        public SendMessageTask(Long id)
        {
            this.id = id;
        }

        @Override
        public void run()
        {
            sendMessage(id);
        }
    }
}
