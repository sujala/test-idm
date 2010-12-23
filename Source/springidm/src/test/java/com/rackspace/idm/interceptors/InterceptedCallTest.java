package com.rackspace.idm.interceptors;

import org.junit.Test;

/**
 * @author john.eo Checks to make sure that the controller methods exist.
 */
public class InterceptedCallTest {

    @Test
    public void controllerMethodsShouldExist() throws IllegalArgumentException {
        // This should load the class and thus all the static variables.
        InterceptedCall loadedObject = InterceptedCall.GET_ACCESS_TOKEN;
    }
}
