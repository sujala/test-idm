package com.rackspace.idm.domain.security;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TokenDNCalculator {

    private static final String TOKEN_FORMAT = "([a-zA-Z0-9\\-\\_]+)";
    private static final String ID_FORMAT = "([a-zA-Z0-9]+)";

    public static final String FEDERATED_USER_TOKEN_DN_FORMAT = "accessToken=%s,cn=TOKENS,uid=%s,ou=users,ou=%s,o=externalproviders,o=rackspace,dc=rackspace,dc=com";
    public static final Pattern FEDERATED_USER_TOKEN_DN_PATTERN = Pattern.compile(String.format(FEDERATED_USER_TOKEN_DN_FORMAT, TOKEN_FORMAT, ID_FORMAT, ID_FORMAT));

    public static String calculateProvisionedUserTokenDN(String userRsId, String webSafeToken) {
        return String.format("accessToken=%s,cn=TOKENS,rsId=%s,ou=users,o=rackspace,dc=rackspace,dc=com", webSafeToken, userRsId);
    }

    public static  String calculateFederatedUserTokenDN(String username, String idpName, String webSafeToken) {
        return String.format(FEDERATED_USER_TOKEN_DN_FORMAT, webSafeToken, username, idpName);
    }

    public static String parseIdentityProviderIdFromFederatedTokenDN(String federatedTokenDn) {
        if (federatedTokenDn != null) {
            Matcher matcher = FEDERATED_USER_TOKEN_DN_PATTERN.matcher(federatedTokenDn);
            if (matcher.matches() && matcher.groupCount() == 3) {
                return matcher.group(3);
            }
        }
        return null;
    }

    public static  String calculateRackerTokenDN(String rackerId, String webSafeToken) {
        return String.format("accessToken=%s,cn=TOKENS,rackerId=%s,ou=rackers,o=rackspace,dc=rackspace,dc=com", webSafeToken, rackerId);
   }

}
