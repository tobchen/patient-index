# Patient Index

Very, very, *very* limited patient indexing.

## Docker Compose

To successfully run *compose.yaml* put the [OpenTelemetry javaagent](https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases) into */otel/opentelemetry-javaagent.jar*

## Patient Index Web Service

To successfully compile the *Patient Index* web service put the HL7v3 *coreschemas* and *multicacheschemas* folders from the HL7v3 Normative Edition CD into */src/main/resources/schemas/hl7v3-ne2008/*

## Development

### (F)AQ

**Why Spring Boot 2.7?**

As of 2023, HAPI FHIR is depending on `javax.servlet`, so until that's upgraded, this project's dependencies won't be either.
