package com.nttdata.builthub.sparql.repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import com.nttdata.builthub.sparql.model.SparQLQuery;

public class SparQLQueryCustomRepositoryImpl implements SparQLQueryCustomRepository {
	static final Logger logger = LoggerFactory.getLogger(SparQLQueryCustomRepositoryImpl.class);

	@PersistenceContext
	private EntityManager entityManager;

	@Override
	public List<SparQLQuery> findAllCustomized(final String username, final Boolean userIsManager) {
		CriteriaBuilder cb = this.entityManager.getCriteriaBuilder();
		CriteriaQuery<SparQLQuery> cq = cb.createQuery(SparQLQuery.class);

		Root<SparQLQuery> root = cq.from(SparQLQuery.class);
		if (!userIsManager) {
			Predicate equalUser = cb.equal(root.get("owner"), username);
			Predicate withoutOwner = cb.isNull(root.get("owner"));
			cq.where(cb.or(equalUser, withoutOwner));
		}

		TypedQuery<SparQLQuery> query = this.entityManager.createQuery(cq);
		return query.getResultList();
	}

	public SparQLQuery saveCustomized(SparQLQuery item, final String username, final Boolean userIsManager) throws IllegalAccessException {
		Timestamp now = Timestamp.from(Instant.now());
		
		if (!StringUtils.hasText(item.getId())) {
			item.setId(username + "-" + UUID.randomUUID().toString().replace("-", ""));
			item.setOwner(username);
			item.setCreationTime(now);
			item.setUpdateTime(now);
			item.setLastUseTime(now);

			this.entityManager.persist(item);

			return item;
		} else {
			SparQLQuery entity = this.entityManager.find(SparQLQuery.class, item.getId());
			if (entity == null) {
				item.setOwner(username);
				item.setCreationTime(now);
				item.setUpdateTime(now);
				item.setLastUseTime(now);

				this.entityManager.persist(item);

				return item;
			} else {
				logger.info("ID:" + entity.getId());
				logger.info("TITLE:" + entity.getTitle());
				logger.info("OWNER:" + entity.getOwner());
				logger.info("CREATION:" + entity.getCreationTime().toString());
				
				if (username.equalsIgnoreCase(entity.getOwner())) {
					entity.setTitle(item.getTitle());
					entity.setDescription(item.getDescription());
					entity.setQuery(item.getQuery());
					//entity.setOwner(item.getOwner());
					//entity.setBoundary(item.getBoundary());
					//entity.setCreationTime(entity.getCreationTime());
					entity.setUpdateTime(now);
					entity.setLastUseTime(now);

					return this.entityManager.merge(entity);
				}
				
				throw new java.lang.IllegalAccessException("You cannot persist a query entity that belongs to another user");
			}
		}
	}

	@Override
	public void deleteCustomized(String id, String username, Boolean userIsManager) throws IllegalAccessException, EntityNotFoundException {
		SparQLQuery entity = this.entityManager.find(SparQLQuery.class, id);
		if (entity == null)
			throw new EntityNotFoundException("The query entity \"" + id + "\" does not exist");
		
		if (username.equalsIgnoreCase(entity.getOwner())) {
			this.entityManager.remove(entity);
		} else {
			throw new java.lang.IllegalAccessException("You cannot remove a query entity that belongs to another user");
		}
	}
}
