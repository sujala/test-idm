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
	public ResponseBuilder validateToken(HttpServletRequest request, String tokenId, String belongsTo,
			String type, HttpHeaders httpHeaders) throws IOException {
		return Response.status(Status.NOT_FOUND);
	}

	@Override
	public ResponseBuilder revokeToken(HttpServletRequest request, String tokenId, HttpHeaders httpHeaders)
			throws IOException {
		return Response.status(Status.NOT_FOUND);
	}

	@Override
	public ResponseBuilder authenticate(HttpServletRequest request, HttpServletResponse response,
			HttpHeaders httpHeaders, String body) throws IOException {
		return Response.status(Status.NOT_FOUND);
	}

	@Override
	public ResponseBuilder adminAuthenticate(HttpServletRequest request, HttpServletResponse response,
			HttpHeaders httpHeaders, String body) throws IOException {
		return Response.status(Status.NOT_FOUND);
	}

	@Override
	public ResponseBuilder createUser(HttpServletRequest request, HttpHeaders httpHeaders, UriInfo uriInfo, User user)
			throws IOException, JAXBException {
		return Response.status(Status.NOT_FOUND);
	}

	@Override
	public ResponseBuilder getUser(HttpServletRequest request, String userId, HttpHeaders httpHeaders)
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
	public ResponseBuilder deleteUser(HttpServletRequest request, String userId, HttpHeaders httpHeaders)
			throws IOException {
		return Response.status(Status.NOT_FOUND);
	}

	@Override
	public ResponseBuilder updateUser(HttpServletRequest request, String userId, HttpHeaders httpHeaders,
			User user) throws IOException, JAXBException {
		return Response.status(Status.NOT_FOUND);
	}

	@Override
	public ResponseBuilder getUserEnabled(HttpServletRequest request, String userId, HttpHeaders httpHeaders)
			throws IOException {
		return Response.status(Status.NOT_FOUND);
	}

	@Override
	public ResponseBuilder setUserEnabled(HttpServletRequest request, String userId,
			UserWithOnlyEnabled user, HttpHeaders httpHeaders)
			throws IOException, JAXBException {
		return Response.status(Status.NOT_FOUND);
	}

	@Override
	public ResponseBuilder getUserKey(HttpServletRequest request, String userId, HttpHeaders httpHeaders)
			throws IOException {
		return Response.status(Status.NOT_FOUND);
	}

	@Override
	public ResponseBuilder setUserKey(HttpServletRequest request, String userId, HttpHeaders httpHeaders,
			UserWithOnlyKey user) throws IOException, JAXBException {
		return Response.status(Status.NOT_FOUND);
	}

	@Override
	public ResponseBuilder getServiceCatalog(HttpServletRequest request, String userId,
			HttpHeaders httpHeaders) throws IOException {
		return Response.status(Status.NOT_FOUND);
	}

	@Override
	public ResponseBuilder getBaseURLRefs(HttpServletRequest request, String userId, HttpHeaders httpHeaders)
			throws IOException {
		return Response.status(Status.NOT_FOUND);
	}

	@Override
	public ResponseBuilder addBaseURLRef(HttpServletRequest request, String userId,
			HttpHeaders httpHeaders, UriInfo uriInfo, BaseURLRef baseUrlRef)
			throws IOException, JAXBException {
		return Response.status(Status.NOT_FOUND);
	}

	@Override
	public ResponseBuilder getBaseURLRef(HttpServletRequest request, String userId, String baseURLId,
			HttpHeaders httpHeaders) throws IOException {
		return Response.status(Status.NOT_FOUND);
	}

	@Override
	public ResponseBuilder deleteBaseURLRef(HttpServletRequest request, String userId, String baseURLId,
			HttpHeaders httpHeaders) throws IOException {
		return Response.status(Status.NOT_FOUND);
	}

	@Override
	public ResponseBuilder getUserGroups(HttpServletRequest request, String userId, HttpHeaders httpHeaders)
			throws IOException {
		return Response.status(Status.NOT_FOUND);
	}

	@Override
	public ResponseBuilder getBaseURLs(HttpServletRequest request, String serviceName,
			HttpHeaders httpHeaders) throws IOException {
		return Response.status(Status.NOT_FOUND);
	}

	@Override
	public ResponseBuilder getBaseURLId(HttpServletRequest request, int baseURLId, String serviceName,
			HttpHeaders httpHeaders) throws IOException {
		return Response.status(Status.NOT_FOUND);
	}

	@Override
	public ResponseBuilder getEnabledBaseURL(HttpServletRequest request, String serviceName,
			HttpHeaders httpHeaders) throws IOException {
		return Response.status(Status.NOT_FOUND);
	}

	@Override
	public ResponseBuilder addBaseURL(HttpServletRequest request,
			HttpHeaders httpHeaders, BaseURL baseUrl) {
		return Response.status(Status.NOT_FOUND);
	}

	@Override
	public ResponseBuilder migrate(HttpServletRequest request, String user, HttpHeaders httpHeaders,
			String body) throws IOException {
		return Response.status(Status.NOT_FOUND);
	}

	@Override
	public ResponseBuilder unmigrate(HttpServletRequest request, String user, HttpHeaders httpHeaders,
			String body) throws IOException {
		return Response.status(Status.NOT_FOUND);
	}

	@Override
	public ResponseBuilder all(HttpServletRequest request, HttpHeaders httpHeaders, String body)
			throws IOException {
		return Response.status(Status.NOT_FOUND);
	}

    @Override
    public ResponseBuilder extensions(HttpHeaders httpHeaders) throws IOException {
        return  Response.status(Status.NOT_FOUND);
    }
}
