package com.rackspace.idm.api.serviceprofile;

import javax.ws.rs.core.UriInfo;

import org.apache.commons.configuration.Configuration;

public class ServiceProfileConfig {
	private final Configuration config;
	private final UriInfo uriInfo;

	public ServiceProfileConfig(Configuration config, final UriInfo uriInfo) {
		this.config = config;
		this.uriInfo = uriInfo;
	}

	String getBaseUrl() {
		return uriInfo.getBaseUri().toString();
	}
	
	String getCloudAuth10ServiceDocumentation() {
		return config.getString("serviceProfile.cloudAuth10.documentation");
	}

	String getCloudAuth11MediaType() {
		return config.getString("serviceProfile.cloudAuth11.mediaType");
	}

	String getCloudAuth11ServiceWadl() {
		return config.getString("serviceProfile.cloudAuth11.wadl.servie");
	}

	String getCloudAuth11AdminWadl() {
		return config.getString("serviceProfile.cloudAuth11.wadl.admin");
	}

	String getCloudAuth11ServiceDocumentation() {
		return config.getString("serviceProfile.cloudAuth11.documentation.service");
	}

	String getCloudAuth11AdminDocumentation() {
		return config.getString("serviceProfile.cloudAuth11.documentation.admin");
	}

	String getCloudAuth20MediaType() {
		return config.getString("serviceProfile.cloudAuth20.mediaType");
	}

	String getCloudAuth20ServiceWadl() {
		return config.getString("serviceProfile.cloudAuth20.wadl.service");
	}

	String getCloudAuth20AdminWadl() {
		return config.getString("serviceProfile.cloudAuth20.wadl.admin");
	}

	String getCloudAuth20ServiceDocumentation() {
		return config.getString("serviceProfile.cloudAuth20.documentation.service");
	}

	String getCloudAuth20AdminDocumentation() {
		return config.getString("serviceProfile.cloudAuth20.documentation.admin");
	}
}
