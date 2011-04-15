package com.rackspace.idm.api.converter;

import java.util.GregorianCalendar;
import java.util.List;

import javax.xml.datatype.*;

import org.joda.time.DateTime;

import com.rackspace.idm.domain.entity.AccessToken;
import com.rackspace.idm.domain.entity.AuthData;
import com.rackspace.idm.domain.entity.CloudEndpoint;
import com.rackspace.idm.jaxb.CloudAuth;
import com.rackspace.idm.jaxb.ObjectFactory;
import com.rackspace.idm.jaxb.ServiceCatalog;

public class AuthConverter {

    private UserConverter userConverter;
    private ClientConverter clientConverter;
    private PermissionConverter permissionConverter;
    private TokenConverter tokenConverter;
    private EndPointConverter endpointConverter;

    private ObjectFactory of = new ObjectFactory();

    public AuthConverter(TokenConverter tokenConverter,
        PermissionConverter permissionConverter,
        ClientConverter clientConverter, UserConverter userConverter,
        EndPointConverter endpointConverter) {
        this.tokenConverter = tokenConverter;
        this.permissionConverter = permissionConverter;
        this.clientConverter = clientConverter;
        this.userConverter = userConverter;
        this.endpointConverter = endpointConverter;
    }

    public com.rackspace.idm.jaxb.Auth toAuthDataJaxb(AuthData auth) {
        com.rackspace.idm.jaxb.Auth authJaxb = of.createAuth();

        if (auth.getAccessToken() != null) {
            authJaxb.setAccessToken(tokenConverter.toAccessTokenJaxb(auth
                .getAccessToken()));
        }

        if (auth.getRefreshToken() != null) {
            authJaxb.setRefreshToken(tokenConverter.toRefreshTokenJaxb(auth
                .getRefreshToken()));
        }

        if (auth.getClient() != null) {
            authJaxb.setClient(clientConverter.toClientJaxbFromBaseClient(auth
                .getClient()));
        }

        if (auth.getUser() != null) {
            authJaxb.setUser(userConverter.toUserJaxbFromBaseUser(auth
                .getUser()));
        }

        if (auth.getPermissions() != null) {
            authJaxb.setPermissions(permissionConverter
                .toPermissionListJaxb(auth.getPermissions()));
        }
        
        if (auth.getPasswordResetOnlyToken() != null && auth.getPasswordResetOnlyToken()) {
            authJaxb.setIsPasswordResetOnlyToken(auth.getPasswordResetOnlyToken());
        }
        
        DateTime passwordExpirationDate = auth.getUserPasswordExpirationDate();
        
        if (passwordExpirationDate != null) {    
            GregorianCalendar gc = new GregorianCalendar();
            gc.setTime(passwordExpirationDate.toDate());
            
            XMLGregorianCalendar xmlPasswordExpirationDate = null;
            try {
                xmlPasswordExpirationDate = DatatypeFactory.newInstance().newXMLGregorianCalendar(gc);
            } catch (DatatypeConfigurationException e) {
                
                e.printStackTrace();
            }
            
            authJaxb.setPasswordExpirationDate(xmlPasswordExpirationDate);
            
            DateTime today = new DateTime();
            int daysToPasswordExpiry = passwordExpirationDate.getDayOfYear() - today.getDayOfYear();
            
            if (daysToPasswordExpiry > 0) {
                authJaxb.setDaysUntilPasswordExpiration(daysToPasswordExpiry);
            } 
        }
        
        return authJaxb;
    }

    public CloudAuth toCloudAuthJaxb(AccessToken accessToken, List<CloudEndpoint> endpoints) {
        CloudAuth auth = of.createCloudAuth();
        
        if (accessToken != null) {
            com.rackspace.idm.jaxb.Token token = of.createToken();
            token.setExpiresIn(accessToken.getExpiration());
            token.setId(accessToken.getTokenString());
            auth.setToken(token);
        }
        
        if (endpoints != null && endpoints.size() > 0) {
            ServiceCatalog catalog = this.endpointConverter.toServiceCatalog(endpoints);
            auth.setServiceCatalog(catalog);
        }
        
        return auth;
    }
}
