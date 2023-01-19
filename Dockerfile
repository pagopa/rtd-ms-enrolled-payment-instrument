FROM amazoncorretto:11 as buildtime

WORKDIR /build
COPY . .

RUN mvn clean package

FROM amazoncorretto:11 as runtime

VOLUME /tmp
WORKDIR /app

COPY --from=buildtime /build/target/*.jar /app/app.jar
# The agent is enabled at runtime via JAVA_TOOL_OPTIONS.
ADD https://github.com/microsoft/ApplicationInsights-Java/releases/download/3.4.2/applicationinsights-agent-3.4.2.jar /app/applicationinsights-agent.jar

EXPOSE 8080

ENTRYPOINT [ "java","-jar","/app/app.jar" ]