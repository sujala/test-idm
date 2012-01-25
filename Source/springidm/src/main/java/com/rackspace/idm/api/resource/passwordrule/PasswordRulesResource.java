package com.rackspace.idm.api.resource.passwordrule;

import com.rackspace.idm.api.converter.PasswordRulesConverter;
import com.rackspace.idm.domain.entity.PasswordComplexityResult;
import com.rackspace.idm.domain.entity.PasswordRule;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.PasswordComplexityService;
import com.rackspace.idm.exception.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

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
    private final AuthorizationService authorizationService;
    final private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    public PasswordRulesResource(
        PasswordComplexityService passwordComplexityService,
        PasswordRulesConverter passwordRulesConverter,
        AuthorizationService authorizationService) {
        this.passwordComplexityService = passwordComplexityService;
        this.passwordRulesConverter = passwordRulesConverter;
        this.authorizationService = authorizationService;
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
    public Response getRules(@HeaderParam("X-Auth-Token") String authHeader) {
        authorizationService.verifyIdmSuperAdminAccess(authHeader);
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
    public Response checkPassword(@HeaderParam("X-Auth-Token") String authHeader,
        com.rackspace.api.idm.v1.UserPassword password) {

        authorizationService.verifyIdmSuperAdminAccess(authHeader);
        if (password == null) {
            throw new BadRequestException("Password cannot be blank");
        }

        logger.debug("Checking password {} against password complexity rules",
            password);

        PasswordComplexityResult result = passwordComplexityService
            .checkPassword(password.getPassword());

        logger
            .debug(
                "Checked password {} against password complexity rules - isValid = {}",
                result.isValidPassword());

        return Response.ok(passwordRulesConverter
            .toPasswordValidationJaxb(result)).build();
    }
}
