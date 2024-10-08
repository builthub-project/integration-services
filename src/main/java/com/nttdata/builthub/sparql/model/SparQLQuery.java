package com.nttdata.builthub.sparql.model;

import java.sql.Timestamp;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name="saved_sparql_query")
public class SparQLQuery {
	@Id
	@Column(name="id")
	private String id;

	@Column(name="title")
	private String title;

	@Column(name="description")
	private String description;

	@Column(name="query_text")
	private String query;

	@Column(name="owner")
	private String owner;

	@Column(name="boundary")
	private String boundary;

	@Column(name="creation_time")
	private Timestamp creationTime; 

	@Column(name="update_time")
	private Timestamp updateTime; 

	@Column(name="use_time")
	private Timestamp lastUseTime;

	public SparQLQuery() {
		super();
	}

	public SparQLQuery(String id, String title, String description, String query, String owner, String boundary,
			Timestamp creationTime, Timestamp updateTime, Timestamp lastUseTime) {
		super();
		this.id = id;
		this.title = title;
		this.description = description;
		this.query = query;
		this.owner = owner;
		this.boundary = boundary;
		this.creationTime = creationTime;
		this.updateTime = updateTime;
		this.lastUseTime = lastUseTime;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getQuery() {
		return query;
	}

	public void setQuery(String query) {
		this.query = query;
	}

	public String getOwner() {
		return owner;
	}

	public void setOwner(String owner) {
		this.owner = owner;
	}

	public String getBoundary() {
		return boundary;
	}

	public void setBoundary(String boundary) {
		this.boundary = boundary;
	}

	public Timestamp getCreationTime() {
		return creationTime;
	}

	public void setCreationTime(Timestamp creationTime) {
		this.creationTime = creationTime;
	}

	public Timestamp getUpdateTime() {
		return updateTime;
	}

	public void setUpdateTime(Timestamp updateTime) {
		this.updateTime = updateTime;
	}

	public Timestamp getLastUseTime() {
		return lastUseTime;
	}

	public void setLastUseTime(Timestamp lastUseTime) {
		this.lastUseTime = lastUseTime;
	}

	@Override
	public int hashCode() {
		return Objects.hash(boundary, creationTime, description, id, lastUseTime, owner, query, title, updateTime);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof SparQLQuery)) {
			return false;
		}
		SparQLQuery other = (SparQLQuery) obj;
		return Objects.equals(boundary, other.boundary) && Objects.equals(creationTime, other.creationTime)
				&& Objects.equals(description, other.description) && Objects.equals(id, other.id)
				&& Objects.equals(lastUseTime, other.lastUseTime) && Objects.equals(owner, other.owner)
				&& Objects.equals(query, other.query) && Objects.equals(title, other.title)
				&& Objects.equals(updateTime, other.updateTime);
	}

	@Override
	public String toString() {
		return "SparQLQuery [" + (id != null ? "id=" + id + ", " : "") + (title != null ? "title=" + title + ", " : "")
				+ (description != null ? "description=" + description + ", " : "")
				+ (query != null ? "query=" + query + ", " : "") + (owner != null ? "owner=" + owner + ", " : "")
				+ (boundary != null ? "boundary=" + boundary + ", " : "")
				+ (creationTime != null ? "creationTime=" + creationTime + ", " : "")
				+ (updateTime != null ? "updateTime=" + updateTime + ", " : "")
				+ (lastUseTime != null ? "lastUseTime=" + lastUseTime : "") + "]";
	}
}
