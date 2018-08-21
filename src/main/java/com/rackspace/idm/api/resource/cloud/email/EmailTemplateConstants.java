package com.rackspace.idm.api.resource.cloud.email;

public final class EmailTemplateConstants {
    private EmailTemplateConstants() {
    }

    /*
        Dynamic properties made available to ForgotPassword templates
     */
    public static final String FORGOT_PASSWORD_TOKEN_BEAN_PROP = "token"; // token obj
    public static final String FORGOT_PASSWORD_USER_BEAN_PROP = "user"; //user obj
    public static final String FORGOT_PASSWORD_TOKEN_STRING_PROP = "token_str"; //string
    public static final String FORGOT_PASSWORD_TOKEN_VALIDITY_PERIOD_PROP = "token_validity_period"; //int
    public static final String FORGOT_PASSWORD_TOKEN_EXPIRATION_PROP = "token_expiration"; //JODA DateTime
    public static final String FORGOT_PASSWORD_USER_NAME_PROP = "username"; //string

    /*
        Dynamic properties made available to MFA templates
     */
    public static final String MFA_USER_NAME_PROP = "username"; //string

     /*
        Dynamic properties made available to unverified user invite template
     */
    public static final String INVITE_USER_REGISTRATION_URL = "registration_url"; // string
    public static final String INVITE_TTL_HOURS_PROP = "ttl_hours"; // string
    public static final String INVITE_YEAR_PROP = "year"; // string
}
