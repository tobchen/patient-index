package de.tobchen.health.patientindex.ws.endpoints;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;
import org.springframework.ws.soap.server.endpoint.annotation.SoapAction;

import de.tobchen.health.patientindex.ws.model.schemas.II;
import de.tobchen.health.patientindex.ws.model.schemas.PRPAIN201309UV02;
import de.tobchen.health.patientindex.ws.model.schemas.PRPAIN201310UV02;
import de.tobchen.health.patientindex.ws.services.QueryService;
import de.tobchen.health.patientindex.ws.util.Hl7v3Utilities;
import de.tobchen.health.patientindex.ws.util.PixQueryResponseBuilder;

@Endpoint
public class PixQueryEndpoint
{
    private final QueryService queryService;

    public PixQueryEndpoint(QueryService queryService)
    {
        this.queryService = queryService;
    }

    @SoapAction("urn:hl7-org:v3:PRPA_IN201309UV02")
    public @ResponsePayload PRPAIN201310UV02 query(@RequestPayload PRPAIN201309UV02 request)
    {
        var parameterList = request.getControlActProcess().getQueryByParameter().getValue().getParameterList();

        var patientIdentifier = parameterList.getPatientIdentifier().get(0).getValue().get(0);
        var idSystem = patientIdentifier.getRoot();
        var idValue = patientIdentifier.getExtension();

        Collection<II> foundIds;

        if (idSystem == null || idValue == null)
        {
            foundIds = null;
        }
        else
        {
            var systemValuesMap = queryService.findIdentifiers(idSystem, idValue);

            if (systemValuesMap.isEmpty())
            {
                foundIds = null;
            }
            else
            {
                systemValuesMap.get(idSystem).remove(idValue);

                var whiteList = new HashSet<String>();
                for (var dataSource : parameterList.getDataSource())
                {
                    whiteList.add(dataSource.getValue().get(0).getRoot());
                }

                if (!whiteList.isEmpty())
                {
                    systemValuesMap.keySet().retainAll(whiteList);
                }

                foundIds = new ArrayList<>();
                for (var systemValues : systemValuesMap.entrySet())
                {
                    for (var value : systemValues.getValue())
                    {
                        foundIds.add(Hl7v3Utilities.createIi(systemValues.getKey(), value));
                    }
                }
            }
        }

        return new PixQueryResponseBuilder(request).buildWithResult(foundIds);
    }
}
