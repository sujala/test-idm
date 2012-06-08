package com.rackspace.idm.api.resource.cloud.v11;

import com.rackspace.idm.exception.BadRequestException;
import com.rackspacecloud.docs.auth.api.v1.*;
import org.junit.Before;
import org.junit.Test;

import javax.xml.bind.JAXBElement;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

/**
 * Created with IntelliJ IDEA.
 * User: kurt
 * Date: 6/7/12
 * Time: 5:14 PM
 * To change this template use File | Settings | File Templates.
 */
public class CredentialUnmarshallerTest {

    CredentialUnmarshaller credentialUnmarshaller;
    private String userCredentialsJSON = "{" +
            "   \"credentials\" : {" +
            "       \"key\" : \"apiKey\"," +
            "       \"username\" : \"username\"" +
            "   }" +
            "}";

    private String mossoCredentialsJSON = "{" +
            "   \"mossoCredentials\" : {" +
            "       \"key\" : \"apiKey\"," +
            "       \"mossoId\" : 123456" +
            "   }" +
            "}";

    private String nastCredentialsJSON = "{" +
            "   \"nastCredentials\" : {" +
            "       \"key\" : \"apiKey\"," +
            "       \"nastId\" : \"nastId\"" +
            "   }" +
            "}";

    private String passwordCredentialsJSON = "{" +
            "   \"passwordCredentials\" : {" +
            "       \"username\" : \"username\"" +
            "       \"password\" : \"password\"" +
            "   }" +
            "}";

    @Before
    public void setUp() throws Exception {
        credentialUnmarshaller = new CredentialUnmarshaller();
    }

    @Test(expected = BadRequestException.class)
    public void unmarshallCredentialsFromJSON_withEmptyJSON_throwsBadRequestException() throws Exception {
        JAXBElement<? extends Credentials> credentials = credentialUnmarshaller.unmarshallCredentialsFromJSON("{ }");
    }

    @Test(expected = BadRequestException.class)
    public void unmarshallCredentialsFromJSON_withInvalidJSON_throwsBadRequestException() throws Exception {
        credentialUnmarshaller.unmarshallCredentialsFromJSON("Invalid JSON");
    }

    @Test(expected = BadRequestException.class)
    public void unmarshallCredentialsFromJSON_withNonCredentialJSON_throwsBadRequestException() throws Exception {
        JAXBElement<? extends Credentials> credentials = credentialUnmarshaller.unmarshallCredentialsFromJSON("{ " +
                "   \"notCredentials\" : \"someValue\"" +
                "}");
    }

    @Test
    public void unmarshallCredentialsFromJSON_withUserCredentials_returnsUserCredentials() throws Exception {
        JAXBElement<? extends Credentials> credentials = credentialUnmarshaller.unmarshallCredentialsFromJSON(userCredentialsJSON);
        assertThat("credentials class", credentials.getValue() , is(UserCredentials.class));
    }

    @Test
    public void unmarshallCredentialsFromJSON_withUserCredentials_setsUsername() throws Exception {
        JAXBElement<? extends Credentials> credentials = credentialUnmarshaller.unmarshallCredentialsFromJSON(userCredentialsJSON);
        UserCredentials userCredentials = (UserCredentials) credentials.getValue();
        assertThat("credentials username", userCredentials.getUsername(), equalTo("username"));
    }

    @Test
    public void unmarshallCredentialsFromJSON_withUserCredentials_setsKey() throws Exception {
        JAXBElement<? extends Credentials> credentials = credentialUnmarshaller.unmarshallCredentialsFromJSON(userCredentialsJSON);
        UserCredentials userCredentials = (UserCredentials) credentials.getValue();
        assertThat("credentials key", userCredentials.getKey(), equalTo("apiKey"));
    }

    @Test
    public void unmarshallCredentialsFromJSON_withMossoCredentials_returnsMossoCredentials() throws Exception {
        JAXBElement<? extends Credentials> credentials = credentialUnmarshaller.unmarshallCredentialsFromJSON(mossoCredentialsJSON);
        assertThat("credentials class", credentials.getValue(), is(MossoCredentials.class));
    }

    @Test
    public void unmarshallCredentialsFromJSON_withMossoCredentials_setsMossoId() throws Exception {
        JAXBElement<? extends Credentials> credentials = credentialUnmarshaller.unmarshallCredentialsFromJSON(mossoCredentialsJSON);
        MossoCredentials mossoCredentials = (MossoCredentials) credentials.getValue();
        assertThat("credentials mosso id", mossoCredentials.getMossoId(), equalTo(123456));
    }

    @Test
    public void unmarshallCredentialsFromJSON_withMossoCredentials_withNoMossId_setsNullMossoId() throws Exception {
        JAXBElement<? extends Credentials> credentials = credentialUnmarshaller.unmarshallCredentialsFromJSON("{" +
                "   \"mossoCredentials\" : {" +
                "       \"key\" : \"apiKey\"," +
                "   }" +
                "}");
        MossoCredentials mossoCredentials = (MossoCredentials) credentials.getValue();
        assertThat("credentials mosso id", mossoCredentials.getMossoId(), equalTo(0));
    }

    @Test
    public void unmarshallCredentialsFromJSON_withMossoCredentials_setsKey() throws Exception {
        JAXBElement<? extends Credentials> credentials = credentialUnmarshaller.unmarshallCredentialsFromJSON(mossoCredentialsJSON);
        MossoCredentials mossoCredentials = (MossoCredentials) credentials.getValue();
        assertThat("credentials mosso id", mossoCredentials.getKey(), equalTo("apiKey"));
    }

    @Test
    public void unmarshallCredentialsFromJSON_withNastCredentials_returnsNastCredentials() throws Exception {
        JAXBElement<? extends Credentials> credentials = credentialUnmarshaller.unmarshallCredentialsFromJSON(nastCredentialsJSON);
        assertThat("credentials class", credentials.getValue(), is(NastCredentials.class));
    }

    @Test
    public void unmarshallCredentialsFromJSON_withNastCredentials_setsNastId() throws Exception {
        JAXBElement<? extends Credentials> credentials = credentialUnmarshaller.unmarshallCredentialsFromJSON(nastCredentialsJSON);
        NastCredentials nastCredentials = (NastCredentials) credentials.getValue();
        assertThat("credentials nastId", nastCredentials.getNastId(), equalTo("nastId"));
    }

    @Test
    public void unmarshallCredentialsFromJSON_withNastCredentials_setsKey() throws Exception {
        JAXBElement<? extends Credentials> credentials = credentialUnmarshaller.unmarshallCredentialsFromJSON(nastCredentialsJSON);
        NastCredentials nastCredentials = (NastCredentials) credentials.getValue();
        assertThat("credentials key", nastCredentials.getKey(), equalTo("apiKey"));
    }

    @Test
    public void unmarshallCredentialsFromJSON_withPasswordCredentials_returnsPasswordCredentials() throws Exception {
        JAXBElement<? extends Credentials> credentials = credentialUnmarshaller.unmarshallCredentialsFromJSON(passwordCredentialsJSON);
        assertThat("credentials class", credentials.getValue(), is(PasswordCredentials.class));
    }

    @Test
    public void unmarshallCredentialsFromJSON_withPasswordCredentials_setsUsername() throws Exception {
        JAXBElement<? extends Credentials> credentials = credentialUnmarshaller.unmarshallCredentialsFromJSON(passwordCredentialsJSON);
        PasswordCredentials passwordCredentials = (PasswordCredentials) credentials.getValue();
        assertThat("credentials", passwordCredentials.getUsername(), equalTo("username"));
    }

    @Test
    public void unmarshallCredentialsFromJSON_withPasswordCredentials_setsPassword() throws Exception {
        JAXBElement<? extends Credentials> credentials = credentialUnmarshaller.unmarshallCredentialsFromJSON(passwordCredentialsJSON);
        PasswordCredentials passwordCredentials = (PasswordCredentials) credentials.getValue();
        assertThat("credentials", passwordCredentials.getPassword(), equalTo("password"));
    }

}
