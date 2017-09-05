package com.rackspace.idm.api.resource.cloud.v20;

import lombok.AllArgsConstructor;
import lombok.Getter;

import javax.annotation.Nullable;

/**
 * Provides params used to return a page of results. Pagination is optional. When no values are provided a standard page
 * size should be returned without using any special ordering or views that would reduce query performance.
 */
@Getter
@AllArgsConstructor
public class PaginationParams {
    /**
     * What index in the list of results to return. If null, starts at first result.
     */
    @Nullable
    private Integer marker;

    /**
     * The maximum number of results to return. If null, returns up to 1000 results.
     */
    @Nullable
    private Integer limit;
}
