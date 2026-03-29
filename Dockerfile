# Backend Dockerfile — Spring Boot
FROM eclipse-temurin:25-jdk-alpine AS build
WORKDIR /app

# Install Maven
RUN apk add --no-cache maven

# Copy pom first for dependency caching
COPY pom.xml ./
RUN mvn dependency:go-offline -B

# Copy source and build
COPY src/ src/
RUN mvn package -DskipTests -B

FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENV JAVA_OPTS="-Xmx256m -Xms128m"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
