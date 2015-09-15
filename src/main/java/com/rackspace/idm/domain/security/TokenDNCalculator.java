package com.rackspace.idm.domain.security;

public final class TokenDNCalculator {

    public static String calculateProvisionedUserTokenDN(String userRsId, String webSafeToken) {
        return String.format("accessToken=%s,cn=TOKENS,rsId=%s,ou=users,o=rackspace,dc=rackspace,dc=com", webSafeToken, userRsId);
    }

    public static  String calculateFederatedUserTokenDN(String username, String idpName, String webSafeToken) {
        return String.format("accessToken=%s,cn=TOKENS,uid=%s,ou=users,ou=%s,o=externalproviders,o=rackspace,dc=rackspace,dc=com", webSafeToken, username, idpName);
    }

    public static  String calculateRackerTokenDN(String rackerId, String webSafeToken) {
        return String.format("accessToken=%s,cn=TOKENS,rackerId=%s,ou=rackers,o=rackspace,dc=rackspace,dc=com", webSafeToken, rackerId);
    }
}
