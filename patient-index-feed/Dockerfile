ARG PIX_VERSION=0.0.1-SNAPSHOT
ARG MAVEN_VERSION=3
ARG JAVA_VERSION=21

FROM patient-index-commons:${PIX_VERSION}-mvn-${MAVEN_VERSION}-java-${JAVA_VERSION} AS build
WORKDIR /app
COPY ./pom.xml .
RUN mvn dependency:go-offline
COPY ./src ./src
RUN mvn package -DskipTests

FROM eclipse-temurin:${JAVA_VERSION}-jre-alpine
COPY --from=build /app/target/*.jar /app/main.jar
CMD [ "java", "-jar", "/app/main.jar" ]
