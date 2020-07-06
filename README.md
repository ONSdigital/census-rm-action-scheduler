# census-rm-action-scheduler

[![Build Status](https://travis-ci.com/ONSdigital/census-rm-action-scheduler.svg?branch=master)](https://travis-ci.com/ONSdigital/census-rm-action-scheduler)

# Overview
This service schedules action rules and batches of fulfilments. The action rules are triggered at a specific time for each rule. The fulfilment batches run on a cron schedule.


The Action Scheduler is implemented with Java 11 & Spring Integration, it is schedule driven, reading & writing data to a SQL DB.

rreigfefubferblahtest24
# Testing

To test this service locally use:

```shell-script
mvn clean install
```   
This will run all of the unit tests, then if successful create a docker image for this application 
then bring up the required docker images from the test [docker compose YAML](src/test/resources/docker-compose.yml) (postgres)
to run the Integration Tests.

# Debug    
 If you want to debug the application/Integration tests start the required docker images by navigating 
 to [src/test/resources/](src/test/resources/) and then run :
 
```shell-script
docker-compose up
```

You should then be able to run the tests from your IDE.

# Configuration
By default the src/main/resources/application.yml is configured for 
[census-rm-docker-dev](https://github.com/ONSdigital/census-rm-docker-dev)

For production the configuration is overridden by the K8S apply script.
