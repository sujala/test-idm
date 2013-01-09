package com.rackspace.idm.api.resource.cloud.atomHopper;

import com.rackspace.docs.core.event.*;
import com.rackspace.docs.event.identity.user.CloudIdentityType;
import com.rackspace.docs.event.identity.user.ResourceTypes;
import com.rackspace.idm.api.resource.cloud.v20.DefaultCloud20Service;
import com.rackspace.idm.domain.entity.Group;
import com.rackspace.idm.domain.entity.TenantRole;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.service.UserService;
import com.rackspace.idm.domain.service.impl.DefaultGroupService;
import com.rackspace.idm.domain.service.impl.DefaultTenantService;
import com.rackspace.idm.util.CryptHelper;
import com.rackspacecloud.docs.auth.api.v1.PasswordCredentials;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringUtils;
import org.apache.http.*;
import org.apache.http.auth.AuthOption;
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
import org.apache.http.message.BasicHeader;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.util.encoders.Base64Encoder;
import org.openstack.docs.identity.api.v2.*;
import org.openstack.docs.identity.api.v2.ObjectFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.w3._2005.atom.Title;
import org.w3._2005.atom.UsageContent;
import org.w3._2005.atom.UsageEntry;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
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

    @Autowired
    private DefaultTenantService defaultTenantService;

    @Autowired
    private DefaultCloud20Service defaultCloud20Service;

    @Autowired
    CryptHelper cryptHelper;

    private HttpClient httpClient;

    private ObjectFactory objectFactory = new ObjectFactory();

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public AtomHopperClient() {
        try {
            SSLSocketFactory sslsf = new SSLSocketFactory(new TrustStrategy() {
                @Override
                public boolean isTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
                    return true;
                }
            }, SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
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

    public void asyncPost(final User user, final String userStatus) {
        Runnable task = new Runnable() {
            @Override
            public void run() {
                try {
                    postUser(user, getAuthToken(), userStatus);
                } catch (Exception e) {
                    logger.warn("AtomHopperClient Exception: " + e);
                }
            }
        };
        new Thread(task, "Atom Hopper").start();
    }

    public void asyncTokenPost(final User user, final String revokedToken) {
        Runnable task = new Runnable() {
            @Override
            public void run() {
                try {
                    postToken(user, getAuthToken() , revokedToken);
                } catch (Exception e) {
                    logger.warn("AtomHopperClient Exception: " + e);
                }
            }
        };
        new Thread(task, "Atom Hopper").start();
    }

    public void postUser(User user, String authToken, String userStatus) throws JAXBException, IOException, HttpException, URISyntaxException {
        try {
            HttpResponse response = null;
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
            } else if (userStatus.equals(AtomHopperConstants.MIGRATED)) {
                entry = createEntryForUser(user, EventType.CREATE, true);
                writer = marshalEntry(entry);
                response = executePostRequest(authToken, writer, config.getString(AtomHopperConstants.ATOM_HOPPER_MIGRATED_URL));
            }
            if (response.getStatusLine().getStatusCode() != HttpServletResponse.SC_CREATED) {
                logger.warn("Failed to create feed for user: " + user.getUsername() + "with Id:" + user.getId());
            }

        } catch (Exception e) {
            logger.warn("AtomHopperClient Exception: " + e);
        }
    }

    public void postToken(User user, String authToken, String revokedToken) throws JAXBException, IOException, HttpException, URISyntaxException {
        try{
            UsageEntry entry = createEntryForRevokeToken(user, revokedToken);
            Writer writer = marshalEntry(entry);
            HttpResponse response = executePostRequest(authToken, writer, config.getString(AtomHopperConstants.ATOM_HOPPER_REVOKED_URL));
            if(response.getStatusLine().getStatusCode() != HttpServletResponse.SC_CREATED) {
                logger.warn("Failed to create feed for revoked token: " + revokedToken);
            }
        } catch (Exception e){
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

    public String getAuthToken() throws IOException, JAXBException {
        logger.warn("Authenticating ...");
        AuthenticationRequest request = new AuthenticationRequest();
        PasswordCredentialsRequiredUsername credentialsBase = new PasswordCredentialsRequiredUsername();
        credentialsBase.setUsername(config.getString("ga.username"));
        credentialsBase.setPassword(config.getString("ga.password"));
        JAXBElement<PasswordCredentialsRequiredUsername> jaxbCredentialsBase = objectFactory.createPasswordCredentials(credentialsBase);
        request.setCredential(jaxbCredentialsBase);
        Response.ResponseBuilder responseBuilder = defaultCloud20Service.authenticate(null, request);
        AuthenticateResponse authenticateResponse = (AuthenticateResponse)responseBuilder.build().getEntity();
        logger.warn("Authenticated user %s", config.getString("ga.username"));
        return authenticateResponse.getToken().getId();
    }

    public Writer marshalEntry(UsageEntry entry) throws JAXBException {
        Writer writer = new StringWriter();
        JAXBContext jc = JAXBContext.newInstance(UsageEntry.class, CloudIdentityType.class, com.rackspace.docs.event.identity.token.CloudIdentityType.class);
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

    public UsageEntry createEntryForUser(User user, EventType eventType, Boolean migrated) throws DatatypeConfigurationException {
        logger.warn("Creating user entry ...");
        CloudIdentityType cloudIdentityType = new CloudIdentityType();
        cloudIdentityType.setDisplayName(user.getUsername());
        cloudIdentityType.setResourceType(ResourceTypes.USER);
        List<Group> groupList = defaultGroupService.getGroupsForUser(user.getId());
        if(groupList != null){
            for(Group group : groupList){
                cloudIdentityType.getGroups().add(group.getName());
            }
        }
        List<TenantRole> tenantRoles = defaultTenantService.getTenantRolesForUser(user);
        if(tenantRoles != null){
            for(TenantRole tenantRole : tenantRoles){
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
        for(Region region : Region.values()){
            if(region.value().equals(user.getRegion())){
                v1Element.setRegion(Region.fromValue(user.getRegion()));
            }
        }

        v1Element.setDataCenter(DC.fromValue(config.getString("atom.hopper.dataCenter")));
        v1Element.setVersion(AtomHopperConstants.VERSION);
        v1Element.getAny().add(cloudIdentityType);
        GregorianCalendar c = new GregorianCalendar();
        c.setTime(new Date());
        c.setTimeZone(TimeZone.getTimeZone("UTC"));
        XMLGregorianCalendar now = DatatypeFactory.newInstance().newXMLGregorianCalendar(c);
        v1Element.setEventTime(now);
        String id = UUID.randomUUID().toString();
        v1Element.setId(id);

        UsageContent usageContent = new UsageContent();
        usageContent.setEvent(v1Element);
        usageContent.setType(MediaType.APPLICATION_XML);

        UsageEntry usageEntry = new UsageEntry();
        usageEntry.setContent(usageContent);
        Title title = new Title();
        title.setValue(AtomHopperConstants.IDENTITY_EVENT);
        usageEntry.setTitle(title);
        logger.warn("Created Identity user entry with id: " + id);
        return usageEntry;
    }

    public UsageEntry createEntryForRevokeToken(User user, String token) throws DatatypeConfigurationException, GeneralSecurityException, InvalidCipherTextException, UnsupportedEncodingException {
        logger.warn("Creating revoke token entry ...");
        com.rackspace.docs.event.identity.token.CloudIdentityType cloudIdentityType = new com.rackspace.docs.event.identity.token.CloudIdentityType();
        cloudIdentityType.setResourceType(com.rackspace.docs.event.identity.token.ResourceTypes.TOKEN);
        cloudIdentityType.setVersion(AtomHopperConstants.VERSION);
        cloudIdentityType.setServiceCode(AtomHopperConstants.CLOUD_IDENTITY);

        List<TenantRole> tenantRoles = defaultTenantService.getTenantRolesForUser(user);
        for(TenantRole tenantRole : tenantRoles){
            if(tenantRole.getTenantIds() != null){
                for(String tenantId : tenantRole.getTenantIds()){
                    cloudIdentityType.getTenants().add(tenantId);
                }
            }
        }

        V1Element v1Element = new V1Element();
        v1Element.setType(EventType.DELETE);
        logger.warn("Encrypting token ...");
        v1Element.setResourceId(encrypt(token));
        logger.warn("Token encrypted");
        for(Region region : Region.values()){
            if(region.value().equals(user.getRegion())){
                v1Element.setRegion(Region.fromValue(user.getRegion()));
            }
        }

        v1Element.setDataCenter(DC.fromValue(config.getString("atom.hopper.dataCenter")));
        v1Element.setVersion(AtomHopperConstants.VERSION);
        v1Element.getAny().add(cloudIdentityType);
        GregorianCalendar c = new GregorianCalendar();
        c.setTime(new Date());
        c.setTimeZone(TimeZone.getTimeZone("UTC"));
        XMLGregorianCalendar now = DatatypeFactory.newInstance().newXMLGregorianCalendar(c);
        v1Element.setEventTime(now);
        String id = UUID.randomUUID().toString();
        v1Element.setId(id);

        UsageContent usageContent = new UsageContent();
        usageContent.setEvent(v1Element);
        usageContent.setType(MediaType.APPLICATION_XML);

        UsageEntry usageEntry = new UsageEntry();
        usageEntry.setContent(usageContent);
        Title title = new Title();
        title.setValue(AtomHopperConstants.IDENTITY_TOKEN_EVENT);
        usageEntry.setTitle(title);
        logger.warn("Created Identity token entry with id: " + id);
        return usageEntry;
    }

    private String encrypt(String text) throws GeneralSecurityException, InvalidCipherTextException, UnsupportedEncodingException {
        byte[] bytes = cryptHelper.encrypt(text, getCipherParameters());
        return new Base64().encodeToString(bytes);
    }

    private String decrypt(String text) throws GeneralSecurityException, InvalidCipherTextException, UnsupportedEncodingException {
        String encryptedBytes = cryptHelper.decrypt(new Base64().decode(text), getCipherParameters());
        return new String(encryptedBytes);
    }

    private CipherParameters getCipherParameters() {
        return cryptHelper.getKeyParams(
                    config.getString("atom.hopper.crypto.password"),
                    config.getString("atom.hopper.crypto.salt")
            );
    }

    public void setConfig(Configuration config) {
        this.config = config;
    }

    public void setDefaultGroupService(DefaultGroupService defaultGroupService) {
        this.defaultGroupService = defaultGroupService;
    }

    public void setDefaultTenantService(DefaultTenantService defaultTenantService){
        this.defaultTenantService = defaultTenantService;
    }

    public void setObjectFactory(ObjectFactory objectFactory){
        this.objectFactory = objectFactory;
    }

    public void setHttpClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public void setDefaultCloud20Service(DefaultCloud20Service defaultCloud20Service) {
        this.defaultCloud20Service = defaultCloud20Service;
    }
}
