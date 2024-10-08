package com.nttdata.builthub.sparql;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Optional;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.StatusType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import com.nttdata.builthub.sparql.model.SparQLQuery;
import com.nttdata.builthub.sparql.repository.SparQLQueryRepository;

@RestController
@CrossOrigin
@RequestMapping("knowage")
public class KnowageServiceImpl {
	static final String SPARQL_ENTRYPOINT = "http://localhost:7200/repositories";
	static final String GRAPHDB_REPOSITORY = "BuiltHub";
	static final String SPARQL_REQUEST_TYPE = "application/sparql-query";
	static final String SPARQL_RESPONSE_TYPE = "application/sparql-results+json";
	static final String RESPONSE_TYPE = "application/json";

	static final Logger logger = LoggerFactory.getLogger(KnowageServiceImpl.class);

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

	@GetMapping(value = "/sparql", produces = RESPONSE_TYPE)
	public ResponseEntity<StreamingResponseBody> sparqlGET(
			@RequestParam(name = "query", required = true) String sparqlQuery) {
		return this.sparqlImpl(sparqlQuery);
	}

	@PostMapping(value = "/sparql", produces = RESPONSE_TYPE)
	public ResponseEntity<StreamingResponseBody> sparqlPOST(@RequestBody String requestBody) {
		String[] params = requestBody.split("&");
		for (String param : params) {
			if (param.startsWith("query=")) {
				String sparqlQuery = URLDecoder.decode(param.substring(6), StandardCharsets.UTF_8);
				return this.sparqlImpl(sparqlQuery);
			}
		}

		return this.sparqlImpl(requestBody);
	}

	private ResponseEntity<StreamingResponseBody> sparqlImpl(String sparqlQuery) throws ResponseStatusException {
		try {
			final Entity<String> query = Entity.entity(sparqlQuery, SPARQL_REQUEST_TYPE);
			final Builder request = this.jerseyClient.target(SPARQL_ENTRYPOINT).path(GRAPHDB_REPOSITORY).request(SPARQL_RESPONSE_TYPE);
			final Response response = request.post(query);
			if (response.getStatus() == HttpURLConnection.HTTP_OK) {
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
				responseHeaders.setContentType(MediaType.valueOf(RESPONSE_TYPE));
				responseHeaders.setContentLanguage(Locale.ENGLISH);
				responseHeaders.set(HttpHeaders.CONTENT_ENCODING, StandardCharsets.UTF_8.name());

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

	@GetMapping(value = "/dataset", produces = RESPONSE_TYPE)
	public ResponseEntity<StreamingResponseBody> dataset(@RequestParam(name = "name", required = true) String queryName) {
		try {
			/**/
			Optional<SparQLQuery> query = this.repository.findById(queryName);
			if (query.isEmpty()) {
				String msg = "SPARQL query [" + queryName + "] not found";
				
				logger.error(msg);

				throw new ResponseStatusException(HttpStatus.NOT_FOUND, msg);
			}
			
			String sparqlQuery = query.get().getQuery();
			final Entity<String> entity = Entity.entity(sparqlQuery, "application/sparql-query");
			final Builder request = this.jerseyClient.target(SPARQL_ENTRYPOINT).path(GRAPHDB_REPOSITORY).request(RESPONSE_TYPE);
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
				responseHeaders.setContentType(MediaType.parseMediaType(RESPONSE_TYPE));

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
