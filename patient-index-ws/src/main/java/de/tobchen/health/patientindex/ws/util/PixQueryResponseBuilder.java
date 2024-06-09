package de.tobchen.health.patientindex.ws.util;

import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.UUID;

import javax.xml.namespace.QName;

import de.tobchen.health.patientindex.ws.model.schemas.AcknowledgementDetailType;
import de.tobchen.health.patientindex.ws.model.schemas.ActClassControlAct;
import de.tobchen.health.patientindex.ws.model.schemas.COCTMT090003UV01AssignedEntity;
import de.tobchen.health.patientindex.ws.model.schemas.CS;
import de.tobchen.health.patientindex.ws.model.schemas.II;
import de.tobchen.health.patientindex.ws.model.schemas.MCCIMT000300UV01Acknowledgement;
import de.tobchen.health.patientindex.ws.model.schemas.MCCIMT000300UV01AcknowledgementDetail;
import de.tobchen.health.patientindex.ws.model.schemas.MCCIMT000300UV01TargetMessage;
import de.tobchen.health.patientindex.ws.model.schemas.MFMIMT700711UV01Custodian;
import de.tobchen.health.patientindex.ws.model.schemas.MFMIMT700711UV01QueryAck;
import de.tobchen.health.patientindex.ws.model.schemas.PN;
import de.tobchen.health.patientindex.ws.model.schemas.PRPAIN201309UV02;
import de.tobchen.health.patientindex.ws.model.schemas.PRPAIN201310UV02;
import de.tobchen.health.patientindex.ws.model.schemas.PRPAIN201310UV02MFMIMT700711UV01ControlActProcess;
import de.tobchen.health.patientindex.ws.model.schemas.PRPAIN201310UV02MFMIMT700711UV01RegistrationEvent;
import de.tobchen.health.patientindex.ws.model.schemas.PRPAIN201310UV02MFMIMT700711UV01Subject1;
import de.tobchen.health.patientindex.ws.model.schemas.PRPAIN201310UV02MFMIMT700711UV01Subject2;
import de.tobchen.health.patientindex.ws.model.schemas.PRPAMT201304UV02Patient;
import de.tobchen.health.patientindex.ws.model.schemas.PRPAMT201304UV02Person;
import de.tobchen.health.patientindex.ws.model.schemas.PRPAMT201307UV02QueryByParameter;
import de.tobchen.health.patientindex.ws.model.schemas.ParticipationTargetSubject;
import de.tobchen.health.patientindex.ws.model.schemas.XActMoodIntentEvent;
import jakarta.xml.bind.JAXBElement;

public class PixQueryResponseBuilder
{
    private final MCCIMT000300UV01TargetMessage targetMessage;
    private final CS processingCode;
    private final JAXBElement<PRPAMT201307UV02QueryByParameter> queryByParameter;

    public PixQueryResponseBuilder(PRPAIN201309UV02 request)
    {
        this.targetMessage = new MCCIMT000300UV01TargetMessage();
        this.targetMessage.setId(request.getId());

        this.processingCode = request.getProcessingCode();

        this.queryByParameter = request.getControlActProcess().getQueryByParameter();
    }

    public PRPAIN201310UV02 buildWithResult(Collection<II> foundIds)
    {
        var response = new PRPAIN201310UV02();

        response.setId(Hl7v3Utilities.createIi(UUID.randomUUID().toString(), null));
        response.setCreationTime(Hl7v3Utilities.createTs(ZonedDateTime.now()));
        response.setInteractionId(Hl7v3Utilities.createIi("2.16.840.1.113883.1.6", "PRPA_IN201310UV02"));
        response.setProcessingCode(processingCode);
        response.setProcessingModeCode(Hl7v3Utilities.createCs(null, "T"));
        response.setAcceptAckCode(Hl7v3Utilities.createCs(null, "NE"));

        // TODO Sender

        // TODO Receiver

        response.getAcknowledgement().add(buildAcknowledgement(foundIds != null));

        response.setControlActProcess(buildControlActProcess(foundIds));

        return response;
    }

    private MCCIMT000300UV01Acknowledgement buildAcknowledgement(boolean isOk)
    {
        var acknowledgement = new MCCIMT000300UV01Acknowledgement();

        acknowledgement.setTypeCode(Hl7v3Utilities.createCs(null, isOk ? "AA" : "AE"));
        acknowledgement.setTargetMessage(targetMessage);

        if (!isOk)
        {
            acknowledgement.getAcknowledgementDetail().add(buildAcknowledgementDetail());
        }

        return acknowledgement;
    }

    private MCCIMT000300UV01AcknowledgementDetail buildAcknowledgementDetail()
    {
        var detail = new MCCIMT000300UV01AcknowledgementDetail();

        detail.setTypeCode(AcknowledgementDetailType.E);
        detail.setCode(Hl7v3Utilities.createCs(null, "204 (Unknown Key Identifier)"));
        // TODO Location

        return detail;
    }

    private PRPAIN201310UV02MFMIMT700711UV01ControlActProcess buildControlActProcess(Collection<II> foundIds)
    {
        var controlActProcess = new PRPAIN201310UV02MFMIMT700711UV01ControlActProcess();

        controlActProcess.setClassCode(ActClassControlAct.CACT);
        controlActProcess.setMoodCode(XActMoodIntentEvent.EVN);
        controlActProcess.setCode(Hl7v3Utilities.createCs("2.16.840.1.113883.1.6", "PRPA_TE201310UV02"));

        controlActProcess.getSubject().add(buildSubject(foundIds));

        controlActProcess.setQueryAck(buildQueryAck(foundIds));

        controlActProcess.setQueryByParameter(queryByParameter);

        return controlActProcess;
    }

    private PRPAIN201310UV02MFMIMT700711UV01Subject1 buildSubject(Collection<II> foundIds)
    {
        var subject = new PRPAIN201310UV02MFMIMT700711UV01Subject1();

        subject.getTypeCode().add("SUBJ");
        subject.setContextConductionInd(Boolean.FALSE);

        if (foundIds != null && !foundIds.isEmpty())
        {
            subject.setRegistrationEvent(buildRegistrationEvent(foundIds));
        }

        return subject;
    }

    private PRPAIN201310UV02MFMIMT700711UV01RegistrationEvent buildRegistrationEvent(Collection<II> foundIds)
    {
        var registrationEvent = new PRPAIN201310UV02MFMIMT700711UV01RegistrationEvent();

        registrationEvent.getClassCode().add("REG");
        registrationEvent.getMoodCode().add("EVN");
        registrationEvent.setStatusCode(Hl7v3Utilities.createCs(null, "active"));
        registrationEvent.setSubject1(buildRegEvnSubject(foundIds));
        registrationEvent.setCustodian(buildCustodian());

        return registrationEvent;
    }

    private PRPAIN201310UV02MFMIMT700711UV01Subject2 buildRegEvnSubject(Collection<II> foundIds)
    {
        var subject = new PRPAIN201310UV02MFMIMT700711UV01Subject2();

        subject.setTypeCode(ParticipationTargetSubject.SBJ);
        subject.setPatient(buildPatient(foundIds));

        return subject;
    }

    private PRPAMT201304UV02Patient buildPatient(Collection<II> foundIds)
    {
        var patient = new PRPAMT201304UV02Patient();

        patient.getClassCode().add("PAT");
        patient.getId().addAll(foundIds);
        patient.setStatusCode(Hl7v3Utilities.createCs(null, "active"));
        patient.setPatientPerson(buildPatientPerson());

        return patient;
    }

    private JAXBElement<PRPAMT201304UV02Person> buildPatientPerson()
    {
        var person = new PRPAMT201304UV02Person();

        person.getClassCode().add("PSN");
        person.setDeterminerCode("INSTANCE");
        person.getName().add(new PN());

        return new JAXBElement<PRPAMT201304UV02Person>(new QName("person"),
            PRPAMT201304UV02Person.class, person);
    }

    private MFMIMT700711UV01Custodian buildCustodian()
    {
        var custodian = new MFMIMT700711UV01Custodian();

        custodian.getTypeCode().add("CST");
        custodian.setAssignedEntity(buildAssignedEntity());

        return custodian;
    }

    private COCTMT090003UV01AssignedEntity buildAssignedEntity()
    {
        var entity = new COCTMT090003UV01AssignedEntity();

        entity.setClassCode("ASSIGNED");
        entity.getId().add(Hl7v3Utilities.createIi(null, null));

        return entity;
    }

    private MFMIMT700711UV01QueryAck buildQueryAck(Collection<II> foundIds)
    {
        var queryAck = new MFMIMT700711UV01QueryAck();

        queryAck.setQueryId(queryByParameter.getValue().getQueryId());
        queryAck.setStatusCode(Hl7v3Utilities.createCs(null, "deliveredResponse"));
        queryAck.setQueryResponseCode(Hl7v3Utilities.createCs(null,
            foundIds != null ? (!foundIds.isEmpty() ? "OK" : "NF") : "AE"));

        return queryAck;
    }
}
