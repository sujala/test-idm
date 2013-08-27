package com.rackspace.idm.api.converter;

import com.rackspace.api.idm.v1.ObjectFactory;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.xml.bind.JAXBElement;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import java.util.Date;

@Component
public class TokenConverter {
    
    private final ObjectFactory of = new ObjectFactory();
    private Logger logger = LoggerFactory.getLogger(TokenConverter.class);

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
            logger.info("failed to create XMLGregorianCalendar: " + e.getMessage());
        }

        return of.createToken(jaxbToken);
    }
}
