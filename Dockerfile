FROM eclipse-temurin:21-jre

WORKDIR /app

COPY employee-launcher/target/otel-employee.jar otel-employee.jar
COPY employee-launcher/target/agent/opentelemetry-javaagent.jar opentelemetry-javaagent.jar

ENTRYPOINT java -javaagent:opentelemetry-javaagent.jar -jar otel-employee.jar