FROM maven:3.9.9-eclipse-temurin-17 AS build

WORKDIR /workspace

COPY pom.xml .
COPY xfg-dev-tech-api/pom.xml xfg-dev-tech-api/pom.xml
COPY xfg-dev-tech-trigger/pom.xml xfg-dev-tech-trigger/pom.xml
COPY xfg-dev-tech-app/pom.xml xfg-dev-tech-app/pom.xml

COPY xfg-dev-tech-api/src xfg-dev-tech-api/src
COPY xfg-dev-tech-trigger/src xfg-dev-tech-trigger/src
COPY xfg-dev-tech-app/src xfg-dev-tech-app/src

RUN mvn -q -DskipTests package

FROM eclipse-temurin:17-jre

WORKDIR /app

COPY --from=build /workspace/xfg-dev-tech-app/target/ai-rag-knowledge-app.jar /app/app.jar

EXPOSE 8090

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
