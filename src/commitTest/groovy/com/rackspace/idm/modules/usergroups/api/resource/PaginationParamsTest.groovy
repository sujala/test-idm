package com.rackspace.idm.modules.usergroups.api.resource

import com.rackspace.idm.api.resource.cloud.v20.PaginationParams
import spock.lang.Specification
import spock.lang.Unroll

class PaginationParamsTest extends Specification {

    def "Default constructor populates pagination w/ defaults"() {
        when:
        PaginationParams params = new PaginationParams()

        then:
        params != null
        params.limit == null
        params.marker == null
        params.effectiveMarker == 0
        params.effectiveLimit == 1000
    }

    @Unroll
    def "Effective values calculated correctly: marker: #marker; limit: #limit"() {
        when:
        PaginationParams params = new PaginationParams(marker, limit)

        then:
        params.limit == limit
        params.marker == marker
        params.effectiveMarker == effectiveMarker
        params.effectiveLimit == effectiveLimit

        where:
        marker | limit | effectiveMarker | effectiveLimit
        null   | null  | 0 | 1000
        null   | 30  | 0 | 30
        10     | null  | 10 | 1000
        5      | 5  | 5 | 5
        -100   | 5000  | 0 | 1000
    }
}
