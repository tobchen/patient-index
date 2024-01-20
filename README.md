# Patient Index

Very, very, *very* limited patient indexing.

## Docker Compose

To successfully run *compose.yaml* put the [OpenTelemetry javaagent](https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases) into */otel/opentelemetry-javaagent.jar*

## Development

### (F)AQ

**Why Spring Boot 2.7?**

As of 2023, HAPI FHIR is depending on `javax.servlet`, so until that's upgraded, this project's dependencies won't be either.
