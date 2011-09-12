package com.rackspace.idm.domain.config;

import com.rackspace.idm.domain.dao.*;
import com.rackspace.idm.domain.service.*;
import com.rackspace.idm.domain.service.impl.*;
import com.rackspace.idm.util.AuthHeaderHelper;
import com.rackspace.idm.util.LdapRouterMBean;
import com.rackspace.idm.util.LoggerMBean;
import com.rackspace.idm.util.WadlTrie;
import com.rackspace.idm.validation.InputValidator;
import org.apache.commons.configuration.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.jmx.export.MBeanExporter;

import javax.validation.Validation;
import javax.validation.Validator;
import java.util.HashMap;
import java.util.Map;

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
    private AuthDao authDao;
    @Autowired
    private EndpointDao endpointDao;
    @Autowired
    private ScopeAccessDao scopeAccessDao;
    @Autowired
    private ApiDocDao apiDocDao;
    @Autowired
    private InputValidator inputValidator;
    @Autowired
    private TenantDao tenantDao;

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
    public ClientService clientService() {
        return new DefaultClientService(scopeAccessDao, clientDao, customerDao,
            userRepo);
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
    public WadlTrie wadlTrie() {
        return new WadlTrie();
    }

    @Bean
    public InputValidator inputValidator() {
        final Validator validator = Validation.buildDefaultValidatorFactory()
            .getValidator();
        return new InputValidator(validator);
    }

    @Bean
    public PasswordComplexityService passwordComplexityService() {
        return new DefaultPasswordComplexityService();
    }

    @Bean
    public ScopeAccessService scopeAccessService() {
        return new DefaultScopeAccessService(userRepo, clientDao,
            scopeAccessDao, authHeaderHelper(), config);
    }

    @Bean
    public UserService userService() {
        return new DefaultUserService(userRepo, authDao, customerDao,
            scopeAccessDao, clientService(), config);
    }

    @Bean
    public OAuthService oauthService() {
        return new DefaultOAuthService(userService(), clientService(),
            authorizationService(), config, inputValidator,
            scopeAccessService());
    }

    @Bean
    public AuthorizationService authorizationService() {
        return new DefaultAuthorizationService(scopeAccessDao, clientDao,
            wadlTrie(), config);
    }

    @Bean
    public ApiDocService apiDocService() {
        return new DefaultApiDocService(apiDocDao);
    }
    
    @Bean
    public TenantService tenantService() {
        return new DefaultTenantService(tenantDao);
    }

    @Bean
    public MBeanExporter exporter() {
        final MBeanExporter exp = new MBeanExporter();
        final Map<String, Object> beans = new HashMap<String, Object>();
        beans.put("com.rackspace.idm:name=loggerMonitoringBean",
            loggerMonitoringBean());
        beans.put("com.rackspace.idm:name=ldapRouterMonitoringBean",
            ldapRouterMonitoringBean());
        exp.setBeans(beans);
        return exp;
    }
}
