package com.rackspace.idm.validation;

public class RegexPatterns {
    public static final String NOT_EMPTY = ".*\\S+.*";

    // Must begin with non-space character
    public static final String USERNAME = "^[\\S].*";

    public static final String EMAIL_ADDRESS = ".+@.+\\.[\\w]+";

    private RegexPatterns(){};
}
