package de.tobchen.health.patientindex.feed.configurations;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig
{
    @Bean
    public TopicExchange topic()
    {
        return new TopicExchange("patient-index.resource");
    }

    @Bean
    public Queue queue()
    {
        return new Queue("patient-index-feed", true);
    }

    @Bean
    public Binding binding(TopicExchange topic, Queue queue)
    {
        return BindingBuilder
            .bind(queue)
            .to(topic)
            .with("Patient");
    }
}
