package com.rackspace.idm.modules.endpointassignment.dao;

import com.rackspace.idm.modules.endpointassignment.entity.Rule;

/**
 * A generic interface that all types of Rule DAOs must implement to provide a common framework for persisting
 * assignment rules
 *
 * @param <T>
 */
public interface RuleDao<T extends Rule> {

    /**
     * Searches for a rule w/ the given id. At most one entry will be retrieved. Null returned if
     * no entry found.
     *
     * @param id
     * @throws com.rackspace.idm.exception.SizeLimitExceededException if more than one result was found
     *
     * @return
     */
    T getById(String id);

    /**
     * Adds a new endpoint assignment rule.
     *
     * @param endpointAssignmentRule
     */
    void addEndpointAssignmentRule(T endpointAssignmentRule);

    /**
     * Update the specified assignment rule.
     *
     * @param endpointAssignmentRule
     */
    void updateEndpointAssignmentRule(T endpointAssignmentRule);

    void deleteEndpointAssignmentRule(T endpointAssignmentRule);
}
