FROM gradle:8.10.0-jdk21 AS build
COPY  . /home/gradle/src
WORKDIR /home/gradle/src
RUN gradle assemble
FROM eclipse-temurin:21-jre-alpine
EXPOSE 8080
RUN mkdir /app
COPY --from=build /home/gradle/src/build/libs/*.jar /app/app.jar

ENTRYPOINT ["java", "-jar", "/app/app.jar"]