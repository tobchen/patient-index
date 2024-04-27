package de.tobchen.health.patientindex.main.components;

import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import ca.uhn.fhir.parser.IParser;
import de.tobchen.health.patientindex.main.events.ResourceChangeEvent;

@Component
public class ResourceChangeReporter
{
    private final Logger logger = LoggerFactory.getLogger(ResourceChangeReporter.class);

    private final RabbitTemplate template;
    private final TopicExchange topic;

    private final IParser parser;

    public ResourceChangeReporter(RabbitTemplate template, TopicExchange topic, IParser parser)
    {
        this.template = template;
        this.topic = topic;

        this.parser = parser;
    }

    @EventListener
    public void resourceChanged(ResourceChangeEvent event)
    {
        var resource = event.resource();

        var json = parser.encodeResourceToString(resource);
        logger.trace(json);

        var key = "resource." + resource.getResourceType().toString();
        logger.debug("Key: {}", key);

        var message = MessageBuilder
            .withBody(json.getBytes(StandardCharsets.UTF_8))
            .build();

        template.send(topic.getName(), key, message);
    }
}
