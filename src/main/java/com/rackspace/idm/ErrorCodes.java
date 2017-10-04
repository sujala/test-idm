package com.rackspace.idm;

import org.apache.commons.lang.StringUtils;

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
    public static final String ERROR_CODE_FEDERATION_MISSING_ISSUE_INSTANT = "FED-012";
    public static final String ERROR_CODE_FEDERATION_USER_NOT_FOUND = "FED-013";

    /* *********************************************************************
    * v2 fed errors
    ********************************************************************* */
    public static final String ERROR_CODE_FEDERATION2_MISSING_RESPONSE_ISSUER = "FED2-000";
    public static final String ERROR_CODE_FEDERATION2_MISSING_BROKER_ISSUER = "FED2-001";
    public static final String ERROR_CODE_FEDERATION2_MISSING_ORIGIN_ISSUER = "FED2-002";
    public static final String ERROR_CODE_FEDERATION2_INVALID_BROKER_ISSUER = "FED2-003";
    public static final String ERROR_CODE_FEDERATION2_INVALID_ORIGIN_ISSUER = "FED2-004";

    public static final String ERROR_CODE_FEDERATION2_MISSING_BROKER_ASSERTION = "FED2-005";
    public static final String ERROR_CODE_FEDERATION2_MISSING_ORIGIN_ASSERTION = "FED2-006";
    public static final String ERROR_CODE_FEDERATION2_INVALID_BROKER_ASSERTION = "FED2-007";
    public static final String ERROR_CODE_FEDERATION2_INVALID_ORIGIN_ASSERTION = "FED2-008";

    public static final String ERROR_CODE_FEDERATION2_MISSING_RESPONSE_SIGNATURE = "FED2-009";
    public static final String ERROR_CODE_FEDERATION2_MISSING_ORIGIN_ASSERTION_SIGNATURE = "FED2-010";
    public static final String ERROR_CODE_FEDERATION_INVALID_BROKER_SIGNATURE = "FED2-011";
    public static final String ERROR_CODE_FEDERATION2_INVALID_ORIGIN_SIGNATURE = "FED2-012";

    public static final String ERROR_CODE_FEDERATION2_INVALID_REQUESTED_TOKEN_EXP = "FED2-013";
    public static final String ERROR_CODE_FEDERATION2_INVALID_REQUIRED_ATTRIBUTE = "FED2-014";
    public static final String ERROR_CODE_FEDERATION2_INVALID_ATTRIBUTE = "FED2-015";
    public static final String ERROR_CODE_FEDERATION2_FORBIDDEN_FEDERATED_ROLE = "FED2-016";
    public static final String ERROR_CODE_FEDERATION2_FORBIDDEN_REACHED_MAX_USERS_LIMIT = "FED2-017";

    public static final String ERROR_CODE_FEDERATION2_MISSING_AUTH_CONTEXT = "FED-018";
    public static final String ERROR_CODE_FEDERATION2_INVALID_AUTH_CONTEXT = "FED-019";
    public static final String ERROR_CODE_FEDERATION2_INVALID_ROLE_ASSIGNMENT = "FED2-020";
    public static final String ERROR_CODE_FEDERATION2_DISABLED_ORIGIN_ISSUER = "FED2-021";

    // *********************************************************************

    //fed racker specific errors
    public static final String ERROR_CODE_FEDERATION_RACKER_NON_EXISTANT_RACKER = "FED_R-001";
    public static final String ERROR_MESSAGE_FORMAT_FEDERATION_RACKER_NON_EXISTANT_RACKER = "The user '%s' is invalid";

    //IDP Management errors
    public static final String ERROR_CODE_IDP_INVALID_APPROVED_DOMAIN_OPTIONS = "FED_IDP-001";
    public static final String ERROR_CODE_IDP_INVALID_APPROVED_DOMAIN_GROUP = "FED_IDP-002";
    public static final String ERROR_CODE_IDP_INVALID_APPROVED_DOMAIN = "FED_IDP-003";
    public static final String ERROR_CODE_IDP_ISSUER_ALREADY_EXISTS = "FED_IDP-004";
    public static final String ERROR_CODE_IDP_NAME_ALREADY_EXISTS = "FED_IDP-005";
    public static final String ERROR_CODE_IDP_EMPTY_APPROVED_DOMAIN = "FED_IDP-006";
    public static final String ERROR_CODE_IDP_EXISTING_APPROVED_DOMAIN_GROUP = "FED_IDP-007";
    public static final String ERROR_CODE_IDP_LIMIT_PER_DOMAIN = "FED_IDP-008";
    public static final String ERROR_CODE_IDP_CANNOT_MANUALLY_UPDATE_CERTS_ON_METADATA_IDP = "FED_IDP-009";

    //generic errors
    public static final String ERROR_CODE_GENERIC_BAD_REQUEST = "GEN-000";
    public static final String ERROR_CODE_REQUIRED_ATTRIBUTE = "GEN-001";
    public static final String ERROR_CODE_MAX_LENGTH_EXCEEDED = "GEN-002";
    public static final String ERROR_CODE_MAX_SEARCH_RESULT_SIZE_EXCEEDED = "GEN-003";
    public static final String ERROR_CODE_NOT_FOUND = "GEN-004";
    public static final String ERROR_CODE_INVALID_ATTRIBUTE = "GEN-005";
    public static final String ERROR_CODE_FORBIDDEN_ACTION = "GEN-006";

    public static final String ERROR_MESSAGE_IDP_NOT_FOUND = "An Identity provider with the specified id was not found";

    //MFA
    public static final String ERROR_CODE_MFA_MIGRATION_MFA_NOT_ENABLED = "MFAM-000";
    public static final String ERROR_CODE_MFA_MIGRATION_OTP_ENABLED = "MFAM-001";

    //Endpoint Assignment
    public static final String ERROR_CODE_EP_MISSING_ENDPOINT = "EP-000";

    // Password policy enforcement
    public static final String ERROR_CODE_PASSWORD_EXPIRED = "AUTH-001";
    public static final String ERROR_CODE_PASSWORD_HISTORY_MATCH = "AUTH-002";
    public static final String ERROR_CODE_CURRENT_PASSWORD_MATCH = "AUTH-003";

    public static String generateErrorCodeFormattedMessage(String errorCode, String message) {
        if (StringUtils.isNotBlank(errorCode)) {
            return String.format("Error code: '%s'; %s", errorCode, message);
        } else {
            return message;
        }
    }
}
