package de.tobchen.health.patientindex.feed.components;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.hl7.fhir.r5.model.Bundle;
import org.hl7.fhir.r5.model.Patient;
import org.jooq.DSLContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import ca.uhn.fhir.rest.api.Constants;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.exceptions.FhirClientConnectionException;
import ca.uhn.fhir.rest.gclient.DateClientParam;
import ca.uhn.fhir.rest.gclient.IQuery;
import de.tobchen.health.patientindex.feed.jooq.public_.Tables;
import de.tobchen.health.patientindex.feed.jooq.public_.enums.MessageStatus;
import de.tobchen.health.patientindex.feed.jooq.public_.tables.records.IdValueRecord;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.context.propagation.TextMapSetter;

@Component
public class PatientPoller
{
    private static final String LOWER_BOUND_KEY = "lower-bound";

    private final Logger logger = LoggerFactory.getLogger(PatientPoller.class);

    private final Tracer tracer;
    private final TextMapPropagator propagator;
    private final TextMapSetter<IQuery<Bundle>> otelSetter;
    
    private final DSLContext dsl;

    private final IGenericClient client;

    private final MessageSender sender;

    public PatientPoller(OpenTelemetry openTelemetry, DSLContext dsl,
        IGenericClient client, MessageSender sender)
    {
        this.tracer = openTelemetry.getTracer(PatientPoller.class.getName());
        this.propagator = openTelemetry.getPropagators().getTextMapPropagator();
        this.otelSetter = new TextMapSetter<IQuery<Bundle>>()
        {
            @Override
            public void set(@Nullable IQuery<Bundle> carrier,
                @Nullable String key, @Nullable String value)
            {
                if (carrier != null)
                {
                    carrier.withAdditionalHeader(key, value);
                }
            }
        };

        this.dsl = dsl;
        this.client = client;
        this.sender = sender;
    }

    @Scheduled(fixedRate = 15, timeUnit = TimeUnit.SECONDS)
    public synchronized void poll()
    {
        var span = tracer.spanBuilder("PatientPoller.poll").startSpan();

        try (var scope = span.makeCurrent())
        {
            var checkUpperBound = OffsetDateTime.now();

            var msgIds = dsl.transactionResult(trx -> {
                var newMsgIds = new ArrayList<Integer>();

                var checkLowerBoundFetch = trx.dsl().select(Tables.ID_VALUE)
                    .from(Tables.ID_VALUE)
                    .where(Tables.ID_VALUE.ID.equal(LOWER_BOUND_KEY))
                    .fetchAny();
                
                IdValueRecord checkLowerBoundRecord;
                if (checkLowerBoundFetch != null)
                {
                    checkLowerBoundRecord = checkLowerBoundFetch.value1();
                }
                else
                {
                    checkLowerBoundRecord = trx.dsl().newRecord(Tables.ID_VALUE);
                    checkLowerBoundRecord.setId(LOWER_BOUND_KEY);
                    checkLowerBoundRecord.setValueTs(checkUpperBound);
                }

                var checkLowerBound = checkLowerBoundRecord.getValueTs();

                try
                {
                    var query = client.search()
                        .forResource(Patient.class)
                        .where(
                            new DateClientParam(Constants.PARAM_LASTUPDATED)
                                .after()
                                .millis(Date.from(checkLowerBound.toInstant()))
                        ).and(
                            new DateClientParam(Constants.PARAM_LASTUPDATED)
                                .beforeOrEquals()
                                .millis(Date.from(checkUpperBound.toInstant()))
                        ).sort()
                        .ascending(Constants.PARAM_LASTUPDATED)
                        .returnBundle(Bundle.class);
                    
                    propagator.inject(Context.current(), query, otelSetter);

                    logger.debug("Querying from {} to {}", checkLowerBound, checkUpperBound);
                    
                    var bundle = query.execute();
                    
                    var entries = bundle.getEntry();
                    if (entries != null)
                    {
                        for (var entry : entries)
                        {
                            var resource = entry.getResource();
                            if (!(resource instanceof Patient))
                            {
                                logger.info("Ignoring non Patient resource");
                                continue;
                            }

                            var patient = (Patient) resource;
                            var id = patient.getIdPart();
                            if (id == null)
                            {
                                logger.info("Ignoring patient without id");
                                continue;
                            }

                            var meta = patient.getMeta();
                            if (meta == null)
                            {
                                logger.info("Ignoring patient {} without meta", id);
                                continue;
                            }

                            var lastUpdated = meta.getLastUpdated();
                            if (lastUpdated == null)
                            {
                                logger.info("Ignoring patient {} without lastUpdated", id);
                                continue;
                            }

                            String linkedId = null;

                            var linkList = patient.getLink();
                            if (linkList != null && linkList.size() > 0)
                            {
                                for (var link : linkList)
                                {
                                    var reference = link.getOther();
                                    if (reference == null)
                                    {
                                        logger.info("Ignoring null link reference for patient {}", id);
                                        continue;
                                    }

                                    var referenceIdType = reference.getReferenceElement();
                                    if (!"Patient".equals(referenceIdType.getResourceType()))
                                    {
                                        logger.info("Ignoring non patient link for patient {}", id);
                                        continue;
                                    }

                                    linkedId = referenceIdType.getIdPart();
                                    if (linkedId == null)
                                    {
                                        logger.info("Ignoring non patient id link for patient {}", id);
                                    }

                                    break;
                                }

                                if (linkedId == null)
                                {
                                    logger.info("Ignoring patient {} with bad link", id);
                                    continue;
                                }
                            }

                            var messageRecord = trx.dsl().newRecord(Tables.MESSAGE);
                            messageRecord.setPatientId(id);
                            messageRecord.setPatientLastUpdated(OffsetDateTime.ofInstant(lastUpdated.toInstant(),
                                ZoneId.systemDefault()));
                            messageRecord.setPatientMergedIntoId(linkedId);
                            messageRecord.setRecordedAt(OffsetDateTime.now());
                            messageRecord.setStatus(MessageStatus.queued);

                            messageRecord.store();

                            newMsgIds.add(messageRecord.getId());
                        }
                    }

                    checkLowerBoundRecord.setValueTs(checkUpperBound);
                    checkLowerBoundRecord.store();
                }
                catch (FhirClientConnectionException e)
                {
                    logger.warn("Cannot connect to FHIR server");
                }

                return newMsgIds;
            });

            for (var id : msgIds)
            {
                sender.queue(id);
            }
        }
        finally
        {
            span.end();
        }
    }
}
