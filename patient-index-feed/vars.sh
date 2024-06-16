export JAVA_TOOL_OPTIONS="-javaagent:../otel/opentelemetry-javaagent.jar"

export OTEL_SERVICE_NAME=patient-index-feed

export OTEL_TRACES_EXPORTER=otlp
export OTEL_METRICS_EXPORTER=none
export OTEL_LOGS_EXPORTER=none

export SPRING_RABBITMQ_HOST=localhost
export SPRING_RABBITMQ_PORT=5672
export SPRING_RABBITMQ_USERNAME=guest
export SPRING_RABBITMQ_PASSWORD=guest
