package com.nttdata.builthub.sparql.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nttdata.builthub.sparql.model.SparQLQuery;

public interface SparQLQueryRepository extends JpaRepository<SparQLQuery, String>, SparQLQueryCustomRepository {

}
