FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

COPY --chown=10001:10001 build/docker/app.jar /app/app.jar

USER 10001:10001

ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=70 -XX:InitialRAMPercentage=20 -XX:+ExitOnOutOfMemoryError"

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
