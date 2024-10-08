package com.nttdata.builthub.sparql;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Optional;

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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import com.nttdata.builthub.sparql.model.SparQLQuery;
import com.nttdata.builthub.sparql.repository.SparQLQueryRepository;

@RestController
@CrossOrigin
@RequestMapping("office")
public class OfficeServiceImpl {
    static final String SPARQL_ENTRYPOINT = "http://localhost:7200/repositories";
    static final String GRAPHDB_REPOSITORY = "BuiltHub";
    static final String DEFAULT_DATASET_RESPONSE_TYPE = "text/csv";

    static final Logger logger = LoggerFactory.getLogger(OfficeServiceImpl.class);

    @Autowired
    private SparQLQueryRepository repository;

    private Client jerseyClient = null;

    @PostConstruct
    public void initialize() {
        this.jerseyClient = ClientBuilder.newClient();
    }

    @PreDestroy
    public void clean() {
        try {
            this.jerseyClient.close();
        } catch (Throwable ignore) {

        }
    }

    @GetMapping(value = "/dataset")
    public ResponseEntity<StreamingResponseBody> dataset(@RequestParam(name = "name", required = true) String queryName,
                                                         @RequestParam(name = "limit", required = false) @Min(1) Integer paramLimit,
                                                         @RequestParam(name = "format", required = false) String paramFormat,
                                                         @RequestHeader(name = "accept", defaultValue = "text/csv") String acceptType) {
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
                sparqlQuery += " LIMIT " + paramLimit.toString();
            }
            /**/
            final Entity<String> entity = Entity.entity(sparqlQuery, "application/sparql-query");
            final Builder request = this.jerseyClient.target(SPARQL_ENTRYPOINT).path(GRAPHDB_REPOSITORY).request(responseType);
            final Response response = request.post(entity);
            final int responseStatus = response.getStatus();
            if (responseStatus == HttpURLConnection.HTTP_OK) {
                StreamingResponseBody responseBody = new StreamingResponseBody() {
                    @Override
                    public void writeTo(OutputStream out) throws IOException {
                        InputStream dataIS = (InputStream) response.getEntity();
                        byte[] dataBuffer = new byte[4 * 1024];
                        int dataSize = 0;

                        while ((dataSize = dataIS.read(dataBuffer)) > 0) {
                            out.write(dataBuffer, 0, dataSize);
                            out.flush();
                        }

                        out.flush();
                    }
                };

                HttpHeaders responseHeaders = new HttpHeaders();
                responseHeaders.set(HttpHeaders.CONTENT_ENCODING, StandardCharsets.UTF_8.name());
                responseHeaders.setContentLanguage(Locale.ENGLISH);
                responseHeaders.setContentType(MediaType.parseMediaType(responseType));
                responseHeaders
                        .setContentDisposition(ContentDisposition.parse("attachment; filename=results." + MimeTypeUtils.getFileExtension(responseType)));

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
}
