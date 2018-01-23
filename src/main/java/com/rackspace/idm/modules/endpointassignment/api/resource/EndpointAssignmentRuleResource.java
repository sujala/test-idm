package com.rackspace.idm.modules.endpointassignment.api.resource;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.EndpointAssignmentRule;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.EndpointAssignmentRules;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.TenantTypeEndpointRule;
import com.rackspace.idm.ErrorCodes;
import com.rackspace.idm.api.resource.cloud.JAXBObjectFactories;
import com.rackspace.idm.api.security.IdentityRole;
import com.rackspace.idm.api.security.RequestContextHolder;
import com.rackspace.idm.domain.entity.CloudBaseUrl;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.EndpointService;
import com.rackspace.idm.event.ApiResourceType;
import com.rackspace.idm.event.IdentityApi;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspace.idm.exception.ExceptionHandler;
import com.rackspace.idm.exception.NotFoundException;
import com.rackspace.idm.exception.SizeLimitExceededException;
import com.rackspace.idm.modules.endpointassignment.entity.TenantTypeRule;
import com.rackspace.idm.modules.endpointassignment.service.RuleService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
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
import java.util.List;

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
    public static final String INVALID_LINKED_TEMPLATE_MSG = "Rule '%s' references an invalid endpoint template with id '%s'.";

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
    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE)
    @POST
    public Response addRule(
            @Context UriInfo uriInfo,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            EndpointAssignmentRule endpointAssignmentRule) {
        try {
            verifyAdminAccess(authToken);

            if (endpointAssignmentRule instanceof TenantTypeEndpointRule) {
                TenantTypeEndpointRule inputRule = (TenantTypeEndpointRule) endpointAssignmentRule;

                // Convert from web to entity format
                TenantTypeRule ruleEntity = convertToEntityForCreate(inputRule);

                TenantTypeRule outputRule = (TenantTypeRule) ruleService.addEndpointAssignmentRule(ruleEntity);

                // Convert from entity to web format
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

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE)
    @GET
    public Response getRules(
            @Context UriInfo uriInfo,
            @HeaderParam(X_AUTH_TOKEN) String authToken) {
        try {
            verifyAdminAccess(authToken);

            List<com.rackspace.idm.modules.endpointassignment.entity.Rule> rules = ruleService.findAllEndpointAssignmentRules();

            EndpointAssignmentRules outputWeb = raxAuthObjectFactory.createEndpointAssignmentRules();
            outputWeb.setTenantTypeEndpointRules(raxAuthObjectFactory.createTenantTypeEndpointRules());

            for (com.rackspace.idm.modules.endpointassignment.entity.Rule rule : rules) {
                if (rule instanceof TenantTypeRule) {
                    outputWeb.getTenantTypeEndpointRules().getTenantTypeEndpointRule().add(convertRuleToWeb((TenantTypeRule) rule, false));
                } else {
                    /* Log if unexpected rule type found. This shouldn't happen as only one rule type currently exists.
                       However, as new rule types are added a coding mistake may lead to the rule not being properly added
                       to the list
                      */
                    logger.warn(String.format("An endpoint assignment rule was returned for list rules, but the type '%s' is not supported", rule.getClass().getCanonicalName()));
                }
            }

            return Response.ok(outputWeb).build();
        } catch (SizeLimitExceededException ex) {
            return exceptionHandler.serviceUnavailableExceptionResponse("The search resulted in too many results").build();
        } catch (Exception e) {
            return exceptionHandler.exceptionResponse(e).build();
        }
    }

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE)
    @GET
    @Path("{ruleId}")
    public Response getRule(
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("ruleId") String ruleId,
            @QueryParam("responseDetail") String responseDetail
    ) {
        try {
            verifyAdminAccess(authToken);

            RuleDetailLevelEnum ruleDetail = RuleDetailLevelEnum.fromString(responseDetail);
            if (ruleDetail == null && StringUtils.isNotBlank(responseDetail)) {
                throw new BadRequestException("responseDetail: Invalid value");
            }

            /* Only current supported rules are TenantTypeRules. When add next, should abstract more (e.g. strategy)
               to avoid ifs and instanceof tests. */
            com.rackspace.idm.modules.endpointassignment.entity.Rule ruleEntity = ruleService.getEndpointAssignmentRule(ruleId);
            if (ruleEntity == null) {
                throw new NotFoundException("The specified rule does not exist");
            }

            TenantTypeEndpointRule outputWeb = convertRuleToWeb((TenantTypeRule) ruleEntity, true, ruleDetail);
            return Response.ok(outputWeb).build();
        } catch (Exception e) {
            return exceptionHandler.exceptionResponse(e).build();
        }
    }

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE)
    @DELETE
    @Path("{ruleId}")
    public Response deleteEndpointAssignmentRule(
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("ruleId") String ruleId) {
        try {
            verifyAdminAccess(authToken);

            ruleService.deleteEndpointAssignmentRule(ruleId);
            return Response.status(HttpServletResponse.SC_NO_CONTENT).build();
        } catch (Exception e) {
            return exceptionHandler.exceptionResponse(e).build();
        }
    }

    private void verifyAdminAccess(String authToken) {
        // Verify the token
        requestContextHolder.getRequestContext().getSecurityContext().getAndVerifyEffectiveCallerToken(authToken);

        // Verify user has appropriate role
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
        return convertRuleToWeb(ruleEntity, includeEndpoints, RuleDetailLevelEnum.MINIMUM);
    }

    private TenantTypeEndpointRule convertRuleToWeb(TenantTypeRule ruleEntity, boolean includeEndpoints, RuleDetailLevelEnum ruleDetailLevelEnum) {
        ObjectFactory openStackIdentityExtKscatalogV1Factory = objFactories.getOpenStackIdentityExtKscatalogV1Factory();
        TenantTypeEndpointRule outputWeb = raxAuthObjectFactory.createTenantTypeEndpointRule();

        outputWeb.setId(ruleEntity.getId());
        outputWeb.setDescription(ruleEntity.getDescription());
        outputWeb.setTenantType(ruleEntity.getTenantType());

        if (includeEndpoints && CollectionUtils.isNotEmpty(ruleEntity.getEndpointTemplateIds())) {
            outputWeb.setEndpointTemplates(openStackIdentityExtKscatalogV1Factory.createEndpointTemplateList());
            for (String templateId : ruleEntity.getEndpointTemplateIds()) {
                EndpointTemplate et = null;
                if (RuleDetailLevelEnum.BASIC == ruleDetailLevelEnum) {
                    // Pull the endpoint to populate the details
                    CloudBaseUrl baseUrl = endpointService.getBaseUrlById(templateId);
                    if (baseUrl != null) {
                        try {
                            et = convertToEndpointTemplate(baseUrl);
                        } catch (NumberFormatException e) {
                            // Log the error, but an error with one endpoint must not cause the whole rule to fail
                            logger.error(generateMissingEndpointError(ruleEntity.getId(), templateId));
                        }
                    } else {
                        // Log the error, but an error with one endpoint must not cause the whole rule to fail
                        logger.error(generateMissingEndpointError(ruleEntity.getId(), templateId));
                    }
                } else {
                    try {
                        et = createEndpointTemplate(Integer.parseInt(templateId));
                    } catch (NumberFormatException e) {
                        // Log the error, but an error with one endpoint must not cause the whole rule to fail
                        logger.error(generateMissingEndpointError(ruleEntity.getId(), templateId));
                    }
                }
                if (et != null) {
                    outputWeb.getEndpointTemplates().getEndpointTemplate().add(et);
                }
            }
        }
        return outputWeb;
    }

    private String generateMissingEndpointError(String ruleId, String invalidTemplateId) {
        return ErrorCodes.generateErrorCodeFormattedMessage(ErrorCodes.ERROR_CODE_EP_MISSING_ENDPOINT, String.format(
                INVALID_LINKED_TEMPLATE_MSG, ruleId, invalidTemplateId));
    }

    /**
     * Map the endpoint properties exposed via the TenantRule
     *
     * @param baseUrl
     * @return
     */
    private EndpointTemplate convertToEndpointTemplate(CloudBaseUrl baseUrl) {
        EndpointTemplate et = createEndpointTemplate(Integer.parseInt(baseUrl.getBaseUrlId()));

        // These properties are displayed for endpoints in service catalogs, so include them
        et.setType(baseUrl.getOpenstackType());
        et.setName(baseUrl.getServiceName());
        et.setRegion(baseUrl.getRegion());
        et.setPublicURL(baseUrl.getPublicUrl());
        et.setEnabled(baseUrl.getEnabled());
        return et;
    }

    private EndpointTemplate createEndpointTemplate(int id) {
        EndpointTemplate et = objFactories.getOpenStackIdentityExtKscatalogV1Factory().createEndpointTemplate();
        et.setId(id);
        return et;
    }
}