package com.rackspace.idm.config;

import javax.validation.Validation;
import javax.validation.Validator;

import net.spy.memcached.MemcachedClient;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

import com.rackspace.idm.dao.AccessTokenDao;
import com.rackspace.idm.dao.AuthDao;
import com.rackspace.idm.dao.ClientDao;
import com.rackspace.idm.dao.CustomerDao;
import com.rackspace.idm.dao.EndpointDao;
import com.rackspace.idm.dao.RefreshTokenDao;
import com.rackspace.idm.dao.RoleDao;
import com.rackspace.idm.dao.UserDao;
import com.rackspace.idm.dao.XdcAccessTokenDao;
import com.rackspace.idm.entities.EmailSettings;
import com.rackspace.idm.entities.RefreshTokenDefaultAttributes;
import com.rackspace.idm.oauth.DefaultOAuthService;
import com.rackspace.idm.oauth.OAuthService;
import com.rackspace.idm.services.AccessTokenService;
import com.rackspace.idm.services.AuthorizationService;
import com.rackspace.idm.services.ClientService;
import com.rackspace.idm.services.CustomerService;
import com.rackspace.idm.services.DefaultAccessTokenService;
import com.rackspace.idm.services.DefaultAuthorizationService;
import com.rackspace.idm.services.DefaultClientService;
import com.rackspace.idm.services.DefaultCustomerService;
import com.rackspace.idm.services.DefaultEmailService;
import com.rackspace.idm.services.DefaultEndpointService;
import com.rackspace.idm.services.DefaultPasswordComplexityService;
import com.rackspace.idm.services.DefaultRefreshTokenService;
import com.rackspace.idm.services.DefaultRoleService;
import com.rackspace.idm.services.DefaultUserService;
import com.rackspace.idm.services.EmailService;
import com.rackspace.idm.services.EndpointService;
import com.rackspace.idm.services.HealthMonitoringService;
import com.rackspace.idm.services.PasswordComplexityService;
import com.rackspace.idm.services.RefreshTokenService;
import com.rackspace.idm.services.RoleService;
import com.rackspace.idm.services.UserService;
import com.rackspace.idm.util.AuthHeaderHelper;
import com.rackspace.idm.util.PingableService;
import com.rackspace.idm.validation.InputValidator;

/**
 * @author john.eo <br/>
 *         Add dependency configurations for services here.<br/>
 *         Note that the @Autowired object is automatically instantiated by
 *         Spring. The methods with @Bean are used by Spring to satisfy for
 *         objects with dependency for the return type.
 */
@org.springframework.context.annotation.Configuration
public class ServiceConfiguration {
    @Autowired
    private UserDao userRepo;
    @Autowired
    private ClientDao clientDao;
    @Autowired
    private AccessTokenDao accessTokenDao;
    @Autowired
    private CustomerDao customerDao;
    @Autowired
    private RefreshTokenDao refreshTokenDao;
    @Autowired
    private RoleDao roleDao;
    @Autowired
    private AuthDao authDao;
    @Autowired
    private MemcachedClient memcached;
    @Autowired
    private EndpointDao endpointDao;
    @Autowired
    private XdcAccessTokenDao xdcTokenDao;

    @Value("#{memcacheStatusRepository}")
    private PingableService memcacheService;

    @Value("#{ldapStatusRepository}")
    private PingableService ldapRepository;

    @Autowired
    private Configuration config;

    public ServiceConfiguration() {
    }

    /**
     * Use for unit tests.
     * 
     * @param config
     * @param logger
     */
    public ServiceConfiguration(Configuration config) {
        this.config = config;
    }

    @Bean
    public AccessTokenService tokenService() {
        Logger logger = LoggerFactory.getLogger(DefaultAccessTokenService.class);
        return new DefaultAccessTokenService(accessTokenDao, clientDao, userService(), xdcTokenDao,
            authHeaderHelper(), config, logger);
    }

    @Bean
    public AuthHeaderHelper authHeaderHelper() {
        return new AuthHeaderHelper();
    }

    @Bean
    public HealthMonitoringService healthMonitoringBean() {
        return new HealthMonitoringService(memcacheService, ldapRepository);
    }

    @Bean
    public ClientService clientService() {
        Logger logger = LoggerFactory.getLogger(DefaultClientService.class);
        return new DefaultClientService(clientDao, customerDao, userRepo, logger);
    }

    @Bean
    public EndpointService endpointService() {
        Logger logger = LoggerFactory.getLogger(DefaultEndpointService.class);
        return new DefaultEndpointService(endpointDao, logger);
    }

    @Bean
    public CustomerService customerService() {
        Logger logger = LoggerFactory.getLogger(DefaultCustomerService.class);
        return new DefaultCustomerService(clientDao, customerDao, userRepo, logger);
    }

    @Bean
    public EmailService emailService() {

        org.apache.commons.configuration.Configuration config;
        String propsFileLoc = "emailservice.properties";

        try {
            config = new PropertiesConfiguration(propsFileLoc);
        } catch (ConfigurationException e) {
            e.printStackTrace();
            throw new RuntimeException("Error encountered when loading the token config file.");
        }

        int smtpPort = config.getInt("smtpPort");
        String smtpHost = config.getString("hostName");
        String smtpUsername = config.getString("smtpUsername");
        String smtpPassword = config.getString("smtpPassword");
        boolean debug = config.getBoolean("debug");
        boolean useSSL = config.getBoolean("SSL");
        boolean useTSL = config.getBoolean("TLS");

        EmailSettings emailSettings = new EmailSettings(smtpPort, smtpHost, smtpUsername, smtpPassword,
            debug, useSSL, useTSL);

        return new DefaultEmailService(emailSettings);
    }

    @Bean
    public InputValidator inputValidator() {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        return new InputValidator(validator);
    }

    @Bean
    public PasswordComplexityService passwordComplexityService() {
        Logger logger = LoggerFactory.getLogger(DefaultPasswordComplexityService.class);
        return new DefaultPasswordComplexityService(logger);
    }

    @Bean
    public RefreshTokenService refreshTokenService() {
        int defaultTokenExpirationSeconds = config.getInt("token.refreshTokenExpirationSeconds");
        String dataCenterPrefix = config.getString("token.dataCenterPrefix");

        RefreshTokenDefaultAttributes defaultAttributes = new RefreshTokenDefaultAttributes(
            defaultTokenExpirationSeconds, dataCenterPrefix);

        Logger logger = LoggerFactory.getLogger(DefaultRefreshTokenService.class);

        return new DefaultRefreshTokenService(defaultAttributes, refreshTokenDao, logger);
    }

    @Bean
    public RoleService roleService() {
        Logger logger = LoggerFactory.getLogger(DefaultRoleService.class);
        return new DefaultRoleService(roleDao, userRepo, logger);
    }

    @Bean
    public UserService userService() {
        Logger logger = LoggerFactory.getLogger(DefaultUserService.class);
        boolean isTrustedServer = config.getBoolean("ldap.server.trusted", false);
        return new DefaultUserService(userRepo, authDao, customerDao, emailService(), clientService(),
            isTrustedServer, logger);
    }

    @Bean
    public OAuthService oauthService() {
        return new DefaultOAuthService(userService(), clientService(), tokenService(), refreshTokenService(),
            authorizationService(), config);
    }

    @Bean
    public AuthorizationService authorizationService() {
        Logger logger = LoggerFactory.getLogger(DefaultAuthorizationService.class);
        return new DefaultAuthorizationService(clientDao, memcached, config, logger);
    }
}
