# ---- build ----
FROM eclipse-temurin:17-jdk AS build
WORKDIR /src
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw && ./mvnw -q -B dependency:go-offline
COPY db db
COPY src src
RUN ./mvnw -q -B package -DskipTests

# ---- run ----
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /src/target/safeguard-japan-*.jar app.jar
# Render の無料プランはメモリ 512MB
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+UseSerialGC"
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
