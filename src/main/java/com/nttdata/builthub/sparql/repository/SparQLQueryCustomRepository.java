package com.nttdata.builthub.sparql.repository;

import java.util.List;

import javax.persistence.EntityNotFoundException;

import com.nttdata.builthub.sparql.model.SparQLQuery;

public interface SparQLQueryCustomRepository {
	List<SparQLQuery> findAllCustomized(final String username, final Boolean userIsManager);
	SparQLQuery saveCustomized(SparQLQuery item, final String username, final Boolean userIsManager) throws IllegalAccessException;
	void deleteCustomized(final String id, final String username, final Boolean userIsManager) throws IllegalAccessException, EntityNotFoundException;
}
