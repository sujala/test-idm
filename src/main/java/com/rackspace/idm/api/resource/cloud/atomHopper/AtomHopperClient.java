package com.rackspace.idm.api.resource.cloud.atomHopper;

import com.rackspace.docs.core.event.*;
import com.rackspace.docs.event.identity.user.CloudIdentityType;
import com.rackspace.docs.event.identity.user.ResourceTypes;
import com.rackspace.idm.domain.entity.Group;
import com.rackspace.idm.domain.entity.TenantRole;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.service.impl.DefaultGroupService;
import org.apache.commons.configuration.Configuration;
import org.apache.http.HttpException;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.w3._2005.atom.Title;
import org.w3._2005.atom.UsageContent;
import org.w3._2005.atom.UsageEntry;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.*;
import java.net.URISyntaxException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;


/**
 * Created by IntelliJ IDEA.
 * User: jorge
 * Date: May 30, 2012
 * Time: 4:12:42 PM
 * To change this template use File | Settings | File Templates.
 */
@Component
public class AtomHopperClient {

    public static final int PORT80 = 80;
    public static final int PORT443 = 443;
    public static final int MAX_TOTAL_CONNECTION = 200;
    public static final int DEFAULT_MAX_PER_ROUTE = 200;
    @Autowired
    private Configuration config;

    @Autowired
    private DefaultGroupService defaultGroupService;

    private HttpClient httpClient;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());




    public AtomHopperClient() {
        try {
            SSLSocketFactory sslsf = new SSLSocketFactory(new TrustStrategy() {
                @Override
                public boolean isTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
                    return true;
                }
            });
            SchemeRegistry schemeRegistry = new SchemeRegistry();
            schemeRegistry.register(
                    new Scheme("http", PORT80, PlainSocketFactory.getSocketFactory()));
            schemeRegistry.register(
                    new Scheme("https", PORT443, sslsf));

            PoolingClientConnectionManager cm = new PoolingClientConnectionManager(schemeRegistry);
            // Increase max total connection to 200
            cm.setMaxTotal(MAX_TOTAL_CONNECTION);
            // Increase default max connection per route to 20
            cm.setDefaultMaxPerRoute(DEFAULT_MAX_PER_ROUTE);

            httpClient = new DefaultHttpClient(cm);
        } catch (Exception e) {
            logger.error("unabled to setup SSL trust manager: {}", e.getMessage());
            httpClient = new DefaultHttpClient();
        }

    }

    /* Created new thread to run the atom hopper post call to make it
     * Asynchronous. The reason Spring @Async annotation was not use was
     * it broke test when sonar ran. Other annotation were used a but
     * similar problems occurred.
     */

    public void asyncPost(final User user, final String authToken, final String userStatus, final String migrationStatus) {
        Runnable task = new Runnable() {
            @Override
            public void run() {
                try {
                    if (userStatus.equals(AtomHopperConstants.MIGRATED)) {
                        postMigrateUser(user, authToken, userStatus, migrationStatus);
                    } else {
                        postUser(user, authToken, userStatus);
                    }
                } catch (Exception e) {
                    logger.warn("AtomHopperClient Exception: " + e);
                }
            }
        };
        new Thread(task, "Atom Hopper").start();
    }

    public void postMigrateUser(User user, String authToken, String userStatus, String migrationStatus) throws JAXBException, IOException, HttpException, URISyntaxException {
        try {
            AtomFeed atomFeed = createAtomFeed(user, AtomHopperConstants.CONTENT_TYPE, migrationStatus);
            Writer writer = marshalFeed(atomFeed);
            HttpResponse response;
            if (userStatus.equals(AtomHopperConstants.MIGRATED)) {
                response = executePostRequest(authToken, writer, config.getString(AtomHopperConstants.ATOM_HOPPER_MIGRATED_URL));
            } else {
                response = null;
            }
            if (response.getStatusLine().getStatusCode() != HttpServletResponse.SC_CREATED) {
                logger.warn("Failed to create feed for user: " + user.getUsername() + "with Id:" + user.getId());
            }

        } catch (Exception e) {
            logger.warn("AtomHopperClient Exception: " + e);
        }
    }

    public void postUser(User user, String authToken, String userStatus) throws JAXBException, IOException, HttpException, URISyntaxException {
        try {
            HttpResponse response;
            Writer writer;
            UsageEntry entry;
            if (userStatus.equals(AtomHopperConstants.DELETED)) {
                entry = createEntryForUser(user, EventType.DELETE, false);
                writer = marshalEntry(entry);
                response = executePostRequest(authToken, writer, config.getString(AtomHopperConstants.ATOM_HOPPER_DELETED_URL));
            } else if (userStatus.equals(AtomHopperConstants.DISABLED)) {
                entry = createEntryForUser(user, EventType.UPDATE, false);
                writer = marshalEntry(entry);
                response = executePostRequest(authToken, writer, config.getString(AtomHopperConstants.ATOM_HOPPER_DISABLED_URL));
            } else {
                response = null;
            }
            if (response.getStatusLine().getStatusCode() != HttpServletResponse.SC_CREATED) {
                logger.warn("Failed to create feed for user: " + user.getUsername() + "with Id:" + user.getId());
            }

        } catch (Exception e) {
            logger.warn("AtomHopperClient Exception: " + e);
        }
    }

    public HttpResponse executePostRequest(String authToken, Writer writer, String url) throws IOException {
        HttpPost httpPost = new HttpPost(url);
        httpPost.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_ATOM_XML);
        httpPost.setHeader("X-Auth-Token", authToken);
        httpPost.setEntity(createRequestEntity(writer.toString()));

        return httpClient.execute(httpPost);
    }

    public Writer marshalFeed(AtomFeed atomFeed) throws JAXBException {
        Writer writer = new StringWriter();
        JAXBContext jc = JAXBContext.newInstance(AtomFeed.class);
        Marshaller marshaller = jc.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FRAGMENT, true);
        marshaller.marshal(atomFeed, writer);
        return writer;
    }

    public Writer marshalEntry(UsageEntry entry) throws JAXBException {
        Writer writer = new StringWriter();
        JAXBContext jc = JAXBContext.newInstance(UsageEntry.class, CloudIdentityType.class);
        Marshaller marshaller = jc.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FRAGMENT, true);
        marshaller.setProperty("com.sun.xml.bind.namespacePrefixMapper", new AHNamespaceMapper());
        marshaller.marshal(entry, writer);
        return writer;
    }

    public InputStreamEntity createRequestEntity(String s) throws UnsupportedEncodingException {
        InputStream isStream = new ByteArrayInputStream(s.getBytes("UTF-8"));
        return new InputStreamEntity(isStream, -1);
    }

    public AtomFeed createAtomFeed(User user, String type, String status) {
        AtomFeed atomFeed = new AtomFeed();
        FeedUser feedUser = new FeedUser();
        Content content = new Content();
        content.setType(type);
        feedUser.setDisplayName(user.getDisplayName());
        feedUser.setId(user.getId());
        feedUser.setUsername(user.getUsername());
        feedUser.setMigrationStatus(status);
        atomFeed.setUser(feedUser);
        atomFeed.setContentType(content);
        return atomFeed;
    }

    public UsageEntry createEntryForUser(User user, EventType eventType, Boolean migrated){
        CloudIdentityType cloudIdentityType = new CloudIdentityType();
        cloudIdentityType.setDisplayName(user.getUsername());
        cloudIdentityType.setResourceType(ResourceTypes.USER);
        List<Group> groupList = defaultGroupService.getGroupsForUser(user.getId());
        if(groupList != null){
            for(Group group : groupList){
                cloudIdentityType.getGroups().add(group.getName());
            }
        }
        if(user.getRoles() != null){
            for(TenantRole tenantRole : user.getRoles()){
                cloudIdentityType.getRoles().add(tenantRole.getName());
            }
        }
        cloudIdentityType.setServiceCode(AtomHopperConstants.CLOUD_IDENTITY);
        cloudIdentityType.setVersion(AtomHopperConstants.VERSION);
        if(migrated){
            cloudIdentityType.setMigrated(migrated);
        }

        V1Element v1Element = new V1Element();
        v1Element.setType(eventType);
        v1Element.setResourceId(user.getId());
        v1Element.setResourceName(user.getUsername());
        v1Element.setRegion(Region.fromValue(user.getRegion()));
        v1Element.setVersion(AtomHopperConstants.VERSION);
        v1Element.getAny().add(cloudIdentityType);

        UsageContent usageContent = new UsageContent();
        usageContent.setEvent(v1Element);
        usageContent.setType(MediaType.APPLICATION_XML);

        UsageEntry usageEntry = new UsageEntry();
        usageEntry.setContent(usageContent);
        Title title = new Title();
        title.setValue(AtomHopperConstants.IDENTITY_EVENT);
        usageEntry.setTitle(title);
        return usageEntry;
    }

    public void setConfig(Configuration config) {
        this.config = config;
    }

    public void setDefaultGroupService(DefaultGroupService defaultGroupService) {
        this.defaultGroupService = defaultGroupService;
    }

    public void setHttpClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

}
