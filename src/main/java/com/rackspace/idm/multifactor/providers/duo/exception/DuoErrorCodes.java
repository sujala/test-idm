package com.rackspace.idm.multifactor.providers.duo.exception;

/**
 * This class provides constants for the various error codes that the Duo REST services can return as part of failure
 * responses. While the codes are not documented on Duo's website, they provided a list to us which is documented at
 * <a href="https://one.rackspace.com/display/auth/Duo+Security+Integration">Duo Security error code list</a>
 */
public final class DuoErrorCodes {
    /**
     * This class not meant to be instantiated
     */
    private DuoErrorCodes() {}


    public static final int ADMIN_API_V1_DUPLICATE_RESOURCE = 40003;
    public static final int ADMIN_API_V1_RESOURCE_NOT_FOUND = 40401;

}
