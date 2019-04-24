FROM openjdk:11-slim

ARG JAR_FILE=census-rm-action-scheduler*.jar
COPY target/$JAR_FILE /opt/census-rm-action-scheduler.jar

COPY healthcheck.sh /opt/healthcheck.sh
RUN chmod +x /opt/healthcheck.sh

CMD exec /usr/bin/java $JAVA_OPTS -jar /opt/census-rm-action-scheduler.jar
