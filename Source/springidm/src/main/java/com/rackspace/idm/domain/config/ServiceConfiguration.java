package com.rackspace.idm.domain.config;

import java.util.HashMap;
import java.util.Map;

import javax.validation.Validation;
import javax.validation.Validator;

import org.apache.commons.configuration.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.jmx.export.MBeanExporter;

import com.rackspace.idm.domain.dao.ApiDocDao;
import com.rackspace.idm.domain.dao.AuthDao;
import com.rackspace.idm.domain.dao.ApplicationDao;
import com.rackspace.idm.domain.dao.CustomerDao;
import com.rackspace.idm.domain.dao.EndpointDao;
import com.rackspace.idm.domain.dao.ScopeAccessDao;
import com.rackspace.idm.domain.dao.TenantDao;
import com.rackspace.idm.domain.dao.UserDao;
import com.rackspace.idm.domain.service.ApiDocService;
import com.rackspace.idm.domain.service.AuthenticationService;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.ApplicationService;
import com.rackspace.idm.domain.service.CustomerService;
import com.rackspace.idm.domain.service.EndpointService;
import com.rackspace.idm.domain.service.TokenService;
import com.rackspace.idm.domain.service.PasswordComplexityService;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.domain.service.TenantService;
import com.rackspace.idm.domain.service.UserService;
import com.rackspace.idm.domain.service.impl.DefaultApiDocService;
import com.rackspace.idm.domain.service.impl.DefaultAuthenticationService;
import com.rackspace.idm.domain.service.impl.DefaultAuthorizationService;
import com.rackspace.idm.domain.service.impl.DefaultApplicationService;
import com.rackspace.idm.domain.service.impl.DefaultCustomerService;
import com.rackspace.idm.domain.service.impl.DefaultEndpointService;
import com.rackspace.idm.domain.service.impl.DefaultTokenService;
import com.rackspace.idm.domain.service.impl.DefaultPasswordComplexityService;
import com.rackspace.idm.domain.service.impl.DefaultScopeAccessService;
import com.rackspace.idm.domain.service.impl.DefaultTenantService;
import com.rackspace.idm.domain.service.impl.DefaultUserService;
import com.rackspace.idm.util.AuthHeaderHelper;
import com.rackspace.idm.util.LdapRouterMBean;
import com.rackspace.idm.util.LoggerMBean;
import com.rackspace.idm.util.WadlTrie;
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

	//IMPORTANT!! This is at the top of the file, because every service is dependent on configuration. We want to ensure
	//this is autowired first. If not, one of the autowired objects in this configuration might be depedent on config, and
	//if it has not been autowired, it will result in null config.
    @Autowired
    private Configuration config;

    @Autowired
    private UserDao userRepo;
    @Autowired
    private ApplicationDao clientDao;
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
    public ApplicationService clientService() {
        return new DefaultApplicationService(scopeAccessDao, clientDao, customerDao,
            userRepo);
    }

    @Bean
    public EndpointService endpointService() {
        return new DefaultEndpointService(endpointDao);
    }

    @Bean
    public CustomerService customerService() {
        return new DefaultCustomerService(clientDao, customerDao, userRepo, tokenService());
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
            scopeAccessDao, tenantDao, endpointDao, authHeaderHelper(), config);
    }

    @Bean
    public UserService userService() {
        return new DefaultUserService(userRepo, authDao,
            scopeAccessDao, clientService(), config, tokenService(), passwordComplexityService());
    }

    @Bean
    public TokenService tokenService() {
        return new DefaultTokenService(clientService(),
            authorizationService(), config, inputValidator,
            scopeAccessService(), userRepo, tenantService());
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
        return new DefaultTenantService(tenantDao, clientDao, userRepo, scopeAccessDao);
    }
    
    @Bean
    public AuthenticationService authenticationService() {
    	return new DefaultAuthenticationService(tokenService(), authDao, tenantService(), scopeAccessService(),
    			clientDao, config, userRepo, customerDao, inputValidator);
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
