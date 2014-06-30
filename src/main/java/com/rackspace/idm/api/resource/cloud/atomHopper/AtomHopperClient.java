package com.rackspace.idm.api.resource.cloud.atomHopper;

import com.rackspace.docs.core.event.DC;
import com.rackspace.docs.core.event.EventType;
import com.rackspace.docs.core.event.Region;
import com.rackspace.docs.core.event.V1Element;
import com.rackspace.docs.event.identity.user.CloudIdentityType;
import com.rackspace.docs.event.identity.user.ResourceTypes;
import com.rackspace.idm.domain.entity.Group;
import com.rackspace.idm.domain.entity.TenantRole;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.service.UserService;
import com.rackspace.idm.domain.service.impl.DefaultTenantService;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.io.IOUtils;
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
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.w3._2005.atom.Title;
import org.w3._2005.atom.UsageContent;
import org.w3._2005.atom.UsageEntry;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.io.*;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: jorge, bernardo
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
    private UserService userService;

    @Autowired
    private DefaultTenantService defaultTenantService;

    @Autowired
    private AtomHopperHelper atomHopperHelper;

    private HttpClient httpClient;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public AtomHopperClient() {
        try {
            final SSLSocketFactory sslsf = new SSLSocketFactory(new TrustStrategy() {
                @Override
                public boolean isTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
                    return true;
                }
            }, SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

            final SchemeRegistry schemeRegistry = new SchemeRegistry();
            schemeRegistry.register(new Scheme("http", PORT80, PlainSocketFactory.getSocketFactory()));
            schemeRegistry.register(new Scheme("https", PORT443, sslsf));

            final PoolingClientConnectionManager cm = new PoolingClientConnectionManager(schemeRegistry);
            // Increase max total connection to 200
            cm.setMaxTotal(MAX_TOTAL_CONNECTION);
            // Increase default max connection per route to 20
            cm.setDefaultMaxPerRoute(DEFAULT_MAX_PER_ROUTE);

            httpClient = new DefaultHttpClient(cm);
        } catch (Exception e) {
            logger.error("unable to setup SSL trust manager: {}", e.getMessage());
            httpClient = new DefaultHttpClient();
        }

    }

    @Async
    public void asyncPost(User user, String userStatus) {
        try {
            postUser(user, atomHopperHelper.getAuthToken(), userStatus);
        } catch (Exception e) {
            logger.warn("AtomHopperClient Exception: " + e);
        }
    }

    @Async
    public void asyncTokenPost(User user, String revokedToken) {
        try {
            postToken(user, atomHopperHelper.getAuthToken(), revokedToken);
        } catch (Exception e) {
            logger.warn("AtomHopperClient Exception: " + e);
        }
    }

    public void postUser(User user, String authToken, String userStatus) throws JAXBException, IOException, HttpException, URISyntaxException {
        try {
            UsageEntry entry = null;
            if (userStatus.equals(AtomHopperConstants.DELETED)) {
                entry = createEntryForUser(user, EventType.DELETE, false);
            } else if (userStatus.equals(AtomHopperConstants.DISABLED)) {
                entry = createEntryForUser(user, EventType.SUSPEND, false);
            } else if (userStatus.equals(AtomHopperConstants.MIGRATED)) {
                entry = createEntryForUser(user, EventType.CREATE, true);
            } else if (userStatus.equals(AtomHopperConstants.GROUP)) {
                entry = createEntryForUser(user, EventType.UPDATE, false);
            } else if (userStatus.equals(AtomHopperConstants.ROLE)) {
                entry = createEntryForUser(user, EventType.UPDATE, false);
            } else if (userStatus.equals(AtomHopperConstants.ENABLED)) {
                entry = createEntryForUser(user, EventType.UNSUSPEND, false);
            } else if (userStatus.equals(AtomHopperConstants.MULTI_FACTOR)) {
                entry = createEntryForUser(user, EventType.UPDATE, false);
            }

            HttpResponse response = null;
            if (entry != null) {
                response = executePostRequest(authToken, marshalEntry(entry), config.getString(AtomHopperConstants.ATOM_HOPPER_URL));
            }

            if (response != null) {
                if (response.getStatusLine().getStatusCode() != HttpServletResponse.SC_CREATED) {
                    final String errorMsg = IOUtils.toString(response.getEntity().getContent(), "UTF-8");
                    logger.warn("Failed to create feed for user: " + user.getUsername() + "with Id:" + user.getId());
                    logger.warn(errorMsg);
                }
                atomHopperHelper.entityConsume(response.getEntity());
            } else {
                logger.warn("AtomHopperClient: Response was null");
            }
        } catch (Exception e) {
            logger.warn("AtomHopperClient Exception: " + e);
        }
    }

    public void postToken(User user, String authToken, String revokedToken) throws JAXBException, IOException, HttpException, URISyntaxException {
        try {
            final UsageEntry entry = createEntryForRevokeToken(user, revokedToken);
            final Writer writer = marshalEntry(entry);
            final HttpResponse response = executePostRequest(authToken, writer, config.getString(AtomHopperConstants.ATOM_HOPPER_URL));
            if (response.getStatusLine().getStatusCode() != HttpServletResponse.SC_CREATED) {
                final String errorMsg = IOUtils.toString(response.getEntity().getContent(), "UTF-8");
                logger.warn("Failed to create feed for revoked token: " + revokedToken);
                logger.warn(errorMsg);
            }
            atomHopperHelper.entityConsume(response.getEntity());
        } catch (Exception e) {
            logger.warn("AtomHopperClient Exception: " + e);
        }
    }

    public HttpResponse executePostRequest(String authToken, Writer writer, String url) throws IOException {
        final HttpPost httpPost = new HttpPost(url);
        httpPost.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_ATOM_XML);
        httpPost.setHeader("X-Auth-Token", authToken);
        httpPost.setEntity(createRequestEntity(writer.toString()));
        return httpClient.execute(httpPost);
    }

    public Writer marshalEntry(UsageEntry entry) throws JAXBException {
        final Writer writer = new StringWriter();
        final JAXBContext jc = JAXBContext.newInstance(UsageEntry.class, CloudIdentityType.class, com.rackspace.docs.event.identity.token.CloudIdentityType.class);
        final Marshaller marshaller = jc.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FRAGMENT, true);
        marshaller.setProperty("com.sun.xml.bind.namespacePrefixMapper", new AHNamespaceMapper());
        marshaller.marshal(entry, writer);
        return writer;
    }

    public InputStreamEntity createRequestEntity(String s) throws UnsupportedEncodingException {
        return new InputStreamEntity(new ByteArrayInputStream(s.getBytes("UTF-8")), -1);
    }

    public UsageEntry createEntryForUser(User user, EventType eventType, Boolean migrated) throws DatatypeConfigurationException {
        logger.warn("Creating user entry ...");

        final CloudIdentityType cloudIdentityType = new CloudIdentityType();
        cloudIdentityType.setDisplayName(user.getUsername());
        cloudIdentityType.setResourceType(ResourceTypes.USER);
        cloudIdentityType.setMultiFactorEnabled(user.isMultiFactorEnabled());
        for (Group group : userService.getGroupsForUser(user.getId())) {
            cloudIdentityType.getGroups().add(group.getGroupId());
        }

        final List<TenantRole> tenantRoles = defaultTenantService.getTenantRolesForUser(user);
        if (tenantRoles != null) {
            for (TenantRole tenantRole : tenantRoles) {
                cloudIdentityType.getRoles().add(tenantRole.getName());
            }
        }

        final String tenantId = defaultTenantService.getMossoIdFromTenantRoles(tenantRoles);

        cloudIdentityType.setServiceCode(AtomHopperConstants.CLOUD_IDENTITY);
        cloudIdentityType.setVersion(AtomHopperConstants.VERSION);
        if (migrated) {
            cloudIdentityType.setMigrated(migrated);
        }

        final String id = UUID.randomUUID().toString();
        final UsageEntry usageEntry = createUsageEntry(cloudIdentityType, eventType, id, user.getId(), user.getUsername(), AtomHopperConstants.IDENTITY_EVENT, tenantId);
        logger.warn("Created Identity user entry with id: " + id);
        return usageEntry;
    }

    public UsageEntry createEntryForRevokeToken(User user, String token) throws DatatypeConfigurationException, GeneralSecurityException, InvalidCipherTextException, UnsupportedEncodingException {
        logger.warn("Creating revoke token entry ...");

        final com.rackspace.docs.event.identity.token.CloudIdentityType cloudIdentityType = new com.rackspace.docs.event.identity.token.CloudIdentityType();
        cloudIdentityType.setResourceType(com.rackspace.docs.event.identity.token.ResourceTypes.TOKEN);
        cloudIdentityType.setVersion(AtomHopperConstants.VERSION);
        cloudIdentityType.setServiceCode(AtomHopperConstants.CLOUD_IDENTITY);

        final List<TenantRole> tenantRoles = defaultTenantService.getTenantRolesForUser(user);
        for (TenantRole tenantRole : tenantRoles) {
            if (tenantRole.getTenantIds() != null) {
                for (String tenantId : tenantRole.getTenantIds()) {
                    cloudIdentityType.getTenants().add(tenantId);
                }
            }
        }

        final String id = UUID.randomUUID().toString();
        final UsageEntry usageEntry = createUsageEntry(cloudIdentityType, EventType.DELETE, id, token, null, AtomHopperConstants.IDENTITY_TOKEN_EVENT, null);
        logger.warn("Created Identity token entry with id: " + id);
        return usageEntry;
    }

    private UsageEntry createUsageEntry(Object cloudIdentityType, EventType eventType, String id, String resourceId, String resourceName, String title, String tenantId) throws DatatypeConfigurationException {
        final V1Element v1Element = new V1Element();
        v1Element.setType(eventType);
        v1Element.setResourceId(resourceId);
        v1Element.setResourceName(resourceName);
        v1Element.setTenantId(tenantId);
        v1Element.setRegion(Region.fromValue(config.getString("atom.hopper.region")));
        v1Element.setDataCenter(DC.fromValue(config.getString("atom.hopper.dataCenter")));
        v1Element.setVersion(AtomHopperConstants.VERSION);
        v1Element.getAny().add(cloudIdentityType);

        final GregorianCalendar c = new GregorianCalendar();
        c.setTime(new Date());
        c.setTimeZone(TimeZone.getTimeZone("UTC"));

        final XMLGregorianCalendar now = DatatypeFactory.newInstance().newXMLGregorianCalendar(c);
        v1Element.setEventTime(now);
        v1Element.setId(id);

        final UsageContent usageContent = new UsageContent();
        usageContent.setEvent(v1Element);
        usageContent.setType(MediaType.APPLICATION_XML);

        final UsageEntry usageEntry = new UsageEntry();
        usageEntry.setContent(usageContent);

        final Title entryTitle = new Title();
        entryTitle.setValue(title);
        usageEntry.setTitle(entryTitle);

        return usageEntry;
    }

    public void setConfig(Configuration config) {
        this.config = config;
    }

    public void setDefaultTenantService(DefaultTenantService defaultTenantService) {
        this.defaultTenantService = defaultTenantService;
    }

    public void setHttpClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public void setAtomHopperHelper(AtomHopperHelper atomHopperHelper) {
        this.atomHopperHelper = atomHopperHelper;
    }

}
