package com.rackspace.idm.validation;

public final class MessageTexts {

    private MessageTexts() {}

    public static final String PARAMETER_VALIDATION_FAILED = "One or more parameters are missing or invalid.";

    public static final String NOT_EMPTY = "may not be blank";
    public static final String USERNAME = "may not be blank, start with a number, or end with @rackspace.com";
    public static final String EMAIL = "provide a valid email address format";
}
