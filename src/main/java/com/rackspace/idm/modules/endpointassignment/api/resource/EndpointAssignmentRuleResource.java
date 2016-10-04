package com.rackspace.idm.modules.endpointassignment.api.resource;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.EndpointAssignmentRule;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.TenantTypeEndpointRule;
import com.rackspace.idm.ErrorCodes;
import com.rackspace.idm.api.resource.cloud.JAXBObjectFactories;
import com.rackspace.idm.api.security.IdentityRole;
import com.rackspace.idm.api.security.RequestContextHolder;
import com.rackspace.idm.domain.entity.CloudBaseUrl;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.EndpointService;
import com.rackspace.idm.exception.ExceptionHandler;
import com.rackspace.idm.modules.endpointassignment.entity.TenantTypeRule;
import com.rackspace.idm.modules.endpointassignment.service.RuleService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpStatus;
import org.openstack.docs.identity.api.ext.os_kscatalog.v1.EndpointTemplate;
import org.openstack.docs.identity.api.ext.os_kscatalog.v1.ObjectFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.net.URI;

/**
 * Implements endpoint assignment services. Initial support is solely for TenantType rules. When the next rule is added
 * this resource should be refactored to better support dynamic processing of multiple rule types without putting if/else
 * statements everywhere testing w/ instanceof using handlers/strategies.
 */
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Component
public class EndpointAssignmentRuleResource {

    private static final String X_AUTH_TOKEN = "X-AUTH-TOKEN";

    @Autowired
    private RequestContextHolder requestContextHolder;

    @Autowired
    private RuleService ruleService;

    @Autowired
    private EndpointService endpointService;

    @Autowired
    private ExceptionHandler exceptionHandler;

    @Autowired
    private AuthorizationService authorizationService;

    @Autowired
    private JAXBObjectFactories objFactories;

    private com.rackspace.docs.identity.api.ext.rax_auth.v1.ObjectFactory raxAuthObjectFactory = new com.rackspace.docs.identity.api.ext.rax_auth.v1.ObjectFactory();

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    /**
     * Add a new endpoint assignment rule.
     *
     * @param uriInfo
     * @param authToken
     * @param endpointAssignmentRule
     * @return
     */
    @POST
    public Response addRule(
            @Context UriInfo uriInfo,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            EndpointAssignmentRule endpointAssignmentRule) {
        try {
            verifyAdminAccess(authToken);

            if (endpointAssignmentRule instanceof TenantTypeEndpointRule) {
                TenantTypeEndpointRule inputRule = (TenantTypeEndpointRule) endpointAssignmentRule;

                //convert from web to entity format
                TenantTypeRule ruleEntity = convertToEntityForCreate(inputRule);

                TenantTypeRule outputRule = (TenantTypeRule) ruleService.addEndpointAssignmentRule(ruleEntity);

                //convert from entity to web format
                TenantTypeEndpointRule outputWeb = convertRuleToWeb(outputRule, true);
                UriBuilder requestUriBuilder = uriInfo.getRequestUriBuilder();
                String id = String.valueOf(outputRule.getId());
                URI build = requestUriBuilder.path(id).build();
                Response.ResponseBuilder response = Response.created(build);

                response.entity(outputWeb);
                return response.build();
            }
            return Response.status(HttpServletResponse.SC_NOT_IMPLEMENTED).build();
        } catch (Exception e) {
            return exceptionHandler.exceptionResponse(e).build();
        }
    }

    @GET
    public Response getRules(
            @Context UriInfo uriInfo,
            @HeaderParam(X_AUTH_TOKEN) String authToken) {
            return Response.status(HttpStatus.SC_NOT_IMPLEMENTED).build();
    }

    @GET
    @Path("{ruleId}")
    public Response getRule(
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("ruleId") String ruleId) {
            return Response.status(HttpStatus.SC_NOT_IMPLEMENTED).build();
    }

    @DELETE
    @Path("{ruleId}")
    public Response deleteEndpointAssignmentRule(
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("ruleId") String ruleId) {
            return Response.status(HttpStatus.SC_NOT_IMPLEMENTED).build();
    }

    private void verifyAdminAccess(String authToken) {
        //verify the token
        requestContextHolder.getRequestContext().getSecurityContext().getAndVerifyEffectiveCallerToken(authToken);

        //verify user has appropriate role
        authorizationService.verifyEffectiveCallerHasRoleByName(IdentityRole.IDENTITY_ENDPOINT_RULE_ADMIN.getRoleName());
    }

    private TenantTypeRule convertToEntityForCreate(TenantTypeEndpointRule inputRule) {
        TenantTypeRule ruleEntity = new TenantTypeRule();
        ruleEntity.setTenantType(StringUtils.lowerCase(inputRule.getTenantType()));
        ruleEntity.setDescription(inputRule.getDescription());

        if (inputRule.getEndpointTemplates() != null) {
            for (EndpointTemplate endpointTemplate : inputRule.getEndpointTemplates().getEndpointTemplate()) {
                Integer endpointTemplateId = endpointTemplate.getId();
                if (endpointTemplateId != null) {
                    ruleEntity.addEndpointTemplate(endpointTemplateId);
                }
            }
        }
        return ruleEntity;
    }

    private TenantTypeEndpointRule convertRuleToWeb(TenantTypeRule ruleEntity, boolean includeEndpoints) {
        ObjectFactory openStackIdentityExtKscatalogV1Factory = objFactories.getOpenStackIdentityExtKscatalogV1Factory();
        TenantTypeEndpointRule outputWeb = raxAuthObjectFactory.createTenantTypeEndpointRule();

        outputWeb.setId(ruleEntity.getId());
        outputWeb.setDescription(ruleEntity.getDescription());
        outputWeb.setTenantType(ruleEntity.getTenantType());

        if (includeEndpoints && CollectionUtils.isNotEmpty(ruleEntity.getEndpointTemplateIds())) {
            outputWeb.setEndpointTemplates(openStackIdentityExtKscatalogV1Factory.createEndpointTemplateList());
            for (String templateId : ruleEntity.getEndpointTemplateIds()) {
                CloudBaseUrl baseUrl = endpointService.getBaseUrlById(templateId);
                if (baseUrl != null) {
                    try {
                        EndpointTemplate et = convertToEndpointTemplate(baseUrl);
                        outputWeb.getEndpointTemplates().getEndpointTemplate().add(et);
                    } catch (NumberFormatException e) {
                        //Log the error, but an error with one endpoint must not cause the whole rule to fail
                        String msg = ErrorCodes.generateErrorCodeFormattedMessage(ErrorCodes.ERROR_CODE_EP_MISSING_ENDPOINT, String.format(
                                "Tenant rule '%s' references an invalid endpoint template with id '%s'. Ids must be numeric.", ruleEntity.getId(), templateId));
                        logger.error(msg);
                    }
                } else {
                    //Log the error, but an error with one endpoint must not cause the whole rule to fail
                    String msg = ErrorCodes.generateErrorCodeFormattedMessage(ErrorCodes.ERROR_CODE_EP_MISSING_ENDPOINT, String.format(
                            "Tenant rule '%s' references an invalid endpoint template with id '%s'", ruleEntity.getId(), templateId));
                    logger.error(msg);
                }
            }
        }
        return outputWeb;
    }

    /**
     * Map the endpoint properties exposed via the TenantRule
     *
     * @param baseUrl
     * @return
     */
    private EndpointTemplate convertToEndpointTemplate(CloudBaseUrl baseUrl) {
        EndpointTemplate et = objFactories.getOpenStackIdentityExtKscatalogV1Factory().createEndpointTemplate();
        et.setId(Integer.parseInt(baseUrl.getBaseUrlId()));
        et.setEnabled(baseUrl.getEnabled());

        //these properties are displayed for endpoints in service catalogs, so include them
        et.setType(baseUrl.getOpenstackType());
        et.setName(baseUrl.getServiceName());
        et.setRegion(baseUrl.getRegion());
        et.setPublicURL(baseUrl.getPublicUrl());
        return et;
    }
}