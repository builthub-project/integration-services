package com.nttdata.builthub.sparql;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.Callable;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.validation.constraints.Min;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.StatusType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import com.nttdata.builthub.sparql.model.SparQLQuery;
import com.nttdata.builthub.sparql.repository.SparQLQueryRepository;

@RestController
@CrossOrigin
public class SparqlServiceImpl {
    static final String SPARQL_ENTRYPOINT = "http://localhost:7200/repositories";
    static final String GRAPHDB_REPOSITORY = "BuiltHub";
    static final String DEFAULT_QUERY_RESPONSE_TYPE = "application/sparql-results+json";
    static final String DEFAULT_DATASET_RESPONSE_TYPE = "text/csv";

    private static final int JERSEY_CONNECTION_SPARQL = 0;
    private static final int JERSEY_CONNECTION_INDICATORS = 1;
    private static final int JERSEY_CONNECTION_OTHER = 2;

    static final Logger logger = LoggerFactory.getLogger(SparqlServiceImpl.class);

    @Autowired
    private SparQLQueryRepository repository;

    private Client[] jerseyClient = {null, null, null};

    @PostConstruct
    public void initialize() {
        this.jerseyClient[JERSEY_CONNECTION_SPARQL] = ClientBuilder.newClient();
        this.jerseyClient[JERSEY_CONNECTION_INDICATORS] = ClientBuilder.newClient();
        this.jerseyClient[JERSEY_CONNECTION_OTHER] = ClientBuilder.newClient();
    }

    @PreDestroy
    public void clean() {
        for (Client client : this.jerseyClient) {
            try {
                client.close();
            } catch (Throwable ignore) {

            }
        }
    }

    @GetMapping(value = "/sparql")
    private ResponseEntity<StreamingResponseBody> sparqlGet(
            @RequestParam(name = "query", required = true) String sparqlQuery,
            @RequestParam(name = "limit", required = false) @Min(1) Integer paramLimit,
            @RequestParam(name = "format", required = false) String paramFormat,
            @RequestHeader(name = "Accept", defaultValue = "application/json") String acceptType) {

        String resultFormat = StringUtils.hasText(paramFormat) ? paramFormat : acceptType;

        return this.sparqlImpl(sparqlQuery, paramLimit, resultFormat);
    }

    @PostMapping(value = "/sparql")
    private ResponseEntity<StreamingResponseBody> sparqlPost(@RequestBody String requestBody,
                                                             @RequestParam(name = "limit", required = false) @Min(1) Integer paramLimit,
                                                             @RequestParam(name = "format", required = false) String paramFormat,
                                                             @RequestHeader(name = "Accept", defaultValue = "application/json") String acceptType) {
        String resultFormat = StringUtils.hasText(paramFormat) ? paramFormat : acceptType;

        String[] params = requestBody.split("&");
        for (String param : params) {
            if (param.startsWith("query=")) {
                String sparqlQuery = URLDecoder.decode(param.substring(6), StandardCharsets.UTF_8);
                return this.sparqlImpl(sparqlQuery, paramLimit, resultFormat);
            }
        }

        return this.sparqlImpl(requestBody, paramLimit, resultFormat);
    }

    private ResponseEntity<StreamingResponseBody> sparqlImpl(String sparqlQuery, Integer queryLimit,
                                                             String resultFormat) {
        Client client = this.jerseyClient[JERSEY_CONNECTION_SPARQL];

        try {
            String responseType = MimeTypeUtils.getResponseFormat(resultFormat, DEFAULT_QUERY_RESPONSE_TYPE);

            final Entity<String> query = Entity.entity(sparqlQuery, "application/sparql-query");
            final Builder request = client.target(SPARQL_ENTRYPOINT).path(GRAPHDB_REPOSITORY).request(responseType);
            final Response response = request.post(query);
            if (response.getStatus() == HttpURLConnection.HTTP_OK) {
                StreamingResponseBody responseBody = out -> {
                    InputStream dataIS = (InputStream) response.getEntity();
                    byte[] dataBuffer = new byte[4 * 1024];
                    int dataSize = 0;

                    while ((dataSize = dataIS.read(dataBuffer)) > 0) {
                        out.write(dataBuffer, 0, dataSize);
                        out.flush();
                    }

                    out.flush();
                };

                HttpHeaders responseHeaders = new HttpHeaders();
                responseHeaders.set(HttpHeaders.CONTENT_ENCODING, StandardCharsets.UTF_8.name());
                responseHeaders.setContentLanguage(Locale.ENGLISH);

                return new ResponseEntity<StreamingResponseBody>(responseBody, responseHeaders, HttpStatus.OK);
            }

            StatusType status = response.getStatusInfo();
            logger.error("*** AWS GRAPHDB REQUEST ERROR [" + Integer.toString(status.getStatusCode()) + "]: "
                    + status.getReasonPhrase());
            if (response.hasEntity()) {
                logger.error(response.readEntity(String.class));
            }

            throw new ResponseStatusException(status.getStatusCode(), status.getReasonPhrase(), null);
        } catch (ResponseStatusException rse) {
            throw rse;
        } catch (Throwable t) {
            logger.error(t.getMessage(), t);

            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, t.getMessage(), t);
        }
    }

    @GetMapping(value = "/dataset")
    public Callable<ResponseEntity<StreamingResponseBody>> dataset(
            @RequestParam(name = "name", required = true) String queryName,
            @RequestParam(name = "limit", required = false) @Min(1) Integer paramLimit,
            @RequestParam(name = "format", required = false) String paramFormat,
            @RequestHeader(name = "accept", defaultValue = "text/csv") String acceptType) {
        Client client = this.jerseyClient[JERSEY_CONNECTION_INDICATORS];
        String resultFormat = StringUtils.hasText(paramFormat) ? paramFormat : acceptType;

        try {
            /**/
            String responseType = MimeTypeUtils.getResponseFormat(resultFormat, DEFAULT_DATASET_RESPONSE_TYPE);

            Optional<SparQLQuery> query = this.repository.findById(queryName);
            if (query.isEmpty()) {
                String msg = "SPARQL query [" + queryName + "] not found";

                logger.error(msg);

                throw new ResponseStatusException(HttpStatus.NOT_FOUND, msg);
            }

            String sparqlQuery = query.get().getQuery();
            /**/
            if (paramLimit != null) {
                sparqlQuery += " LIMIT " + paramLimit;

            }
            /**/
            final Entity<String> entity = Entity.entity(sparqlQuery, "application/sparql-query");
            final Builder request = client.target(SPARQL_ENTRYPOINT).path(GRAPHDB_REPOSITORY).request(responseType);
            final Response response = request.post(entity);
            final int responseStatus = response.getStatus();
            if (responseStatus == HttpURLConnection.HTTP_OK) {
                return () -> {
                    StreamingResponseBody responseBody = out -> {
                        InputStream dataIS = (InputStream) response.getEntity();
                        byte[] dataBuffer = new byte[4 * 1024];
                        int dataSize = 0;

                        while ((dataSize = dataIS.read(dataBuffer)) > 0) {
                            out.write(dataBuffer, 0, dataSize);
                            out.flush();
                        }

                        out.flush();
                    };

                    HttpHeaders responseHeaders = new HttpHeaders();
                    responseHeaders.set(HttpHeaders.CONTENT_ENCODING, StandardCharsets.UTF_8.name());
                    responseHeaders.setContentLanguage(Locale.ENGLISH);
                    responseHeaders.setContentType(MediaType.parseMediaType(responseType));
                    responseHeaders.setContentDisposition(ContentDisposition
                            .parse("attachment; filename=results." + MimeTypeUtils.getFileExtension(responseType)));

                    return new ResponseEntity<>(responseBody, responseHeaders, HttpStatus.OK);
                };
            }

            StatusType status = response.getStatusInfo();
            logger.error("*** AWS GRAPHDB REQUEST ERROR [" + status.getStatusCode() + "]: "
                    + status.getReasonPhrase());
            if (response.hasEntity()) {
                logger.error(response.readEntity(String.class));
            }

            throw new ResponseStatusException(status.getStatusCode(), status.getReasonPhrase(), null);
        } catch (ResponseStatusException rse) {
            throw rse;
        } catch (Throwable t) {
            logger.error(t.getMessage(), t);

            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, t.getMessage(), t);
        }
    }
}
