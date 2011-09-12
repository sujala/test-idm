package com.rackspace.idm.api.resource.cloud;

import com.rackspace.idm.cloudv11.jaxb.*;
import org.apache.commons.configuration.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriInfo;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;

@Component
public class DelegateCloud11Service implements Cloud11Service {

    public void setCloudClient(CloudClient cloudClient) {
        this.cloudClient = cloudClient;
    }

    @Autowired
    private CloudClient cloudClient;

    public void setConfig(Configuration config) {
        this.config = config;
    }

    @Autowired
    private Configuration config;

    public void setDefaultCloud11Service(DefaultCloud11Service defaultCloud11Service) {
        this.defaultCloud11Service = defaultCloud11Service;
    }

    @Autowired
    private DefaultCloud11Service defaultCloud11Service;

    public static void setOBJ_FACTORY(ObjectFactory OBJ_FACTORY) {
        DelegateCloud11Service.OBJ_FACTORY = OBJ_FACTORY;
    }

    private static com.rackspace.idm.cloudv11.jaxb.ObjectFactory OBJ_FACTORY = new com.rackspace.idm.cloudv11.jaxb.ObjectFactory();

    public void setMarshaller(Marshaller marshaller) {
        this.marshaller = marshaller;
    }

    private Marshaller marshaller;

    public DelegateCloud11Service() throws JAXBException {
        JAXBContext jaxbContext = JAXBContext
            .newInstance("com.rackspace.idm.cloudv11.jaxb");
        marshaller = jaxbContext.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
    }

    @Override
    public Response.ResponseBuilder validateToken(String tokenId,
        String belongsTo, String type, HttpHeaders httpHeaders)
        throws IOException {

        Response.ResponseBuilder serviceResponse = defaultCloud11Service
            .validateToken(tokenId, belongsTo, type, httpHeaders);
        // We have to clone the ResponseBuilder from above because once we build
        // it below its gone.
        Response.ResponseBuilder clonedServiceResponse = serviceResponse
            .clone();
        if (clonedServiceResponse.build().getStatus() == HttpServletResponse.SC_NOT_FOUND) {
            HashMap<String, String> queryParams = new HashMap<String, String>();
            queryParams.put("belongsTo", belongsTo);
            queryParams.put("type", type);
            String path = getCloudAuthV11Url().concat(
                getPath("token/" + tokenId, queryParams));
            return cloudClient.get(path, httpHeaders);
        }
        return serviceResponse;
    }

    @Override
    public Response.ResponseBuilder authenticate(HttpServletResponse response,
        HttpHeaders httpHeaders, String body) throws IOException {
        Response.ResponseBuilder serviceResponse = defaultCloud11Service.authenticate(response, httpHeaders, body);
        // We have to clone the ResponseBuilder from above because once we build
        // it below its gone.
        Response.ResponseBuilder clonedServiceResponse = serviceResponse.clone();
        if (clonedServiceResponse.build().getStatus() == HttpServletResponse.SC_NOT_FOUND) {
            return cloudClient.post(getCloudAuthV11Url().concat("auth"), httpHeaders, body);
        }
        return serviceResponse;
    }

    @Override
    public Response.ResponseBuilder adminAuthenticate(
        HttpServletResponse response, HttpHeaders httpHeaders, String body)
        throws IOException {
        Response.ResponseBuilder serviceResponse = defaultCloud11Service.adminAuthenticate(response, httpHeaders, body);
        // We have to clone the ResponseBuilder from above because once we build
        // it below its gone.
        Response.ResponseBuilder clonedServiceResponse = serviceResponse
            .clone();
        int status = clonedServiceResponse.build().getStatus();
        if (status == HttpServletResponse.SC_NOT_FOUND) {
            return cloudClient.post(getCloudAuthV11Url().concat("auth-admin"),
                httpHeaders, body);
        }
        return serviceResponse;
    }

    @Override
    public Response.ResponseBuilder revokeToken(String tokenId,
        HttpHeaders httpHeaders) throws IOException {

        Response.ResponseBuilder serviceResponse = defaultCloud11Service
            .revokeToken(tokenId, httpHeaders);
        // We have to clone the ResponseBuilder from above because once we build
        // it below its gone.
        Response.ResponseBuilder clonedServiceResponse = serviceResponse
            .clone();
        if (clonedServiceResponse.build().getStatus() == HttpServletResponse.SC_NOT_FOUND) {
            return cloudClient.delete(
                getCloudAuthV11Url().concat("token/" + tokenId), httpHeaders);
        }
        return serviceResponse;
    }

    @Override
    public Response.ResponseBuilder getUserFromMossoId(
        HttpServletRequest request, int mossoId, HttpHeaders httpHeaders)
        throws IOException {
        Response.ResponseBuilder serviceResponse = defaultCloud11Service
            .getUserFromMossoId(request, mossoId, httpHeaders);
        // We have to clone the ResponseBuilder from above because once we build
        // it below its gone.
        Response.ResponseBuilder clonedServiceResponse = serviceResponse
            .clone();
        if (clonedServiceResponse.build().getStatus() == HttpServletResponse.SC_NOT_FOUND) {
            return cloudClient.get(
                    getCloudAuthV11Url().concat("mosso/" + mossoId), httpHeaders);
        }
        return serviceResponse;
    }

    @Override
    public Response.ResponseBuilder getUserFromNastId(
        HttpServletRequest request, String nastId, HttpHeaders httpHeaders)
        throws IOException {
        Response.ResponseBuilder serviceResponse = defaultCloud11Service
            .getUserFromNastId(request, nastId, httpHeaders);
        // We have to clone the ResponseBuilder from above because once we build
        // it below its gone.
        Response.ResponseBuilder clonedServiceResponse = serviceResponse
            .clone();
        if (clonedServiceResponse.build().getStatus() == HttpServletResponse.SC_NOT_FOUND) {
            return cloudClient.get(getCloudAuthV11Url()
                    .concat("nast/" + nastId), httpHeaders);
        }
        return serviceResponse;
    }

    @Override
    public Response.ResponseBuilder getBaseURLs(String serviceName,
        HttpHeaders httpHeaders) throws IOException {
        Response.ResponseBuilder serviceResponse = defaultCloud11Service.getBaseURLs(serviceName, httpHeaders);

        Response.ResponseBuilder clonedServiceResponse = serviceResponse.clone();

        if (clonedServiceResponse.build().getStatus() == HttpServletResponse.SC_NOT_FOUND) {
            HashMap<String, String> queryParams = new HashMap<String, String>();
            queryParams.put("serviceName", serviceName);
            String path = getCloudAuthV11Url().concat(
                getPath("baseURLs", queryParams));
            return cloudClient.get(path, httpHeaders);
        }

        return serviceResponse;
    }

    @Override
    public Response.ResponseBuilder getBaseURLId(int baseURLId,
        String serviceName, HttpHeaders httpHeaders) throws IOException {

        Response.ResponseBuilder serviceResponse = defaultCloud11Service
            .getBaseURLId(baseURLId, serviceName, httpHeaders);
        // We have to clone the ResponseBuilder from above because once we build
        // it below its gone.
        Response.ResponseBuilder clonedServiceResponse = serviceResponse
            .clone();

        if (clonedServiceResponse.build().getStatus() == HttpServletResponse.SC_NOT_FOUND) {
            HashMap<String, String> queryParams = new HashMap<String, String>();
            queryParams.put("serviceName", serviceName);
            String path = getCloudAuthV11Url().concat(
                getPath("baseURLs/" + baseURLId, queryParams));
            return cloudClient.get(path, httpHeaders);
        }
        return serviceResponse;
    }

    @Override
    public Response.ResponseBuilder getEnabledBaseURL(String serviceName,
        HttpHeaders httpHeaders) throws IOException {
        try {
            //TODO
            return defaultCloud11Service.getEnabledBaseURL(serviceName,httpHeaders);
        } catch (Exception e) {
        }

        HashMap<String, String> queryParams = new HashMap<String, String>();
        queryParams.put("serviceName", serviceName);
        String path = getCloudAuthV11Url().concat(
                getPath("baseURLs/enabled", queryParams));
        return cloudClient.get(path, httpHeaders);
    }

    @Override
    public Response.ResponseBuilder migrate(String user,
        HttpHeaders httpHeaders, String body) throws IOException {
        try {
            return defaultCloud11Service.migrate(user, httpHeaders, body);
        } catch (Exception e) {
        }
        String path = "migration/" + user + "/migrate";
        return cloudClient.post(getCloudAuthV11Url().concat(path), httpHeaders,
                body);
    }

    @Override
    public Response.ResponseBuilder unmigrate(String user,
        HttpHeaders httpHeaders, String body) throws IOException {
        try {
            return defaultCloud11Service.unmigrate(user, httpHeaders, body);
        } catch (Exception e) {
        }
        String path = "migration/" + user + "/unmigrate";
        return cloudClient.post(getCloudAuthV11Url().concat(path), httpHeaders,
            body);
    }

    @Override
    public Response.ResponseBuilder all(HttpHeaders httpHeaders, String body)
        throws IOException {
        try {
            return defaultCloud11Service.all(httpHeaders, body);
        } catch (Exception e) {
        }
        String path = "migration/all";
        return cloudClient.post(getCloudAuthV11Url().concat(path), httpHeaders,
                body);
    }

    @Override
    public Response.ResponseBuilder createUser(HttpHeaders httpHeaders,
        User user) throws IOException {

        try {
            return defaultCloud11Service.createUser(httpHeaders, user);
        } catch (Exception e) {
        }

        String body = this.marshallObjectToString(OBJ_FACTORY.createUser(user));
        return cloudClient.post(getCloudAuthV11Url().concat("users"),
                httpHeaders, body);

    }

    @Override
    public Response.ResponseBuilder getUser(String userId,
        HttpHeaders httpHeaders) throws IOException {
        Response.ResponseBuilder serviceResponse = defaultCloud11Service
            .getUser(userId, httpHeaders);
        // We have to clone the ResponseBuilder from above because once we build
        // it below its gone.
        Response.ResponseBuilder clonedServiceResponse = serviceResponse
            .clone();

        if (clonedServiceResponse.build().getStatus() == HttpServletResponse.SC_NOT_FOUND) {
            serviceResponse = cloudClient.get(
                getCloudAuthV11Url().concat("users/" + userId), httpHeaders);
        }
        return serviceResponse;
    }

    @Override
    public Response.ResponseBuilder deleteUser(String userId,
        HttpHeaders httpHeaders) throws IOException {
        Response.ResponseBuilder serviceResponse = defaultCloud11Service
            .deleteUser(userId, httpHeaders);
        // We have to clone the ResponseBuilder from above because once we build
        // it below its gone.
        Response.ResponseBuilder clonedServiceResponse = serviceResponse
            .clone();

        if (clonedServiceResponse.build().getStatus() == HttpServletResponse.SC_NOT_FOUND) {
            return cloudClient.delete(
                getCloudAuthV11Url().concat("users/" + userId), httpHeaders);
        }
        return serviceResponse;
    }

    @Override
    public Response.ResponseBuilder updateUser(String userId,
        HttpHeaders httpHeaders, User user) throws IOException {
        Response.ResponseBuilder serviceResponse = defaultCloud11Service
            .updateUser(userId, httpHeaders, user);
        // We have to clone the ResponseBuilder from above because once we build
        // it below its gone.
        Response.ResponseBuilder clonedServiceResponse = serviceResponse
            .clone();

        if (clonedServiceResponse.build().getStatus() == HttpServletResponse.SC_NOT_FOUND) {
            String body = this.marshallObjectToString(OBJ_FACTORY
                .createUser(user));
            return cloudClient.put(
                getCloudAuthV11Url().concat("users/" + userId), httpHeaders,
                body);
        }
        return serviceResponse;
    }

    @Override
    public Response.ResponseBuilder getUserEnabled(String userId,
        HttpHeaders httpHeaders) throws IOException {

        Response.ResponseBuilder serviceResponse = defaultCloud11Service
            .getUserEnabled(userId, httpHeaders);
        // We have to clone the ResponseBuilder from above because once we build
        // it below its gone.
        Response.ResponseBuilder clonedServiceResponse = serviceResponse
            .clone();

        if (clonedServiceResponse.build().getStatus() == HttpServletResponse.SC_NOT_FOUND) {
            String path = "users/" + userId + "/enabled";
            return cloudClient.get(getCloudAuthV11Url().concat(path),
                    httpHeaders);
        }
        return serviceResponse;
    }

    @Override
    public Response.ResponseBuilder setUserEnabled(String userId,
        UserWithOnlyEnabled user, HttpHeaders httpHeaders) throws IOException {
        Response.ResponseBuilder serviceResponse = defaultCloud11Service
            .setUserEnabled(userId, user, httpHeaders);
        // We have to clone the ResponseBuilder from above because once we build
        // it below its gone.
        Response.ResponseBuilder clonedServiceResponse = serviceResponse
            .clone();

        if (clonedServiceResponse.build().getStatus() == HttpServletResponse.SC_NOT_FOUND) {
            String path = "users/" + userId + "/enabled";
            String body = this.marshallObjectToString(OBJ_FACTORY.createUser(user));
            return cloudClient.put(getCloudAuthV11Url().concat(path),
                    httpHeaders, body);
        }
        return serviceResponse;
    }

    @Override
    public Response.ResponseBuilder getUserKey(String userId,
        HttpHeaders httpHeaders) throws IOException {
        Response.ResponseBuilder serviceResponse = defaultCloud11Service
            .getUserKey(userId, httpHeaders);
        // We have to clone the ResponseBuilder from above because once we build
        // it below its gone.
        Response.ResponseBuilder clonedServiceResponse = serviceResponse
            .clone();

        if (clonedServiceResponse.build().getStatus() == HttpServletResponse.SC_NOT_FOUND) {
            String path = "users/" + userId + "/key";
            return cloudClient.get(getCloudAuthV11Url().concat(path),
                httpHeaders);
        }
        return serviceResponse;
    }

    @Override
    public Response.ResponseBuilder setUserKey(String userId,
        HttpHeaders httpHeaders, UserWithOnlyKey user) throws IOException {
        Response.ResponseBuilder serviceResponse = defaultCloud11Service
            .setUserKey(userId, httpHeaders, user);
        // We have to clone the ResponseBuilder from above because once we build
        // it below its gone.
        Response.ResponseBuilder clonedServiceResponse = serviceResponse
            .clone();

        if (clonedServiceResponse.build().getStatus() == HttpServletResponse.SC_NOT_FOUND) {
            String body = this.marshallObjectToString(OBJ_FACTORY.createUser(user));
            String path = "users/" + userId + "/key";
            return cloudClient.put(getCloudAuthV11Url().concat(path),
                    httpHeaders, body);
        }
        return serviceResponse;
    }

    @Override
    public Response.ResponseBuilder getServiceCatalog(String userId,
        HttpHeaders httpHeaders) throws IOException {

        Response.ResponseBuilder serviceResponse = defaultCloud11Service.getServiceCatalog(userId, httpHeaders);

        // We have to clone the ResponseBuilder from above because once we build
        // it below its gone.
        Response.ResponseBuilder clonedServiceResponse = serviceResponse.clone();

        if (clonedServiceResponse.build().getStatus() == HttpServletResponse.SC_NOT_FOUND) {
            String path = "users/" + userId + "/serviceCatalog";
            return cloudClient.get(getCloudAuthV11Url().concat(path), httpHeaders);
        }

        return serviceResponse;
    }

    @Override
    public Response.ResponseBuilder getBaseURLRefs(String userId,
        HttpHeaders httpHeaders) throws IOException {
        Response.ResponseBuilder serviceResponse = defaultCloud11Service
            .getBaseURLRefs(userId, httpHeaders);
        // We have to clone the ResponseBuilder from above because once we build
        // it below its gone.
        Response.ResponseBuilder clonedServiceResponse = serviceResponse
            .clone();

        if (clonedServiceResponse.build().getStatus() == HttpServletResponse.SC_NOT_FOUND) {
            String path = "users/" + userId + "/baseURLRefs";
            return cloudClient.get(getCloudAuthV11Url().concat(path),
                httpHeaders);
        }
        return serviceResponse;
    }

    @Override
    public ResponseBuilder addBaseURL(HttpServletRequest request,
        HttpHeaders httpHeaders, BaseURL baseUrl) {
        Response.ResponseBuilder serviceResponse = defaultCloud11Service
            .addBaseURL(request, httpHeaders, baseUrl);
        return serviceResponse;
    }

    @Override
    public Response.ResponseBuilder addBaseURLRef(String userId,
        HttpHeaders httpHeaders, UriInfo uriInfo, BaseURLRef baseUrlRef)
        throws IOException {
        Response.ResponseBuilder serviceResponse = defaultCloud11Service
            .addBaseURLRef(userId, httpHeaders, uriInfo, baseUrlRef);
        // We have to clone the ResponseBuilder from above because once we build
        // it below its gone.
        Response.ResponseBuilder clonedServiceResponse = serviceResponse
            .clone();

        if (clonedServiceResponse.build().getStatus() == HttpServletResponse.SC_NOT_FOUND) {
            String body = this.marshallObjectToString(OBJ_FACTORY
                .createBaseURLRef(baseUrlRef));
            String path = "users/" + userId + "/baseURLRefs";
            return cloudClient.post(getCloudAuthV11Url().concat(path),
                httpHeaders, body);
        }
        return serviceResponse;
    }

    @Override
    public Response.ResponseBuilder getBaseURLRef(String userId,
        String baseURLId, HttpHeaders httpHeaders) throws IOException {
        Response.ResponseBuilder serviceResponse = defaultCloud11Service
            .getBaseURLRef(userId, baseURLId, httpHeaders);
        // We have to clone the ResponseBuilder from above because once we build
        // it below its gone.
        Response.ResponseBuilder clonedServiceResponse = serviceResponse
            .clone();

        if (clonedServiceResponse.build().getStatus() == HttpServletResponse.SC_NOT_FOUND) {
            String path = "users/" + userId + "/baseURLRefs/" + baseURLId;
            return cloudClient.get(getCloudAuthV11Url().concat(path),
                httpHeaders);
        }
        return serviceResponse;
    }

    @Override
    public Response.ResponseBuilder deleteBaseURLRef(String userId,
        String baseURLId, HttpHeaders httpHeaders) throws IOException {
        Response.ResponseBuilder serviceResponse = defaultCloud11Service
            .deleteBaseURLRef(userId, baseURLId, httpHeaders);
        // We have to clone the ResponseBuilder from above because once we build
        // it below its gone.
        Response.ResponseBuilder clonedServiceResponse = serviceResponse
            .clone();

        if (clonedServiceResponse.build().getStatus() == HttpServletResponse.SC_NOT_FOUND) {
            String path = "users/" + userId + "/baseURLRefs/" + baseURLId;
            return cloudClient.delete(getCloudAuthV11Url().concat(path),
                httpHeaders);
        }
        return serviceResponse;
    }

    @Override
    public Response.ResponseBuilder getUserGroups(String userId,
        HttpHeaders httpHeaders) throws IOException {
        try {
            return defaultCloud11Service.getUserGroups(userId, httpHeaders);
        } catch (Exception e) {
        }
        String path = "user/" + userId + "/groups";
        return cloudClient.get(getCloudAuthV11Url().concat(path), httpHeaders);
    }

    private String getPath(String path, HashMap<String, String> queryParams) {
        String result = path;
        String queryString = "";

        if (queryParams != null) {
            for (String key : queryParams.keySet()) {
                if (queryParams.get(key) != null) {
                    queryString += key + "=" + queryParams.get(key) + "&";
                }
            }

            if (queryString.length() > 0) {
                result += "?"
                    + queryString.substring(0, queryString.length() - 1);
            }
        }

        return result;
    }

    private String getCloudAuthV11Url() {
        return config.getString("cloudAuth11url");
    }

    private String marshallObjectToString(Object jaxbObject) {

        StringWriter sw = new StringWriter();

        try {
            marshaller.marshal(jaxbObject, sw);
        } catch (JAXBException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return sw.toString();

    }
}
