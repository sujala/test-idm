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
     * Return the marker value to use. If the marker is null or < 0, will return 0; else returns the marker value
     *
     * @return
     */
    public int getEffectiveMarker() {
        return marker != null && marker >= 0 ? marker : 0;
    }

    /**
     * Return the limit value to use. If the limit is null or > 1000, will return 1000; else returns the limit value
     *
     * @return
     */
    public int getEffectiveLimit() {
        return limit != null && limit <= 1000 ? limit : 1000;
    }


}
