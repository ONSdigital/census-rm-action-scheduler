FROM openjdk:11-slim

ARG JAR_FILE=census-rm-action-scheduler*.jar
COPY target/$JAR_FILE /opt/census-rm-action-scheduler.jar

RUN apt-get update
RUN apt-get -yq install curl
RUN apt-get -yq clean

COPY healthcheck.sh /opt/healthcheck.sh
RUN chmod +x /opt/healthcheck.sh

CMD exec /usr/local/openjdk-11/bin/java $JAVA_OPTS -jar /opt/census-rm-action-scheduler.jar
