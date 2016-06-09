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
import com.rackspace.idm.domain.service.UserService;
import com.rackspace.idm.domain.service.impl.DefaultTenantService;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.Validate;
import org.apache.http.*;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.config.SocketConfig;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.*;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHeaderElementIterator;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
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
    private IdentityConfig identityConfig;

    @Autowired
    private UserService userService;

    @Autowired
    private IdentityUserService identityUserService;

    @Autowired
    private DefaultTenantService defaultTenantService;

    @Autowired
    private AtomHopperHelper atomHopperHelper;

    private CloseableHttpClient httpClient;
    private IdleConnectionMonitorThread idleConnectionMonitorThread;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public AtomHopperClient() {
    }

    /**
     * Initialize the client
     */
    @PostConstruct
    public void init() {
        if (identityConfig.getStaticConfig().useFeedsConfigurableHttpClient()) {
            httpClient = createHttpClient();
        } else {
            httpClient = createLegacyHttpClient();
        }
    }

    /**
     * Used to shutdown the connection manager
     */
    @PreDestroy
    public void destroy() {
        if (idleConnectionMonitorThread != null) {
            //shutdown the daemon just to help clean threads up. Since daemon, shouldn't technically be necessary.
            idleConnectionMonitorThread.shutdown();
        }

        //the connection manager has a finalize as well, but this allows spring to shut down on application context
        // closing without relying on iffy finalizers
        try {
            httpClient.close();
        } catch (Exception e) {
            logger.debug("Error closing httpclient. Ignoring since closing", e);
        }
    }

    private CloseableHttpClient createLegacyHttpClient() {
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

            return new DefaultHttpClient(cm);
        } catch (Exception e) {
            logger.error("unable to setup SSL trust manager: {}", e.getMessage());
            return new DefaultHttpClient();
        }
    }

    private CloseableHttpClient createHttpClient() {
        SSLContext sslContext = null;
        try {
            //TODO - we should not trust all certs and hosts like this...
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

        //create the connection factor to use weak trust strategy/verifier, then register it for use for https connections
        SSLConnectionSocketFactory sslFactory = new SSLConnectionSocketFactory(sslContext, hostnameVerifier);
        Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("http", PlainConnectionSocketFactory.getSocketFactory())
                .register("https", sslFactory)
                .build();

        /*
        create pooling connection manager to reuse connections across threads and initialize with custom registry for
        custom ssl trusts, and custom timeout to check for connection validity before use
         */
        PoolingHttpClientConnectionManager poolingHttpClientConnectionManager = new PoolingHttpClientConnectionManager(registry);

        poolingHttpClientConnectionManager.setMaxTotal(identityConfig.getStaticConfig().getFeedsMaxTotalConnections());
        poolingHttpClientConnectionManager.setDefaultMaxPerRoute(identityConfig.getStaticConfig().getFeedsMaxConnectionsPerRoute());

        HttpClientConnectionEvictionStrategyType evictionStrategyType = identityConfig.getStaticConfig().getFeedsEvictionStrategy();
        if (evictionStrategyType == HttpClientConnectionEvictionStrategyType.ON_USE) {
            poolingHttpClientConnectionManager.setValidateAfterInactivity(identityConfig.getStaticConfig().getFeedsOnUseEvictionValidateAfterInactivity());
        } else if (evictionStrategyType == HttpClientConnectionEvictionStrategyType.DAEMON) {
            idleConnectionMonitorThread = new IdleConnectionMonitorThread(poolingHttpClientConnectionManager, identityConfig);
        }

        //set default connection params based on settings when started
        SocketConfig socketConfig = SocketConfig.copy(SocketConfig.DEFAULT)
                .setSoTimeout(identityConfig.getStaticConfig().getFeedsNewConnectionSocketTimeout())
                .build();

        //set default connection params used for post socket creation based on settings during app launch. The settings
        //will be overridden at request time.
        RequestConfig requestConfig = RequestConfig.copy(RequestConfig.DEFAULT)
                .setSocketTimeout(identityConfig.getReloadableConfig().getFeedsSocketTimeout())
                .setConnectTimeout(identityConfig.getReloadableConfig().getFeedsConnectionTimeout())
                .setConnectionRequestTimeout(identityConfig.getReloadableConfig().getFeedsConnectionRequestTimeout())
                .build();

        /*
        custom keep alive strategy. See 2.6 https://hc.apache.org/httpcomponents-client-4.5.x/tutorial/html/connmgmt.html
         */
        ConnectionKeepAliveStrategy autoCloseKeepAliveStrategy = new ConnectionKeepAliveStrategy() {
            @Override
            public long getKeepAliveDuration(HttpResponse response, HttpContext context) {
                HeaderElementIterator it = new BasicHeaderElementIterator
                        (response.headerIterator(HTTP.CONN_KEEP_ALIVE));
                while (it.hasNext()) {
                    HeaderElement he = it.nextElement();
                    String param = he.getName();
                    String value = he.getValue();
                    if (value != null && param.equalsIgnoreCase
                            ("timeout")) {
                        return Long.parseLong(value) * 1000;
                    }
                }
                return identityConfig.getReloadableConfig().getFeedsConnectionKeepAliveDefault();
            }
        };

        //create the client and set the custom context
        HttpClientBuilder b = HttpClientBuilder.create()
                .setSSLContext(sslContext)
                .setConnectionManager(poolingHttpClientConnectionManager)
                .setDefaultRequestConfig(requestConfig)
                .setDefaultSocketConfig(socketConfig)
                .setKeepAliveStrategy(autoCloseKeepAliveStrategy)
                ;

        CloseableHttpClient client = b.build();

        //start eviction daemon
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
            logger.warn("AtomHopperClient Exception posting token trr: ", e);
        }
    }

    @Async
    public void asyncPostUserTrr(BaseUser user, TokenRevocationRecord trr) {
        Validate.isTrue(user.getId().equals(trr.getTargetIssuedToId()));
        try {
            postUserTrr(user, trr);
        } catch (Exception e) {
            logger.warn("AtomHopperClient Exception posting user trr: ", e);
        }
    }

    public void postUserTrr(BaseUser user, TokenRevocationRecord trr) throws IOException {
        Validate.isTrue(user.getId().equals(trr.getTargetIssuedToId()));
        HttpPost httpPost = null;
        HttpResponse response = null;
        try {
            final UsageEntry entry = createUserTrrEntry(user, trr);

            httpPost = generatePostRequest(marshalEntry(entry));
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

    public void postUser(EndUser user, String userStatus) throws JAXBException, IOException, HttpException, URISyntaxException {
        HttpPost httpPost = null;
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
            }

            if (entry != null) {
                httpPost = generatePostRequest(marshalEntry(entry));
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

    public void postToken(EndUser user, String revokedToken) throws JAXBException, IOException, HttpException, URISyntaxException {
        HttpPost httpPost = null;
        HttpResponse response = null;
        try {
            final UsageEntry entry = createEntryForRevokeToken(user, revokedToken);
            httpPost = generatePostRequest(marshalEntry(entry));
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

    private HttpPost generatePostRequest(Writer writer) throws IOException {
        String authToken = atomHopperHelper.getAuthToken();

        //set connection params based on settings when run
        RequestConfig requestConfig = RequestConfig.copy(RequestConfig.DEFAULT)
                .setSocketTimeout(identityConfig.getReloadableConfig().getFeedsSocketTimeout())
                .setConnectTimeout(identityConfig.getReloadableConfig().getFeedsConnectionTimeout())
                .setConnectionRequestTimeout(identityConfig.getReloadableConfig().getFeedsConnectionRequestTimeout())
                .build();

        final HttpPost httpPost = new HttpPost(config.getString(AtomHopperConstants.ATOM_HOPPER_URL));
        httpPost.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_ATOM_XML);
        httpPost.setHeader("X-Auth-Token", authToken);
        httpPost.setEntity(createRequestEntity(writer.toString()));
        httpPost.setConfig(requestConfig);
        return httpPost;
    }

    public Writer marshalEntry(UsageEntry entry) throws JAXBException {
        final Writer writer = new StringWriter();
        final JAXBContext jc = JAXBContext.newInstance(UsageEntry.class, CloudIdentityType.class, com.rackspace.docs.event.identity.token.CloudIdentityType.class, com.rackspace.docs.event.identity.trr.user.CloudIdentityType.class);
        final Marshaller marshaller = jc.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FRAGMENT, true);
        marshaller.setProperty("com.sun.xml.bind.namespacePrefixMapper", new AHNamespaceMapper());
        marshaller.marshal(entry, writer);
        return writer;
    }

    public InputStreamEntity createRequestEntity(String s) throws UnsupportedEncodingException {
        return new InputStreamEntity(new ByteArrayInputStream(s.getBytes("UTF-8")), -1);
    }

    public UsageEntry createEntryForUser(EndUser user, EventType eventType, Boolean migrated) throws DatatypeConfigurationException {
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

    public UsageEntry createEntryForRevokeToken(EndUser user, String token) throws DatatypeConfigurationException, GeneralSecurityException, InvalidCipherTextException, UnsupportedEncodingException {
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

    /**
     * @param user
     * @param trr
     * @return
     *
     * @throws java.lang.IllegalArgumentException if one of the authenticatedByMethodGroups contains an auth method not
     * recognized by feed schema or error creating date for token creation date
     */
    private UsageEntry createUserTrrEntry(BaseUser user, TokenRevocationRecord trr) {
        logger.debug("Creating user trr entry ...");
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
        logger.debug("Created user trr entry with id: " + id);
        return usageEntry;
    }

    private UsageEntry createUsageEntry(Object cloudIdentityType, EventType eventType, String id, String resourceId, String resourceName, String title, String tenantId) {
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
                        wait(identityConfig.getReloadableConfig().getFeedsDaemonEvictionFrequency());

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
