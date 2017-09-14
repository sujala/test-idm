package com.rackspace.idm.api.resource.pagination;

import com.rackspace.idm.api.resource.IdmPathUtils;
import com.rackspace.idm.domain.entity.PaginatorContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.core.UriInfo;
import java.net.URI;

/**
 * @deprecated Use IdmPathUtils directly.
 */
@Deprecated
@Component
public class DefaultPaginator<T> implements Paginator<T> {

    @Autowired
    private IdmPathUtils idmPathUtils;

    @Override
    public String createLinkHeader(UriInfo uriInfo, PaginatorContext<T> context) {
        return idmPathUtils.createLinkHeader(uriInfo, context);
    }
}
