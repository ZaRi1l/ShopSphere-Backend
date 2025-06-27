# Java 17~23 중 서버에 맞게 선택
FROM openjdk:17-jdk-slim

# JAR 넣기
ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} app.jar

# 포트
EXPOSE 8080

# 실행
ENTRYPOINT ["java","-jar","/app.jar"]
