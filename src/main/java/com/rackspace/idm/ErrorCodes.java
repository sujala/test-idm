package com.rackspace.idm;

/**
 * Contains the constants used to uniquely identify the different type of error codes identity will issue. Further
 * information on these can be found on the wiki at
 *
 * @see <a href="https://one.rackspace.com/display/auth/Log+Message+Prefixes">https://one.rackspace.com/display/auth/Log+Message+Prefixes</a>
 */
public final class ErrorCodes {
    public static final String ERROR_CODE_MIGRATION_READ_ONLY_ENTITY_CODE = "MIG-001";
    public static final String ERROR_CODE_MIGRATION_READ_ONLY_ENTITY_MESSAGE = "The modification of this entity has been temporarily disabled.";


    public static final String ERROR_CODE_ERROR_CREATING_QR_CODE = "OTP-001";
    public static final String ERROR_CODE_ERROR_ENCODING_TOTP_URI = "OTP-002";
    public static final String ERROR_CODE_OTP_MISSING_SECURE_RANDOM_ALGORITHM = "OTP-003";
    public static final String ERROR_CODE_INVALID_NUM_OTP_DIGITS = "OTP-004";
    public static final String ERROR_CODE_HOTP_ENCRYPTION_ERROR = "OTP-005";
    public static final String ERROR_CODE_DELETE_OTP_DEVICE_FORBIDDEN_STATE = "OTP-006";
    public static final String ERROR_CODE_OTP_BYPASS_MISSING_HASH_ALGORITHM = "OTP-007";
    public static final String ERROR_CODE_OTP_BYPASS_ERROR_ENCODING = "OTP-008";

    public static final String ERROR_CODE_DELETE_MOBILE_PHONE_FORBIDDEN_STATE = "MOBP-000";

    //federation errors
    public static final String ERROR_CODE_FEDERATION_INVALID_PROVIDER = "FED-000";
    public static final String ERROR_CODE_FEDERATION_INVALID_SIGNATURE = "FED-001";
    public static final String ERROR_CODE_FEDERATION_MISSING_SIGNATURE = "FED-002";
    public static final String ERROR_CODE_FEDERATION_INVALID_ISSUER = "FED-003";
    public static final String ERROR_CODE_FEDERATION_MISSING_ASSERTION = "FED-004";
    public static final String ERROR_CODE_FEDERATION_MISSING_SUBJECT = "FED-005";
    public static final String ERROR_CODE_FEDERATION_MISSING_USERNAME = "FED-006";
    public static final String ERROR_CODE_FEDERATION_MISSING_SUBJECT_NOTONORAFTER = "FED-007";
    public static final String ERROR_CODE_FEDERATION_INVALID_SUBJECT_NOTONORAFTER = "FED-008";
    public static final String ERROR_CODE_FEDERATION_MISSING_AUTH_INSTANT = "FED-009";

    public static final String ERROR_CODE_FEDERATION_MISSING_AUTH_CONTEXT_CLASSREF = "FED-010";
    public static final String ERROR_CODE_FEDERATION_INVALID_AUTH_CONTEXT_CLASSREF = "FED-011";


    //fed racker specific errors
    public static final String ERROR_CODE_FEDERATION_RACKER_NON_EXISTANT_RACKER = "FED_R-001";
    public static final String ERROR_MESSAGE_FORMAT_FEDERATION_RACKER_NON_EXISTANT_RACKER = "The user '%s' is invalid";

    public static String generateErrorCodeFormattedMessage(String errorCode, String message) {
        return String.format("Error code: '%s'; %s", errorCode, message);
    }
}
