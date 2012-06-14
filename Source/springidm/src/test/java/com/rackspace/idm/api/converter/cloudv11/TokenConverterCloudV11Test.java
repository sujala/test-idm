package com.rackspace.idm.api.converter.cloudv11;

import com.rackspace.idm.domain.entity.UserScopeAccess;
import com.rackspacecloud.docs.auth.api.v1.Token;
import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Created with IntelliJ IDEA.
 * User: Yung Lai
 * Date: 6/13/12
 * Time: 10:10 AM
 * To change this template use File | Settings | File Templates.
 */
public class TokenConverterCloudV11Test {
    private TokenConverterCloudV11 tokenConverterCloudV11;

    @Before
    public void setUp() throws Exception {
        tokenConverterCloudV11 = new TokenConverterCloudV11();
    }

    @Test
    public void toCloudv11TokenJaxb_createsToken_succeeds() throws Exception {
        UserScopeAccess userScopeAccess = new UserScopeAccess();
        userScopeAccess.setAccessTokenString("token");
        userScopeAccess.setAccessTokenExp(new Date(3000, 1, 1));
        Token token = tokenConverterCloudV11.toCloudv11TokenJaxb(userScopeAccess);
        assertThat("token id", token.getId(), equalTo("token"));
    }

    @Test
    public void toCloudv11TokenJaxb_tokenExpireIsNull_returnsToken() throws Exception {
        UserScopeAccess userScopeAccess = new UserScopeAccess();
        userScopeAccess.setAccessTokenString("token");
        Token token = tokenConverterCloudV11.toCloudv11TokenJaxb(userScopeAccess);
        assertThat("token id", token.getId(), equalTo("token"));
    }

    @Test
    public void toCloudv11TokenJaxb_tokenStringIsNull_returnsToken() throws Exception {
        UserScopeAccess userScopeAccess = new UserScopeAccess();
        Token token = tokenConverterCloudV11.toCloudv11TokenJaxb(userScopeAccess);
        assertThat("token id", token.getId(), equalTo(null));
    }
}
