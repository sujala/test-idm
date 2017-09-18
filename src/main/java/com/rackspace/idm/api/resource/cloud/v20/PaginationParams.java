package com.rackspace.idm.api.resource.cloud.v20;

import lombok.Getter;
import lombok.Setter;

import javax.annotation.Nullable;

/**
 * Provides params used to return a page of results. Pagination is optional. When no values are provided a standard page
 * size should be returned and pagination only used if result size exceeds the standard page size.
 */
@Getter
@Setter
public class PaginationParams {
    /**
     * What index in the list of results to return. If null, starts at first result.
     */
    @Nullable
    private Integer marker;

    /**
     * The maximum number of results to return.
     */
    @Nullable
    private Integer limit;

    public PaginationParams(Integer marker, Integer limit) {
        this.marker = marker;
        this.limit = limit;
    }

    public PaginationParams() {}

    /**
     * Return the marker value to use. Returns the set marker, or, if null, 0.
     *
     * @return
     */
    public int getEffectiveMarker() {
        return marker != null ? marker : 0;
    }

    /**
     * Return the limit value to use. Returns the set limit, or, if null, 1000.
     *
     * @return
     */
    public int getEffectiveLimit() {
        return limit != null ? limit : 1000;
    }


}
