ARG MAVEN_VERSION=3
ARG JAVA_VERSION=21

FROM maven:${MAVEN_VERSION}-eclipse-temurin-${JAVA_VERSION}-alpine AS build
WORKDIR /mlib
COPY ./pom.xml ./pom.xml
RUN mvn dependency:go-offline
COPY ./src ./src
RUN mvn package -DskipTests

FROM maven:${MAVEN_VERSION}-eclipse-temurin-${JAVA_VERSION}-alpine
COPY --from=build /mlib/target/*.jar /mlib/pix-commons.jar
RUN mvn org.apache.maven.plugins:maven-install-plugin:2.5.2:install-file -Dfile=/mlib/pix-commons.jar
