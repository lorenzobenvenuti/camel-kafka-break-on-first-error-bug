# camel-kafka-break-on-first-error-bug

A simple Camel route using `ThrottlingExceptionRoutePolicy` with a Kafka consumer 

## How to reproduce the issue

* Run Kafka: `docker-compose -f docker-compose.yaml up`
* Run the application: `mvn spring-boot:run`
* Put the downstream system in "outage mode": `curl -X POST -H "content-type: application/json" -d '{ "healthy": false }' http://localhost:8080/healthy`
* Send two messages: `./send-message.sh "Message 1"; ./send-message.sh "Message 2"`
* Resolve downstream system "outage": `curl -X POST -H "content-type: application/json" -d '{ "healthy": true }' http://localhost:8080/healthy`
* Wait for the circuit to be closed again

**Expected result**: each message is consumed once

**Actual result**: only the second message is consumed
   
## Notes

* Judging from the logs, the consumer seeks back to the right offset but then the offset is
overwritten. I suspect this happens because the consumer is committing the offset when it's paused. 
* Switching to manual commits solves the issue
