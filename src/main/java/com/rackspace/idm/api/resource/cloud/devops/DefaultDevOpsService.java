package com.rackspace.idm.api.resource.cloud.devops;

import com.rackspace.idm.api.filter.LdapLoggingFilter;
import com.rackspace.idm.domain.entity.ScopeAccess;
import com.rackspace.idm.domain.security.encrypters.CacheableKeyCzarCrypterLocator;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.domain.service.UserService;
import com.rackspace.idm.exception.NotAuthorizedException;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.File;
import java.io.IOException;

@Component
public class DefaultDevOpsService implements DevOpsService {
    private static final Logger LOG = Logger.getLogger(DefaultDevOpsService.class);

    @Autowired
    private AuthorizationService authorizationService;

    @Autowired
    private ScopeAccessService scopeAccessService;

    @Autowired
    UserService userService;

    @Autowired
    private Configuration globalConfig;

    @Autowired(required = false)
    private CacheableKeyCzarCrypterLocator cacheableKeyCzarCrypterLocator;

    @Override
    @Async
    public void encryptUsers(String authToken) {
        authorizationService.verifyServiceAdminLevelAccess(getScopeAccessForValidToken(authToken));
        userService.reEncryptUsers();
    }

    @Override
    public Response.ResponseBuilder getLdapLog(UriInfo uriInfo, String authToken, String logName) {
        authorizationService.verifyServiceAdminLevelAccess(getScopeAccessForValidToken(authToken));

        if (!isLdapLoggingAllowed()) {
            throw new WebApplicationException(404);
        }

        try {
            File logDir = getLogParentDir();
            File logFile = new File(logDir, logName);
            String logContents = FileUtils.readFileToString(logFile);

            Response.ResponseBuilder response = Response.ok().entity(logContents);

            return response;
        } catch (IOException e) {
            LOG.error("Encountered exception creating ldap log. Logging disabled", e);
            throw new RuntimeException("Error retrieving log", e);
        }
    }

    @Override
    public Response.ResponseBuilder getKeyMetadata(String authToken) {
        authorizationService.verifyServiceAdminLevelAccess(getScopeAccessForValidToken(authToken));
        if (cacheableKeyCzarCrypterLocator == null) {
            return Response.noContent();
        } else {
            final com.rackspace.docs.identity.api.ext.rax_auth.v1.ObjectFactory factory = new com.rackspace.docs.identity.api.ext.rax_auth.v1.ObjectFactory();
            return Response.ok().entity(factory.createMetadata(cacheableKeyCzarCrypterLocator.getCacheInfo()));
        }
    }

    @Override
    public Response.ResponseBuilder resetKeyMetadata(String authToken) {
        authorizationService.verifyServiceAdminLevelAccess(getScopeAccessForValidToken(authToken));
        if (cacheableKeyCzarCrypterLocator == null) {
            return Response.noContent();
        } else {
            cacheableKeyCzarCrypterLocator.resetCache();
            final com.rackspace.docs.identity.api.ext.rax_auth.v1.ObjectFactory factory = new com.rackspace.docs.identity.api.ext.rax_auth.v1.ObjectFactory();
            return Response.ok().entity(factory.createMetadata(cacheableKeyCzarCrypterLocator.getCacheInfo()));
        }
    }

    private File getLogParentDir() {
        String logFileDir = globalConfig.getString(LdapLoggingFilter.UNBOUNDID_LOG_LOCATION_PROP_NAME, LdapLoggingFilter.UNBOUNDID_LOG_LOCATION_DEFAULT);
        File logDir = new File(logFileDir);
        if (!logDir.exists()) {
            if (!logDir.mkdirs()) {
                throw new IllegalStateException("Could not create directories '" + logFileDir + " for ldap log files.");
            }
        }
        if (!logDir.isDirectory()) {
            throw new IllegalArgumentException("'" + logFileDir + "' is not a directory. Please set " + LdapLoggingFilter.UNBOUNDID_LOG_LOCATION_PROP_NAME + " to a directory.");
        }
        return logDir;
    }

    private boolean isLdapLoggingAllowed() {
        return globalConfig.getBoolean(LdapLoggingFilter.UNBOUND_LOG_ALLOW_PROP_NAME, LdapLoggingFilter.UNBOUND_LOG_ALLOW_DEFAULT);
    }

    private ScopeAccess getScopeAccessForValidToken(String authToken) {
        String errMsg = "No valid token provided. Please use the 'X-Auth-Token' header with a valid token.";
        if (StringUtils.isBlank(authToken)) {
            throw new NotAuthorizedException(errMsg);
        }
        ScopeAccess authScopeAccess = this.scopeAccessService.getScopeAccessByAccessToken(authToken);
        if (authScopeAccess == null || (authScopeAccess.isAccessTokenExpired(new DateTime()))) {
            throw new NotAuthorizedException(errMsg);
        }
        return authScopeAccess;
    }
}
