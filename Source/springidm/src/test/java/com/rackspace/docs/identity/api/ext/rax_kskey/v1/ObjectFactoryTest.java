package com.rackspace.docs.identity.api.ext.rax_kskey.v1;

import org.junit.Before;
import org.junit.Test;

import javax.xml.bind.JAXBElement;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * Created with IntelliJ IDEA.
 * User: Yung Lai
 * Date: 7/9/12
 * Time: 4:09 PM
 * To change this template use File | Settings | File Templates.
 */
public class ObjectFactoryTest {
    ObjectFactory objectFactory;

    @Before
    public void setUp() throws Exception {
        objectFactory = new ObjectFactory();
    }

    @Test
    public void createApiKeyCredentials_returnsNewCreatedObject() throws Exception {
        ApiKeyCredentials result = objectFactory.createApiKeyCredentials();
        assertThat("api key", result.apiKey, equalTo(null));
    }

    @Test
    public void createApiKeyCredentialsWithOnlyApiKey_returnsNewCreatedObject() throws Exception {
        ApiKeyCredentialsWithOnlyApiKey result = objectFactory.createApiKeyCredentialsWithOnlyApiKey();
        assertThat("api key", result.apiKey, equalTo(null));
    }

    @Test
    public void createApiKeyCredentials_returnsNewJAXBElementObject() throws Exception {
        JAXBElement<ApiKeyCredentials> result = objectFactory.createApiKeyCredentials(new ApiKeyCredentials());
        assertThat("api key", result.getValue().apiKey, equalTo(null));
    }
}
