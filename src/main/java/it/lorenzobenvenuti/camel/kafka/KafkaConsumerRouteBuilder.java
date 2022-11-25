package it.lorenzobenvenuti.camel.kafka;

import java.util.List;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.throttling.ThrottlingExceptionRoutePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class KafkaConsumerRouteBuilder extends RouteBuilder {

    public static final String ROUTE_ID = "kafkaConsumer";

    private static final Logger LOGGER = LoggerFactory.getLogger(ROUTE_ID);

    @Autowired
    DownstreamDependency downstreamDependency;

    @Override
    public void configure() throws Exception {
        onException(DownstreamException.class)
                .log("Downstream system exception caught - opening circuit");

        onException(Exception.class)
                .log("Another exception caught")
                .handled(true);

        from("kafka:my-topic?groupId=my-group&breakOnFirstError=true")
                .routeId(ROUTE_ID)
                .routePolicy(throttlingExceptionRoutePolicy())
                .log("Received ${body}")
                .process(exchange ->
                        downstreamDependency.process(exchange.getIn().getBody(String.class)));
    }

    private ThrottlingExceptionRoutePolicy throttlingExceptionRoutePolicy() {
        ThrottlingExceptionRoutePolicy routePolicy = new ThrottlingExceptionRoutePolicy(
                1,
                Long.MAX_VALUE,
                30000,
                List.of(DownstreamException.class)
        );
        routePolicy.setHalfOpenHandler(() -> {
            final boolean healthy = downstreamDependency.isHealthy();
            LOGGER.info("Downstream dependency healthy={}", healthy);
            return healthy;
        });
        return routePolicy;
    }
}
