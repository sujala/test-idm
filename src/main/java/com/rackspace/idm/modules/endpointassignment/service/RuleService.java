package com.rackspace.idm.modules.endpointassignment.service;

import com.rackspace.idm.modules.endpointassignment.entity.Rule;

import java.util.List;
import java.util.Set;

/**
 * <p>
 * Performs common business logic associated with endpoint assignment rules.
 * </p>
 */
public interface RuleService {

    /**
     * Persists the new assignment rule. Any provided ID is ignored and populated with a generated one. The returned rule may,
     * or may not be, the same as that passed in. All rules are guaranteed to have globally unique IDs.
     *
     * @param rule
     * @return
     */
    Rule addEndpointAssignmentRule(Rule rule);

    /**
     * Retrieves the assignment rule with the given id. Returns null if a rule with the specified ID is not found.
     *
     * @param ruleId
     * @return
     */
    Rule getEndpointAssignmentRule(String ruleId);

    /**
     * Deletes the specified endpoint assignment rule.
     *
     * @param ruleId
     * @throws com.rackspace.idm.exception.NotFoundException if a rule with the specified id is not found
     */
    void deleteEndpointAssignmentRule(String ruleId);

    /**
     * Retrieves a list of endpoint assignment rules. Returns an empty list if no rules are found.
     *
     * @return
     * @throws com.rackspace.idm.exception.SizeLimitExceededException If more than allowed number of results are returned
     */
    List<Rule> findAllEndpointAssignmentRules();

    /**
     * Retrieves a list of endpoint assignment rules. Returns an empty list if no rules are found.
     *
     * @return
     * @throws com.rackspace.idm.exception.SizeLimitExceededException If more than allowed number of results are returned
     */
    List<Rule> findEndpointAssignmentRulesForTenantType(Set<String> types);
}
