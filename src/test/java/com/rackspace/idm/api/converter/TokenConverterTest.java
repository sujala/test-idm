package com.rackspace.idm.api.converter;

import com.rackspace.api.idm.v1.DelegatedToken;
import com.rackspace.api.idm.v1.DelegatedTokenList;
import com.rackspace.api.idm.v1.Token;
import com.rackspace.api.idm.v1.TokenList;
import com.rackspace.idm.domain.entity.DelegatedClientScopeAccess;
import com.rackspace.idm.domain.entity.Permission;
import org.junit.Before;
import org.junit.Test;

import javax.xml.bind.JAXBElement;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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


    @Test
    public void toDelegatedTokenJaxb_withDelegatedClientScopeAccess_setsId() throws Exception {
        DelegatedClientScopeAccess tokenDO = new DelegatedClientScopeAccess();
        tokenDO.setRefreshTokenString("refreshTokenString");
        DelegatedToken token = tokenConverter.toDelegatedTokenJaxb(tokenDO, new ArrayList<Permission>());
        assertThat("token Id", token.getId(), equalTo("refreshTokenString"));
    }

    @Test
    public void toDelegatedTokenJaxb_withDelegatedClientScopeAccess_setsClientId() throws Exception {
        DelegatedClientScopeAccess tokenDO = new DelegatedClientScopeAccess();
        tokenDO.setClientId("clientId");
        DelegatedToken token = tokenConverter.toDelegatedTokenJaxb(tokenDO, new ArrayList<Permission>());
        assertThat("token client Id", token.getClientId(), equalTo("clientId"));
    }

    @Test
    public void toDelegatedTokenJaxb_withDelegatedClientScopeAccess_setsExpires() throws Exception {
        DelegatedClientScopeAccess tokenDO = new DelegatedClientScopeAccess();
        tokenDO.setRefreshTokenExp(new Date(1));
        DelegatedToken token = tokenConverter.toDelegatedTokenJaxb(tokenDO, new ArrayList<Permission>());
        assertThat("token expires", token.getExpires().toGregorianCalendar().getTimeInMillis(), equalTo(1L));
    }

    @Test
    public void toTokensJaxb_withTokenList_returnsJaxbTokenList_withCorrectSize() throws Exception {
        List<DelegatedClientScopeAccess> tokenList = new ArrayList<DelegatedClientScopeAccess>();
        DelegatedClientScopeAccess delegatedClientScopeAccess = new DelegatedClientScopeAccess();
        delegatedClientScopeAccess.setRefreshTokenString("refreshTokenString");
        tokenList.add(delegatedClientScopeAccess);
        tokenList.add(delegatedClientScopeAccess);
        JAXBElement<TokenList> tokenListJAXBElement = tokenConverter.toTokensJaxb(tokenList);
        assertThat("token list jaxb size", tokenListJAXBElement.getValue().getToken().size(), equalTo(2));
    }

    @Test
    public void toTokensJaxb_withEmptyTokenList_returnsEmptyList() throws Exception {
        JAXBElement<TokenList> tokenListJAXBElement = tokenConverter.toTokensJaxb(new ArrayList<DelegatedClientScopeAccess>());
        assertThat("token list jaxb size", tokenListJAXBElement.getValue().getToken().size(), equalTo(0));
    }

    @Test
    public void toTokensJaxb_withTokenList_withNewTokenObjects_returnsEmptyList() throws Exception {
        List<DelegatedClientScopeAccess> tokenList = new ArrayList<DelegatedClientScopeAccess>();
        tokenList.add(new DelegatedClientScopeAccess());
        JAXBElement<TokenList> tokenListJAXBElement = tokenConverter.toTokensJaxb(tokenList);
        assertThat("token list jaxb size", tokenListJAXBElement.getValue().getToken().size(), equalTo(0));
    }

    @Test
    public void toDelegatedTokensJaxb_withTokenList_returnsJaxbTokenList_WithCorrectSize() throws Exception {
        List<DelegatedClientScopeAccess> tokenList = new ArrayList<DelegatedClientScopeAccess>();
        tokenList.add(new DelegatedClientScopeAccess());
        tokenList.add(new DelegatedClientScopeAccess());
        JAXBElement<DelegatedTokenList> delegatedTokenListJAXBElement = tokenConverter.toDelegatedTokensJaxb(tokenList);
        assertThat("jaxb token list size", delegatedTokenListJAXBElement.getValue().getDelegatedToken().size(), equalTo(2));
    }

    @Test
    public void toDelegatedTokensJaxb_withEmptyTokenList_returnsJaxbTokenList_WithZeroSize() throws Exception {
        List<DelegatedClientScopeAccess> tokenList = new ArrayList<DelegatedClientScopeAccess>();
        JAXBElement<DelegatedTokenList> delegatedTokenListJAXBElement = tokenConverter.toDelegatedTokensJaxb(tokenList);
        assertThat("jaxb token list size", delegatedTokenListJAXBElement.getValue().getDelegatedToken().size(), equalTo(0));
    }
}
