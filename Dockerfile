FROM openjdk:11-slim-stretch as build
RUN groupadd --gid 999 actionscheduler && \
    useradd --create-home --system --uid 999 --gid actionscheduler actionscheduler
USER actionscheduler
FROM gcr.io/distroless/java:11
COPY --from=build /opt/ /opt
COPY --from=build /etc/passwd /etc/passwd
