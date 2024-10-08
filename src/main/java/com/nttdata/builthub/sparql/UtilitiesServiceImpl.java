package com.nttdata.builthub.sparql;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.Callable;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@RestController
@CrossOrigin
@RequestMapping("util")
public class UtilitiesServiceImpl {
    static final Logger logger = LoggerFactory.getLogger(UtilitiesServiceImpl.class);

    @PostConstruct
    public void initialize() {
    }

    @PreDestroy
    public void clean() {
    }

    @GetMapping(value = "/logs", produces = "text/plain")
    public Callable<ResponseEntity<StreamingResponseBody>> logs(@RequestParam(name = "file", required = false, defaultValue = "catalina.out") String logFilename) {
        try {
            return () -> {
                StreamingResponseBody responseBody = out -> {
                    FileInputStream fileStream = null;
                    byte[] dataBuffer = new byte[4 * 1024];
                    int dataSize = 0;

                    try {
                        fileStream = new FileInputStream("/usr/share/tomcat/logs/" + logFilename);
                        while ((dataSize = fileStream.read(dataBuffer)) > 0) {
                            out.write(dataBuffer, 0, dataSize);
                            out.flush();
                        }

                        out.flush();
                    } finally {
                        try {
                            if (fileStream != null) {
                                fileStream.close();
                            }
                        } catch (IOException ignored) {
                        }

                    }
                };

                return new ResponseEntity<>(responseBody, HttpStatus.OK);
            };
        } catch (Throwable t) {
            logger.error(t.getMessage(), t);

            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, t.getMessage(), t);
        }
    }
}
