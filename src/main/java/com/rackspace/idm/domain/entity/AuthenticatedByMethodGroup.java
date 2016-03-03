package com.rackspace.idm.domain.entity;

import com.rackspace.idm.exception.UnrecognizedAuthenticationMethodException;
import lombok.EqualsAndHashCode;
import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@EqualsAndHashCode
public final class AuthenticatedByMethodGroup {
    /**
     * Includes ALL authenticatedBy methods <b>except</b> impersonation, which are a special class and must be specified
     * separately
     */
    public static final AuthenticatedByMethodGroup ALL = new AuthenticatedByMethodGroup();
    public static final AuthenticatedByMethodGroup NULL = getGroup(AuthenticatedByMethodEnum.NULL);
    public static final AuthenticatedByMethodGroup APIKEY = getGroup(AuthenticatedByMethodEnum.APIKEY);
    public static final AuthenticatedByMethodGroup PASSWORD = getGroup(AuthenticatedByMethodEnum.PASSWORD);
    public static final AuthenticatedByMethodGroup IMPERSONATION = getGroup(AuthenticatedByMethodEnum.IMPERSONATION);
    public static final AuthenticatedByMethodGroup FEDERATION = getGroup(AuthenticatedByMethodEnum.FEDERATION);
    public static final AuthenticatedByMethodGroup PASSCODE = getGroup(AuthenticatedByMethodEnum.PASSCODE);
    public static final AuthenticatedByMethodGroup RSAKEY = getGroup(AuthenticatedByMethodEnum.RSAKEY);
    public static final AuthenticatedByMethodGroup PASSWORD_PASSCODE = getGroup(AuthenticatedByMethodEnum.PASSWORD, AuthenticatedByMethodEnum.PASSCODE);
    public static final AuthenticatedByMethodGroup PASSWORD_OTPPASSCODE = getGroup(AuthenticatedByMethodEnum.PASSWORD, AuthenticatedByMethodEnum.OTPPASSCODE);
    public static final AuthenticatedByMethodGroup EMAIL = getGroup(AuthenticatedByMethodEnum.EMAIL);

    private List<AuthenticatedByMethodEnum> authenticatedByMethods = new ArrayList<AuthenticatedByMethodEnum>();
    private boolean allAuthenticatedByMethods = true;

    private AuthenticatedByMethodGroup() {}

    public static final AuthenticatedByMethodGroup getGroup(AuthenticatedByMethodEnum... methods) {
        AuthenticatedByMethodGroup group = new AuthenticatedByMethodGroup();
        group.allAuthenticatedByMethods = false;
        for (AuthenticatedByMethodEnum method : methods) {
            group.authenticatedByMethods.add(method);
        }
        return group;
    }

    public static final AuthenticatedByMethodGroup getGroup(List<String> methods) {
        if (CollectionUtils.isEmpty(methods)) {
            return NULL;
        }

        AuthenticatedByMethodGroup group = new AuthenticatedByMethodGroup();
        group.allAuthenticatedByMethods = false;
        for (String method : methods) {
            AuthenticatedByMethodEnum convertedMethod = AuthenticatedByMethodEnum.fromValue(method);
            if (convertedMethod == null) {
                //if can't translate any method, throw error
                throw new UnrecognizedAuthenticationMethodException("TokenAuthMethod", String.format("Unrecognized authentication method '%s'", method));
            }
            group.authenticatedByMethods.add(convertedMethod);
        }
        return group;
    }

    public static final AuthenticatedByMethodGroup getAllGroup() {
        return ALL;
    }

    public boolean matches(AuthenticatedByMethodGroup that) {
        return this.equals(that)
                || CollectionUtils.isEqualCollection(this.authenticatedByMethods, that.authenticatedByMethods)
                || (this.allAuthenticatedByMethods == true && that.allAuthenticatedByMethods == true);
    }

    public List<AuthenticatedByMethodEnum> getAuthenticatedByMethods() {
        return Collections.unmodifiableList(authenticatedByMethods);
    }

    public List<String> getAuthenticatedByMethodsAsValues() {
        List<String> vals = new ArrayList<String>(authenticatedByMethods.size());
        for (AuthenticatedByMethodEnum authenticatedByMethod : authenticatedByMethods) {
            vals.add(authenticatedByMethod.value);
        }
        return vals;
    }


    public boolean isAllAuthenticatedByMethods() {
        return allAuthenticatedByMethods;
    }

    @Override
    public String toString() {
        return "AuthenticatedByMethodGroup{" +
                "authenticatedByMethods=" + authenticatedByMethods +
                ", allAuthenticatedByMethods=" + allAuthenticatedByMethods +
                '}';
    }
}
