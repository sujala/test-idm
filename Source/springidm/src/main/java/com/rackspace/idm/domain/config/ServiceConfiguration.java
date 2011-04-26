package com.rackspace.idm.domain.config;

import java.util.HashMap;
import java.util.Map;

import javax.validation.Validation;
import javax.validation.Validator;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.jmx.export.MBeanExporter;

import com.rackspace.idm.domain.dao.ApiDocDao;
import com.rackspace.idm.domain.dao.AuthDao;
import com.rackspace.idm.domain.dao.ClientDao;
import com.rackspace.idm.domain.dao.CustomerDao;
import com.rackspace.idm.domain.dao.EndpointDao;
import com.rackspace.idm.domain.dao.RoleDao;
import com.rackspace.idm.domain.dao.ScopeAccessObjectDao;
import com.rackspace.idm.domain.dao.UserDao;
import com.rackspace.idm.domain.entity.EmailSettings;
import com.rackspace.idm.domain.service.ApiDocService;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.ClientService;
import com.rackspace.idm.domain.service.CustomerService;
import com.rackspace.idm.domain.service.EmailService;
import com.rackspace.idm.domain.service.EndpointService;
import com.rackspace.idm.domain.service.OAuthService;
import com.rackspace.idm.domain.service.PasswordComplexityService;
import com.rackspace.idm.domain.service.RoleService;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.domain.service.UserService;
import com.rackspace.idm.domain.service.impl.DefaultApiDocService;
import com.rackspace.idm.domain.service.impl.DefaultAuthorizationService;
import com.rackspace.idm.domain.service.impl.DefaultClientService;
import com.rackspace.idm.domain.service.impl.DefaultCustomerService;
import com.rackspace.idm.domain.service.impl.DefaultEmailService;
import com.rackspace.idm.domain.service.impl.DefaultEndpointService;
import com.rackspace.idm.domain.service.impl.DefaultOAuthService;
import com.rackspace.idm.domain.service.impl.DefaultPasswordComplexityService;
import com.rackspace.idm.domain.service.impl.DefaultRoleService;
import com.rackspace.idm.domain.service.impl.DefaultScopeAccessService;
import com.rackspace.idm.domain.service.impl.DefaultUserService;
import com.rackspace.idm.util.AuthHeaderHelper;
import com.rackspace.idm.util.LdapRouterMBean;
import com.rackspace.idm.util.LoggerMBean;
import com.rackspace.idm.util.MemcacheMBean;
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
    private CustomerDao customerDao;
    @Autowired
    private RoleDao roleDao;
    @Autowired
    private AuthDao authDao;
    @Autowired
    private EndpointDao endpointDao;
    @Autowired
    private ScopeAccessObjectDao scopeAccessDao;
    @Autowired
    private ApiDocDao apiDocDao;
    @Autowired
    private InputValidator inputValidator;

    @Autowired
    private Configuration config;

    public ServiceConfiguration() {
    }

    /**
     * Use for unit tests.
     * 
     * @param config
     */
    public ServiceConfiguration(Configuration config) {
        this.config = config;
    }

    @Bean
    public AuthHeaderHelper authHeaderHelper() {
        return new AuthHeaderHelper();
    }

    @Bean
    public LoggerMBean loggerMonitoringBean() {
        return new LoggerMBean();
    }

    @Bean
    public LdapRouterMBean ldapRouterMonitoringBean() {
        return new LdapRouterMBean();
    }

    @Bean
    public MemcacheMBean memcacheMonitoringBean() {
        return new MemcacheMBean();
    }

    @Bean
    public ClientService clientService() {
        return new DefaultClientService(clientDao, customerDao, userRepo);
    }

    @Bean
    public EndpointService endpointService() {
        return new DefaultEndpointService(endpointDao);
    }

    @Bean
    public CustomerService customerService() {
        return new DefaultCustomerService(clientDao, customerDao, userRepo);
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
        return new DefaultPasswordComplexityService();
    }

    @Bean
    public RoleService roleService() {
        return new DefaultRoleService(roleDao, userRepo);
    }
    
    @Bean
    public ScopeAccessService scopeAccessService() {
        return new DefaultScopeAccessService(userService(),
            clientService(), scopeAccessDao);
    }

    @Bean
    public UserService userService() {
        boolean isTrustedServer = config.getBoolean("ldap.server.trusted", false);
        return new DefaultUserService(userRepo, authDao, customerDao, emailService(), clientService(),
            isTrustedServer);
    }

    @Bean
    public OAuthService oauthService() {
        return new DefaultOAuthService(userService(), clientService(),
            authorizationService(), config, inputValidator, scopeAccessService());
    }

    @Bean
    public AuthorizationService authorizationService() {
        return new DefaultAuthorizationService(scopeAccessDao, clientService(), config);
    }
    
    @Bean
    public ApiDocService apiDocService() {
        return new DefaultApiDocService(apiDocDao);
    }

    @Bean
    public MBeanExporter exporter() {
        MBeanExporter exp = new MBeanExporter();
        Map<String, Object> beans = new HashMap<String, Object>();
        beans.put("com.rackspace.idm:name=loggerMonitoringBean", loggerMonitoringBean());
        beans.put("com.rackspace.idm:name=ldapRouterMonitoringBean", ldapRouterMonitoringBean());
        beans.put("com.rackspace.idm:name=memcacheMonitoringBean", memcacheMonitoringBean());
        exp.setBeans(beans);
        return exp;
    }
}
