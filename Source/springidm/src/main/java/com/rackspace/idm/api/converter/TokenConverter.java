package com.rackspace.idm.api.converter;

import java.util.Date;
import java.util.List;

import javax.xml.bind.JAXBElement;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;

import com.rackspace.api.idm.v1.Token;
import org.joda.time.DateTime;

import com.rackspace.api.idm.v1.ObjectFactory;
import com.rackspace.idm.domain.entity.DelegatedClientScopeAccess;
import com.rackspace.idm.domain.entity.Permission;

public class TokenConverter {
    
    private final ObjectFactory of = new ObjectFactory();
    
    public TokenConverter() {
    }

    public JAXBElement<com.rackspace.api.idm.v1.Token> toTokenJaxb(String tokenString,
        Date expiration) {
    	if (tokenString == null) {
    		return null;
    	}
    	
        com.rackspace.api.idm.v1.Token jaxbToken = of.createToken();

        jaxbToken.setId(tokenString);

        try {
            if (expiration != null) {

                jaxbToken.setExpires(DatatypeFactory.newInstance()
                    .newXMLGregorianCalendar(
                        new DateTime(expiration).toGregorianCalendar()));
            }

        } catch (DatatypeConfigurationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return of.createToken(jaxbToken);
    }

    public com.rackspace.api.idm.v1.DelegatedToken toDelegatedTokenJaxb(
        DelegatedClientScopeAccess token, List<Permission> perms) {
        com.rackspace.api.idm.v1.DelegatedToken jaxbToken = of
            .createDelegatedToken();

        jaxbToken.setId(token.getRefreshTokenString());
        jaxbToken.setClientId(token.getClientId());

        try {
            jaxbToken.setExpires(DatatypeFactory.newInstance()
                .newXMLGregorianCalendar(
                    new DateTime(token.getRefreshTokenExp())
                        .toGregorianCalendar()));

        } catch (DatatypeConfigurationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return jaxbToken;
    }

    public JAXBElement<com.rackspace.api.idm.v1.TokenList> toTokensJaxb(
        List<DelegatedClientScopeAccess> scopeAccessList) {
        com.rackspace.api.idm.v1.TokenList jaxbTokens = of.createTokenList();

        for (DelegatedClientScopeAccess u : scopeAccessList) {
            JAXBElement<Token> token = toTokenJaxb(u.getRefreshTokenString(), u.getRefreshTokenExp());
            if(token != null) {
                jaxbTokens.getToken().add(token.getValue());
            }
        }
        return of.createTokens(jaxbTokens);
    }

    public JAXBElement<com.rackspace.api.idm.v1.DelegatedTokenList> toDelegatedTokensJaxb(
        List<DelegatedClientScopeAccess> scopeAccessList) {
        com.rackspace.api.idm.v1.DelegatedTokenList jaxbTokens = of
            .createDelegatedTokenList();

        for (DelegatedClientScopeAccess u : scopeAccessList) {
            com.rackspace.api.idm.v1.DelegatedToken token = toDelegatedTokenJaxb(u, null);
            jaxbTokens.getDelegatedToken().add(token);
        }
        return of.createDelegatedTokens(jaxbTokens);
    }
}
