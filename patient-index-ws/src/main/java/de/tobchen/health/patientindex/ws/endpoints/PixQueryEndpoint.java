package de.tobchen.health.patientindex.ws.endpoints;

import java.util.HashSet;
import java.util.Set;

import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;
import org.springframework.ws.soap.server.endpoint.annotation.SoapAction;

import de.tobchen.health.patientindex.ws.model.schemas.PRPAIN201309UV02;
import de.tobchen.health.patientindex.ws.model.schemas.PRPAIN201309UV02QUQIMT021001UV01ControlActProcess;
import de.tobchen.health.patientindex.ws.model.schemas.PRPAIN201310UV02;

@Endpoint
public class PixQueryEndpoint
{
    @SoapAction("urn:hl7-org:v3:PRPA_IN201309UV02")
    public @ResponsePayload PRPAIN201310UV02 query(@RequestPayload PRPAIN201309UV02 request)
    {
        var inputParams = getInputParams(request.getControlActProcess());

        return null;
    }

    private InputParams getInputParams(PRPAIN201309UV02QUQIMT021001UV01ControlActProcess controlActProcess)
    {
        if (controlActProcess == null)
        {
            throw new IllegalArgumentException("Control act process is null");
        }

        var queryByParameter = controlActProcess.getQueryByParameter();
        if (queryByParameter == null)
        {
            throw new RuntimeException("Missing QueryByParameter");
        }

        var queryByParameterValue = queryByParameter.getValue();
        if (queryByParameterValue == null)
        {
            throw new RuntimeException("Missing QueryByParameter value");
        }

        var parameterList = queryByParameterValue.getParameterList();
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

        var patientSystem = patientIdentifierValue.getRoot();
        var patientValue = patientIdentifierValue.getExtension();
        if (patientSystem == null || patientValue == null)
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

        return new InputParams(patientSystem, patientValue, queriedSystems);
    }

    private record InputParams(String system, String value, Set<String> queriedSystems) { }
}
