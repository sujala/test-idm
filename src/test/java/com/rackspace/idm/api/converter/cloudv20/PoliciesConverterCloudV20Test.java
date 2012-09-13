package com.rackspace.idm.api.converter.cloudv20;

import com.rackspace.idm.api.resource.cloud.JAXBObjectFactories;
import com.rackspace.idm.domain.entity.Policies;
import org.junit.Before;
import org.junit.Test;

/**
 * Created by IntelliJ IDEA.
 * User: jorge
 * Date: 9/13/12
 * Time: 4:27 PM
 * To change this template use File | Settings | File Templates.
 */
public class PoliciesConverterCloudV20Test{

    PoliciesConverterCloudV20 policiesConverterCloudV20;
    JAXBObjectFactories objectFactories;

    @Before
    public void setUp() throws Exception {
        policiesConverterCloudV20 = new PoliciesConverterCloudV20();
        objectFactories = new JAXBObjectFactories();
        policiesConverterCloudV20.setObjFactories(objectFactories);
    }

    @Test
    public void testToPolicies() throws Exception {


    }

    @Test
    public void testToPoliciesDO() throws Exception {

    }


}
