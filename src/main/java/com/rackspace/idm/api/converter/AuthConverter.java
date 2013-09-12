package com.rackspace.idm.api.converter;

import com.rackspace.api.idm.v1.*;
import com.rackspace.idm.domain.entity.AuthData;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.bind.JAXBElement;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.util.GregorianCalendar;

@Component
public class AuthConverter {

    @Autowired
    private UserConverter userConverter;
    @Autowired
    private ApplicationConverter clientConverter;
    @Autowired
    private TokenConverter tokenConverter;

    private final ObjectFactory objectFactory = new ObjectFactory();
    private Logger logger = LoggerFactory.getLogger(AuthConverter.class);

    public JAXBElement<com.rackspace.api.idm.v1.AuthData> toAuthDataJaxb(AuthData authData) {
        com.rackspace.api.idm.v1.AuthData authJaxb = objectFactory.createAuthData();

        JAXBElement<Token> accessToken = tokenConverter.toTokenJaxb(authData.getAccessToken(), authData.getAccessTokenExpiration());

        JAXBElement<Token> refreshToken = tokenConverter.toTokenJaxb(authData.getRefreshToken(), null);

        JAXBElement<Application> application = clientConverter.toApplicationJaxbFromApplication(authData.getApplication());

        JAXBElement<User> user = userConverter.toUserJaxbFromUser(authData.getUser());

        JAXBElement<Racker> racker = userConverter.toRackerJaxbFromRacker(authData.getRacker());

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
        authJaxb.setIsPasswordResetOnlyToken(authData.isPasswordResetOnlyToken());
        if (authData.getPasswordExpirationDate() != null) {
            authJaxb.setPasswordExpirationDate(toXmlGregorianCalender(authData.getPasswordExpirationDate()));
        }
        authJaxb.setDaysUntilPasswordExpiration(authData.getDaysUntilPasswordExpiration());

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
            logger.info("Failed to convert GregorianCalendar to XmlGregorianCalendar");
        }

        return null;
    }
}
