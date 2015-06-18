package com.rackspace.idm.domain.dao;

import com.rackspace.idm.domain.entity.Racker;

/**
 * Common operations for Racker based Identity providers
 */
public interface FederatedRackerDao extends FederatedBaseUserDao<Racker> {
    /**
     * Rackers don't store a username, but a rackerId. Rackers are expected to have unique usernames. Federated rackers
     * have a rackerId of {username}@{idp uri}. The idp uri MUST NOT ever change for an IDP so, when given a
     * username to search for, a search for rackerIds that being with the username will return the unique racker, if
     * one exists.
     *
     * @param username
     * @param uri
     * @return
     */
    Racker getUserByUsernameForIdentityProviderUri(String username, String uri);
}
