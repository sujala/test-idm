package com.rackspace.idm.multifactor.providers;

import com.google.i18n.phonenumbers.Phonenumber;
import com.rackspace.idm.domain.entity.MobilePhone;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.multifactor.providers.duo.domain.DuoPhone;

/**
 */
public interface UserManagement {



    /**
     * Create a "user" in the provider's system (whatever form that may take) for the specified IDM user.
     * The idmUser must not be modified as part of this request. If a provider user already exists in the provider system for
     * this user, return the existing provider user. The idmUser "id" field is considered globally unique.
     *
     */
    ProviderUser createUser(User idmUser);

    /**
     * Given the providerId returned from the {@link #createUser(com.rackspace.idm.domain.entity.User)} retrieves the
     * ProviderUser. Returns null if the user is not found.
     *
     * @param providerId
     * @return
     */
    ProviderUser getUserById(String providerId);

    /**
     * Deletes the user with the given provider-specific identifier from the provider system. If the user does not exist, this is a no-op. If
     * the user is also associated to a device, these associates should be removed. At this time, devices are NOT removed.
     *
     * @throws com.rackspace.idm.multifactor.providers.exceptions.DeleteUserException if there was a problem deleting the user from the provider
     */
    void deleteUserById(String providerUserId);

    /**
     * Associates a phone with the specified user in the provider system. Creates the phone, if necessary, or links
     * to an existing phone. Returns a link to the phone in the provider system.
     *
     * @param providerUserId
     * @param mobilePhone
     * @throws com.rackspace.idm.exception.NotFoundException if the user does not exist
     * @return MobilePhone includes an id to reference the phone
     */
    ProviderPhone linkMobilePhoneToUser(String providerUserId, MobilePhone mobilePhone);

    /**
     * Retrieve the ProviderPhone associated with the providerId, or null if no provider phone exists with specified id.
     * @param providerId
     * @return
     */
    ProviderPhone getPhoneById(String providerId);

    /**
     * Removes the association between the phone and user. If the phone does not exist, this is a NO-OP.
     *
     * @throws com.rackspace.idm.exception.NotFoundException if the user does not exist
     * @param providerPhoneId
     */
    void unlinkMobilePhoneFromUser(String providerUserId, String providerPhoneId);


}
