package com.rackspace.idm.api.converter.cloudv11;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;

import org.joda.time.DateTime;

import com.rackspace.idm.domain.entity.UserScopeAccess;
import com.rackspacecloud.docs.auth.api.v1.Token;

public class TokenConverterCloudV11 {
    
    private final com.rackspacecloud.docs.auth.api.v1.ObjectFactory OBJ_FACTORY = new com.rackspacecloud.docs.auth.api.v1.ObjectFactory();
    
    public Token toCloudv11TokenJaxb(UserScopeAccess usa) {
        
        com.rackspacecloud.docs.auth.api.v1.Token token = OBJ_FACTORY.createToken();
    
        if (usa.getAccessTokenString() != null) {
            
            token.setId(usa.getAccessTokenString());
            
            try {
                if (usa.getAccessTokenExp() != null) {

                    token.setExpires(DatatypeFactory.newInstance()
                        .newXMLGregorianCalendar(
                            new DateTime(usa.getAccessTokenExp()).toGregorianCalendar()));
                }

            } catch (DatatypeConfigurationException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        return token;
    }

}
