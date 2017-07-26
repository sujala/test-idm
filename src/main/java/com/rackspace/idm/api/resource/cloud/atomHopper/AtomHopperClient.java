package com.rackspace.idm.api.resource.cloud.atomHopper;

import com.rackspace.docs.core.event.DC;
import com.rackspace.docs.core.event.EventType;
import com.rackspace.docs.core.event.Region;
import com.rackspace.docs.core.event.V1Element;
import com.rackspace.docs.event.identity.trr.user.ValuesEnum;
import com.rackspace.docs.event.identity.user.CloudIdentityType;
import com.rackspace.docs.event.identity.user.ResourceTypes;
import com.rackspace.idm.domain.config.IdentityConfig;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.service.IdentityUserService;
import com.rackspace.idm.domain.service.TenantService;
import com.rackspace.idm.domain.service.impl.DefaultTenantService;
import com.rackspace.idm.exception.IdmException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.Validate;
import org.apache.http.HttpException;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.config.SocketConfig;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.w3._2005.atom.Title;
import org.w3._2005.atom.UsageContent;
import org.w3._2005.atom.UsageEntry;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
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
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Component
public class AtomHopperClient {

    @Autowired
    private IdentityUserService identityUserService;

    @Autowired
    private TenantService defaultTenantService;

    @Autowired
    private AtomHopperHelper atomHopperHelper;

    @Autowired
    private IdentityConfig identityConfig;

    private CloseableHttpClient httpClient;
    private IdleConnectionMonitorThread idleConnectionMonitorThread;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private static final Class[] JAXB_CONTEXT_FEED_ENTRY_CONTEXT_PATH = new Class[] {UsageEntry.class, CloudIdentityType.class, com.rackspace.docs.event.identity.token.CloudIdentityType.class, com.rackspace.docs.event.identity.trr.user.CloudIdentityType.class, com.rackspace.docs.event.identity.idp.CloudIdentityType.class};
    private static final JAXBContext JAXB_CONTEXT_FEED_ENTRY;

    static {
        try {
            JAXB_CONTEXT_FEED_ENTRY = JAXBContext.newInstance(JAXB_CONTEXT_FEED_ENTRY_CONTEXT_PATH);
        } catch (JAXBException e) {
            throw new IdmException("Error initializing JAXBContext for cloud feed events", e);
        }
    }

    public AtomHopperClient() {
    }

    /**
     * Initialize the client
     */
    @PostConstruct
    public void init() {
        httpClient = createHttpClient();
    }

    /**
     * Used to shutdown the connection manager
     */
    @PreDestroy
    public void destroy() {
        if (idleConnectionMonitorThread != null) {
            // Shutdown the daemon just to help clean threads up. Since daemon, shouldn't technically be necessary.
            idleConnectionMonitorThread.shutdown();
        }

        /*
            The connection manager has a finalize as well, but this allows spring to shut down on application context
            closing without relying on iffy finalizers
        */
        if (httpClient != null) {
            try {
                httpClient.close();
            } catch (Exception e) {
                logger.debug("Error closing httpclient. Ignoring since closing", e);
            }
        }
    }

    private CloseableHttpClient createHttpClient() {
        //TODO - we should not trust all certs and hosts like this...
        SSLContext sslContext = null;
        try {
            sslContext = org.apache.http.ssl.SSLContextBuilder.create().loadTrustMaterial(new TrustStrategy() {
                public boolean isTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
                    return true;
                }
            }).build();
        } catch (NoSuchAlgorithmException|KeyManagementException|KeyStoreException ex) {
            logger.error("Unable to setup SSL trust manager for cloud feeds client", ex);
            throw new IllegalStateException("Unable to setup SSL trust manager for cloud feeds client. Can not run without Cloud Feeds secure connection", ex);
        }

        HostnameVerifier hostnameVerifier = NoopHostnameVerifier.INSTANCE;

        // Create the connection factor to use weak trust strategy/verifier, then register it for use for https connections
        SSLConnectionSocketFactory sslFactory = new SSLConnectionSocketFactory(sslContext, hostnameVerifier);
        Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("http", PlainConnectionSocketFactory.getSocketFactory())
                .register("https", sslFactory)
                .build();

        /*
        Create pooling connection manager to reuse connections across threads. initialize with custom registry to accept
        all SSL
         */
        PoolingHttpClientConnectionManager poolingHttpClientConnectionManager = new PoolingHttpClientConnectionManager(registry);

        // Set default connection params based on static properties
        SocketConfig socketConfig = SocketConfig.copy(SocketConfig.DEFAULT)
                .setSoTimeout(identityConfig.getStaticConfig().getFeedsNewConnectionSocketTimeout())
                .build();

        /*
        Configure the pool based on pass through configuration properties
         */
        poolingHttpClientConnectionManager.setMaxTotal(identityConfig.getStaticConfig().getFeedsMaxTotalConnections());
        poolingHttpClientConnectionManager.setDefaultMaxPerRoute(identityConfig.getStaticConfig().getFeedsMaxConnectionsPerRoute());

        if (identityConfig.getStaticConfig().getFeedsDeamonEnabled()) {
            idleConnectionMonitorThread = new IdleConnectionMonitorThread(poolingHttpClientConnectionManager, identityConfig);
        } else {
            poolingHttpClientConnectionManager.setValidateAfterInactivity(identityConfig.getStaticConfig().getFeedsOnUseEvictionValidateAfterInactivity());
        }

        poolingHttpClientConnectionManager.setDefaultSocketConfig(socketConfig);

        /*
         Set default connection params used for post socket creation based on settings during app launch. The settings
         will be overridden at request time.
          */
        RequestConfig requestConfig = RequestConfig.copy(RequestConfig.DEFAULT)
                .setSocketTimeout(identityConfig.getReloadableConfig().getFeedsSocketTimeout())
                .setConnectTimeout(identityConfig.getReloadableConfig().getFeedsConnectionTimeout())
                .setConnectionRequestTimeout(identityConfig.getReloadableConfig().getFeedsConnectionRequestTimeout())
                .build();

        ConnectionKeepAliveStrategy connectionKeepAliveStrategy = new AtomHopperConnectionKeepAliveStrategy(identityConfig);

        //create the client and set the custom context
        HttpClientBuilder b = HttpClientBuilder.create()
                .setSSLContext(sslContext)
                .setConnectionManager(poolingHttpClientConnectionManager)
                .setDefaultRequestConfig(requestConfig)
                .setKeepAliveStrategy(connectionKeepAliveStrategy)
                ;

        CloseableHttpClient client = b.build();

        if (idleConnectionMonitorThread != null) {
            idleConnectionMonitorThread.setDaemon(true);
            idleConnectionMonitorThread.start();
        }

        return client;
    }

    @Async
    public void asyncPost(EndUser user, String userStatus) {
        try {
            postUser(user, userStatus);
        } catch (Exception e) {
            logger.warn("AtomHopperClient Exception posting user change: ", e);
        }
    }

    @Async
    public void asyncTokenPost(EndUser user, String revokedToken) {
        try {
            postToken(user, revokedToken);
        } catch (Exception e) {
            logger.warn("AtomHopperClient Exception posting token TRR: ", e);
        }
    }

    @Async
    public void asyncPostUserTrr(BaseUser user, TokenRevocationRecord trr) {
        Validate.isTrue(user.getId().equals(trr.getTargetIssuedToId()));
        try {
            postUserTrr(user, trr);
        } catch (Exception e) {
            logger.warn("AtomHopperClient Exception posting user TRR: ", e);
        }
    }

    @Async
    public void asyncPostIdpEvent(IdentityProvider idp, EventType eventType) {
        if(identityConfig.getReloadableConfig().postIdpFeedEvents()) {
            Validate.notNull(idp);
            Validate.notNull(idp.getUri());
            Validate.isTrue(eventType == EventType.CREATE || eventType == EventType.UPDATE || eventType == EventType.DELETE);
            try {
                postIdpEvent(idp, eventType);
            } catch (Exception e) {
                logger.warn("AtomHopperClient Exception posting IDP event: ", e);
            }
        }
    }

    private void postUserTrr(BaseUser user, TokenRevocationRecord trr) throws IOException {
        Validate.isTrue(user.getId().equals(trr.getTargetIssuedToId()));
        HttpResponse response = null;
        try {
            final UsageEntry entry = createUserTrrEntry(user, trr);
            HttpPost httpPost = generatePostRequest(marshalEntry(entry));
            response = httpClient.execute(httpPost);
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != HttpServletResponse.SC_CREATED) {
                final String errorMsg = IOUtils.toString(response.getEntity().getContent(), "UTF-8");
                logger.warn(String.format("Failed to post user TRR feed event for userId: %s. Returned status code: %s with body %s", user.getId(), statusCode, errorMsg));
            }
        } catch (Exception e) {
            logger.warn("AtomHopperClient Exception posting User TRR: ", e);
        } finally {
            if (response != null) {
                //always close the stream to release connection back to pool
                logger.debug("Consuming entity to release connection back to pool");
                atomHopperHelper.entityConsume(response.getEntity());
            }
        }
    }

    private void postUser(EndUser user, String userStatus) throws JAXBException, IOException, HttpException, URISyntaxException {
        HttpResponse response = null;
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
            } else if (userStatus.equals(AtomHopperConstants.UPDATE)) {
                entry = createEntryForUser(user, EventType.UPDATE, false);
            }

            if (entry != null) {
                HttpPost httpPost = generatePostRequest(marshalEntry(entry));
                response = httpClient.execute(httpPost);
            }

            if (response != null) {
                int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode != HttpServletResponse.SC_CREATED) {
                    final String errorMsg = IOUtils.toString(response.getEntity().getContent(), "UTF-8");
                    logger.warn(String.format("Failed to post User Feed event for user: %s with Id: %s. StatusCode: %s; ResponseBody: %s", user.getUsername(), user.getId(), statusCode, errorMsg));
                }
            } else {
                logger.warn("AtomHopperClient: Response was null");
            }
        } catch (Exception e) {
            logger.warn("AtomHopperClient Exception posting user change", e);
        } finally {
            if (response != null) {
                //always close the stream to release connection back to pool
                logger.debug("Consuming entity to release connection back to pool");
                atomHopperHelper.entityConsume(response.getEntity());
            }
        }
    }

    private void postToken(EndUser user, String revokedToken) throws JAXBException, IOException, HttpException, URISyntaxException {
        HttpResponse response = null;
        try {
            final UsageEntry entry = createEntryForRevokeToken(user, revokedToken);

            HttpPost httpPost = generatePostRequest(marshalEntry(entry));
            response = httpClient.execute(httpPost);
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != HttpServletResponse.SC_CREATED) {
                final String errorMsg = IOUtils.toString(response.getEntity().getContent(), "UTF-8");
                logger.warn(String.format("Failed to post Token TRR event for revoked token: %s. Returned status code: %s; responseBody: %s", revokedToken, statusCode, errorMsg));
            }
        } catch (Exception e) {
            logger.warn("AtomHopperClient Exception posting Token TRR", e);
        } finally {
            if (response != null) {
                //always close the stream to release connection back to pool
                logger.debug("Consuming entity to release connection back to pool");
                atomHopperHelper.entityConsume(response.getEntity());
            }
        }
    }

    private void postIdpEvent(IdentityProvider idp, EventType eventType) throws IOException {
        HttpResponse response = null;
        try {
            final UsageEntry entry = createEntryForIdp(idp, eventType);

            HttpPost httpPost = generatePostRequest(marshalEntry(entry));
            response = httpClient.execute(httpPost);
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != HttpServletResponse.SC_CREATED) {
                final String errorMsg = IOUtils.toString(response.getEntity().getContent(), "UTF-8");
                logger.warn(String.format("Failed to post IDP event for IDP: %s. Returned status code: %s; responseBody: %s", idp.getUri(), statusCode, errorMsg));
            }
        } catch (Exception e) {
            logger.warn("AtomHopperClient Exception posting IDP event", e);
        } finally {
            if (response != null) {
                //always close the stream to release connection back to pool
                logger.debug("Consuming entity to release connection back to pool");
                atomHopperHelper.entityConsume(response.getEntity());
            }
        }
    }

    private HttpPost generatePostRequest(Writer writer) throws IOException {
        String authToken = atomHopperHelper.getAuthToken();

        //set connection params based on settings when run
        RequestConfig requestConfig = RequestConfig.copy(RequestConfig.DEFAULT)
                .setSocketTimeout(identityConfig.getReloadableConfig().getFeedsSocketTimeout())
                .setConnectTimeout(identityConfig.getReloadableConfig().getFeedsConnectionTimeout())
                .setConnectionRequestTimeout(identityConfig.getReloadableConfig().getFeedsConnectionRequestTimeout())
                .build();

        final HttpPost httpPost = new HttpPost(identityConfig.getReloadableConfig().getAtomHopperUrl());
        httpPost.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_ATOM_XML);
        httpPost.setHeader("X-Auth-Token", authToken);
        httpPost.setEntity(createRequestEntity(writer.toString()));
        httpPost.setConfig(requestConfig);
        return httpPost;
    }

    private Writer marshalEntry(UsageEntry entry) throws JAXBException {
        final Writer writer = new StringWriter();
        JAXBContext context = JAXB_CONTEXT_FEED_ENTRY;
        if (!identityConfig.getReloadableConfig().reuseJaxbContext()) {
            //TODO causes memory leak...only left for backwards compatibility. Must be removed in future version.
            context = JAXBContext.newInstance(JAXB_CONTEXT_FEED_ENTRY_CONTEXT_PATH);
        }

        final Marshaller marshaller = context.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FRAGMENT, true);
        marshaller.setProperty("com.sun.xml.bind.namespacePrefixMapper", new AHNamespaceMapper());
        marshaller.marshal(entry, writer);
        return writer;
    }

    private InputStreamEntity createRequestEntity(String s) throws UnsupportedEncodingException {
        return new InputStreamEntity(new ByteArrayInputStream(s.getBytes("UTF-8")), -1);
    }

    private UsageEntry createEntryForUser(EndUser user, EventType eventType, Boolean migrated) throws DatatypeConfigurationException {
        logger.debug("Creating user entry ...");

        final CloudIdentityType cloudIdentityType = new CloudIdentityType();

        cloudIdentityType.setResourceType(ResourceTypes.USER);

        String username = user.getUsername();
        if (user instanceof User) {
            //only applicable for provisioned users
            cloudIdentityType.setMultiFactorEnabled(((User)user).isMultiFactorEnabled());
        }
        else if (user instanceof FederatedUser) {
            FederatedUser fedUser = (FederatedUser) user;
            username = String.format("%s@%s", fedUser.getUsername(), fedUser.getFederatedIdpUri());
        }
        cloudIdentityType.setDisplayName(username);

        for (Group group : identityUserService.getGroupsForEndUser(user.getId())) {
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
        final UsageEntry usageEntry = createUsageEntry(cloudIdentityType, eventType, id, user.getId(), username, AtomHopperConstants.IDENTITY_EVENT, tenantId);
        logger.debug("Created Identity user entry with id: " + id);
        return usageEntry;
    }

    private UsageEntry createEntryForRevokeToken(EndUser user, String token) throws DatatypeConfigurationException, GeneralSecurityException, InvalidCipherTextException, UnsupportedEncodingException {
        logger.debug("Creating revoke token entry ...");

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
        logger.debug("Created Identity token entry with id: " + id);
        return usageEntry;
    }

    private UsageEntry createEntryForIdp(IdentityProvider idp, EventType eventType) throws DatatypeConfigurationException, GeneralSecurityException, InvalidCipherTextException, UnsupportedEncodingException {
        logger.debug("Creating IDP entry ...");

        final com.rackspace.docs.event.identity.idp.CloudIdentityType cloudIdentityType = new com.rackspace.docs.event.identity.idp.CloudIdentityType();
        cloudIdentityType.setResourceType(com.rackspace.docs.event.identity.idp.ResourceTypes.IDP);
        cloudIdentityType.setVersion(AtomHopperConstants.VERSION);
        cloudIdentityType.setServiceCode(AtomHopperConstants.CLOUD_IDENTITY);
        cloudIdentityType.setIssuer(idp.getUri());

        final String id = UUID.randomUUID().toString();
        final UsageEntry usageEntry = createUsageEntry(cloudIdentityType, eventType, id, idp.getProviderId(), null, AtomHopperConstants.IDENTITY_IDP_EVENT, null);
        logger.debug("Created IDP entry with id: " + id);
        return usageEntry;
    }

    /**
     * @param user
     * @param trr
     * @return
     *
     * @throws java.lang.IllegalArgumentException if one of the authenticatedByMethodGroups contains an auth method not
     * recognized by feed schema or error creating date for token creation date
     */
    private UsageEntry createUserTrrEntry(BaseUser user, TokenRevocationRecord trr) {
        logger.debug("Creating user TRR entry ...");
        Validate.isTrue(user.getId().equals(trr.getTargetIssuedToId()));

        final com.rackspace.docs.event.identity.trr.user.CloudIdentityType cloudIdentityType = new com.rackspace.docs.event.identity.trr.user.CloudIdentityType();
        cloudIdentityType.setResourceType(com.rackspace.docs.event.identity.trr.user.ResourceTypes.TRR_USER);
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

        //set creation date
        //TODO: Fix xjc binding to do the conversion automatically so the datatypes on generated pojos are DateTime
        final GregorianCalendar c = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        c.setTime(trr.getTargetCreatedBefore());
        try {
            final XMLGregorianCalendar creationDateCal = DatatypeFactory.newInstance().newXMLGregorianCalendar(c);
            cloudIdentityType.setTokenCreationDate(creationDateCal);
        } catch (DatatypeConfigurationException e) {
            logger.error("Error creating calendar instance to set token creation date", e);
            throw new IllegalStateException("Error creating User TRR Event", e);
        }

        //create authByLists
        for (AuthenticatedByMethodGroup authenticatedByMethodGroup : trr.getTargetAuthenticatedByMethodGroups()) {
            if (authenticatedByMethodGroup.isAllAuthenticatedByMethods()) {
                cloudIdentityType.getTokenAuthenticatedBy().clear();
                break;
            }
            List<String> authByValues = authenticatedByMethodGroup.getAuthenticatedByMethodsAsValues();
            com.rackspace.docs.event.identity.trr.user.CloudIdentityType.TokenAuthenticatedBy tab = new com.rackspace.docs.event.identity.trr.user.CloudIdentityType.TokenAuthenticatedBy();
            for (String authByValue : authByValues) {
                try {
                    ValuesEnum val = ValuesEnum.valueOf(authByValue); //throws IllegalArgumentException if can't be converted to enum
                    tab.getValues().add(val);
                } catch (IllegalArgumentException e) {
                    String message = String.format("Error converting User TRR to feed event. Invalid authMethod '%s'", authByValue);
                    logger.error(message, e);
                    throw new IllegalStateException(message, e);
                }
            }
            cloudIdentityType.getTokenAuthenticatedBy().add(tab);
        }

        final String id = UUID.randomUUID().toString();
        final UsageEntry usageEntry = createUsageEntry(cloudIdentityType, EventType.DELETE, id, user.getId(), null, AtomHopperConstants.IDENTITY_USER_TRR_EVENT, null);
        logger.debug("Created user TRR entry with id: " + id);
        return usageEntry;
    }

    private UsageEntry createUsageEntry(Object cloudIdentityType, EventType eventType, String id, String resourceId, String resourceName, String title, String tenantId) {
        final V1Element v1Element = new V1Element();
        v1Element.setType(eventType);
        v1Element.setResourceId(resourceId);
        v1Element.setResourceName(resourceName);
        v1Element.setTenantId(tenantId);
        v1Element.setRegion(Region.fromValue(identityConfig.getReloadableConfig().getAtomHopperRegion()));
        v1Element.setDataCenter(DC.fromValue(identityConfig.getReloadableConfig().getAtomHopperDataCenter()));
        v1Element.setVersion(AtomHopperConstants.VERSION);
        v1Element.getAny().add(cloudIdentityType);

        final GregorianCalendar c = new GregorianCalendar();
        c.setTime(new Date());
        c.setTimeZone(TimeZone.getTimeZone("UTC"));

        try {
            final XMLGregorianCalendar now = DatatypeFactory.newInstance().newXMLGregorianCalendar(c);
            v1Element.setEventTime(now);
        } catch (DatatypeConfigurationException e) {
            logger.error("Error creating calendar instance to set token creation date", e);
            throw new IllegalStateException("Error creating feed entry", e);
        }

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

    /**
     * Used to evict expired connections from the httpclient connection manager.
     *
     * @see <a href="https://hc.apache.org/httpcomponents-client-4.5.x/tutorial/html/connmgmt.html">HttpComponents Doc</a>
     */
    public static class IdleConnectionMonitorThread extends Thread {

        private final HttpClientConnectionManager connMgr;
        private IdentityConfig identityConfig;
        private volatile boolean shutdown;

        public IdleConnectionMonitorThread(HttpClientConnectionManager connMgr, IdentityConfig identityConfig) {
            super();
            this.connMgr = connMgr;
            this.identityConfig = identityConfig;
        }

        @Override
        public void run() {
            try {
                while (!shutdown) {
                    synchronized (this) {
                        int delay = identityConfig.getReloadableConfig().getFeedsDaemonEvictionFrequency();
                        if (delay > 0) {
                            wait(delay);
                        }

                        // Close expired connections
                        connMgr.closeExpiredConnections();

                        // Optionally, close connections
                        // that have been idle longer than x ms
                        int idleLimit = identityConfig.getReloadableConfig().getFeedsDaemonEvictionCloseIdleConnectionsAfter();
                        if (idleLimit > 0) {
                            connMgr.closeIdleConnections(idleLimit, TimeUnit.MILLISECONDS);
                        }
                    }
                }
            } catch (InterruptedException ex) {
                // terminate
            }
        }

        public void shutdown() {
            shutdown = true;
            synchronized (this) {
                notifyAll();
            }
        }
    }
}
