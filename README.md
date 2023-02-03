# camel-kafka-break-on-first-error-bug

A simple Camel route using `ThrottlingExceptionRoutePolicy` with a Kafka consumer, showing that the message that caused the circuit to be opened is lost.

See [this issue](https://issues.apache.org/jira/browse/CAMEL-18760)

## How to reproduce the issue

* Run Kafka: `docker-compose -f docker-compose.yaml up`
* Run the application: `mvn spring-boot:run`
* Put the downstream system in "outage mode": `curl -X POST -H "content-type: application/json" -d '{ "healthy": false }' http://localhost:8080/healthy`
* Send two messages: `./send-message.sh "Message 1"; ./send-message.sh "Message 2"`
* Resolve downstream system "outage": `curl -X POST -H "content-type: application/json" -d '{ "healthy": true }' http://localhost:8080/healthy`
* Wait for the circuit to be closed again

**Expected result**: each message is consumed once

**Actual result**: only the second message is consumed

Tested with 3.18.3, 3.20.1, 3.21.0-SNAPSHOT (on 2022-02-03 at 16:30 UTC)

## Notes

* Judging from the logs, the consumer seeks back to the right offset but then the offset is
overwritten. I suspect this happens because the consumer is committing the offset when it's paused. 
* Switching to an implementation with manual offset commit (`-Dspring-boot.run.profiles=manual-commit`)
  solves the issue

