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
    public static final String ERROR_CODE_FEDERATION2_INVALID_REQUESTED_TOKEN_EXP_MSG = "Token expiration cannot exceed '%s' seconds.";
    public static final String ERROR_CODE_FEDERATION2_INVALID_REQUIRED_ATTRIBUTE = "FED2-014";
    public static final String ERROR_CODE_FEDERATION2_INVALID_ATTRIBUTE = "FED2-015";
    public static final String ERROR_CODE_FEDERATION2_FORBIDDEN_FEDERATED_ROLE = "FED2-016";
    public static final String ERROR_CODE_FEDERATION2_FORBIDDEN_REACHED_MAX_USERS_LIMIT = "FED2-017";

    public static final String ERROR_CODE_FEDERATION2_MISSING_AUTH_CONTEXT = "FED-018";
    public static final String ERROR_CODE_FEDERATION2_INVALID_AUTH_CONTEXT = "FED-019";
    public static final String ERROR_CODE_FEDERATION2_INVALID_ROLE_ASSIGNMENT = "FED2-020";
    public static final String ERROR_CODE_FEDERATION2_DISABLED_ORIGIN_ISSUER = "FED2-021";
    public static final String ERROR_CODE_FEDERATION2_INVALID_GROUP_ASSIGNMENT = "FED2-022";

    // *********************************************************************

    //fed racker specific errors
    public static final String ERROR_CODE_FEDERATION_RACKER_NON_EXISTANT_RACKER = "FED_R-001";
    public static final String ERROR_MESSAGE_FORMAT_FEDERATION_RACKER_NON_EXISTANT_RACKER = "The user '%s' is invalid";

    //IDP Management errors
    public static final String ERROR_CODE_IDP_INVALID_APPROVED_DOMAIN_OPTIONS = "FED_IDP-001";
    public static final String ERROR_CODE_IDP_INVALID_APPROVED_DOMAIN_GROUP = "FED_IDP-002";
    public static final String ERROR_CODE_IDP_INVALID_APPROVED_DOMAIN = "FED_IDP-003";
    public static final String ERROR_CODE_IDP_ISSUER_ALREADY_EXISTS = "FED_IDP-004";
    public static final String ERROR_CODE_IDP_ISSUER_ALREADY_EXISTS_MSG = "Provider already exists with this issuer or entityID";

    public static final String ERROR_CODE_IDP_NAME_ALREADY_EXISTS = "FED_IDP-005";
    public static final String ERROR_CODE_IDP_EMPTY_APPROVED_DOMAIN = "FED_IDP-006";
    public static final String ERROR_CODE_IDP_EXISTING_APPROVED_DOMAIN_GROUP = "FED_IDP-007";
    public static final String ERROR_CODE_IDP_LIMIT_PER_DOMAIN = "FED_IDP-008";
    public static final String ERROR_CODE_IDP_CANNOT_MANUALLY_UPDATE_CERTS_ON_METADATA_IDP = "FED_IDP-009";
    public static final String ERROR_CODE_IDP_EMAIL_DOMAIN_ALREADY_ASSIGNED = "FED_IDP-010";
    public static final String ERROR_CODE_IDP_INVALID_EMAIL_DOMAIN_OPTIONS = "FED_IDP-011";

    //generic errors
    public static final String ERROR_CODE_GENERIC_BAD_REQUEST = "GEN-000";
    public static final String ERROR_CODE_REQUIRED_ATTRIBUTE = "GEN-001";
    public static final String ERROR_CODE_MAX_LENGTH_EXCEEDED = "GEN-002";
    public static final String ERROR_CODE_MAX_SEARCH_RESULT_SIZE_EXCEEDED = "GEN-003";
    public static final String ERROR_CODE_NOT_FOUND = "GEN-004";
    public static final String ERROR_CODE_INVALID_ATTRIBUTE = "GEN-005";
    public static final String ERROR_CODE_FORBIDDEN_ACTION = "GEN-006";
    public static final String ERROR_CODE_INVALID_VALUE = "GEN-007";
    public static final String ERROR_CODE_THRESHOLD_REACHED = "GEN-008";
    public static final String ERROR_CODE_DATA_INTEGRITY = "GEN-009";
    public static final String ERROR_CODE_CONFLICT = "GEN-010";

    public static final String ERROR_CODE_DOMAIN_DEFAULT_MISSING_USER_ADMIN = "DOM-008";
    public static final String ERROR_CODE_DOMAIN_DEFAULT_NO_ENABLED_USER_ADMIN = "DOM-009";

    public static final String ERROR_MESSAGE_IDP_NOT_FOUND = "An Identity provider with the specified id was not found";

    public static final String ERROR_CODE_INVALID_DOMAIN_FOR_USER = "DAT-000";

    //MFA
    public static final String ERROR_CODE_MFA_MIGRATION_MFA_NOT_ENABLED = "MFAM-000";
    public static final String ERROR_CODE_MFA_MIGRATION_OTP_ENABLED = "MFAM-001";

    //PHONE PIN ERROR CODES
    public static final String ERROR_CODE_PHONE_PIN_NOT_FOUND = "PP-000";
    public static final String ERROR_CODE_PHONE_PIN_BAD_REQUEST = "PP-001";
    public static final String ERROR_CODE_PHONE_PIN_FORBIDDEN_ACTION = "PP-002";
    public static final String ERROR_CODE_PHONE_PIN_LOCKED = "PP-004";
    public static final String ERROR_CODE_USER_ALREADY_HAS_A_PHONE_PIN = "User already has a phone PIN";

    //PHONE PIN ERROR MESSAGES
    public static final String ERROR_MESSAGE_PHONE_PIN_BAD_REQUEST = "Use six numeric digits (such as 871694). A Phone PIN cannot include more than three repeating numbers. (444 is OK, but 4444 is not.) A Phone PIN cannot include more than three sequential numbers. (234 is OK, but 2345 is not.)";
    public static final String ERROR_CODE_PHONE_PIN_NOT_FOUND_MSG = "The user has not set a Phone PIN.";
    public static final String ERROR_MESSAGE_PHONE_PIN_LOCKED = "User's PIN is locked.";

    // Unverified users
    public static final String ERROR_CODE_UNVERIFIED_USERS_DOMAIN_WITHOUT_ACCOUNT_ADMIN = "UU-000";
    public static final String ERROR_CODE_UNVERIFIED_USERS_DOMAIN_WITHOUT_ACCOUNT_ADMIN_MESSAGE = "Cannot create invite user for domain with no enabled account admin.";
    public static final String ERROR_CODE_UNVERIFIED_USERS_INVITE_NOT_FOUND = "UU-001";
    public static final String ERROR_CODE_UNVERIFIED_USERS_INVITE_NOT_FOUND_MESSAGE = "The specified invitation was not found.";

    //Endpoint Assignment
    public static final String ERROR_CODE_EP_MISSING_ENDPOINT = "EP-000";

    // Password policy enforcement
    public static final String ERROR_CODE_PASSWORD_EXPIRED = "AUTH-001";
    public static final String ERROR_CODE_PASSWORD_HISTORY_MATCH = "AUTH-002";
    public static final String ERROR_CODE_CURRENT_PASSWORD_MATCH = "AUTH-003";

    /**
     * User provided invalid credentials
     */
    public static final String ERROR_CODE_AUTH_INVALID_CREDENTIALS = "AUTH-004";
    public static final String ERROR_CODE_AUTH_INVALID_CREDENTIALS_MSG = "Unable to authenticate user with credentials provided.";

    /**
     * User provided invalid api key in auth request
     */
    public static final String ERROR_CODE_AUTH_INVALID_API_KEY = "AUTH-008";
    public static final String ERROR_CODE_AUTH_INVALID_API_KEY_MSG = "Username or api key is invalid.";

    /**
     * User provided invalid token in auth request
     */
    public static final String ERROR_CODE_AUTH_INVALID_TOKEN = "AUTH-005";
    public static final String ERROR_CODE_AUTH_INVALID_TOKEN_MSG = "Token not authenticated.";

    /**
     * User provided domain in auth request to which user does not have access
     */
    public static final String ERROR_CODE_AUTH_INVALID_DOMAIN = "AUTH-006";
    public static final String ERROR_CODE_AUTH_INVALID_DOMAIN_MSG = "Not authorized for the domain.";

    /**
     * User provided tenant in auth request to which user does not have access
     */
    public static final String ERROR_CODE_AUTH_INVALID_TENANT = "AUTH-007";
    public static final String ERROR_CODE_AUTH_INVALID_TENANT_MSG = "Not authorized for the tenant.";


    // Delegation Agreements
    public static final String ERROR_CODE_DA_NOT_ALLOWED_FOR_RCN = "DA-000";
    public static final String ERROR_CODE_DELEGATE_ALREADY_EXISTS = "DA-001";

    // User role assignments
    public static final String ERROR_CODE_DUP_ROLE_ASSIGNMENT = "ROLE-000";
    public static final String ERROR_CODE_USER_MANAGE_ON_NON_DEFAULT_USER = "ROLE-001";

    public static final String ERROR_CODE_RACKER_PROXY_SEARCH = "RACK-000";

    public static final String ERROR_CODE_DUP_ROLE_ASSIGNMENT_MSG = "A given role can only be specified once";

    public static final String ERROR_CODE_ROLE_ASSIGNMENT_MISSING_FOR_TENANTS_MSG = "All role assignments must include 'forTenants' field";
    public static final String ERROR_CODE_ROLE_ASSIGNMENT_INVALID_FOR_TENANTS_MSG = "Role assignments can only be for all tenants or specific tenants in group's domain.";
    public static final String ERROR_CODE_ROLE_ASSIGNMENT_NONEXISTANT_ROLE_MSG_PATTERN = "Invalid assignment for role '%s'. Role does not exist.";
    public static final String ERROR_CODE_ROLE_ASSIGNMENT_GLOBAL_ROLE_ASSIGNMENT_ONLY_MSG_PATTERN = "Invalid assignment for role '%s'. This role must be assigned globally.";
    public static final String ERROR_CODE_ROLE_ASSIGNMENT_TENANT_ASSIGNMENT_ONLY_MSG_PATTERN = "Invalid assignment for role '%s'. This role must be assigned to explicit tenants.";
    public static final String ERROR_CODE_ROLE_ASSIGNMENT_FORBIDDEN_ASSIGNMENT_MSG_PATTERN = "Invalid assignment for role '%s'. Not authorized to assign this role.";
    public static final String ERROR_CODE_ROLE_ASSIGNMENT_WRONG_TENANTS_MSG_PATTERN = "Invalid assignment for role '%s'. Not authorized to assign this role with provided tenants.";
    public static final String ERROR_CODE_ROLE_ASSIGNMENT_MAX_TENANT_ASSIGNMENT_MSG_PATTERN = "A maximum of %d tenant assignments are allowed when granting roles.";

    public static final String ERROR_CODE_ROLE_ASSIGNMENT_NONEXISTANT_TENANT_MSG_PATTERN = "Invalid assignment for role '%s'. Tenant does not exist.";
    public static final String ERROR_CODE_ROLE_ASSIGNMENT_WRONG_DOMAIN_TENANT_MSG_PATTERN = "Invalid assignment for role '%s'. Tenant must belong to domain '%s'.";

    public static final String ERROR_CODE_USER_MANAGE_ON_NON_DEFAULT_USER_MSG = "Cannot add the 'identity:user-manage' role to non default-user.";

    public static final String ERROR_CODE_MUTUALLY_EXCLUSIVE_QUERY_PARAMS_FOR_LIST_USERS_MSG_PATTERN = "The '%s' parameter can not be used with the '%s' parameter.";
    public static final String ERROR_CODE_USER_GROUP_CANNOT_BE_CREATED_IN_DEFAULT_DOMAIN_MSG = "The user group cannot be created in default domain";

    //Password Blacklist
    public static final String ERROR_CODE_BLACKLISTED_PASSWORD = "PB-000";
    public static final String ERROR_CODE_BLACKLISTED_PASSWORD_MSG = "The password you submitted resembles one or more found in compromised data sets on the Internet, so we did not save it. Instead, use a new unique, strong password.";

    //Password Validation service error messages
    public static final String ERROR_CODE_VALIDATE_PASSWORD_COMPOSITION_FAILED = "VP-000";
    public static final String ERROR_CODE_VALIDATE_PASSWORD_COMPOSITION_FAILED_MSG = "Passwords must be between 8 and 100 characters in length and must contain at least one uppercase, one lowercase, and one numeric character. While passwords cannot begin with a space, they can contain a space, and they can start with or include any of the following special characters: ~ ! @ # $ % ^ & * _ - + = ` | \\ ( ) { } [ ] : ; \" ' <> , . ? / £ ";

    public static final String ERROR_CODE_VALIDATE_PASSWORD_BLACKLIST_FAILED = "VP-001";
    public static final String ERROR_CODE_VALIDATE_PASSWORD_BLACKLIST_FAILED_MSG = "The password you submitted might have been compromised in data breaches outside of Rackspace. Choose another password.";

    public static final String ERROR_CODE_VALIDATE_PASSWORD_BLACKLIST_SKIPPED = "VP-002";
    public static final String ERROR_CODE_VALIDATE_PASSWORD_BLACKLIST_SKIPPED_MSG = "Password screening was skipped.";

    public static final String ERROR_CODE_VALIDATE_PASSWORD_BLACKLIST_DISABLED = "VP-003";
    public static final String ERROR_CODE_VALIDATE_PASSWORD_BLACKLIST_DISABLED_MSG = "Password screening is currently disabled.";

    public static final String ERROR_CODE_VALIDATE_PASSWORD_BLACKLIST_INDETERMINATE_RETRY = "VP-004";
    public static final String ERROR_CODE_VALIDATE_PASSWORD_BLACKLIST_INDETERMINATE_RETRY_MSG = "Password screening is experiencing some difficulties. Try again later.";

    public static final String ERROR_CODE_VALIDATE_PASSWORD_BLACKLIST_INDETERMINATE_ERROR = "VP-005";
    public static final String ERROR_CODE_VALIDATE_PASSWORD_BLACKLIST_INDETERMINATE_ERROR_MSG = "Password screening is experiencing some difficulties. Contact an administrator.";

    /**
     * PLEASE ADD REUSABLE ERROR MESSAGES HERE.
     */
    public static final String ERROR_MESSAGE_ONLY_ONE_USER_ADMIN_ALLOWED = "The domain %s has more than one user admin. Only one user admin can exist per domain.";
    public static final String ERROR_MESSAGE_IDP_CANNOT_ASSIGN_ROLE_TO_TENANT = "Identity provider cannot assign role to tenant '%s'.";

   public static String generateErrorCodeFormattedMessage(String errorCode, String message) {
        if (StringUtils.isNotBlank(errorCode)) {
            return String.format("Error code: '%s'; %s", errorCode, message);
        } else {
            return message;
        }
    }
}
