package de.tobchen.health.patientindex.configurations;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.lang.Nullable;

@ConfigurationProperties(prefix = "patient-index")
public record GeneralConfig(
    @Nullable
    Boolean productionMode
) { }
