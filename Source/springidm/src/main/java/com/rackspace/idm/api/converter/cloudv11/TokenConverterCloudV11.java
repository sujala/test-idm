package com.rackspace.idm.api.converter.cloudv11;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;

import org.joda.time.DateTime;

import com.rackspace.idm.cloud.jaxb.Token;
import com.rackspace.idm.domain.entity.UserScopeAccess;

public class TokenConverterCloudV11 {
    
    private final com.rackspace.idm.cloud.jaxb.ObjectFactory OBJ_FACTORY = new com.rackspace.idm.cloud.jaxb.ObjectFactory();
    
    public Token toCloudv10TokenJaxb(UserScopeAccess usa) {
        
        com.rackspace.idm.cloud.jaxb.Token token = OBJ_FACTORY.createToken();
    
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
