package de.tobchen.health.patientindex.ws.endpoints;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;
import org.springframework.ws.soap.server.endpoint.annotation.SoapAction;

import de.tobchen.health.patientindex.ws.model.schemas.ActClassControlAct;
import de.tobchen.health.patientindex.ws.model.schemas.CD;
import de.tobchen.health.patientindex.ws.model.schemas.COCTMT090003UV01AssignedEntity;
import de.tobchen.health.patientindex.ws.model.schemas.CS;
import de.tobchen.health.patientindex.ws.model.schemas.II;
import de.tobchen.health.patientindex.ws.model.schemas.MCCIMT000300UV01Acknowledgement;
import de.tobchen.health.patientindex.ws.model.schemas.MCCIMT000300UV01Sender;
import de.tobchen.health.patientindex.ws.model.schemas.MCCIMT000300UV01TargetMessage;
import de.tobchen.health.patientindex.ws.model.schemas.MFMIMT700711UV01Custodian;
import de.tobchen.health.patientindex.ws.model.schemas.MFMIMT700711UV01QueryAck;
import de.tobchen.health.patientindex.ws.model.schemas.PRPAIN201309UV02;
import de.tobchen.health.patientindex.ws.model.schemas.PRPAIN201309UV02QUQIMT021001UV01ControlActProcess;
import de.tobchen.health.patientindex.ws.model.schemas.PRPAIN201310UV02;
import de.tobchen.health.patientindex.ws.model.schemas.PRPAIN201310UV02MFMIMT700711UV01ControlActProcess;
import de.tobchen.health.patientindex.ws.model.schemas.PRPAIN201310UV02MFMIMT700711UV01RegistrationEvent;
import de.tobchen.health.patientindex.ws.model.schemas.PRPAIN201310UV02MFMIMT700711UV01Subject1;
import de.tobchen.health.patientindex.ws.model.schemas.PRPAIN201310UV02MFMIMT700711UV01Subject2;
import de.tobchen.health.patientindex.ws.model.schemas.PRPAMT201304UV02Patient;
import de.tobchen.health.patientindex.ws.model.schemas.PRPAMT201307UV02QueryByParameter;
import de.tobchen.health.patientindex.ws.model.schemas.ParticipationTargetSubject;
import de.tobchen.health.patientindex.ws.model.schemas.TS;
import de.tobchen.health.patientindex.ws.model.schemas.XActMoodIntentEvent;
import de.tobchen.health.patientindex.ws.services.QueryService;
import de.tobchen.health.patientindex.ws.util.Hl7v3Utilities;

@Endpoint
public class PixQueryEndpoint
{
    private final Logger logger = LoggerFactory.getLogger(PixQueryEndpoint.class);
    
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmssZZ");

    private final QueryService queryService;

    public PixQueryEndpoint(QueryService queryService)
    {
        this.queryService = queryService;
    }

    @SoapAction("urn:hl7-org:v3:PRPA_IN201309UV02")
    public @ResponsePayload PRPAIN201310UV02 query(@RequestPayload PRPAIN201309UV02 request)
    {
        var id = new II();
        id.setRoot(UUID.randomUUID().toString());

        var creationTime = new TS();
        creationTime.setValue(formatter.format(ZonedDateTime.now()));

        var interactionId = new II();
        interactionId.setRoot("2.16.840.1.113883.1.6");
        interactionId.setExtension("PRPA_IN201310UV02");

        var processingModeCode = new CS();
        processingModeCode.setCode("T");

        var acceptAckCode = new CS();
        acceptAckCode.setCode("NE");

        MCCIMT000300UV01Sender sender = null;
        var requestReceiverList = request.getReceiver();
        if (requestReceiverList.size() > 0)
        {
            sender = Hl7v3Utilities.convertReceiverToSender300(requestReceiverList.get(0));

            if (requestReceiverList.size() > 1)
            {
                logger.info("Request had more than one receiver; picked first one for sender");
            }
        }

        var acknowledgement = createAcknowledgement(request.getId());

        var controlActProcess = createControlProcessAct(request.getControlActProcess());

        var response = new PRPAIN201310UV02();

        response.setId(id);
        response.setCreationTime(creationTime);
        response.setInteractionId(interactionId);
        response.setProcessingCode(request.getProcessingCode());
        response.setProcessingModeCode(processingModeCode);
        response.setAcceptAckCode(acceptAckCode);
        response.getReceiver().add(Hl7v3Utilities.convertSenderToReceiver300(request.getSender()));
        response.setSender(sender);
        response.getAcknowledgement().add(acknowledgement);
        response.setControlActProcess(controlActProcess);

        return response;
    }

    private PRPAIN201310UV02MFMIMT700711UV01ControlActProcess createControlProcessAct(
        PRPAIN201309UV02QUQIMT021001UV01ControlActProcess requestControlActProcess)
    {
        if (requestControlActProcess == null)
        {
            throw new IllegalArgumentException("Missing control act process");
        }

        var queryByParameter = requestControlActProcess.getQueryByParameter();
        if (queryByParameter == null)
        {
            throw new RuntimeException("Missing query by parameter");
        }

        var queryByParameterValue = queryByParameter.getValue();
        if (queryByParameterValue == null)
        {
            throw new RuntimeException("Invalid query by parameter value");
        }

        var registrationEvent = createRegistrationEvent(queryByParameterValue);

        var triggerCode = new CD();
        triggerCode.setCode("PRPA_TE201310UV02");

        var queryResponseCode = new CS();
        queryResponseCode.setCode(registrationEvent != null ? "OK" : "NF");

        var queryAck = new MFMIMT700711UV01QueryAck();
        queryAck.setQueryId(queryByParameterValue.getQueryId());
        queryAck.setQueryResponseCode(queryResponseCode);

        var subject = new PRPAIN201310UV02MFMIMT700711UV01Subject1();
        subject.getTypeCode().add("SUBJ");
        subject.setRegistrationEvent(registrationEvent);

        var controlProcessAct = new PRPAIN201310UV02MFMIMT700711UV01ControlActProcess();
        controlProcessAct.setClassCode(ActClassControlAct.CACT);
        controlProcessAct.setMoodCode(XActMoodIntentEvent.EVN);
        controlProcessAct.setCode(triggerCode);
        controlProcessAct.getSubject().add(subject);
        controlProcessAct.setQueryAck(queryAck);
        controlProcessAct.setQueryByParameter(queryByParameter);

        return controlProcessAct;
    }

    private PRPAIN201310UV02MFMIMT700711UV01RegistrationEvent createRegistrationEvent(
        PRPAMT201307UV02QueryByParameter queryByParameter)
    {
        PRPAIN201310UV02MFMIMT700711UV01RegistrationEvent registrationEvent;

        var queryParams = getQueryParams(queryByParameter);
        var queriedSystems = queryParams.queriedSystems();

        var patient = new PRPAMT201304UV02Patient();
        var patientIdList = patient.getId();

        var otherIdentifiers = queryService.findOtherIdentifiers(queryParams.queriedIdentifier());
        for (var identifier : otherIdentifiers)
        {
            if (queriedSystems.isEmpty() || queriedSystems.contains(identifier.getRoot()))
            {
                patientIdList.add(identifier);
            }
        }

        if (!patientIdList.isEmpty())
        {
            var patientStatusCode = new CS();
            patientStatusCode.setCode("active");

            patient.getClassCode().add("PAT");
            patient.setStatusCode(patientStatusCode);

            var statusCode = new CS();
            statusCode.setCode("active");

            var subject = new PRPAIN201310UV02MFMIMT700711UV01Subject2();
            subject.setTypeCode(ParticipationTargetSubject.SBJ);
            subject.setPatient(patient);

            var assignedEntity = new COCTMT090003UV01AssignedEntity();
            assignedEntity.setClassCode("ASSIGNED");
            assignedEntity.getId().add(Hl7v3Utilities.createNullIi());

            var custodian = new MFMIMT700711UV01Custodian();
            custodian.getTypeCode().add("CST");
            custodian.setAssignedEntity(assignedEntity);

            registrationEvent = new PRPAIN201310UV02MFMIMT700711UV01RegistrationEvent();
            registrationEvent.getClassCode().add("REG");
            registrationEvent.getMoodCode().add("EVN");
            registrationEvent.setStatusCode(statusCode);
            registrationEvent.setSubject1(subject);
            registrationEvent.setCustodian(custodian);
        }
        else
        {
            registrationEvent = null;
        }

        return registrationEvent;
    }

    private QueryParams getQueryParams(PRPAMT201307UV02QueryByParameter queryByParameter)
    {
        var parameterList = queryByParameter.getParameterList();
        if (parameterList == null)
        {
            throw new RuntimeException("Missing parameter list");
        }

        var patientIdentifiers = parameterList.getPatientIdentifier();
        if (patientIdentifiers == null || patientIdentifiers.size() != 1)
        {
            throw new RuntimeException("Invalid patient identifier count");
        }

        var patientIdentifer = patientIdentifiers.get(0);
        if (patientIdentifer == null)
        {
            throw new RuntimeException("Missing patient identifier");
        }

        var patientIdentifierValues = patientIdentifer.getValue();
        if (patientIdentifierValues == null || patientIdentifierValues.size() != 1)
        {
            throw new RuntimeException("Invalid patient identifier value count");
        }

        var patientIdentifierValue = patientIdentifierValues.get(0);
        if (patientIdentifierValue == null)
        {
            throw new RuntimeException("Missing patient identifier value");
        }

        if (patientIdentifierValue.getExtension() == null || patientIdentifierValue.getRoot() == null)
        {
            throw new RuntimeException("Invalid patient identifier value");
        }

        var queriedSystems = new HashSet<String>();
        var dataSources = parameterList.getDataSource();
        if (dataSources != null)
        {
            for (var dataSource : dataSources)
            {
                var values = dataSource.getValue();
                if (values != null)
                {
                    for (var value : values)
                    {
                        var root = value.getRoot();
                        if (root != null)
                        {
                            queriedSystems.add(root);
                        }
                    }
                }
            }
        }

        return new QueryParams(patientIdentifierValue, queriedSystems);
    }

    private MCCIMT000300UV01Acknowledgement createAcknowledgement(II requestId)
    {
        /*
         * Super weird:
         * The IHE PIXV3 Query specification mentions AA, AE:
         * https://profiles.ihe.net/ITI/TF/Volume2/ITI-45.html#3.45.4.2.3
         * But the IHE example query responses use CA
         * 
         * I'm sticking with AA because specification > example
         */

        var typeCode = new CS();
        typeCode.setCode("AA");

        var targetMessage = new MCCIMT000300UV01TargetMessage();
        targetMessage.setId(requestId);

        var acknowledgement = new MCCIMT000300UV01Acknowledgement();
        acknowledgement.setTypeCode(typeCode);
        acknowledgement.setTargetMessage(targetMessage);

        return acknowledgement;
    }

    private record QueryParams(II queriedIdentifier, Set<String> queriedSystems) { }
}
