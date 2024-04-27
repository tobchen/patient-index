package de.tobchen.health.patientindex.main.configurations;

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
}
