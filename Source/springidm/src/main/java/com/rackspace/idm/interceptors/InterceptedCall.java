package com.rackspace.idm.interceptors;

import java.lang.reflect.Method;

import com.rackspace.idm.controllers.CustomerController;
import com.rackspace.idm.controllers.TokenController;
import com.rackspace.idm.controllers.UsersController;
import com.rackspace.idm.controllers.VersionController;

class InterceptedCall {
    static final InterceptedCall GET_APP_VERSIONS = new InterceptedCall(
        VersionController.class, "getVersionInfo");
    static final InterceptedCall SET_USER_PASSWORD = new InterceptedCall(
        UsersController.class, "setUserPassword");
    static final InterceptedCall GET_ACCESS_TOKEN = new InterceptedCall(
        TokenController.class, "getAccessToken");
    static final InterceptedCall SET_LOCK_STATUS = new InterceptedCall(
        CustomerController.class, "setCustomerLockStatus");
    static final InterceptedCall ADD_FIRST_USER = new InterceptedCall(
        CustomerController.class, "addFirstUser");

    private Class<? extends Object> controllerClass;
    private Method interceptedMethod;

    /**
     * Don't use the constructor outside this class, except in unit tests.
     * 
     * @param controllerClass
     * @param interceptedMethodStr
     * @throws IllegalArgumentException
     *             if the method does not exist in the class
     */
    InterceptedCall(Class<? extends Object> controllerClass,
        String interceptedMethodStr) throws IllegalArgumentException {
        this.controllerClass = controllerClass;
        for (Method aMethod : controllerClass.getMethods()) {
            if (aMethod.getName().equals(interceptedMethodStr)) {
                this.interceptedMethod = aMethod;
                break;
            }
        }

        if (interceptedMethod == null) {
            throw new IllegalArgumentException(controllerClass + " "
                + interceptedMethodStr + " does not exist!");
        }
    }

    boolean matches(Class<? extends Object> controllerClass,
        Method controllerMethod) {
        if (this.controllerClass.equals(controllerClass)
            && this.interceptedMethod.equals(controllerMethod)) {
            return true;
        }

        return false;
    }

    Class<? extends Object> getControllerClass() {
        return controllerClass;
    }

    Method getInterceptedMethod() {
        return interceptedMethod;
    }
}
