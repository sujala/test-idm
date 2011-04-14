package com.rackspace.idm.domain.service;

import java.util.List;

import org.joda.time.DateTime;

import com.rackspace.idm.domain.entity.AccessToken;
import com.rackspace.idm.domain.entity.BaseClient;
import com.rackspace.idm.domain.entity.BaseUser;
import com.rackspace.idm.domain.entity.Client;
import com.rackspace.idm.domain.entity.User;

public interface AccessTokenService {

    boolean authenticateAccessToken(String accessTokenStr);

    AccessToken getTokenByUsernameAndPassword(BaseClient client, String username, String password,
        int expirationSeconds, DateTime currentTime);

    AccessToken getTokenByUsernameAndApiCredentials(BaseClient client, String username, String apiKey,
        DateTime currentTime);

    AccessToken getTokenByNastIdAndApiCredentials(BaseClient client, String nastId, String apiKey,
        int expirationSeconds, DateTime currentTime);

    AccessToken getTokenByMossoIdAndApiCredentials(BaseClient client, int mossoId, String apiKey,
        int expirationSeconds, DateTime currentTime);

    AccessToken getTokenByBasicCredentials(BaseClient client, BaseUser user, int expirationSeconds,
        DateTime currentTime);

    AccessToken getAccessTokenByAuthHeader(String authHeader);

    AccessToken getAccessTokenByTokenString(String tokenString);

    AccessToken getAccessTokenByTokenStringGlobally(String tokenString);

    AccessToken getAccessTokenForUser(BaseUser user, BaseClient client, DateTime expiresAfter);

    AccessToken getAccessTokenForClient(BaseClient client, DateTime expiresAfter);

    AccessToken createAccessTokenForUser(BaseUser user, BaseClient client, int expirationSeconds);

    AccessToken createPasswordResetAccessTokenForUser(User user, String clientId);

    AccessToken createPasswordResetAccessTokenForUser(User user, String clientId, int expirationTimeInSeconds);
    
    AccessToken createPasswordResetAccessTokenForUser(String userName, String clientId);    

    AccessToken createAccessTokenForClient(BaseClient client);

    AccessToken createAccessTokenForClient(BaseClient client, int expirationSeconds);

    AccessToken createAccessTokenForUser(String username, String clientId);

    AccessToken createAccessTokenForUser(String username, String clientId, int expirationSeconds);

    int getDefaultTokenExpirationSeconds();

    int getCloudAuthDefaultTokenExpirationSeconds();

    AccessToken validateToken(String tokenString);

    void delete(String tokenString);

    void deleteAllForOwner(String owner);

    void deleteAllGloballyForOwner(String owner);

    void deleteAllGloballyForCustomer(String customerId, List<User> users, List<Client> clients);

    boolean passwordRotationDurationElapsed(String userName);
}
