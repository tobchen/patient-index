export JAVA_TOOL_OPTIONS="-javaagent:../otel/opentelemetry-javaagent.jar"

export OTEL_SERVICE_NAME=patient-index-ws

export OTEL_TRACES_EXPORTER=otlp
export OTEL_METRICS_EXPORTER=none
export OTEL_LOGS_EXPORTER=none

export SERVER_PORT=9080

export PATIENT_INDEX_PID_OID=0.0.0
export PATIENT_INDEX_FHIR_SERVER="http://localhost:8080/fhir/r5"
