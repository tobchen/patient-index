package de.tobchen.health.patientindex.ws.configurations;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;

@Configuration
public class OpenTelemetryConfig {
    @Bean
    public OpenTelemetry openTelemetry()
    {
        var openTelemetry = GlobalOpenTelemetry.get();
        if (openTelemetry == null)
        {
            openTelemetry = OpenTelemetry.noop();
            if (openTelemetry == null)
            {
                throw new RuntimeException("Cannot init noop OpenTelemetry");
            }
        }

        return openTelemetry;
    }
}
