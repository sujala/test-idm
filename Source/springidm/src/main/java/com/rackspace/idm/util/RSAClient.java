package com.rackspace.idm.util;

import com.rsa.authagent.authapi.AuthSession;
import com.rsa.authagent.authapi.AuthSessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Created by IntelliJ IDEA.
 * User: Hector
 * Date: 3/1/12
 * Time: 2:10 PM
 */
@Component
public class RSAClient {

    private AuthSessionFactory api;

    private static Logger logger = LoggerFactory.getLogger(RSAClient.class);

    public boolean authenticate(String userID, String passCode) {
        try {
            api = AuthSessionFactory.getInstance();
            AuthSession authSession = api.createUserSession();
            authSession.lock(userID);
            int status = authSession.check(userID, passCode);
            if (status == AuthSession.ACCESS_OK) {
                return true;
            }
            authSession.close();
            api.shutdown();
            return false;
        } catch (Exception e) {
            logger.info("error authentication racker with rsa credentials: {}", e.getMessage());
            return false;
        }
    }
}
