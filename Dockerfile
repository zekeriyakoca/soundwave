FROM eclipse-temurin:25-jdk AS build
WORKDIR /app

COPY gradlew gradlew
COPY gradle gradle
COPY settings.gradle.kts build.gradle.kts ./
COPY src src

RUN chmod +x gradlew
RUN ./gradlew --no-daemon clean bootJar

FROM eclipse-temurin:25-jre AS runtime
WORKDIR /app

COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
