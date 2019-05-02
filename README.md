# census-rm-action-scheduler
Rewritten Action Service for Census - has backwards compatibility with legacy XML format for message output.

The Action Scheduler consumes the CaseCreatedEvent and UacUpdated events from the fanout exchange.

The Action Scheduler exposes a RESTful API to allow new Action Plans, Action Rules and Action Types to be set up - this is only used by the Ops tool and acceptance tests.

The Action Scheduler runs on a polling interval, checking to see if any Action Rules need to be "triggered".