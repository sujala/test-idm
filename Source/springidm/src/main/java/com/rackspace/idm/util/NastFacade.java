package com.rackspace.idm.util;

/**
 * Created by IntelliJ IDEA.
 * User: Hector
 * Date: 10/18/11
 * Time: 5:44 PM
 */

import com.rackspace.idm.exception.ApiException;
import com.rackspacecloud.docs.auth.api.v1.User;
import org.apache.commons.lang.StringUtils;
import org.apache.xmlrpc.XmlRpcException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.MalformedURLException;

@Component
public class NastFacade {
    @Autowired
    private NastConfiguration authConfiguration;

    @Autowired
    private NastXmlRpcClientWrapper nastXMLRpcClientWrapper;

    public Boolean removeNastUser(String nastAccountId) {
        Boolean response = Boolean.FALSE;

        if (authConfiguration.isNastXmlRpcEnabled()) {
            try {
                response = nastXMLRpcClientWrapper.removeResellerStorageAccount(nastAccountId);
            } catch (Exception e) {
                throw new ApiException(500, e.getMessage(), "could not remove reseller");
            }
        }

        return response;
    }

    public String addNastUser(User user) {
        try {
            if (!authConfiguration.isNastXmlRpcEnabled()) {
                user.setNastId(null);
                return null;
            }

            String nastAccountId;
            nastAccountId = addNastUser();
            user.setNastId(nastAccountId);

            return nastAccountId;
        } catch (Exception e) {
            throw new ApiException(500, e.getMessage(), "could not add nast user");
        }
    }

    /**
     * Call 2-arguments NAST add_account() method
     *
     * @param resellerName reseller name
     * @param resellerId   reseller account ID
     * @return NAST account ID
     * @throws MalformedURLException MalformedURLException
     * @throws XmlRpcException       XmlRpcException
     */
    String addNastUser(String resellerName, String resellerId) throws MalformedURLException, XmlRpcException {
        return addNastUser(new String[]{resellerName, resellerId});
    }

    /**
     * Call single-argument NAST add_account() method with default reseller name
     *
     * @return NAST account ID
     * @throws MalformedURLException MalformedURLException
     * @throws XmlRpcException       XmlRpcException
     */
    String addNastUser() throws MalformedURLException, XmlRpcException {
        if (!authConfiguration.isNastXmlRpcEnabled()) {
            return null;
        }

        return addNastUser(new String[]{authConfiguration.getNastResellerName()});
    }

    String addNastUser(String[] parameters) throws MalformedURLException, XmlRpcException {
        String response = nastXMLRpcClientWrapper.addResellerStorageAccount(parameters);

        return !StringUtils.isEmpty(response) ? response : null;
    }

    static boolean hasResellerInfo(String resellerName, String resellerId) {
        return !(StringUtils.isBlank(resellerName) || StringUtils.isBlank(resellerId));
    }

    public void setAuthConfiguration(NastConfiguration authConfiguration) {
        this.authConfiguration = authConfiguration;
    }

    public void setNastXMLRpcClientWrapper(NastXmlRpcClientWrapper nastXMLRpcClientWrapper) {
        this.nastXMLRpcClientWrapper = nastXMLRpcClientWrapper;
    }
}

