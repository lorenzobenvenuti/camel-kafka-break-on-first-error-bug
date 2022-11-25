package it.lorenzobenvenuti.camel.kafka;

import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class DownstreamDependency {

    private static final Logger LOGGER = LoggerFactory.getLogger(DownstreamDependency.class);

    private AtomicBoolean healthy = new AtomicBoolean(true);

    public void setHealthy(boolean healthy) {
        LOGGER.info("Setting healthy={}", healthy);
        this.healthy.set(healthy);
    }

    public boolean isHealthy() {
        return healthy.get();
    }

    public void process(String message) {
        if (!healthy.get()) {
            throw new DownstreamException();
        }
        LOGGER.info("Message {} has been processed", message);
    }

}
