package com.rackspace.idm.api.resource.cloud.v11;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;
import javax.xml.bind.JAXBException;

import org.springframework.stereotype.Component;

import com.rackspacecloud.docs.auth.api.v1.BaseURL;
import com.rackspacecloud.docs.auth.api.v1.BaseURLRef;
import com.rackspacecloud.docs.auth.api.v1.User;
import com.rackspacecloud.docs.auth.api.v1.UserWithOnlyEnabled;
import com.rackspacecloud.docs.auth.api.v1.UserWithOnlyKey;

@Component
public class DummyCloud11Service implements Cloud11Service {

	@Override
	public ResponseBuilder validateToken(String tokenId, String belongsTo,
			String type, HttpHeaders httpHeaders) throws IOException {
		return Response.status(Status.NOT_FOUND);
	}

	@Override
	public ResponseBuilder revokeToken(String tokenId, HttpHeaders httpHeaders)
			throws IOException {
		return Response.status(Status.NOT_FOUND);
	}

	@Override
	public ResponseBuilder authenticate(HttpServletResponse response,
			HttpHeaders httpHeaders, String body) throws IOException {
		return Response.status(Status.NOT_FOUND);
	}

	@Override
	public ResponseBuilder adminAuthenticate(HttpServletResponse response,
			HttpHeaders httpHeaders, String body) throws IOException {
		return Response.status(Status.NOT_FOUND);
	}

	@Override
	public ResponseBuilder createUser(HttpHeaders httpHeaders, User user)
			throws IOException, JAXBException {
		return Response.status(Status.NOT_FOUND);
	}

	@Override
	public ResponseBuilder getUser(String userId, HttpHeaders httpHeaders)
			throws IOException {
		return Response.status(Status.NOT_FOUND);
	}

	@Override
	public ResponseBuilder getUserFromMossoId(HttpServletRequest request,
			int mossoId, HttpHeaders httpHeaders) throws IOException {
		return Response.status(Status.NOT_FOUND);
	}

	@Override
	public ResponseBuilder getUserFromNastId(HttpServletRequest request,
			String nastId, HttpHeaders httpHeaders) throws IOException {
		return Response.status(Status.NOT_FOUND);
	}

	@Override
	public ResponseBuilder deleteUser(String userId, HttpHeaders httpHeaders)
			throws IOException {
		return Response.status(Status.NOT_FOUND);
	}

	@Override
	public ResponseBuilder updateUser(String userId, HttpHeaders httpHeaders,
			User user) throws IOException, JAXBException {
		return Response.status(Status.NOT_FOUND);
	}

	@Override
	public ResponseBuilder getUserEnabled(String userId, HttpHeaders httpHeaders)
			throws IOException {
		return Response.status(Status.NOT_FOUND);
	}

	@Override
	public ResponseBuilder setUserEnabled(String userId,
			UserWithOnlyEnabled user, HttpHeaders httpHeaders)
			throws IOException, JAXBException {
		return Response.status(Status.NOT_FOUND);
	}

	@Override
	public ResponseBuilder getUserKey(String userId, HttpHeaders httpHeaders)
			throws IOException {
		return Response.status(Status.NOT_FOUND);
	}

	@Override
	public ResponseBuilder setUserKey(String userId, HttpHeaders httpHeaders,
			UserWithOnlyKey user) throws IOException, JAXBException {
		return Response.status(Status.NOT_FOUND);
	}

	@Override
	public ResponseBuilder getServiceCatalog(String userId,
			HttpHeaders httpHeaders) throws IOException {
		return Response.status(Status.NOT_FOUND);
	}

	@Override
	public ResponseBuilder getBaseURLRefs(String userId, HttpHeaders httpHeaders)
			throws IOException {
		return Response.status(Status.NOT_FOUND);
	}

	@Override
	public ResponseBuilder addBaseURLRef(String userId,
			HttpHeaders httpHeaders, UriInfo uriInfo, BaseURLRef baseUrlRef)
			throws IOException, JAXBException {
		return Response.status(Status.NOT_FOUND);
	}

	@Override
	public ResponseBuilder getBaseURLRef(String userId, String baseURLId,
			HttpHeaders httpHeaders) throws IOException {
		return Response.status(Status.NOT_FOUND);
	}

	@Override
	public ResponseBuilder deleteBaseURLRef(String userId, String baseURLId,
			HttpHeaders httpHeaders) throws IOException {
		return Response.status(Status.NOT_FOUND);
	}

	@Override
	public ResponseBuilder getUserGroups(String userId, HttpHeaders httpHeaders)
			throws IOException {
		return Response.status(Status.NOT_FOUND);
	}

	@Override
	public ResponseBuilder getBaseURLs(String serviceName,
			HttpHeaders httpHeaders) throws IOException {
		return Response.status(Status.NOT_FOUND);
	}

	@Override
	public ResponseBuilder getBaseURLId(int baseURLId, String serviceName,
			HttpHeaders httpHeaders) throws IOException {
		return Response.status(Status.NOT_FOUND);
	}

	@Override
	public ResponseBuilder getEnabledBaseURL(String serviceName,
			HttpHeaders httpHeaders) throws IOException {
		return Response.status(Status.NOT_FOUND);
	}

	@Override
	public ResponseBuilder addBaseURL(HttpServletRequest request,
			HttpHeaders httpHeaders, BaseURL baseUrl) {
		return Response.status(Status.NOT_FOUND);
	}

	@Override
	public ResponseBuilder migrate(String user, HttpHeaders httpHeaders,
			String body) throws IOException {
		return Response.status(Status.NOT_FOUND);
	}

	@Override
	public ResponseBuilder unmigrate(String user, HttpHeaders httpHeaders,
			String body) throws IOException {
		return Response.status(Status.NOT_FOUND);
	}

	@Override
	public ResponseBuilder all(HttpHeaders httpHeaders, String body)
			throws IOException {
		return Response.status(Status.NOT_FOUND);
	}
}
