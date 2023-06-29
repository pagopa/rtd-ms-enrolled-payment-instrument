FROM maven:3.9.0-amazoncorretto-17 as buildtime

WORKDIR /build
COPY . .

RUN mvn clean package -DskipTests

FROM amazoncorretto:17.0.6-al2023 as runtime

VOLUME /tmp
WORKDIR /app

COPY --from=buildtime /build/target/*.jar /app/app.jar
# The agent is enabled at runtime via JAVA_TOOL_OPTIONS.
ADD https://github.com/microsoft/ApplicationInsights-Java/releases/download/3.4.11/applicationinsights-agent-3.4.11.jar /app/applicationinsights-agent.jar

EXPOSE 8080

ENTRYPOINT [ "java","-jar","/app/app.jar" ]
