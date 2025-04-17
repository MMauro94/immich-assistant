FROM gradle:8-jdk17-corretto-al2023 AS build

COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN gradle uberJar --no-daemon -PjarName=app

FROM openjdk:17-alpine

WORKDIR /media
VOLUME /media

COPY --from=build /home/gradle/src/build/libs/app.jar /app/app.jar

ENTRYPOINT ["java", "-jar", "/app/app.jar"]