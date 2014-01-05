package com.rackspace.idm.multifactor.providers.duo.service;

import com.google.common.collect.ImmutableMap;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import com.rackspace.idm.domain.entity.MobilePhone;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.exception.DuplicateException;
import com.rackspace.idm.exception.NotFoundException;
import com.rackspace.idm.multifactor.providers.ProviderPhone;
import com.rackspace.idm.multifactor.providers.UserManagement;
import com.rackspace.idm.multifactor.providers.duo.config.AdminApiConfig;
import com.rackspace.idm.multifactor.providers.duo.domain.*;
import com.rackspace.idm.multifactor.providers.duo.exception.DuoCreateUserException;
import com.rackspace.idm.multifactor.providers.duo.exception.DuoDeleteUserException;
import com.rackspace.idm.multifactor.providers.duo.exception.DuoErrorCodes;
import com.rackspace.idm.multifactor.providers.duo.exception.GenericDuoException;
import com.rackspace.idm.multifactor.providers.duo.util.DuoJsonResponseReader;
import com.rackspace.idm.multifactor.providers.duo.util.InMemoryDuoJsonResponseReader;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import lombok.Getter;
import lombok.Setter;
import org.apache.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Collections;
import java.util.Map;

@Component
public class DuoUserManagement implements UserManagement {
    private static final String ADMIN_ENDPOINT_BASE_PATH = "/admin/v1";

    @Autowired
    @Getter
    @Setter
    private AdminApiConfig adminApiConfig;

    /*
     * Wire in a reader if a bean exists. If not, use the default in-memory implementation
     */
    @Autowired(required = false)
    private DuoJsonResponseReader duoJsonResponseReader = new InMemoryDuoJsonResponseReader();

    @Autowired(required = false)
    DuoRequestHelperFactory duoRequestHelperFactory = new SingletonDuoRequestHelperFactory();

    private DuoRequestHelper duoRequestHelper;

    private WebResource ADMIN_ENDPOINT_BASE_RESOURCE;

    private WebResource USER_ENDPOINT_RESOURCE;

    private WebResource PHONE_ENDPOINT_RESOURCE;

    private PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();

    /**
     * Must be called after dependencies are injected and before the services are used.
     */
    @PostConstruct
    protected void init() {
        this.duoRequestHelper = duoRequestHelperFactory.getInstance(adminApiConfig);

        ADMIN_ENDPOINT_BASE_RESOURCE = duoRequestHelper.createWebResource(ADMIN_ENDPOINT_BASE_PATH);
        USER_ENDPOINT_RESOURCE = ADMIN_ENDPOINT_BASE_RESOURCE.path("users");
        PHONE_ENDPOINT_RESOURCE = ADMIN_ENDPOINT_BASE_RESOURCE.path("phones");
    }

    /**
     * Creates a new user within Duo Security. The username within duo is set to the user's rsId since users are
     * allowed
     * to change their usernames
     * within IDM. Duo generates a unique id for every new user, which is returned as the providerId within the
     * ProviderUser result.
     *
     * @param idmUser
     * @return
     */
    @Override
    public DuoUser createUser(User idmUser) {
        return createOrGetUser(idmUser);
    }

    @Override
    public DuoUser getUserById(String providerId) {
        WebResource userResource = USER_ENDPOINT_RESOURCE.path(providerId);
        ClientResponse clientResponse = duoRequestHelper.makeGetRequest(userResource, Collections.EMPTY_MAP, ClientResponse.class);
        DuoResponse<DuoUser> response = duoJsonResponseReader.fromDuoResponse(clientResponse, DuoUser.class);

        if (response.isFailure()) {
            FailureResult failedResult = response.getFailureResult();
            if (clientResponse.getStatus() == HttpStatus.SC_NOT_FOUND
                    && failedResult.getCode() == DuoErrorCodes.ADMIN_API_V1_RESOURCE_NOT_FOUND) {
                return null;
            }
            throw new GenericDuoException(failedResult);
        }

        return response.getSuccessResult();
    }

    @Override
    public void deleteUserById(String providerId) {
        WebResource deleteUserResource = USER_ENDPOINT_RESOURCE.path(providerId);
        ClientResponse clientResponse = duoRequestHelper.makeDeleteRequest(deleteUserResource, Collections.EMPTY_MAP, ClientResponse.class);
        DuoResponse<String> response = duoJsonResponseReader.fromDuoResponse(clientResponse, String.class);

        if (response.isFailure()) {
            FailureResult failedResult = response.getFailureResult();
            throw new DuoDeleteUserException(failedResult, String.format("Error deleting user with external provider id '%s'. %s", providerId, failedResult.getMessage()));
        }
    }

    /**
     * Associates a phone within duo to the user. Within Duo Security a phone number is globally unique. If there is an
     * existing phone with the specified number, it will be used; otherwise a new phone will be created. In either
     * case,
     * the phone the user is linked to will be returned.
     *
     * @param providerUserId
     * @param mobilePhone
     * @return
     */
    @Override
    public DuoPhone linkMobilePhoneToUser(String providerUserId, MobilePhone mobilePhone) {
        ProviderPhone phone = createOrGetMobilePhone(mobilePhone);
        addPhoneToUser(providerUserId, phone.getProviderId());

        //get the updated phone since the user should now be listed
        return getPhoneByNumber(mobilePhone.getStandardizedTelephoneNumber());
    }

    @Override
    public void unlinkMobilePhoneFromUser(String providerUserId, String providerPhoneId) {
        WebResource phoneToUserResource = USER_ENDPOINT_RESOURCE.path(providerUserId).path("phones").path(providerPhoneId);
        ClientResponse clientResponse = duoRequestHelper.makeDeleteRequest(phoneToUserResource, Collections.EMPTY_MAP, ClientResponse.class);
        DuoResponse<DuoPhone> response = duoJsonResponseReader.fromDuoResponse(clientResponse, DuoPhone.class);

        if (response.isFailure()) {
            switch (clientResponse.getStatus()) {
                case HttpStatus.SC_NOT_FOUND: {
                    throw new NotFoundException("User '" + providerUserId + "' was not found. " + response.getFailureResult().getMessage());
                }
                default:
                    throw new GenericDuoException(response.getFailureResult());
            }
        }
        //TODO: See if the phone is linked to any other users, and if not, delete the phone itself. For now we just leave the phones
    }

    @Override
    public DuoPhone getPhoneById(String providerId) {
        WebResource userResource = PHONE_ENDPOINT_RESOURCE.path(providerId);
        ClientResponse clientResponse = duoRequestHelper.makeGetRequest(userResource, Collections.EMPTY_MAP, ClientResponse.class);
        DuoResponse<DuoPhone> response = duoJsonResponseReader.fromDuoResponse(clientResponse, DuoPhone.class);

        if (response.isFailure()) {
            FailureResult failedResult = response.getFailureResult();
            if (clientResponse.getStatus() == HttpStatus.SC_NOT_FOUND
                    && failedResult.getCode() == DuoErrorCodes.ADMIN_API_V1_RESOURCE_NOT_FOUND) {
                return null;
            }
            throw new GenericDuoException(failedResult);
        }

        return response.getSuccessResult();
    }


    /**
     * Either creates a new phone within Duo Security, or retrieves an existing phone that has the same phone number.
     * <p/>
     * This method works by attempting to add
     * the phone first, and relies on Duo Security to throw an exception if a
     * phone
     * with the same number already exists. Adding and catching the exception is preferred to checking existence
     * and then adding because
     * <ol>
     * <li>the common case is that the phone will not exist yet so the "get" check would be an unnecessary call</li>
     * <li>Even if check first with a get, there's still the possibility that a concurrent request could have created
     * a phone with the same number after the check, but before the add, which would cause the same Duplicate exception
     * that
     * would need to be handled.</li>
     * </ol>
     *
     * @param mobilePhone
     * @return
     */
    private ProviderPhone createOrGetMobilePhone(MobilePhone mobilePhone) {
        ProviderPhone providerPhone;

        try {
            providerPhone = addMobilePhone(mobilePhone);
        } catch (DuplicateException ex) {
            providerPhone = getPhoneByNumber(mobilePhone.getStandardizedTelephoneNumber());
        }

        /*
         * One of the above should work in "normal" situations and providerPhone won't be null. However, there is an unlikely concurrency scenario where a
         * pre-existing phone caused a DuplicateException to be thrown and the phone was then deleted after the
         * addMobilePhone threw an exception but before the phone was subsequently retrieved. In this scenario, the "getPhone..." will return null.
          * We can't continue in this case and should just fail.
          *
         */
        if (providerPhone == null) {
            throw new RuntimeException("Could not add or retrieve phone from duo");
        }
        return providerPhone;
    }

    private ProviderPhone addMobilePhone(MobilePhone mobilePhone) {
        Map<String, String> map = ImmutableMap.<String, String>builder()
                .put("number", canonicalizePhoneNumberToString(mobilePhone.getStandardizedTelephoneNumber()))
                .put("type", PhoneType.MOBILE.toDuoSecurityCode())
                .build();

        ClientResponse clientResponse = duoRequestHelper.makePostRequest(PHONE_ENDPOINT_RESOURCE, map, ClientResponse.class);
        DuoResponse<DuoPhone> response = duoJsonResponseReader.fromDuoResponse(clientResponse, DuoPhone.class);

        if (response.isSuccess()) {
            return response.getSuccessResult();
        } else if (clientResponse.getStatus() == HttpStatus.SC_BAD_REQUEST
                && response.getFailureResult().getCode() == DuoErrorCodes.ADMIN_API_V1_DUPLICATE_RESOURCE) {
            throw new DuplicateException("Phone already exists");
        } else {
            throw new GenericDuoException(response.getFailureResult());
        }
    }

    private DuoPhone getPhoneByNumber(Phonenumber.PhoneNumber phoneNumber) {
        Map<String, String> map = ImmutableMap.<String, String>builder()
                .put("number", canonicalizePhoneNumberToString(phoneNumber))
                .build();

        ClientResponse clientResponse = duoRequestHelper.makeGetRequest(PHONE_ENDPOINT_RESOURCE, map, ClientResponse.class);
        DuoResponse<DuoPhone[]> response = duoJsonResponseReader.fromDuoResponse(clientResponse, DuoPhone[].class);

        if (response.isSuccess()) {
            if (response.getSuccessResult().length == 0) {
                return null;
            } else if (response.getSuccessResult().length == 1) {
                return response.getSuccessResult()[0];
            } else {
                throw new IllegalStateException("Retrieving phone '" + canonicalizePhoneNumberToString(phoneNumber) + "' resulted in '" + response.getSuccessResult().length + "' results! One, and only one, expected.");
            }
        } else {
            throw new GenericDuoException(response.getFailureResult());
        }
    }

    private void addPhoneToUser(String providerUserId, String providerPhoneId) {
        WebResource phoneToUserResource = USER_ENDPOINT_RESOURCE.path(providerUserId).path("phones");

        Map<String, String> map = ImmutableMap.<String, String>builder()
                .put("phone_id", providerPhoneId)
                .build();

        ClientResponse clientResponse = duoRequestHelper.makePostRequest(phoneToUserResource, map, ClientResponse.class);
        DuoResponse<String> response = duoJsonResponseReader.fromDuoResponse(clientResponse, String.class); //response will be empty string in positive case

        if (response.isFailure()) {
            switch (clientResponse.getStatus()) {
                case HttpStatus.SC_NOT_FOUND: {
                    throw new NotFoundException("User '" + providerUserId + "' was not found. " + response.getFailureResult().getMessage());
                }
                default:
                    throw new GenericDuoException(response.getFailureResult());
            }
        }
    }

    private DuoUser getUserByUsername(String username) {
        WebResource userResource = USER_ENDPOINT_RESOURCE;
        Map<String, String> map = ImmutableMap.<String, String>builder()
                .put("username", username)
                .build();

        ClientResponse clientResponse = duoRequestHelper.makeGetRequest(userResource, map, ClientResponse.class);
        DuoResponse<DuoUser[]> response = duoJsonResponseReader.fromDuoResponse(clientResponse, DuoUser[].class);

        if (response.isSuccess()) {
            if (response.getSuccessResult().length == 0) {
                return null;
            } else if (response.getSuccessResult().length == 1) {
                return response.getSuccessResult()[0];
            } else {
                throw new IllegalStateException("Retrieving user with username '" + username + "' resulted in '" + response.getSuccessResult().length + "' results! One, and only one, expected.");
            }
        } else {
            throw new GenericDuoException(response.getFailureResult());
        }
    }

    private void deleteMobilePhone(String providerPhoneId) {
        WebResource deletePhoneUser = PHONE_ENDPOINT_RESOURCE.path(providerPhoneId);
        duoRequestHelper.makeDeleteRequest(deletePhoneUser, Collections.EMPTY_MAP, String.class);
    }

    /**
     * Either creates a new user within Duo Security, or retrieves an existing user that has the same ldap userId.
     * <p/>
     * This method works by attempting to add the user first, and relying on Duo Security to throw an exception if a
     * user
     * with the same username already exists. Adding and catching the exception is preferred to checking existence
     * and then adding because
     * <ol>
     * <li>99% of the time the phone will be unique so the "get" check would be an unnecessary call</li>
     * <li>Even if check first with a get, there's still the possibility that a concurrent request could have created
     * a phone with the same number after the check, but before the add, which would cause the same Duplicate exception
     * that
     * would need to be handled.</li>
     * </ol>
     *
     * @param user
     * @return
     */
    private DuoUser createOrGetUser(User user) {
        DuoUser duoUser = null;
        try {
            duoUser = createUserInDuo(user);
        } catch (DuplicateException ex) {
            duoUser = getUserByUsername(user.getId()); //username in duo is the user's ldap id
        }
        if (duoUser == null) {
            throw new RuntimeException("Could not add or retrieve user from duo");
        }

        //current state is to only use active status profiles so we should never get an inactive profile. If we do, there's an issue.
        if (duoUser.getStatusAsEnum() != UserStatus.ACTIVE) {
            throw new IllegalStateException("Duo User '" + duoUser.getProviderId() + "' is not in active status. Status = '" + duoUser.getStatus() + "'");
        }

        return duoUser;
    }

    private DuoUser createUserInDuo(User idmUser) {
        Map<String, String> map = ImmutableMap.<String, String>builder()
                .put("username", idmUser.getId())
                .build();

        ClientResponse clientResponse = duoRequestHelper.makePostRequest(USER_ENDPOINT_RESOURCE, map, ClientResponse.class);
        DuoResponse<DuoUser> response = duoJsonResponseReader.fromDuoResponse(clientResponse, DuoUser.class);

        if (response.isSuccess()) {
            return response.getSuccessResult();
        } else if (clientResponse.getStatus() == HttpStatus.SC_BAD_REQUEST
                && response.getFailureResult().getCode() == DuoErrorCodes.ADMIN_API_V1_DUPLICATE_RESOURCE) {
            throw new DuplicateException("User already exists");
        } else {
            throw new DuoCreateUserException(response.getFailureResult());
        }
    }

    /**
     * Duo Security requires E.164 formatted numbers
     *
     * @param phoneNumber
     * @return
     */
    private String canonicalizePhoneNumberToString(Phonenumber.PhoneNumber phoneNumber) {
        return phoneNumberUtil.format(phoneNumber, PhoneNumberUtil.PhoneNumberFormat.E164);
    }

    private enum PhoneType {
        UNKNOWN("unknown"), MOBILE("mobile"), LANDLINE("landline");

        private String duoSecurityCode;

        private PhoneType(String duoSecurityCode) {
            this.duoSecurityCode = duoSecurityCode;
        }

        public String toDuoSecurityCode() {
            return duoSecurityCode;
        }
    }
}
