FROM openjdk:8-jre
ARG JAR_NAME

COPY entrypoint.sh /
COPY config.json /
COPY build/libs/$JAR_NAME /Bakend.jar

ENTRYPOINT ["sh",  "/entrypoint.sh"]