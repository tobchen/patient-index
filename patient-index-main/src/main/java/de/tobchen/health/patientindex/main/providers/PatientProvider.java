package de.tobchen.health.patientindex.main.providers;

import java.util.ArrayList;
import java.util.List;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r5.model.OperationOutcome;
import org.hl7.fhir.r5.model.Parameters;
import org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent;
import org.hl7.fhir.r5.model.Patient;
import org.hl7.fhir.r5.model.Reference;
import org.hl7.fhir.r5.model.OperationOutcome.IssueSeverity;
import org.hl7.fhir.r5.model.OperationOutcome.IssueType;
import org.hl7.fhir.r5.model.OperationOutcome.OperationOutcomeIssueComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import ca.uhn.fhir.rest.annotation.Create;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.RequiredParam;
import ca.uhn.fhir.rest.annotation.ResourceParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.annotation.Update;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import de.tobchen.health.patientindex.main.services.PatientService;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Tracer;

@Service
public class PatientProvider implements IResourceProvider
{
    private final Logger logger = LoggerFactory.getLogger(PatientProvider.class);

    private final Tracer tracer;

    private final PatientService service;

    public PatientProvider(OpenTelemetry openTelemetry, PatientService service)
    {
        this.tracer = openTelemetry.getTracer(PatientProvider.class.getName());
        this.service = service;
    }

    @Override
    public Class<? extends IBaseResource> getResourceType()
    {
        return Patient.class;
    }

    @Create
    public MethodOutcome create(@ResourceParam Patient patient) throws JsonProcessingException
    {
        var span = tracer.spanBuilder("PatientProvider.create").startSpan();

        try (var scope = span.makeCurrent())
        {
            var outcome = service.createOrUpdate(patient);

            span.setAttribute("audit.action", "create");
            span.setAttribute("audit.patient", outcome.getId().getIdPart());

            logger.debug("Patient created!");

            return outcome;
        }
        catch (Throwable t)
        {
            span.recordException(t);
            throw t;
        }
        finally
        {
            span.end();
        }
    }

    @Update
    public MethodOutcome update(@Nullable @IdParam IIdType idType, @ResourceParam Patient patient)
        throws JsonProcessingException
    {
        var span = tracer.spanBuilder("PatientProvider.update").startSpan();

        try (var scope = span.makeCurrent())
        {
            var idPart = idType.getIdPart();
            if (idPart == null)
            {
                throw new InvalidRequestException("Id is missing id part");
            }

            var outcome = service.createOrUpdate(patient);

            span.setAttribute("audit.action", outcome.getCreated().booleanValue() ? "create" : "update");
            span.setAttribute("audit.patient", outcome.getId().getIdPart());

            logger.debug("Patient created or updated!");

            return outcome;
        }
        catch (Throwable t)
        {
            span.recordException(t);
            throw t;
        }
        finally
        {
            span.end();
        }
    }

    @Read
    public @Nullable Patient read(@IdParam IIdType id) throws JsonProcessingException
    {
        var span = tracer.spanBuilder("PatientProvider.read").startSpan();

        try (var scope = span.makeCurrent())
        {
            Patient patient = service.get(id);

            if (patient != null)
            {
                span.setAttribute("audit.action", "read");
                span.setAttribute("audit.patient", patient.getIdPart());
            }

            return patient;
        }
        catch (Throwable t)
        {
            span.recordException(t);
            throw t;
        }
        finally
        {
            span.end();
        }
    }

    @Search
    public List<Patient> searchByIdentifier(
        @RequiredParam(name = Patient.SP_IDENTIFIER) TokenParam resourceIdentifier)
        throws JsonProcessingException
    {
        var span = tracer.spanBuilder("PatientProvider.searchByIdentifier").startSpan();

        try (var scope = span.makeCurrent())
        {
            var patients = service.findByIdentifier(resourceIdentifier.getSystem(), resourceIdentifier.getValue());
            
            var ids = new ArrayList<String>();
            for (var patient : patients)
            {
                ids.add(patient.getIdPart());
            }

            span.setAttribute("audit.action", "search");
            span.setAttribute(AttributeKey.stringArrayKey("audit.patient"), ids);

            return patients;
        }
        catch (Throwable t)
        {
            span.recordException(t);
            throw t;
        }
        finally
        {
            span.end();
        }
    }

    @Operation(name = "$merge", idempotent = false)
    public Parameters merge(@OperationParam(name = "source-patient", min = 1, max = 1) Reference sourceReference,
        @OperationParam(name = "target-patient", min = 1, max = 1) Reference targetReference)
        throws JsonProcessingException
    {
        var span = tracer.spanBuilder("PatientProvider.merge").startSpan();

        try (var scope = span.makeCurrent())
        {
            var sourceId = sourceReference.getReferenceElement();
            var targetId = targetReference.getReferenceElement();

            if (sourceId == null || targetId == null)
            {
                throw new InvalidRequestException("Needs source and target ids");
            }
            else if (sourceId.hasBaseUrl() || targetId.hasBaseUrl())
            {
                throw new InvalidRequestException("Cannot handle absolute references");
            }
            else if (!"Patient".equals(sourceId.getResourceType())
                || !"Patient".equals(targetId.getResourceType()))
            {
                throw new InvalidRequestException("Source and target must be Patient");
            }

            var mergeResult = service.merge(sourceId, targetId);

            var parameters = new Parameters()
                .addParameter(new ParametersParameterComponent("input")
                    .setResource(new Parameters()
                        .addParameter("source-patient", sourceReference)
                        .addParameter("target-patient", targetReference)
                    )
                );

            parameters.addParameter().setName("outcome").setResource(
                new OperationOutcome(new OperationOutcomeIssueComponent(IssueSeverity.SUCCESS, IssueType.SUCCESS)));
            parameters.addParameter().setName("result").setResource(mergeResult.target());

            span.setAttribute("audit.action", "merge");
            span.setAttribute("audit.patient.soure", sourceId.getIdPart());
            span.setAttribute("audit.patient.target", targetId.getIdPart());

            return parameters;
        }
        catch (Throwable t)
        {
            span.recordException(t);
            throw t;
        }
        finally
        {
            span.end();
        }
    }
}
