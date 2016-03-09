package com.rackspace.idm.api.resource.cloud.email;

public final class EmailTemplateConstants {
    private EmailTemplateConstants() {
    }

    public static final String FORGOT_PASSWORD_TOKEN_BEAN_PROP = "token"; // token obj
    public static final String FORGOT_PASSWORD_USER_BEAN_PROP = "user"; //user obj
    public static final String FORGOT_PASSWORD_TOKEN_STRING_PROP = "token_str"; //string
    public static final String FORGOT_PASSWORD_TOKEN_VALIDITY_PERIOD_PROP = "token_validity_period"; //int
    public static final String FORGOT_PASSWORD_TOKEN_EXPIRATION_PROP = "token_expiration"; //JODA DateTime
    public static final String FORGOT_PASSWORD_USER_NAME_PROP = "username"; //string

    public static final String MFA_USER_NAME_PROP = "username"; //string

}
