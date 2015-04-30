package com.rackspace.idm;

/**
 * Contains the constants used to uniquely identify the different type of error codes identity will issue. Further
 * information on these can be found on the wiki at
 *
 * @see <a href="https://one.rackspace.com/display/auth/Log+Message+Prefixes">https://one.rackspace.com/display/auth/Log+Message+Prefixes</a>
 */
public final class ErrorCodes {
    public static final String ERROR_CODE_ERROR_CREATING_QR_CODE = "OTP-001";
    public static final String ERROR_CODE_ERROR_ENCODING_TOTP_URI = "OTP-002";
    public static final String ERROR_CODE_OTP_MISSING_SECURE_RANDOM_ALGORITHM = "OTP-003";
    public static final String ERROR_CODE_INVALID_NUM_OTP_DIGITS = "OTP-004";
    public static final String ERROR_CODE_HOTP_ENCRYPTION_ERROR = "OTP-005";
    public static final String ERROR_CODE_DELETE_OTP_DEVICE_FORBIDDEN_STATE = "OTP-006";
    public static final String ERROR_CODE_OTP_BYPASS_MISSING_HASH_ALGORITHM = "OTP-007";
    public static final String ERROR_CODE_OTP_BYPASS_ERROR_ENCODING = "OTP-008";
}
