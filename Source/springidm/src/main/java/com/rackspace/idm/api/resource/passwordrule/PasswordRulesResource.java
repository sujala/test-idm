package com.rackspace.idm.api.resource.passwordrule;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.rackspace.idm.api.converter.PasswordRulesConverter;
import com.rackspace.idm.domain.entity.PasswordComplexityResult;
import com.rackspace.idm.domain.entity.PasswordRule;
import com.rackspace.idm.domain.service.PasswordComplexityService;
import com.rackspace.idm.exception.BadRequestException;

/**
 * Password Complexity Rules
 *
 */
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Component
public class PasswordRulesResource {

    private final PasswordComplexityService passwordComplexityService;
    private final PasswordRulesConverter passwordRulesConverter;
    final private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    public PasswordRulesResource(
        PasswordComplexityService passwordComplexityService,
        PasswordRulesConverter passwordRulesConverter) {
        this.passwordComplexityService = passwordComplexityService;
        this.passwordRulesConverter = passwordRulesConverter;
    }

    /**
     * Gets a list of password complexity rules
     * 
     * @response.representation.200.qname {http://docs.rackspacecloud.com/idm/api/v1.0}passwordRules
     * @response.representation.400.qname {http://docs.rackspacecloud.com/idm/api/v1.0}badRequest
     * @response.representation.401.qname {http://docs.rackspacecloud.com/idm/api/v1.0}unauthorized
     * @response.representation.403.qname {http://docs.rackspacecloud.com/idm/api/v1.0}forbidden
     * @response.representation.404.qname {http://docs.rackspacecloud.com/idm/api/v1.0}itemNotFound
     * @response.representation.500.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serverError
     * @response.representation.503.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serviceUnavailable
     * 
     * @param authHeader HTTP Authorization header for authenticating the caller.
     */
    @GET
    public Response getRules(@HeaderParam("Authorization") String authHeader) {

        logger.debug("Getting password complexity rules");
        List<PasswordRule> rules = passwordComplexityService.getRules();
        logger.debug("Got password complexity rules: {}", rules);
        return Response.ok(passwordRulesConverter.toPaswordRulesJaxb(rules)).build();
    }

    /**
     * Validates a password against the password complexity rules
     * 
     * @request.representation.qname {http://docs.rackspacecloud.com/idm/api/v1.0}userPassword
     * @response.representation.200.qname {http://docs.rackspacecloud.com/idm/api/v1.0}passwordValidation
     * @response.representation.400.qname {http://docs.rackspacecloud.com/idm/api/v1.0}badRequest
     * @response.representation.401.qname {http://docs.rackspacecloud.com/idm/api/v1.0}unauthorized
     * @response.representation.403.qname {http://docs.rackspacecloud.com/idm/api/v1.0}forbidden
     * @response.representation.404.qname {http://docs.rackspacecloud.com/idm/api/v1.0}itemNotFound
     * @response.representation.500.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serverError
     * @response.representation.503.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serviceUnavailable
     * 
     * @param authHeader HTTP Authorization header for authenticating the caller.
     * @param password The password to validate.
     */
    @POST
    @Path("/validation")
    public Response checkPassword(@HeaderParam("Authorization") String authHeader,
        com.rackspace.idm.jaxb.UserPassword password) {
        
        if (password == null) {
            throw new BadRequestException("Password cannot be blank");
        }

        logger.debug("Checking password {} against password complexity rules",
            password);

        PasswordComplexityResult result = passwordComplexityService
            .checkPassword(password.getPassword());

        com.rackspace.idm.jaxb.PasswordValidation jaxbValidation = passwordRulesConverter
            .toPasswordValidationJaxb(result);

        logger
            .debug(
                "Checked password {} against password complexity rules - isValid = {}",
                result.isValidPassword());

        return Response.ok(jaxbValidation).build();
    }
}
