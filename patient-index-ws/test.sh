java -javaagent:../otel/opentelemetry-javaagent.jar \
    -Dotel.service.name=patient-index-ws \
    -Dotel.traces.exporter=otlp \
    -Dotel.metrics.exporter=none \
    -Dotel.logs.exporter=none \
    -Dserver.port=9080 \
    -jar target/patient-index-ws-0.0.1-SNAPSHOT.jar
