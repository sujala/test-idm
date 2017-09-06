package com.rackspace.idm.api.resource;

import org.springframework.stereotype.Component;

import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.net.URI;

/**
 * A utility component to generate paths in the IDM environment. Make this a component from the getgo as anticipate
 * use of IdentityConfig to fix the known issue with creating paths w/ cloud in them.
 */
@Component
public class IdmPathUtils {

    /**
     * Creates the generic location header based on the requested location by tacking on id.
     *
     * TODO: Fix this so handles '/cloud' appropriately for staging/production environments where the VIPs are
     * set up to not require 'cloud', but NGINX adds in.
     *
     * @param uriInfo
     * @param id
     * @return
     */
    public URI createLocationHeaderValue(UriInfo uriInfo, String id) {
        UriBuilder requestUriBuilder = uriInfo.getRequestUriBuilder();
        return requestUriBuilder.path(id).build();
    }
}
