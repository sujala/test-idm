package com.rackspace.idm.domain.entity;

public class FilterParam {

	private FilterParamName param;
	private Object value;
	
	public FilterParam() {
	}
	
	public FilterParam(FilterParamName param, Object value) {
		this.param = param;
		this.value = value;
	}
	
	public enum FilterParamName {
		USERNAME, RCN, APPLICATION_ID, TENANT_ID, ROLE_NAME, ROLE_ID, APPLICATION_NAME, DOMAIN_ID, GROUP_ID, CN, MIGRATED, ENABLED, IN_MIGRATION
	}

	public FilterParamName getParam() {
		return param;
	}

	public void setParam(FilterParamName param) {
		this.param = param;
	}

	public Object getValue() {
		return value;
	}

	public String getStrValue() {
		return value.toString();
	}
	
	public void setValue(Object value) {
		this.value = value;
	}
}
