package com.rackspace.idm.api.resource.cloud;

import java.io.IOException;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import org.springframework.stereotype.Component;

@Component
public class DefaultCloud11Service implements Cloud11Service {

	@Override
	public Response.ResponseBuilder validateToken(String belongsTo, String type,
			HttpHeaders httpHeaders) throws IOException {
		// TODO Auto-generated method stub
		throw new IOException("Not Implemented");
	}
}
