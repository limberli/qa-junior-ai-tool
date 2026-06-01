FROM maven:3.9.6-amazoncorretto-17-al2023 AS builder
WORKDIR /build

COPY pom.xml .
COPY common/pom.xml common/pom.xml
COPY host-orchestrator/pom.xml host-orchestrator/pom.xml
COPY tester-agent/pom.xml tester-agent/pom.xml
COPY analyst-agent/pom.xml analyst-agent/pom.xml
COPY e2e-tests/pom.xml e2e-tests/pom.xml

RUN mvn dependency:go-offline -pl tester-agent -am -q

COPY common/src common/src
COPY tester-agent/src tester-agent/src

RUN mvn package -pl tester-agent -am -DskipTests

FROM --platform=linux/amd64 eclipse-temurin:17-jre-alpine
WORKDIR /app

COPY --from=builder /build/tester-agent/target/*.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]