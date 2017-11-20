package com.rackspace.idm.event;

import com.rackspace.idm.domain.service.IdentityUserTypeEnum;
import lombok.Getter;
import org.apache.http.annotation.ThreadSafe;

/**
 * An immutable object representing the caller of an API service. This must be Immutable and threadsafe as it could
 * be read asynchronously as well as concurrently by various event listeners.
 */
@Getter
@ThreadSafe
public class Caller {

    private String id;
    private String username;
    private IdentityUserTypeEnum userType;

    public Caller(String id, String username, IdentityUserTypeEnum userType) {
        this.username = username;
        this.id = id;
        this.userType = userType;
    }
}
