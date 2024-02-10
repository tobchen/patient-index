package de.tobchen.health.patientindex.feed.components;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import de.tobchen.health.patientindex.feed.model.enums.MessageStatus;
import de.tobchen.health.patientindex.feed.model.repositories.MessageRepository;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.metrics.Meter;

@Component
public class MetricsStarter implements ApplicationListener<ApplicationReadyEvent>
{
    private final Meter meter;
    private final MessageRepository repository;

    public MetricsStarter(OpenTelemetry openTelemetry, MessageRepository repository)
    {
        this.meter = openTelemetry.getMeter("patient-index-feed");
        this.repository = repository;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event)
    {
        meter.upDownCounterBuilder("queued")
            .setDescription("Queued messages")
            .buildWithCallback(measurement -> {
                measurement.record(repository.countByStatus(MessageStatus.QUEUED));
            });
        
        meter.upDownCounterBuilder("sent")
            .setDescription("Sent messages")
            .buildWithCallback(measurement -> {
                measurement.record(repository.countByStatus(MessageStatus.SENT));
            });
        
        meter.upDownCounterBuilder("failed")
            .setDescription("Failed messages")
            .buildWithCallback(measurement -> {
                measurement.record(repository.countByStatus(MessageStatus.FAILED));
            });
    }
}
