package de.tobchen.health.patientindex.feed.components;

import org.jooq.DSLContext;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import de.tobchen.health.patientindex.feed.jooq.public_.Tables;
import de.tobchen.health.patientindex.feed.jooq.public_.enums.MessageStatus;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.metrics.Meter;

@Component
public class MetricsStarter implements ApplicationListener<ApplicationReadyEvent>
{
    private final Meter meter;
    
    private final DSLContext dsl;

    public MetricsStarter(OpenTelemetry openTelemetry, DSLContext dsl)
    {
        this.meter = openTelemetry.getMeter("patient-index-feed");

        this.dsl = dsl;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event)
    {
        meter.upDownCounterBuilder("queued")
            .setDescription("Queued messages")
            .buildWithCallback(measurement -> {
                var messageCount = dsl.selectCount()
                    .from(Tables.MESSAGE)
                    .where(Tables.MESSAGE.STATUS.equal(MessageStatus.queued))
                    .fetchAny();
                measurement.record(messageCount.value1());
            });
        
        meter.upDownCounterBuilder("sent")
            .setDescription("Sent messages")
            .buildWithCallback(measurement -> {
                var messageCount = dsl.selectCount()
                    .from(Tables.MESSAGE)
                    .where(Tables.MESSAGE.STATUS.equal(MessageStatus.sent))
                    .fetchAny();
                measurement.record(messageCount.value1());
            });
        
        meter.upDownCounterBuilder("failed")
            .setDescription("Failed messages")
            .buildWithCallback(measurement -> {
                var messageCount = dsl.selectCount()
                    .from(Tables.MESSAGE)
                    .where(Tables.MESSAGE.STATUS.equal(MessageStatus.failed))
                    .fetchAny();
                measurement.record(messageCount.value1());
            });
    }
}
