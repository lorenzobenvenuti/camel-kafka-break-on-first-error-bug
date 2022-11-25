package it.lorenzobenvenuti.camel.kafka;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DownstreamDependencyController {

    @Autowired
    DownstreamDependency downstreamDependency;

    @PostMapping("/healthy")
    public void healthy(@RequestBody DownstreamSettings downstreamSettings) {
        downstreamDependency.setHealthy(downstreamSettings.isHealthy());
    }

}
