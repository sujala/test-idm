package com.rackspace.idm.controllers;

import javax.ws.rs.core.Response;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import com.rackspace.idm.config.LoggerFactoryWrapper;
import com.rackspace.idm.converters.PasswordRulesConverter;
import com.rackspace.idm.services.DefaultPasswordComplexityService;
import com.rackspace.idm.services.PasswordComplexityService;
import com.rackspace.idm.test.stub.StubLogger;

public class PasswordComplexityControllerTests {
    PasswordComplexityController controller;
    PasswordComplexityService service;
    String authHeader = "Token token=asdf1234";
    String goodPassword = "Ab1$XXXX";
    
    PasswordRulesConverter passwordRulesConverter = new PasswordRulesConverter();
    
    @Before
    public void setUp() {
        service = new DefaultPasswordComplexityService(new StubLogger());
        
        controller = new PasswordComplexityController(service, passwordRulesConverter, new LoggerFactoryWrapper());
    }
    
    @Test
    public void shouldGetRules() {
        
        MockHttpServletResponse response = new MockHttpServletResponse();
        
        com.rackspace.idm.jaxb.PasswordRules rules = controller.getRules();
        
        Assert.assertTrue(rules.getPasswordRules().size() == 5);
        Assert.assertTrue(response.getStatus() == Response.Status.OK.getStatusCode());
    }
    
    @Test
    public void shouldGetPassingResult() {
        MockHttpServletResponse response = new MockHttpServletResponse();
        
        com.rackspace.idm.jaxb.PasswordValidation result = controller.checkPassword(goodPassword);
        
        Assert.assertTrue(result.isValidPassword());
        Assert.assertTrue(response.getStatus() == Response.Status.OK.getStatusCode());
    }
}
