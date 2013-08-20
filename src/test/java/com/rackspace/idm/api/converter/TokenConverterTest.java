package com.rackspace.idm.api.converter;

import com.rackspace.api.idm.v1.Token;
import org.junit.Before;
import org.junit.Test;

import javax.xml.bind.JAXBElement;
import java.util.Date;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

/**
 * Created with IntelliJ IDEA.
 * User: kurt
 * Date: 6/12/12
 * Time: 12:46 PM
 * To change this template use File | Settings | File Templates.
 */
public class TokenConverterTest {
    TokenConverter tokenConverter;

    @Before
    public void setUp() throws Exception {
        tokenConverter = new TokenConverter();
    }

    @Test
    public void toTokenJaxb_withTokenString_setsId() throws Exception {
        JAXBElement<Token> token = tokenConverter.toTokenJaxb("tokenString", null);
        assertThat("token id", token.getValue().getId(), equalTo("tokenString"));
    }

    @Test
    public void toTokenJaxb_withExpiration_setsExpires() throws Exception {
        JAXBElement<Token> token = tokenConverter.toTokenJaxb("tokenString", new Date(1));
        assertThat("token expires", token.getValue().getExpires().toGregorianCalendar().getTimeInMillis(), equalTo(1L));
    }

    @Test
    public void toTokenJaxb_withOutExpiration_setsExpiresToNull() throws Exception {
        JAXBElement<Token> token = tokenConverter.toTokenJaxb("tokenString", null);
        assertThat("token expires", token.getValue().getExpires(), nullValue());
    }

    @Test
    public void toTokenJaxb_withNullTokenString_returnsNull() throws Exception {
        JAXBElement<Token> token = tokenConverter.toTokenJaxb(null, new Date(1));
        assertThat("token", token, nullValue());
    }
}
