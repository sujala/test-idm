package com.rackspace.idm.api.converter;

import java.util.Date;
import java.util.List;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;

import org.joda.time.DateTime;

import com.rackspace.idm.domain.entity.DelegatedClientScopeAccess;
import com.rackspace.idm.domain.entity.Permission;
import com.rackspace.idm.jaxb.ObjectFactory;
import com.rackspace.idm.jaxb.Permissions;

public class TokenConverter {
    private final PermissionConverter permissionConverter;
    
    private final ObjectFactory of = new ObjectFactory();
    
    public TokenConverter(PermissionConverter permissionConverter) {
        this.permissionConverter = permissionConverter;
    }

    public com.rackspace.idm.jaxb.Token toTokenJaxb(String tokenString,
        Date expiration) {
        com.rackspace.idm.jaxb.Token jaxbToken = of.createToken();

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

        return jaxbToken;
    }

    public com.rackspace.idm.jaxb.Token toTokenJaxb(String tokenString,
        Date expiration, Permissions permList) {
        com.rackspace.idm.jaxb.Token jaxbToken = of.createToken();

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

        if (permList != null) {
            jaxbToken.setPermissionList(permList);
        }

        return jaxbToken;
    }

    public com.rackspace.idm.jaxb.DelegatedToken toDelegatedTokenJaxb(
        DelegatedClientScopeAccess token, List<Permission> perms) {
        com.rackspace.idm.jaxb.DelegatedToken jaxbToken = of
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

        if (perms != null && perms.size() > 0) {
            jaxbToken.setPermissionList(permissionConverter.toPermissionListJaxb(perms));
        }

        return jaxbToken;
    }

    public com.rackspace.idm.jaxb.Tokens toTokensJaxb(
        List<DelegatedClientScopeAccess> scopeAccessList) {
        com.rackspace.idm.jaxb.Tokens jaxbTokens = of.createTokens();

        for (DelegatedClientScopeAccess u : scopeAccessList) {
            com.rackspace.idm.jaxb.Token token = toTokenJaxb(
                u.getRefreshTokenString(), u.getRefreshTokenExp());
            jaxbTokens.getTokens().add(token);
        }
        return jaxbTokens;
    }

    public com.rackspace.idm.jaxb.DelegatedTokens toDelegatedTokensJaxb(
        List<DelegatedClientScopeAccess> scopeAccessList) {
        com.rackspace.idm.jaxb.DelegatedTokens jaxbTokens = of
            .createDelegatedTokens();

        for (DelegatedClientScopeAccess u : scopeAccessList) {
            com.rackspace.idm.jaxb.DelegatedToken token = toDelegatedTokenJaxb(u, null);
            jaxbTokens.getDelegatedTokens().add(token);
        }
        return jaxbTokens;
    }
}
