package it.lorenzobenvenuti.camel.kafka;

import java.util.List;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.kafka.KafkaConstants;
import org.apache.camel.component.kafka.consumer.KafkaManualCommit;
import org.apache.camel.throttling.ThrottlingExceptionRoutePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("manual-commit")
public class ManualCommitKafkaConsumerRouteBuilder extends RouteBuilder {

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
                .handled(true)
                .process(this::commit);;

        from("kafka:my-topic?groupId=my-group&breakOnFirstError=true&" +
                            "allowManualCommit=true&autoCommitEnable=false")
                .routeId(ROUTE_ID)
                .routePolicy(throttlingExceptionRoutePolicy())
                .log("Received ${body}")
                .process(exchange ->
                        downstreamDependency.process(exchange.getIn().getBody(String.class)))
                .process(this::commit);
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

    private void commit(Exchange exchange) {
        Boolean lastOne = exchange.getIn().getHeader(KafkaConstants.LAST_RECORD_BEFORE_COMMIT, Boolean.class);
        if (lastOne) {
            LOGGER.info("Committing offset {}", exchange.getIn().getHeader(KafkaConstants.OFFSET));
            KafkaManualCommit manual =
                    exchange.getIn().getHeader(KafkaConstants.MANUAL_COMMIT, KafkaManualCommit.class);
            manual.commit();
        }
    }

}
