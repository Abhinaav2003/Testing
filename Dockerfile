# Step 1: Build the application using Maven
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# Step 2: Run with lightweight JRE (smaller image = faster Koyeb cold start)
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

# JVM tuned for Koyeb free tier (256MB RAM limit)
ENTRYPOINT ["java", \
  "-Xms64m", \
  "-Xmx180m", \
  "-XX:+UseSerialGC", \
  "-XX:MaxMetaspaceSize=96m", \
  "-jar", "app.jar"]
