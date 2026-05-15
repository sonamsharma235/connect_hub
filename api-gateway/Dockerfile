FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copy root parent pom
COPY pom.xml ./pom.xml

# Install parent pom
RUN mvn install -f pom.xml -N -DskipTests

# Build and install common-lib first
COPY common-lib/pom.xml ./common-lib/pom.xml
COPY common-lib/src ./common-lib/src
RUN mvn install -f common-lib/pom.xml -DskipTests

# Now build the actual service
COPY api-gateway/pom.xml ./api-gateway/pom.xml
COPY api-gateway/src ./api-gateway/src
RUN mvn clean package -f api-gateway/pom.xml -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/api-gateway/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
