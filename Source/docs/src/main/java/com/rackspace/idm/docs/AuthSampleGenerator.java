package com.rackspace.idm.docs;

import java.io.IOException;

import javax.xml.bind.JAXBException;

import com.rackspace.idm.jaxb.Auth;
import com.rackspace.idm.jaxb.AuthCredentials;
import com.rackspace.idm.jaxb.AuthGrantType;
import com.rackspace.idm.jaxb.Token;

public class AuthSampleGenerator extends SampleGenerator {
    private AuthSampleGenerator() {
        super();
    }
    
    public static void main(String[] args) throws JAXBException, IOException {
        AuthSampleGenerator sampleGen = new AuthSampleGenerator();

        sampleGen.marshalToFiles(sampleGen.getAuth(), "auth");
        sampleGen.marshalToFiles(sampleGen.getAuthCredentials(), "auth_credentials");
    }
    
    private Auth getAuth() {
        Auth auth = of.createAuth();
        
        Token access = of.createToken();
        access.setExpiresIn(new Integer(3600));
        access.setId("ab48a9efdfedb23ty3494");
        
        auth.setAccessToken(access);
        
        Token refresh = of.createToken();
        refresh.setId("8792gdfskjbadf98y234r");
        
        auth.setRefreshToken(refresh);
        
        return auth;
    }
    
    private AuthCredentials getAuthCredentials() {
        AuthCredentials creds = of.createAuthCredentials();
        creds.setClientId("8972348923400fdshasdf");
        creds.setClientSecret("0923899flewriudsb");
        creds.setGrantType(AuthGrantType.PASSWORD);
        creds.setUsername("testuser");
        creds.setPassword("P@ssword1");
        return creds;
    }
}
