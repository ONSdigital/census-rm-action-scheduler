# census-rm-action-scheduler

[![Build Status](https://travis-ci.com/ONSdigital/census-rm-action-scheduler.svg?branch=master)](https://travis-ci.com/ONSdigital/census-rm-action-scheduler)

# Overview
This service schedules and distributes actions on census cases. Distributing a case entails sending out certain details like address fields so that it can be actioned. These actions are either letter/questionnaire print runs or fieldwork visits.
Cases are ingested into this service by message queue and must be associated with an Action Plan. The Action Plan links the cases to a set of Action Rules, which store the type and date of the actions to be run, as well as classifiers which can be used to filter which cases are included. The Action Scheduler runs on a polling interval, checking to see if any Action Rules need to be "triggered". 

As well as scheduled actions this service also handles ad hoc fulfilment requests. These are real time requests for actions on a census case e.g. mailing out a questionnaire which was requested over the phone. These requests are converted into the appropriate action request messages, which may involve extra work such as generating a new QID/UAC pair for the case (via an API call) and distributed.

The Action Scheduler is implemented with Java 11 & Spring Integration, it is both event and schedule driven listening and publishing to rabbitmq and persisting data to a SQL DB.


#  Entrypoints / MessageEndpoints

There are multiple entry points to this application, these can be found in the messaging folder/package, each 
class in here is a listener to a queue (defined in the application yml).  These classes are annotated 
@MessageEndpoint and each consists of a receiveMessage function bound to a queue and marked @Transactional.  The 
 @Transactional part wraps every queuing & database action under this function into 1 big transaction.  If any of this 
fails they all get rolled back and the original message will be returned unharmed to the rabbit queue.  After several
failures the MessageException Service is configured to place a bad message onto a backoff queue.

The Action Scheduler consumes the `CASE_CREATED`, `CASE_UPDATED`, `UAC_UPDATED` and `FULFILMENT_REQUEST` events from the fanout exchange.

The Action Scheduler exposes a RESTful API to allow new Action Plans, Action Rules and Action Types to be set up - this is only used by the Ops tool and acceptance tests. For production set up we use SQL scripts stored in the [Action Plans and Rules directory of the census rm DDL repo](https://github.com/ONSdigital/census-rm-ddl/tree/master/manual_scripts/action_plans_and_rules).


# Testing

To test this service locally use:

```shell-scrip
mvn clean install
```   
This will run all of the unit tests, then if successful create a docker image for this application 
then bring up the required docker images from the test [docker compose YAML](src/test/resources/docker-compose.yml) (postgres, uacqidservice and rabbit)
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

The queues are defined in test [definitions.json](src/test/resources/definitions.json) for Integration Tests.
