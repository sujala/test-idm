package com.rackspace.idm.api.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class RequestContextHolder {

    @Autowired
    private RequestContext requestContext;

    public void setImpersonated(boolean flag) {
        requestContext.setImpersonated(flag);
    }

    /**
     * This method returns 'true' when the request was made using an impersonated token.
     *
     * @return
     */
    public boolean isImpersonated() {
        return requestContext.isImpersonated();
    }

}
