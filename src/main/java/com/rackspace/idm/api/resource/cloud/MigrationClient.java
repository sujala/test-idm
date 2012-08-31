package com.rackspace.idm.api.resource.cloud;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.ws.rs.core.MediaType;
import javax.xml.bind.JAXBException;

import org.apache.http.HttpException;
import org.apache.http.HttpHeaders;
import org.apache.ws.commons.util.Base64;
import org.openstack.docs.identity.api.ext.os_kscatalog.v1.EndpointTemplateList;
import org.openstack.docs.identity.api.v2.AuthenticateResponse;
import org.openstack.docs.identity.api.v2.AuthenticationRequest;
import org.openstack.docs.identity.api.v2.CredentialListType;
import org.openstack.docs.identity.api.v2.EndpointList;
import org.openstack.docs.identity.api.v2.ObjectFactory;
import org.openstack.docs.identity.api.v2.PasswordCredentialsRequiredUsername;
import org.openstack.docs.identity.api.v2.RoleList;
import org.openstack.docs.identity.api.v2.Tenants;
import org.openstack.docs.identity.api.v2.User;
import org.openstack.docs.identity.api.v2.UserList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Groups;
import com.rackspace.docs.identity.api.ext.rax_kskey.v1.ApiKeyCredentials;
import com.rackspace.docs.identity.api.ext.rax_ksqa.v1.SecretQA;
import com.rackspace.idm.domain.entity.EndPoints;
import com.rackspace.idm.exception.IdmException;
import com.rackspacecloud.docs.auth.api.v1.BaseURLList;
import com.sun.jersey.api.client.Client;

import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.client.urlconnection.HTTPSProperties;

public class MigrationClient {

    private static final String USERS = "users/";
    private static final String FAILED_TO_CALL_CLOUD = "failed to call cloud";
    private static final String X_AUTH_TOKEN = "X-AUTH-TOKEN";
	
	private ObjectFactory objectFactory = new ObjectFactory();

    private Logger logger = LoggerFactory.getLogger(MigrationClient.class);
	
	private Client client;
	private Client client11;
	
    private String cloud11Host = "";
	private String cloud20Host = "";
	
	private boolean ignoreSSL = true;
	
	public MigrationClient() {
        
		SSLContext sc = null;
        
        if (ignoreSSL) {
	        TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager(){
	            public X509Certificate[] getAcceptedIssuers(){return null;}
	            public void checkClientTrusted(X509Certificate[] certs, String authType){}
	            public void checkServerTrusted(X509Certificate[] certs, String authType){}
	        }};
	
	        // Install the all-trusting trust manager
	        try {
	            ClientConfig config = new DefaultClientConfig();
	            
	            sc = SSLContext.getInstance("TLS");
	            sc.init(null, trustAllCerts, new SecureRandom());

	            config.getProperties().put(HTTPSProperties.PROPERTY_HTTPS_PROPERTIES, new HTTPSProperties(
	                new HostnameVerifier() {
	                    @Override
	                    public boolean verify( String s, SSLSession sslSession ) {
	                    	return false;
	                    }
	                }, sc
	            ));
	            
	            client = Client.create(config);
	            client11 = Client.create(config);
	        } catch (Exception e) {
	            ;
	        }
        } else {
        	client = Client.create();	
        }
	}
	
	public AuthenticateResponse authenticateWithPassword(String username, String password) throws URISyntaxException, HttpException, IOException, JAXBException {
		
		ObjectMarshaller<AuthenticationRequest> marshaller = new ObjectMarshaller<AuthenticationRequest>();
		
        AuthenticationRequest request = new AuthenticationRequest();
        PasswordCredentialsRequiredUsername passwordCredentialsRequiredUsername = new PasswordCredentialsRequiredUsername();
        passwordCredentialsRequiredUsername.setUsername(username);
        passwordCredentialsRequiredUsername.setPassword(password);
        
        request.setCredential(objectFactory.createPasswordCredentials(passwordCredentialsRequiredUsername));
        
        String requestString = marshaller.marshal(objectFactory.createAuth(request), AuthenticationRequest.class);

        WebResource webResource = client.resource(cloud20Host + "tokens");

        String response = webResource
            .type(MediaType.APPLICATION_XML)
            .accept(MediaType.APPLICATION_XML)
            .post(String.class, requestString);
        
        ObjectMarshaller<AuthenticateResponse> unmarshaller = new ObjectMarshaller<AuthenticateResponse>();
        return unmarshaller.unmarshal(response, AuthenticateResponse.class);
	}

    public AuthenticateResponse authenticateWithApiKey(String username, String apiKey) throws URISyntaxException, HttpException, IOException, JAXBException {

		ObjectMarshaller<AuthenticationRequest> marshaller = new ObjectMarshaller<AuthenticationRequest>();

        AuthenticationRequest request = new AuthenticationRequest();
        ApiKeyCredentials apiKeyCredentials = new ApiKeyCredentials();
        apiKeyCredentials.setUsername(username);
        apiKeyCredentials.setApiKey(apiKey);

        request.setCredential(objectFactory.createCredential(apiKeyCredentials));

        String requestString = marshaller.marshal(objectFactory.createAuth(request), AuthenticationRequest.class);

        WebResource webResource = client.resource(cloud20Host + "tokens");

        String response = webResource
            .type(MediaType.APPLICATION_XML)
            .accept(MediaType.APPLICATION_XML)
            .post(String.class, requestString);

        ObjectMarshaller<AuthenticateResponse> unmarshaller = new ObjectMarshaller<AuthenticateResponse>();
        return unmarshaller.unmarshal(response, AuthenticateResponse.class);
	}
	
    public Tenants getTenants(String token) throws URISyntaxException, HttpException, IOException, JAXBException {
        WebResource webResource = client.resource(cloud20Host + "tenants");

        String response = webResource
            .type(MediaType.APPLICATION_XML)
            .accept(MediaType.APPLICATION_XML)
            .header(X_AUTH_TOKEN, token)
            .get(String.class);

        ObjectMarshaller<Tenants> unmarshaller = new ObjectMarshaller<Tenants>();
        return unmarshaller.unmarshal(response, Tenants.class);
    }

    public EndpointList getEndpointsByToken(String adminToken, String token) throws URISyntaxException, HttpException, IOException, JAXBException {
        WebResource webResource = client.resource(cloud20Host + "tokens/" + token + "/endpoints");

        String response = webResource
            .type(MediaType.APPLICATION_XML)
            .accept(MediaType.APPLICATION_XML)
            .header(X_AUTH_TOKEN, adminToken)
            .get(String.class);

        ObjectMarshaller<EndpointList> unmarshaller = new ObjectMarshaller<EndpointList>();
        return unmarshaller.unmarshal(response, EndPoints.class);
    }

    public User getUser(String token, String username) throws URISyntaxException, HttpException, IOException, JAXBException {
        String response = "";
        try {
            WebResource webResource = client.resource(cloud20Host + "users?name=" + username);

            response = webResource
                .type(MediaType.APPLICATION_XML)
                .accept(MediaType.APPLICATION_XML)
                .header(X_AUTH_TOKEN, token)
                .get(String.class);

            ObjectMarshaller<User> unmarshaller = new ObjectMarshaller<User>();
            return unmarshaller.unmarshal(response, User.class);
        } catch (Exception ex) {
            logger.error(response);
            return null;
        }
    }

    public RoleList getRolesForUser(String token, String userId) {
        try {
            WebResource webResource = client.resource(cloud20Host + USERS + userId + "/roles");

            String response = webResource
                .type(MediaType.APPLICATION_XML)
                .accept(MediaType.APPLICATION_XML)
                .header(X_AUTH_TOKEN, token)
                .get(String.class);

            ObjectMarshaller<RoleList> unmarshaller = new ObjectMarshaller<RoleList>();
            return unmarshaller.unmarshal(response, RoleList.class);
        } catch (Exception e) {
            logger.info("getRolesForUSer failed with exception {}", e.getMessage());
            throw new IdmException(FAILED_TO_CALL_CLOUD, e);
        }
    }
    
    public UserList getUsers(String token) throws URISyntaxException, HttpException, IOException {
        UserList userList = new UserList();

        WebResource webResource = client.resource(cloud20Host + "users");

        String response = webResource
            .type(MediaType.APPLICATION_XML)
            .accept(MediaType.APPLICATION_XML)
            .header(X_AUTH_TOKEN, token)
            .get(String.class);

    	try {
            ObjectMarshaller<User> unmarshaller = new ObjectMarshaller<User>();
            User user = unmarshaller.unmarshal(response, User.class);
            userList.getUser().add(user);
        }catch(Exception ex1){
            try {
                ObjectMarshaller<UserList> unmarshaller = new ObjectMarshaller<UserList>();
                userList = unmarshaller.unmarshal(response, UserList.class);
            }catch (Exception ex2){
                logger.info("getUsers failed with exception {}", ex2.getMessage());
                throw new IdmException(ex2);
            }
        }

    	return userList;
    }

    public SecretQA getSecretQA(String token, String userId) throws URISyntaxException, HttpException, IOException, JAXBException {
        WebResource webResource = client.resource(cloud20Host + USERS + userId +"/RAX-KSQA/secretqa");

        String response = webResource
            .type(MediaType.APPLICATION_XML)
            .accept(MediaType.APPLICATION_XML)
            .header(X_AUTH_TOKEN, token)
            .get(String.class);

        ObjectMarshaller<SecretQA> unmarshaller = new ObjectMarshaller<SecretQA>();
        return unmarshaller.unmarshal(response, Groups.class);
    }

    public Groups getGroups(String token) {

        String response;
        try {
            WebResource webResource = client.resource(cloud20Host + "RAX-GRPADM/groups");
            
            response = webResource
                .type(MediaType.APPLICATION_XML)
                .accept(MediaType.APPLICATION_XML)
                .header(X_AUTH_TOKEN, token)
                .get(String.class);

            ObjectMarshaller<Groups> unmarshaller = new ObjectMarshaller<Groups>();
            return unmarshaller.unmarshal(response, Groups.class);
        } catch (Exception e) {
            logger.info("getGroups failed with exception {}", e.getMessage());
            throw new IdmException(FAILED_TO_CALL_CLOUD, e);
        }
    }

    public Groups getGroupsForUser(String token, String userId) {
        try {
            WebResource webResource = client.resource(cloud20Host + USERS + userId + "/RAX-KSGRP");
            String response = webResource
                .type(MediaType.APPLICATION_XML)
                .accept(MediaType.APPLICATION_XML)
                .header(X_AUTH_TOKEN, token)
                .get(String.class);

            ObjectMarshaller<Groups> unmarshaller = new ObjectMarshaller<Groups>();
            return unmarshaller.unmarshal(response, Groups.class);
        } catch (Exception e) {
            logger.info("getGroupsForUser failed with exception {}", e.getMessage());
            throw new IdmException(FAILED_TO_CALL_CLOUD, e);
        }
    }

    public CredentialListType getUserCredentials(String token, String userId) throws URISyntaxException, HttpException, IOException, JAXBException {
        WebResource webResource = client.resource(cloud20Host + USERS + userId + "/OS-KSADM/credentials");
        String response = webResource
            .type(MediaType.APPLICATION_XML)
            .accept(MediaType.APPLICATION_XML)
            .header(X_AUTH_TOKEN, token)
            .get(String.class);

        CredentialListType credentialListType = new CredentialListType();
        ObjectMarshaller<CredentialListType> unmarshaller = new ObjectMarshaller<CredentialListType>();
        credentialListType = unmarshaller.unmarshal(response, CredentialListType.class);
        return credentialListType;
    }

    public com.rackspacecloud.docs.auth.api.v1.User getUserTenantsBaseUrls(String username, String password, String userId) throws URISyntaxException, HttpException, IOException, JAXBException {
        String response;
        try {
            client11.addFilter(new HTTPBasicAuthFilter(username, password));
            WebResource webResource = client11.resource(cloud11Host + USERS + userId + ".xml");
            response = webResource
                .get(String.class);

            ObjectMarshaller<com.rackspacecloud.docs.auth.api.v1.User> unmarshaller = new ObjectMarshaller<com.rackspacecloud.docs.auth.api.v1.User>();
            return unmarshaller.unmarshal(response, com.rackspacecloud.docs.auth.api.v1.User.class);
        } catch (Exception e) {
            logger.info("get User Base Urls call to cloud failed: {}", e.getMessage());
            throw new IdmException(FAILED_TO_CALL_CLOUD, e);
        }
    }

    public BaseURLList getBaseUrls(String username, String password) throws URISyntaxException, HttpException, IOException, JAXBException {
        client11.addFilter(new HTTPBasicAuthFilter(username, password));
        WebResource webResource = client11.resource(cloud11Host + "baseURLs.xml");
        String response = webResource
            .get(String.class);

        ObjectMarshaller<BaseURLList> unmarshaller = new ObjectMarshaller<BaseURLList>();
        return unmarshaller.unmarshal(response, BaseURLList.class);
    }

    public RoleList getRoles(String adminToken) throws URISyntaxException, HttpException, IOException, JAXBException {
        WebResource webResource = client.resource(cloud20Host + "OS-KSADM/roles");
        
        String response = webResource
            .accept(MediaType.APPLICATION_XML)
            .header(X_AUTH_TOKEN, adminToken)
            .get(String.class);

    	ObjectMarshaller<RoleList> unmarshaller = new ObjectMarshaller<RoleList>();
        return unmarshaller.unmarshal(response, RoleList.class);
    }

    public EndpointTemplateList getEndpointTemplates(String adminToken) {
        String response;
        try {
            WebResource webResource = client.resource(cloud20Host + "OS-KSCATALOG/endpointTemplates");
            response = webResource
                .type(MediaType.APPLICATION_XML)
                .accept(MediaType.APPLICATION_XML)
                .header(X_AUTH_TOKEN, adminToken)
                .get(String.class);

            ObjectMarshaller<EndpointTemplateList> unmarshaller = new ObjectMarshaller<EndpointTemplateList>();
            return unmarshaller.unmarshal(response, EndPoints.class);
        } catch (Exception e) {
            logger.info("get EndpointTemplateList call to cloud failed: {}", e.getMessage());
            throw new IdmException(FAILED_TO_CALL_CLOUD, e);
        }
    }

	public void setCloud20Host(String host) {
		this.cloud20Host = host;
	}

	public void setCloud11Host(String host) {
		this.cloud11Host = host;
	}

    public void setClient(Client client) {
        this.client = client;
    }
}
