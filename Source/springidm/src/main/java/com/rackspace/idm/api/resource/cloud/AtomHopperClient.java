package com.rackspace.idm.api.resource.cloud;

import com.rackspace.idm.api.converter.cloudv20.UserConverterCloudV20;
import com.rackspace.idm.domain.entity.User;
import org.apache.commons.configuration.Configuration;
import org.apache.http.HttpException;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.openstack.docs.identity.api.v2.ObjectFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import javax.ws.rs.core.MediaType;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.*;
import java.net.URISyntaxException;


/**
 * Created by IntelliJ IDEA.
 * User: jorge
 * Date: May 30, 2012
 * Time: 4:12:42 PM
 * To change this template use File | Settings | File Templates.
 */
@Component
public class AtomHopperClient {

    @Autowired
    private UserConverterCloudV20 userConverterCloudV20;

    @Autowired
    private Configuration config;

    DefaultHttpClient httpClient;

    private ObjectFactory objectFactory = new ObjectFactory();

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Async
    public void postUser(User user, String authToken, String userStatus) throws JAXBException, IOException, HttpException, URISyntaxException {
        try {
            org.openstack.docs.identity.api.v2.User user20 = userConverterCloudV20.toUser(user);
            Writer writer = marshal20User(user20);
            HttpResponse response;
            if (userStatus.equals("deleted")) {
                response = executePostRequest(authToken, writer, config.getString("atomHopperDeletedUrl"));
            } else if (userStatus.equals("disabled")) {
                response = executePostRequest(authToken, writer, config.getString("atomHopperDisabledUrl"));
            } else {
                response = null;
            }
            if (response.getStatusLine().getStatusCode() != 201) {
                logger.warn("Failed to create delete feed for user: " + user20.getUsername() + "with Id:" + user20.getId());
            }
        } catch (Exception e) {
            logger.warn("AtomHopperClient Exception: " + e);
        }
    }

    public HttpResponse executePostRequest(String authToken, Writer writer, String url) throws IOException {
        httpClient = new DefaultHttpClient();
        HttpPost httpPost = new HttpPost(url);
        httpPost.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_ATOM_XML);
        httpPost.setHeader("X-Auth-Token", authToken);
        httpPost.setEntity(createRequestEntity(writer.toString()));

        return httpClient.execute(httpPost);
    }

    public Writer marshal20User(org.openstack.docs.identity.api.v2.User user20) throws JAXBException {
        Writer writer = new StringWriter();
        JAXBContext jc = JAXBContext.newInstance(org.openstack.docs.identity.api.v2.User.class);
        Marshaller marshaller = jc.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FRAGMENT, true);
        marshaller.marshal(objectFactory.createUser(user20), writer);
        return writer;
    }

    public InputStreamEntity createRequestEntity(String s) throws UnsupportedEncodingException {
        InputStream isStream = new ByteArrayInputStream(createEntryPayload(s).getBytes("UTF-8"));
        InputStreamEntity reqEntity = new InputStreamEntity(isStream, -1);
        return reqEntity;
    }

    public String createEntryPayload(String userObject) {
        return "<entry xmlns=\"http://www.w3.org/2005/Atom\">" + userObject + "</entry>";
    }

    public void setUserConverterCloudV20(UserConverterCloudV20 userConverterCloudV20) {
        this.userConverterCloudV20 = userConverterCloudV20;
    }

    public void setConfig(Configuration config) {
        this.config = config;
    }

    public void setObjectFactory(ObjectFactory objectFactory) {
        this.objectFactory = objectFactory;
    }
}
