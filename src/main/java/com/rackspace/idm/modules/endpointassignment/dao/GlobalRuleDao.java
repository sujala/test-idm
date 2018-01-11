package com.rackspace.idm.modules.endpointassignment.dao;

import com.rackspace.idm.modules.endpointassignment.entity.Rule;

import java.util.List;
import java.util.Set;

/**
 * Performs operations that span all endpoint assignment rule types.
 */
public interface GlobalRuleDao {

    /**
     * Searches for a rule w/ the given id. At most one entry will be retrieved. Null returned if
     * no entry found.
     *
     * @param id
     * @throws com.rackspace.idm.exception.SizeLimitExceededException if more than one result was found
     *
     * @return
     */
    Rule getById(String id);

    /**
     * Retrieve all endpoint assignment rules up to a maximum of the number of records that can be retrieved by CA at
     * a time without paging.
     *
     * @throws com.rackspace.idm.exception.SizeLimitExceededException if returns too many results.
     * @return
     */
    List<Rule> findAll();

    /**
     * Delete the specified endpoint assignment rule
     *
     * @param endpointAssignmentRuleId
     */
    void deleteEndpointAssignmentRule(String endpointAssignmentRuleId);

    /**
     * Retrieve endpoint assignment rules that match tenant type
     *
     * @throws com.rackspace.idm.exception.SizeLimitExceededException if returns too many results.
     * @return
     */
    List<Rule> findByTenantTypes(Set<String> types);

    /**
     * Retrieves a list of endpoint assignment rules that map the endpointTemplate, specified by ID, to a tenant type.
     *
     * @param endpointTemplateId
     * @throws com.rackspace.idm.exception.SizeLimitExceededException if returns too many results.
     * @return
     */
    List<Rule> findEndpointAssignmentRulesForEndpointTemplateId(String endpointTemplateId);

}
