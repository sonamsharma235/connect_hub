FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Install parent POM first
COPY pom.xml ./pom.xml
RUN mvn install -N -DskipTests

# Build common-lib
COPY common-lib/pom.xml ./common-lib/pom.xml
COPY common-lib/src ./common-lib/src
RUN mvn install -f common-lib/pom.xml -DskipTests

# Build discovery-server
COPY discovery-server/pom.xml ./discovery-server/pom.xml
COPY discovery-server/src ./discovery-server/src
RUN mvn clean package -f discovery-server/pom.xml -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*
COPY --from=build /app/discovery-server/target/*.jar app.jar
EXPOSE 8761
ENTRYPOINT ["java", "-jar", "app.jar"]
