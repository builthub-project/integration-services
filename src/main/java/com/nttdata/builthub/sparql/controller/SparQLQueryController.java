package com.nttdata.builthub.sparql.controller;

import java.util.List;

import javax.persistence.EntityNotFoundException;
import javax.servlet.http.HttpServletRequest;

import org.keycloak.KeycloakSecurityContext;
import org.keycloak.representations.AccessToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;

import com.nttdata.builthub.sparql.model.SparQLQuery;
import com.nttdata.builthub.sparql.service.SparQLQueryService;

@CrossOrigin
@RestController
@RequestMapping("SparQLQuery")
public class SparQLQueryController {
	static final Logger logger = LoggerFactory.getLogger(SparQLQueryController.class);
	static final String ROLE_MANAGER = "builthub-manager";

	@Autowired
	private SparQLQueryService service;

	@GetMapping()
	public ResponseEntity<List<SparQLQuery>> getAll() {
		KeycloakSecurityContext ksc = this.getSecurityContext();
		AccessToken at = ksc.getToken();
		String username = at.getPreferredUsername();
		Boolean userIsManager = this.isUserInRole(ROLE_MANAGER);
		/**/
		return new ResponseEntity<List<SparQLQuery>>(service.findAll(username, userIsManager), HttpStatus.OK);
	}

	@GetMapping("/{id}")
	public ResponseEntity<SparQLQuery> getById(@PathVariable("id") String id) {
		return new ResponseEntity<>(service.findById(id), HttpStatus.OK);
	}

	@PutMapping()
	public ResponseEntity<SparQLQuery> save(@RequestBody SparQLQuery item) {
		try {
			KeycloakSecurityContext ksc = this.getSecurityContext();
			AccessToken at = ksc.getToken();
			String username = at.getPreferredUsername();
			Boolean userIsManager = this.isUserInRole(ROLE_MANAGER);
			/**/
			SparQLQuery entity = service.save(item, username, userIsManager);
			logger.info("Query saved: " + entity.getId());
			
			return new ResponseEntity<SparQLQuery>(entity, HttpStatus.OK);
		} catch (IllegalAccessException e) {
			logger.error("Forbidden query save!!",  e.getMessage(), e);
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage(), e);
		} catch (Exception e) {
			logger.error("Query error!!",  e.getMessage(), e);
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e);
		}
	}

	@PostMapping()
	public ResponseEntity<SparQLQuery> update(@RequestBody SparQLQuery item) {
		try {
			KeycloakSecurityContext ksc = this.getSecurityContext();
			AccessToken at = ksc.getToken();
			String username = at.getPreferredUsername();
			Boolean userIsManager = this.isUserInRole(ROLE_MANAGER);
			/**/
			SparQLQuery entity = service.update(item, username, userIsManager);
			logger.info("Query updated: " + entity.getId());
			
			return new ResponseEntity<SparQLQuery>(entity, HttpStatus.OK);
		} catch (IllegalAccessException e) {
			logger.error("Forbidden query update!!",  e.getMessage(), e);
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage(), e);
		} catch (Exception e) {
			logger.error("Query error!!",  e.getMessage(), e);
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e);
		}
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<SparQLQuery> delete(@PathVariable("id") String id) {
		try {
			KeycloakSecurityContext ksc = this.getSecurityContext();
			AccessToken at = ksc.getToken();
			String username = at.getPreferredUsername();
			Boolean userIsManager = this.isUserInRole(ROLE_MANAGER);
			
			service.delete(id, username, userIsManager);
			
			logger.info("Query removed: " + id);
			return new ResponseEntity<SparQLQuery>(HttpStatus.OK);
		} catch (IllegalAccessException e) {
			logger.error("Forbidden query deletion!!",  e.getMessage(), e);
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage(), e);
		} catch (EntityNotFoundException e) {
			logger.error("Entity not found!!",  e.getMessage(), e);
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
		} catch (Exception e) {
			logger.error("Query error!!",  e.getMessage(), e);
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e);
		}
	}

	private KeycloakSecurityContext getSecurityContext() {
		final HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
				.getRequest();
		return (KeycloakSecurityContext) request.getAttribute(KeycloakSecurityContext.class.getName());
	}

	private Boolean isUserInRole(final String role) {
		final HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
				.getRequest();

		return request.isUserInRole(role);
	}
}
