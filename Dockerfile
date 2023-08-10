FROM maven:3.9.3-amazoncorretto-17@sha256:4ab7db7bd5f95e58b0ba1346ff29d6abdd9b73e5fd89c5140edead8b037386ff AS buildtime

WORKDIR /build
COPY . .

RUN mvn clean package -DskipTests

FROM amazoncorretto:17.0.8-alpine3.18@sha256:0c61f12abfb091be48474e836e6802ff3a93e8e038e0460af8c7f447ccbd3901 AS runtime

# operation needed because amazoncorretto do not contain the shadow-utils package
RUN yum install -y /usr/sbin/adduser
RUN useradd --uid 10000 runner

VOLUME /tmp
WORKDIR /app

COPY --from=buildtime /build/target/*.jar /app/app.jar
# The agent is enabled at runtime via JAVA_TOOL_OPTIONS.
ADD https://github.com/microsoft/ApplicationInsights-Java/releases/download/3.4.15/applicationinsights-agent-3.4.15.jar /app/applicationinsights-agent.jar

RUN chown -R runner:runner /app

EXPOSE 8080

USER 10000

ENTRYPOINT [ "java","-jar","/app/app.jar" ]
