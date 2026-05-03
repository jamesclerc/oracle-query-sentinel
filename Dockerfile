FROM eclipse-temurin:17-jdk AS build
WORKDIR /workspace
COPY pom.xml ./
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw -B -ntp -DskipTests package || \
    (apt-get update && apt-get install -y maven && mvn -B -ntp -DskipTests package)

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /workspace/target/oracle-query-sentinel-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
