package com.rackspace.idm.domain.entity;

public final class Entity {
	
	public static final Integer USER = 1;
	public static final Integer APPLICATION = 2;
	public static final Integer ROLE = 3;
	public static final Integer CUSTOMER = 4;
	
	private Integer entityType;
	private String entityId;
	
	public static Entity createUserEntity(String entityId) {
		return new Entity(USER, entityId);
	}
	
	public static Entity createApplicationEntity(String entityId) {
		return new Entity(APPLICATION, entityId);
	}
	
	private Entity(Integer entityType, String entityId) {
		this.entityType = entityType;
		this.entityId = entityId;
	}

	public Integer getEntityType() {
		return entityType;
	}

	public String getEntityId() {
		return entityId;
	}
}
