package com.rackspace.idm.api.resource.cloud.v10;

import com.rackspace.idm.GlobalConstants;
import com.rackspace.idm.api.converter.cloudv11.EndpointConverterCloudV11;
import com.rackspace.idm.api.resource.cloud.v20.AuthResponseTuple;
import com.rackspace.idm.api.resource.cloud.v20.AuthWithApiKeyCredentials;
import com.rackspace.idm.api.resource.cloud.v20.AuthenticateResponseService;
import com.rackspace.idm.api.security.RequestContextHolder;
import com.rackspace.idm.domain.config.IdentityConfig;
import com.rackspace.idm.domain.entity.Tenant;
import com.rackspace.idm.domain.entity.UserAuthenticationResult;
import com.rackspace.idm.domain.entity.UserScopeAccess;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.domain.service.ServiceCatalogInfo;
import com.rackspace.idm.event.ApiResourceType;
import com.rackspace.idm.event.IdentityApi;
import com.rackspace.idm.exception.ForbiddenException;
import com.rackspace.idm.exception.NotAuthenticatedException;
import com.rackspace.idm.exception.UserDisabledException;
import com.rackspacecloud.docs.auth.api.v1.Endpoint;
import com.rackspacecloud.docs.auth.api.v1.Service;
import com.rackspacecloud.docs.auth.api.v1.ServiceCatalog;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
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

    public static final String HEADER_AUTH_USER = "X-Auth-User";
    public static final String HEADER_AUTH_KEY = "X-Auth-Key";
    public static final String HEADER_STORAGE_USER = "X-Storage-User";
    public static final String HEADER_STORAGE_PASS = "X-Storage-Pass";

    public static final String AUTH_V1_0_FAILED_MSG = "Bad username or password";

    public static final String SERVICENAME_CLOUD_FILES = "cloudFiles";
    public static final String SERVICENAME_CLOUD_FILES_CDN = "cloudFilesCDN";
    public static final String SERVICENAME_CLOUD_SERVERS = "cloudServers";

    public static final String CACHE_CONTROL = "Cache-Control";

    private final ScopeAccessService scopeAccessService;
    private final EndpointConverterCloudV11 endpointConverterCloudV11;
    private final AuthWithApiKeyCredentials authWithApiKeyCredentials;
    private final AuthorizationService authorizationService;

    @Autowired
    AuthenticateResponseService authenticateResponseService;

    @Autowired
    IdentityConfig identityConfig;

    @Autowired
    private RequestContextHolder requestContextHolder;

    @Autowired
    public Cloud10VersionResource(ScopeAccessService scopeAccessService,
        EndpointConverterCloudV11 endpointConverterCloudV11,
        AuthWithApiKeyCredentials authWithApiKeyCredentials,
        AuthorizationService authorizationService) {
        this.scopeAccessService = scopeAccessService;
        this.endpointConverterCloudV11 = endpointConverterCloudV11;
        this.authWithApiKeyCredentials = authWithApiKeyCredentials;
        this.authorizationService = authorizationService;
    }

    @IdentityApi(apiResourceType = ApiResourceType.AUTH)
    @GET
    public Response getCloud10VersionInfo(@Context HttpHeaders httpHeaders,
        @HeaderParam(HEADER_AUTH_USER) String username,
        @HeaderParam(HEADER_AUTH_KEY) String key,
        @HeaderParam(HEADER_STORAGE_USER) String storageUser,
        @HeaderParam(HEADER_STORAGE_PASS) String storagePass) throws IOException {

        Response.ResponseBuilder builder = Response.noContent();

        if(username == null && storageUser != null) {
            username = storageUser;
            key = storagePass;
        }

        if(StringUtils.isBlank(username)){
            return builder.status(HttpServletResponse.SC_UNAUTHORIZED).entity(AUTH_V1_0_FAILED_MSG).build();
        }

        try {
            requestContextHolder.getAuthenticationContext().setUsername(username);

            UserAuthenticationResult result = authWithApiKeyCredentials.authenticate(username, key);
            ServiceCatalogInfo scInfo = scopeAccessService.getServiceCatalogInfo(result.getUser());

            //verify the user is allowed to login
            if (authorizationService.restrictUserAuthentication(scInfo)) {
                throw new ForbiddenException(GlobalConstants.ALL_TENANTS_DISABLED_ERROR_MESSAGE);
            }

            //create the scope access (if necessary)
            AuthResponseTuple authResponseTuple = scopeAccessService.createScopeAccessForUserAuthenticationResult(result);
            UserScopeAccess usa = authResponseTuple.getUserScopeAccess();

            ServiceCatalog catalog = endpointConverterCloudV11.toServiceCatalog(scInfo.getUserEndpoints());

            List<Service> services = catalog.getService();

            builder.header(HEADER_AUTH_TOKEN, usa.getAccessTokenString());

            for (Service service : services) {
                if (SERVICENAME_CLOUD_FILES.equals(service.getName())) {
                    List<Endpoint> endpoints = service.getEndpoint();
                    addValuetoHeather(HEADER_STORAGE_URL, endpoints.get(0).getPublicURL(), builder);
                    builder.header(HEADER_STORAGE_TOKEN, usa.getAccessTokenString());
                }

                if (SERVICENAME_CLOUD_FILES_CDN.equals(service.getName())) {
                    List<Endpoint> endpoints = service.getEndpoint();
                    addValuetoHeather(HEADER_CDN_URL, endpoints.get(0).getPublicURL(), builder);
                }

                if (SERVICENAME_CLOUD_SERVERS.equals(service.getName())) {
                    List<Endpoint> endpoints = service.getEndpoint();
                    addValuetoHeather(HEADER_SERVER_MANAGEMENT_URL, endpoints.get(0).getPublicURL(), builder);
                }
            }

            long secondsLeft = (usa.getAccessTokenExp().getTime() - new Date().getTime()) / DateUtils.MILLIS_PER_SECOND;
            builder.header(CACHE_CONTROL, "s-maxage=" + secondsLeft);

            if (identityConfig.getReloadableConfig().shouldIncludeTenantInV10AuthResponse()) {
                Tenant tenant = authenticateResponseService.getTenantForAuthResponseTenantHeader(scInfo);
                if (tenant != null) {
                    builder.header(GlobalConstants.X_TENANT_ID, tenant.getTenantId());
                }
            }

            return builder.build();
        } catch(ForbiddenException fex) {
            return builder.status(HttpServletResponse.SC_FORBIDDEN).entity(fex.getMessage()).build();
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

}
