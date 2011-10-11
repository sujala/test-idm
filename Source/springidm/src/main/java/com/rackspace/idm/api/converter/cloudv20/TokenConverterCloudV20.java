package com.rackspace.idm.api.converter.cloudv20;

import java.util.Date;
import java.util.GregorianCalendar;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.openstack.docs.identity.api.v2.Token;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.rackspace.idm.api.resource.cloud.JAXBObjectFactories;
import com.rackspace.idm.domain.entity.ScopeAccess;
import com.rackspace.idm.domain.entity.HasAccessToken;

@Component
public class TokenConverterCloudV20 {
    
    @Autowired
    private JAXBObjectFactories OBJ_FACTORIES;
    
    public Token toToken(ScopeAccess scopeAccess) {
        Token token = OBJ_FACTORIES.getOpenStackIdentityV2Factory().createToken();
        
        if (scopeAccess instanceof HasAccessToken) {
            
            token.setId(((HasAccessToken)scopeAccess).getAccessTokenString());
            
            Date expires = ((HasAccessToken)scopeAccess).getAccessTokenExp();
            GregorianCalendar gc = new GregorianCalendar();
            gc.setTime(expires);
            
            XMLGregorianCalendar expiresDate = null;
            try {
                expiresDate = DatatypeFactory.newInstance().newXMLGregorianCalendar(gc);
            } catch (DatatypeConfigurationException e) {
                
                e.printStackTrace();
            }
            token.setExpires(expiresDate);
        }
        
        return token;
    }
}
