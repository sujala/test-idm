package com.rackspace.idm.api.resource.cloud.v11;

import com.rackspace.idm.api.resource.cloud.CloudClient;
import com.rackspace.idm.api.serviceprofile.CloudContractDescriptionBuilder;
import org.apache.commons.configuration.Configuration;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.mock;

/**
 * Created with IntelliJ IDEA.
 * User: kurt
 * Date: 6/26/12
 * Time: 12:44 PM
 * To change this template use File | Settings | File Templates.
 */
public class Cloud11VersionResourceTest {

    Configuration config;
    CloudClient cloudClient;
    CloudContractDescriptionBuilder cloudContractDescriptionBuilder;
    Cloud11VersionResource cloudV11VersionResource;

    @Before
    public void setUp() throws Exception {
        config = mock(Configuration.class);
        cloudClient = mock(CloudClient.class);
        cloudContractDescriptionBuilder = mock(CloudContractDescriptionBuilder.class);
        cloudV11VersionResource = new Cloud11VersionResource(config, cloudClient, cloudContractDescriptionBuilder);
    }


}
