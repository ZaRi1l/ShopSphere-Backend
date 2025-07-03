FROM openjdk:17-jdk-slim

# app.jar은 이미 EC2에 복사되어 있음
COPY app.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar"]

