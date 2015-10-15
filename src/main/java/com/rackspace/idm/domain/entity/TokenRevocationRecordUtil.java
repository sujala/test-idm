package com.rackspace.idm.domain.entity;

import org.apache.commons.lang.StringUtils;

import java.util.*;

public final class TokenRevocationRecordUtil {

    private TokenRevocationRecordUtil() {}

    public static String getNextId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public static List<AuthenticatedByMethodGroup> getAuthdByGroupsFromAuthByStrings(Iterable<String> internalTargetAuthenticatedBy) {
        if(internalTargetAuthenticatedBy == null || !internalTargetAuthenticatedBy.iterator().hasNext()) {
            return Collections.EMPTY_LIST;
        }

        List<AuthenticatedByMethodGroup> authBy = new ArrayList<AuthenticatedByMethodGroup>();

        for (String flattenedAuthBySet : internalTargetAuthenticatedBy) {
            if (TokenRevocationRecord.AUTHENTICATED_BY_EMPTY_LIST_SUBSTITUTE.equals(flattenedAuthBySet)) {
                authBy.add(AuthenticatedByMethodGroup.NULL);
            } else if (TokenRevocationRecord.AUTHENTICATED_BY_ALL_SUBSTITUTE.equals(flattenedAuthBySet)){
                authBy.add(AuthenticatedByMethodGroup.ALL);
            } else {
                String[] authByValues = StringUtils.splitPreserveAllTokens(flattenedAuthBySet, ",");
                authBy.add(AuthenticatedByMethodGroup.getGroup(Arrays.asList(authByValues)));
            }
        }
        return authBy;
    }

    /**
     * A list of authentication groups that define which tokens should be revoked.
     *
     * @param authenticatedByMethodGroups
     */
    public static List<String> getAuthByStringsFromAuthByGroups(List<AuthenticatedByMethodGroup> authenticatedByMethodGroups) {
        List<String> flattenedAuthBy = new ArrayList<String>();
        for (AuthenticatedByMethodGroup authBySet : authenticatedByMethodGroups) {
            String flattenedSet;
            if (authBySet.matches(AuthenticatedByMethodGroup.NULL)) {
                //substitute for the empty set since ldap can't handle null/blanks
                flattenedSet = TokenRevocationRecord.AUTHENTICATED_BY_EMPTY_LIST_SUBSTITUTE;
            } else if (authBySet.matches(AuthenticatedByMethodGroup.ALL)) {
                //substitute for the empty set since ldap can't handle null/blanks
                flattenedSet = TokenRevocationRecord.AUTHENTICATED_BY_ALL_SUBSTITUTE;
            } else {
                flattenedSet = StringUtils.join(authBySet.getAuthenticatedByMethodsAsValues(), ",");
            }
            flattenedAuthBy.add(flattenedSet);
        }
        return flattenedAuthBy;
    }

}
