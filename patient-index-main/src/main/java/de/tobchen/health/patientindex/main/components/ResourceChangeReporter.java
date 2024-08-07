package de.tobchen.health.patientindex.main.components;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessagePropertiesBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import ca.uhn.fhir.context.FhirContext;
import de.tobchen.health.patientindex.main.events.ResourceChangeEvent;

@Component
public class ResourceChangeReporter
{
    private final Logger logger = LoggerFactory.getLogger(ResourceChangeReporter.class);

    private final Optional<RabbitTemplate> template;
    private final TopicExchange topic;

    private final FhirContext context;

    public ResourceChangeReporter(Optional<RabbitTemplate> template, TopicExchange topic, FhirContext context)
    {
        this.template = template;
        this.topic = topic;

        this.context = context;
    }

    @EventListener
    public void resourceChanged(ResourceChangeEvent event)
    {
        var resource = event.resource();

        var json = context.newJsonParser().encodeResourceToString(resource);
        logger.trace(json);

        var key = resource.getResourceType().toString();
        logger.debug("Key: {}", key);

        var messageProperties = MessagePropertiesBuilder
            .newInstance()
            .setContentType("application/fhir+json")
            .setMessageId(UUID.randomUUID().toString())
            .setTimestamp(new Date())
            .build();

        var message = MessageBuilder
            .withBody(json.getBytes(StandardCharsets.UTF_8))
            .andProperties(messageProperties)
            .build();

        template.ifPresent(t -> t.send(topic.getName(), key, message));
    }
}
