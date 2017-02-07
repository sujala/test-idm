package com.rackspace.idm.domain.service.impl;

import com.rackspace.idm.GlobalConstants;
import com.rackspace.idm.api.error.ApiError;
import com.rackspace.idm.audit.Audit;
import com.rackspace.idm.domain.config.IdentityConfig;
import com.rackspace.idm.domain.dao.AuthDao;
import com.rackspace.idm.domain.dao.RackerDao;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.service.*;
import com.rackspace.idm.exception.*;
import com.rackspace.idm.util.RSAClient;
import com.rackspace.idm.validation.AuthorizationCodeCredentialsCheck;
import com.rackspace.idm.validation.BasicCredentialsCheck;
import com.rackspace.idm.validation.InputValidator;
import com.rackspace.idm.validation.RefreshTokenCredentialsCheck;
import org.apache.commons.configuration.Configuration;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tuckey.web.filters.urlrewrite.utils.StringUtils;

import javax.validation.groups.Default;
import java.util.List;
import java.util.UUID;

@Component
public class DefaultAuthenticationService implements AuthenticationService {

    @Autowired
    private ApplicationService applicationService;
    @Autowired
    private TenantService tenantService;
    @Autowired
    private ScopeAccessService scopeAccessService;
    @Autowired
    private AuthDao authDao;
    @Autowired
    private UserService userService;
    @Autowired
    private InputValidator inputValidator;
    @Autowired
    private Configuration config;
    @Autowired
    private RSAClient rsaClient;
    @Autowired
    private IdentityUserService identityUserService;
    @Autowired
    private IdentityConfig identityConfig;
    @Autowired
    private AuthorizationService authorizationService;
    @Autowired(required = false)
    private RackerDao rackerDao;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public UserAuthenticationResult authenticateDomainUsernamePassword(String username, String password, Domain domain) {
        // ToDo: Check what Domain to authenticate against, default Rackers
        if(!isTrustedServer()){
            throw new ForbiddenException();
        }
        return authenticateRacker(username, password, false);
    }

    @Override
    public UserAuthenticationResult authenticateDomainRSA(String username, String tokenkey, Domain domain) {
        // ToDo: Check what Domain to authenticate against, default Rackers
        if(!isTrustedServer()){
            throw new ForbiddenException();
        }
        return authenticateRacker(username, tokenkey, true);
    }

    @Override
    public void setAuthDao(AuthDao authDao) {
        this.authDao = authDao;
    }

    @Override
    public void setTenantService(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    @Override
    public void setScopeAccessService(ScopeAccessService scopeAccessService) {
        this.scopeAccessService = scopeAccessService;
    }

    @Override
    public void setApplicationService(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @Override
    public void setConfig(Configuration appConfig) {
        this.config = appConfig;
    }

    @Override
    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    @Override
    public void setInputValidator(InputValidator inputValidator) {
        this.inputValidator = inputValidator;
    }

    UserAuthenticationResult authenticateRacker(String username, String password, boolean usesRSAAuth) {
        logger.debug("Authenticating Racker: {}", username);

        if (!isTrustedServer()) {
            throw new ForbiddenException();
        }
        boolean authenticated;
        if (usesRSAAuth) {
            authenticated = rsaClient.authenticate(username, password);
        } else {
            authenticated = authDao.authenticate(username, password);
        }
        logger.debug("Authenticated Racker {} : {}", username, authenticated);

        if(!authenticated) {
            throw new NotAuthorizedException("Unable to authenticate user with credentials provided.");
        }

        Racker racker = userService.getRackerByRackerId(username);
        return new UserAuthenticationResult(racker, authenticated);
    }

    void validateCredentials(final Credentials trParam) {
        ApiError error = null;

        if (trParam == null || trParam.getGrantType() == null) {
            throw new BadRequestException("Invalid request: Missing or malformed parameter(s).");
        }

        switch (trParam.getOAuthGrantType()) {
            case PASSWORD:
                error = inputValidator.validate(trParam, Default.class, BasicCredentialsCheck.class);
                break;

            case REFRESH_TOKEN:
                error = inputValidator.validate(trParam, Default.class, RefreshTokenCredentialsCheck.class);
                break;

            case AUTHORIZATION_CODE:
                error = inputValidator.validate(trParam, Default.class, AuthorizationCodeCredentialsCheck.class);
                break;

            default:
                error = inputValidator.validate(trParam);
        }

        if (error != null) {
            String msg = String.format("Bad request parameters: %s", error.getMessage());
            logger.warn(msg);
            throw new BadRequestException(msg);
        }
    }

    boolean isTrustedServer() {
        return config.getBoolean("ldap.server.trusted", false);
    }
}
