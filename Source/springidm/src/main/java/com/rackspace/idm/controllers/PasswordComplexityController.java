package com.rackspace.idm.controllers;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.annotations.providers.jaxb.json.Mapped;
import org.jboss.resteasy.annotations.providers.jaxb.json.XmlNsMap;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.rackspace.idm.GlobalConstants;
import com.rackspace.idm.config.LoggerFactoryWrapper;
import com.rackspace.idm.converters.PasswordRulesConverter;
import com.rackspace.idm.entities.passwordcomplexity.PasswordComplexityResult;
import com.rackspace.idm.entities.passwordcomplexity.PasswordRule;
import com.rackspace.idm.services.PasswordComplexityService;

/**
 * Password complexity resource
 */
@Path("/passwordrules")
@NoCache
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Component
public class PasswordComplexityController {

    private PasswordComplexityService passwordComplexityService;
    private PasswordRulesConverter passwordRulesConverter;
    private Logger logger;

    @Autowired
    public PasswordComplexityController(
        PasswordComplexityService passwordComplexityService,
        PasswordRulesConverter passwordRulesConverter,
        LoggerFactoryWrapper logger) {
        this.passwordComplexityService = passwordComplexityService;

        this.passwordRulesConverter = passwordRulesConverter;

        this.logger = logger.getLogger(PasswordComplexityController.class);
    }

    /**
     * Password Complexity Rules resource.
     * 
     * List of Rules required for password complexity
     * 
     * @RequestHeader Authorization Authorization header. For Example - Token
     *                token="XXXX"
     * @return A list of password complexity rules
     * 
     * @HTTP 200 If rules are found
     * @HTTP 401 If unauthorized
     */
    @GET
    @Path("/")
    @Mapped(namespaceMap = {@XmlNsMap(namespace = GlobalConstants.XML_NAMESPACE, jsonName = GlobalConstants.JSON_NAMESPACE)})
    public com.rackspace.idm.jaxb.PasswordRules getRules() {

        logger.debug("Getting password complexity rules");

        List<PasswordRule> rules = passwordComplexityService.getRules();

        logger.debug("Got password complexity rules: {}", rules);

        return passwordRulesConverter.toPaswordRulesJaxb(rules);
    }

    /**
     * Password Complexity Result resource.
     * 
     * A Result which lists all rules and whether a password passed the rule
     * 
     * @RequestHeader Authorization Authorization header. For Example - Token
     *                token="XXXX"
     * @return A Password Complexity Result
     * 
     * @HTTP 200 If result is returned
     * @HTTP 401 If unauthorized
     */
    @GET
    @Path("/validation/{password}")
    @Mapped(namespaceMap = {@XmlNsMap(namespace = GlobalConstants.XML_NAMESPACE, jsonName = GlobalConstants.JSON_NAMESPACE)})
    public com.rackspace.idm.jaxb.PasswordValidation checkPassword(
        @PathParam("password") String password) {

        logger.debug("Checking password {} against password complexity rules",
            password);

        PasswordComplexityResult result = passwordComplexityService
            .checkPassword(password);

        com.rackspace.idm.jaxb.PasswordValidation jaxbValidation = passwordRulesConverter
            .toPasswordValidationJaxb(result);

        logger
            .debug(
                "Checked password {} against password complexity rules - isValid = {}",
                result.isValidPassword());

        return jaxbValidation;
    }
}
