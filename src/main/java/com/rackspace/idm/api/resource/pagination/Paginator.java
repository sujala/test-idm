package com.rackspace.idm.api.resource.pagination;

import com.rackspace.idm.domain.entity.PaginatorContext;

import javax.ws.rs.core.UriInfo;

/**
 * Created with IntelliJ IDEA.
 * User: jacob
 * Date: 10/17/12
 * Time: 9:40 AM
 * To change this template use File | Settings | File Templates.
 */

public interface Paginator<T> {
    String createLinkHeader(UriInfo uriInfo, PaginatorContext<T> context);
}
