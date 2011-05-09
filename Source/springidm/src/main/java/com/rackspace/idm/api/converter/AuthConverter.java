package com.rackspace.idm.api.converter;

import java.util.GregorianCalendar;
import java.util.List;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.joda.time.DateTime;

import com.rackspace.idm.domain.entity.CloudEndpoint;
import com.rackspace.idm.domain.entity.PasswordResetScopeAccessObject;
import com.rackspace.idm.domain.entity.RackerScopeAccess;
import com.rackspace.idm.domain.entity.ScopeAccess;
import com.rackspace.idm.domain.entity.UserScopeAccess;
import com.rackspace.idm.domain.entity.hasAccessToken;
import com.rackspace.idm.domain.entity.hasRefreshToken;
import com.rackspace.idm.jaxb.CloudAuth;
import com.rackspace.idm.jaxb.ObjectFactory;
import com.rackspace.idm.jaxb.ServiceCatalog;

public class AuthConverter {

    private final UserConverter userConverter;
    private final ClientConverter clientConverter;
    private final TokenConverter tokenConverter;
    private final EndPointConverter endpointConverter;

    private final ObjectFactory of = new ObjectFactory();

    public AuthConverter(TokenConverter tokenConverter,
        ClientConverter clientConverter, UserConverter userConverter,
        EndPointConverter endpointConverter) {
        this.tokenConverter = tokenConverter;
        this.clientConverter = clientConverter;
        this.userConverter = userConverter;
        this.endpointConverter = endpointConverter;
    }

    public com.rackspace.idm.jaxb.Auth toAuthDataJaxb(ScopeAccess scopeAccess) {
        com.rackspace.idm.jaxb.Auth authJaxb = of.createAuth();

        DateTime passwordExpirationDate = null;

        if (scopeAccess instanceof hasAccessToken) {
            hasAccessToken tokenScopeAccessObject = ((hasAccessToken)scopeAccess);
            authJaxb.setAccessToken(tokenConverter.toTokenJaxb(
                    tokenScopeAccessObject.getAccessTokenString(), 
                    tokenScopeAccessObject.getAccessTokenExp()));
        }

        if (scopeAccess instanceof hasRefreshToken) {
            hasRefreshToken tokenScopeAccessObject = ((hasRefreshToken)scopeAccess);
            authJaxb.setRefreshToken(tokenConverter.toTokenJaxb(
                    tokenScopeAccessObject.getRefreshTokenString(), null));
        }

        if (scopeAccess.getClientId() != null) {
            authJaxb.setClient(clientConverter.toClientJaxbFromClient(scopeAccess
                .getClientId(), scopeAccess.getClientRCN()));
        }

        if (scopeAccess instanceof UserScopeAccess) {
            UserScopeAccess userScopeAccess = (UserScopeAccess) scopeAccess;
            passwordExpirationDate = userScopeAccess.getUserPasswordExpirationDate();
            
            if(userScopeAccess.getUsername() != null) {
                authJaxb.setUser(userConverter.toUserJaxbFromUser(userScopeAccess.getUsername(), 
                        userScopeAccess.getUserRCN()));
            }
        }
        
        if (scopeAccess instanceof RackerScopeAccess) {
            RackerScopeAccess rackerScopeAccess = (RackerScopeAccess) scopeAccess;
            
            if(rackerScopeAccess.getRackerId() != null) {
                authJaxb.setRacker(userConverter.toRackerJaxb(rackerScopeAccess.getRackerId()));
            }
        }
        
        if (scopeAccess instanceof PasswordResetScopeAccessObject) {
            authJaxb.setIsPasswordResetOnlyToken(true);
            passwordExpirationDate = ((PasswordResetScopeAccessObject)scopeAccess).getUserPasswordExpirationDate();
        }
        
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

    public CloudAuth toCloudAuthJaxb(UserScopeAccess usa, List<CloudEndpoint> endpoints) {
        CloudAuth auth = of.createCloudAuth();
        
        if (usa.getAccessTokenString() != null) {
            com.rackspace.idm.jaxb.Token token = this.tokenConverter.toTokenJaxb(
                    usa.getAccessTokenString(), usa.getAccessTokenExp());
            auth.setToken(token);
        }
        
        if (endpoints != null && endpoints.size() > 0) {
            ServiceCatalog catalog = this.endpointConverter.toServiceCatalog(endpoints);
            auth.setServiceCatalog(catalog);
        }
        
        return auth;
    }
}
