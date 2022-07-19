FROM maven:3.8.4-jdk-11-slim as buildtime

WORKDIR /build
COPY . .

RUN mvn clean package

FROM adoptopenjdk/openjdk11:alpine-jre as runtime

VOLUME /tmp
WORKDIR /app

COPY --from=buildtime /build/target/*.jar /app/app.jar
# The agent is enabled at runtime via JAVA_TOOL_OPTIONS.
ADD https://github.com/microsoft/ApplicationInsights-Java/releases/download/3.2.7/applicationinsights-agent-3.2.7.jar /app/applicationinsights-agent.jar

ENTRYPOINT ["java","-jar","/app/app.jar"]