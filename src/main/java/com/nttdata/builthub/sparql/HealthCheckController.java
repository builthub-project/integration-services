package com.nttdata.builthub.sparql;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin
@RestController
@RequestMapping("/api/health")
public class HealthCheckController {
    private Logger logger = LoggerFactory.getLogger(getClass());

    @GetMapping
    public void healthCheck() {
        logger.info("Health check called");
    }

}
