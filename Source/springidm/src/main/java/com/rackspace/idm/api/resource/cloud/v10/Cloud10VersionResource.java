package com.rackspace.idm.api.resource.cloud.v10;

import com.rackspace.idm.api.converter.cloudv11.EndpointConverterCloudV11;
import com.rackspace.idm.api.resource.cloud.CloudClient;
import com.rackspace.idm.domain.entity.OpenstackEndpoint;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.entity.UserScopeAccess;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.domain.service.UserService;
import com.rackspace.idm.exception.NotAuthenticatedException;
import com.rackspace.idm.exception.UserDisabledException;
import com.rackspacecloud.docs.auth.api.v1.Endpoint;
import com.rackspacecloud.docs.auth.api.v1.Service;
import com.rackspacecloud.docs.auth.api.v1.ServiceCatalog;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.Date;
import java.util.List;

/**
 * Cloud Auth 1.0 API Version
 * 
 */
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Component
public class Cloud10VersionResource {

    public static final String HEADER_AUTH_TOKEN = "X-Auth-Token";
    public static final String HEADER_STORAGE_TOKEN = "X-Storage-Token";
    public static final String HEADER_STORAGE_URL = "X-Storage-Url";
    public static final String HEADER_CDN_URL = "X-CDN-Management-Url";
    public static final String HEADER_SERVER_MANAGEMENT_URL = "X-Server-Management-Url";
    public static final String HEADER_STORAGE_INTERNAL_URL = "X-Storage-Internal-Url";

    public static final String HEADER_AUTH_USER = "X-Auth-User";
    public static final String HEADER_AUTH_KEY = "X-Auth-Key";
    public static final String HEADER_STORAGE_USER = "X-Storage-User";
    public static final String HEADER_STORAGE_PASS = "X-Storage-Pass";

    public static final String AUTH_V1_0_FAILED_MSG = "Bad username or password";
    public static final String AUTH_V1_0_USR_DISABLED_MSG = "Account disabled";

    public static final String SERVICENAME_CLOUD_FILES = "cloudFiles";
    public static final String SERVICENAME_CLOUD_FILES_CDN = "cloudFilesCDN";
    public static final String SERVICENAME_CLOUD_SERVERS = "cloudServers";

    public static final String CACHE_CONTROL = "Cache-Control";

    private final Configuration config;
    private final CloudClient cloudClient;
    private final ScopeAccessService scopeAccessService;
    private final EndpointConverterCloudV11 endpointConverterCloudV11;
    private final UserService userService;

    @Autowired
    public Cloud10VersionResource(Configuration config,
        CloudClient cloudClient, ScopeAccessService scopeAccessService,
        EndpointConverterCloudV11 endpointConverterCloudV11,
        UserService userService) {
        this.config = config;
        this.cloudClient = cloudClient;
        this.scopeAccessService = scopeAccessService;
        this.endpointConverterCloudV11 = endpointConverterCloudV11;
        this.userService = userService;
    }

    @GET
    public Response getCloud10VersionInfo(@Context HttpHeaders httpHeaders,
        @HeaderParam(HEADER_AUTH_USER) String username,
        @HeaderParam(HEADER_AUTH_KEY) String key) throws IOException {

        Response.ResponseBuilder builder = Response.noContent();

        if(StringUtils.isBlank(username)){
            return builder.status(HttpServletResponse.SC_UNAUTHORIZED).entity(AUTH_V1_0_FAILED_MSG).build();
        }

        User user = this.userService.getUser(username);

        if(useCloudAuth() && !userService.isMigratedUser(user)) {
            Response cloudResponse = cloudClient.get(getCloudAuthV10Url(), httpHeaders).build();
            if (cloudResponse.getStatus() == 204 && user != null) {
                String token = cloudResponse.getMetadata().getFirst("X-Auth-Token").toString();
                scopeAccessService.updateUserScopeAccessTokenForClientIdByUser(user, getCloudAuthClientId(), token,
                        new DateTime().plusSeconds(getDefaultCloudAuthTokenExpirationSeconds()).toDate());
                return cloudResponse;
            }else if (user == null) {
                return cloudResponse;
            }
        }
        if (user == null) {
            return builder.status(HttpServletResponse.SC_UNAUTHORIZED).entity(AUTH_V1_0_FAILED_MSG).build();
        }

        try {
            UserScopeAccess usa = scopeAccessService.getUserScopeAccessForClientIdByUsernameAndApiCredentials(username, key, getCloudAuthClientId());
            List<OpenstackEndpoint> endpointlist = scopeAccessService.getOpenstackEndpointsForScopeAccess(usa);

            ServiceCatalog catalog = endpointConverterCloudV11.toServiceCatalog(endpointlist);

            List<Service> services = catalog.getService();

            builder.header(HEADER_AUTH_TOKEN, usa.getAccessTokenString());

            for (Service service : services) {
                if (SERVICENAME_CLOUD_FILES.equals(service.getName())) {
                    List<Endpoint> endpoints = service.getEndpoint();
                    for (Endpoint endpoint : endpoints) {
                        // Use single existing endpoint even if it's not default
                        if (endpoints.size() == 1 || endpoint.isV1Default()) {
                            addValuetoHeather(HEADER_STORAGE_URL, endpoint.getPublicURL(), builder);
                            builder.header(HEADER_STORAGE_TOKEN, usa.getAccessTokenString());
                            addValuetoHeather(HEADER_STORAGE_INTERNAL_URL, endpoint.getInternalURL(), builder);
                        }
                    }
                }

                if (SERVICENAME_CLOUD_FILES_CDN.equals(service.getName())) {
                    List<Endpoint> endpoints = service.getEndpoint();
                    for (Endpoint endpoint : endpoints) {
                        // Use single existing endpoint even if it's not default
                        if (endpoints.size() == 1 || endpoint.isV1Default()) {
                            addValuetoHeather(HEADER_CDN_URL, endpoint.getPublicURL(), builder);
                        }
                    }
                }

                if (SERVICENAME_CLOUD_SERVERS.equals(service.getName())) {
                    List<Endpoint> endpoints = service.getEndpoint();
                    for (Endpoint endpoint : endpoints) {
                        // Use single existing endpoint even if it's not default
                        if (endpoints.size() == 1 || endpoint.isV1Default()) {
                            addValuetoHeather(HEADER_SERVER_MANAGEMENT_URL,
                                endpoint.getPublicURL(), builder);
                        }
                    }
                }

            }

            long secondsLeft = (usa.getAccessTokenExp().getTime() - new Date().getTime()) / DateUtils.MILLIS_PER_SECOND;
            builder.header(CACHE_CONTROL, "s-maxage=" + secondsLeft);
            return builder.build();
        } catch (NotAuthenticatedException nae) {
            String errMsg = AUTH_V1_0_FAILED_MSG;
            return builder.status(HttpServletResponse.SC_UNAUTHORIZED).entity(errMsg).build();
        } catch (UserDisabledException ude) {
            String errMsg = AUTH_V1_0_FAILED_MSG;
            return builder.status(HttpServletResponse.SC_FORBIDDEN).entity(errMsg).build();
        } catch (Exception ex) {
            return builder.status(HttpServletResponse.SC_INTERNAL_SERVER_ERROR).build();
        }

    }

    private String getCloudAuthV10Url() {
        return config.getString("cloudAuth10url");
    }

    private boolean useCloudAuth() {
        return config.getBoolean("useCloudAuth", false);
    }

    private String getCloudAuthClientId() {
        return config.getString("cloudAuth.clientId");
    }

    /**
     * Will add NON-empty value to header.
     * @param builder
     */
    public static void addValuetoHeather(final String headerName,
        final String value, Response.ResponseBuilder builder) {
        if (!StringUtils.isEmpty(value)) {
            builder.header(headerName, value);
        }


    }

    private int getDefaultCloudAuthTokenExpirationSeconds() {
        return config.getInt("token.cloudAuthExpirationSeconds");
    }
}
