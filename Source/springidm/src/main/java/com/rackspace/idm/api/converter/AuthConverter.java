package com.rackspace.idm.api.converter;

import java.util.GregorianCalendar;

import javax.xml.bind.JAXBElement;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.joda.time.DateTime;

import com.rackspace.api.idm.v1.Application;
import com.rackspace.api.idm.v1.ObjectFactory;
import com.rackspace.api.idm.v1.Racker;
import com.rackspace.api.idm.v1.Token;
import com.rackspace.api.idm.v1.User;
import com.rackspace.idm.domain.entity.AuthData;

public class AuthConverter {

    private final UserConverter userConverter;
    private final ApplicationConverter clientConverter;
    private final TokenConverter tokenConverter;

    private final ObjectFactory objectFactory = new ObjectFactory();

    public AuthConverter(TokenConverter tokenConverter,
        ApplicationConverter clientConverter, UserConverter userConverter) {
        this.tokenConverter = tokenConverter;
        this.clientConverter = clientConverter;
        this.userConverter = userConverter;
    }

    public JAXBElement<com.rackspace.api.idm.v1.AuthData> toAuthDataJaxb(AuthData authData) {
        com.rackspace.api.idm.v1.AuthData authJaxb = objectFactory
            .createAuthData();

        JAXBElement<Token> accessToken = tokenConverter.toTokenJaxb(
            authData.getAccessToken(), authData.getAccessTokenExpiration());

        JAXBElement<Token> refreshToken = tokenConverter.toTokenJaxb(
            authData.getRefreshToken(), null);

        JAXBElement<Application> application = clientConverter
            .toApplicationJaxbFromApplication(authData.getApplication());

        JAXBElement<User> user = userConverter.toUserJaxbFromUser(authData
            .getUser());

        JAXBElement<Racker> racker = userConverter
            .toRackerJaxbFromRacker(authData.getRacker());

        if (accessToken != null) {
            authJaxb.setAccessToken(accessToken.getValue());
        }
        if (refreshToken != null) {
            authJaxb.setRefreshToken(refreshToken.getValue());
        }
        if (application != null) {
            authJaxb.setApplication(application.getValue());
        }
        if (user != null) {
            authJaxb.setUser(user.getValue());
        }
        if (racker != null) {
            authJaxb.setRacker(racker.getValue());
        }
        authJaxb.setIsPasswordResetOnlyToken(authData
            .isPasswordResetOnlyToken());
        if (authData.getPasswordExpirationDate() != null) {
            authJaxb.setPasswordExpirationDate(toXmlGregorianCalender(authData
                .getPasswordExpirationDate()));
        }
        authJaxb.setDaysUntilPasswordExpiration(authData
            .getDaysUntilPasswordExpiration());

        return objectFactory.createAuth(authJaxb);
    }

    XMLGregorianCalendar toXmlGregorianCalender(DateTime dateTime) {
        try {
            if (dateTime != null) {
                GregorianCalendar gc = new GregorianCalendar();
                gc.setTime(dateTime.toDate());
                XMLGregorianCalendar xmlDate = DatatypeFactory.newInstance()
                    .newXMLGregorianCalendar(gc);
                return xmlDate;
            }

        } catch (DatatypeConfigurationException e) {
            e.printStackTrace();
        }

        return null;
    }

    // public com.rackspace.api.idm.v1.Auth toAuthDataJaxb(ScopeAccess
    // scopeAccess) {
    // com.rackspace.api.idm.v1.Auth authJaxb = objectFactory.createAuth();
    //
    // DateTime passwordExpirationDate = null;
    //
    // if (scopeAccess instanceof HasAccessToken) {
    // HasAccessToken tokenScopeAccessObject = ((HasAccessToken)scopeAccess);
    //
    // authJaxb.setAccessToken(tokenConverter.toTokenJaxb(
    // tokenScopeAccessObject.getAccessTokenString(),
    // tokenScopeAccessObject.getAccessTokenExp()));
    // }
    //
    // if (scopeAccess instanceof HasRefreshToken) {
    // HasRefreshToken tokenScopeAccessObject = ((HasRefreshToken)scopeAccess);
    // authJaxb.setRefreshToken(tokenConverter.toTokenJaxb(
    // tokenScopeAccessObject.getRefreshTokenString(), null));
    // }
    //
    // if (scopeAccess instanceof ClientScopeAccess) {
    // authJaxb.setApplication(clientConverter.toClientJaxbFromClient(scopeAccess
    // .getClientId(), scopeAccess.getClientRCN()));
    // }
    //
    // if (scopeAccess instanceof UserScopeAccess) {
    // UserScopeAccess userScopeAccess = (UserScopeAccess) scopeAccess;
    // passwordExpirationDate = userScopeAccess.getUserPasswordExpirationDate();
    //
    // if(userScopeAccess.getUsername() != null) {
    // authJaxb.setUser(userConverter.toUserJaxbFromUser(userScopeAccess.getUsername(),
    // userScopeAccess.getUserRCN()));
    // }
    // }
    //
    // if (scopeAccess instanceof DelegatedClientScopeAccess) {
    // DelegatedClientScopeAccess dcsa = (DelegatedClientScopeAccess)
    // scopeAccess;
    // authJaxb.setUser(userConverter.toUserJaxbFromUser(dcsa.getUsername(),
    // dcsa.getUserRCN()));
    //
    // }
    //
    // if (scopeAccess instanceof RackerScopeAccess) {
    // RackerScopeAccess rackerScopeAccess = (RackerScopeAccess) scopeAccess;
    //
    // if(rackerScopeAccess.getRackerId() != null) {
    // authJaxb.setRacker(userConverter.toRackerJaxb(rackerScopeAccess.getRackerId()));
    // }
    // }
    //
    // if (scopeAccess instanceof PasswordResetScopeAccess) {
    // authJaxb.setIsPasswordResetOnlyToken(true);
    // passwordExpirationDate =
    // ((PasswordResetScopeAccess)scopeAccess).getUserPasswordExpirationDate();
    // }
    //
    // if (passwordExpirationDate != null) {
    // GregorianCalendar gc = new GregorianCalendar();
    // gc.setTime(passwordExpirationDate.toDate());
    //
    // XMLGregorianCalendar xmlPasswordExpirationDate = null;
    // try {
    // xmlPasswordExpirationDate =
    // DatatypeFactory.newInstance().newXMLGregorianCalendar(gc);
    // } catch (DatatypeConfigurationException e) {
    //
    // e.printStackTrace();
    // }
    //
    // authJaxb.setPasswordExpirationDate(xmlPasswordExpirationDate);
    //
    // DateTime today = new DateTime();
    // int daysToPasswordExpiry = passwordExpirationDate.getDayOfYear() -
    // today.getDayOfYear();
    //
    // if (daysToPasswordExpiry > 0) {
    // authJaxb.setDaysUntilPasswordExpiration(daysToPasswordExpiry);
    // }
    // }
    //
    // return authJaxb;
    // }
}
