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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletResponse;
import java.net.MalformedURLException;

@Component
public class NastFacade {
    @Autowired
    private NastConfiguration configuration;

    @Autowired
    private NastXmlRpcClientWrapper clientWrapper;

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    public Boolean removeNastUser(String nastAccountId) {
        Boolean response = Boolean.FALSE;

        if (configuration.isNastXmlRpcEnabled()) {
            try {
                response = clientWrapper.removeResellerStorageAccount(nastAccountId);
            } catch (Exception e) {
                throw new ApiException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage(), "could not remove reseller", e);
            }
        }

        return response;
    }

    public String addNastUser(User user) {
        String nastAccountId = null;

        if (configuration.isNastXmlRpcEnabled()) {
            try {
                nastAccountId = addNastUser();
            } catch (Exception e) {
                logger.error("could not add nast user: %s", e.getMessage());
            }
        }

        user.setNastId(nastAccountId);

        return nastAccountId;
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
        if (!configuration.isNastXmlRpcEnabled()) {
            return null;
        }

        return addNastUser(new String[]{configuration.getNastResellerName()});
    }

    String addNastUser(String[] parameters) throws MalformedURLException, XmlRpcException {
        String response = clientWrapper.addResellerStorageAccount(parameters);

        return !StringUtils.isEmpty(response) ? response : null;
    }

    static boolean hasResellerInfo(String resellerName, String resellerId) {
        return !(StringUtils.isBlank(resellerName) || StringUtils.isBlank(resellerId));
    }

    public void setAuthConfiguration(NastConfiguration configuration) {
        this.configuration = configuration;
    }

    public void setNastXMLRpcClientWrapper(NastXmlRpcClientWrapper clientWrapper) {
        this.clientWrapper = clientWrapper;
    }
}

