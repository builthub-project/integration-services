package com.nttdata.builthub.sparql.service;

import java.util.List;
import java.util.NoSuchElementException;

import javax.persistence.EntityNotFoundException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nttdata.builthub.sparql.model.SparQLQuery;
import com.nttdata.builthub.sparql.repository.SparQLQueryRepository;

@Service
public class SparQLQueryService {

    @Autowired
    private SparQLQueryRepository repository;

    public List<SparQLQuery> findAll(final String username, final Boolean userIsManager) {
        return repository.findAllCustomized(username, userIsManager);
    }

    public SparQLQuery findById(String id) {
        return repository.findById(id).orElseThrow(() -> new NoSuchElementException());
    }

    public boolean existById(String id) {
        return repository.existsById(id);
    }

    @Transactional
    public SparQLQuery save(SparQLQuery item, final String username, final Boolean userIsManager) throws IllegalAccessException {
        return repository.saveCustomized(item, username, userIsManager);
    }

    @Transactional
    public SparQLQuery update(SparQLQuery item, final String username, final Boolean userIsManager) throws IllegalAccessException {
        return repository.saveCustomized(item, username, userIsManager);
    }

    @Transactional
    public void delete(String id, final String username, final Boolean userIsManager) throws IllegalAccessException, EntityNotFoundException {
        repository.deleteCustomized(id, username, userIsManager);
    }
}
