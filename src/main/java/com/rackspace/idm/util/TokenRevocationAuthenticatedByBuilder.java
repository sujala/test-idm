package com.rackspace.idm.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TokenRevocationAuthenticatedByBuilder {





    private List<List<String>> authByList = new ArrayList<List<String>>();

    private TokenRevocationAuthenticatedByBuilder() {}

    public static TokenRevocationAuthenticatedByBuilder newBuilder() {
        return new TokenRevocationAuthenticatedByBuilder();
    }

    public TokenRevocationAuthenticatedByBuilder addAuthSet(String... authSet) {
        authByList.add(Arrays.asList(authSet));
        return this;
    }

    public List<List<String>> build() {
        List<List<String>> result = authByList;
        authByList = new ArrayList<List<String>>();
        return result;
    }
}
