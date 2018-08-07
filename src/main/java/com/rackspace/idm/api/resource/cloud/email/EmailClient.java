package com.rackspace.idm.api.resource.cloud.email;

import com.rackspace.idm.domain.entity.ScopeAccess;
import com.rackspace.idm.domain.entity.User;

public interface EmailClient {
    boolean sendMultiFactorLockoutOutMessage(User user);
    void asyncSendMultiFactorLockedOutMessage(User user);

    boolean sendMultiFactorEnabledMessage(User user);
    void asyncSendMultiFactorEnabledMessage(User user);

    boolean sendMultiFactorDisabledMessage(User user);
    void asyncSendMultiFactorDisabledMessage(User user);

    /**
     * Send the forgot password flow with the subject/content based on the templates for the specified portal.
     *
     * @param user
     * @param token
     * @param portal
     */
    boolean sendForgotPasswordMessage(User user, ScopeAccess token, String portal);

    /**
     * Send the forgot password email asynchronously
     * @param user
     * @param token
     * @param portal
     */
    void asyncSendForgotPasswordMessage(User user, ScopeAccess token, String portal);

    /**
     * Send the unverified user invite with the subject/content based on the templates.
     *
     * @param user
     */
    boolean sendUnverifiedUserInviteMessage(User user);

    /**
     * Send the unverified user invite HTML based email asynchronously
     * @param user
     * @throws IllegalArgumentException If unverified user's id, email, or registration code are null
     */
    void asyncSendUnverifiedUserInviteMessage(User user);
}
