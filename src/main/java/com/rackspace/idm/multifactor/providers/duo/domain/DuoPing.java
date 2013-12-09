package com.rackspace.idm.multifactor.providers.duo.domain;

import lombok.Getter;
import lombok.Setter;

/**
 * Response object for <a href="https://www.duosecurity.com/docs/authapi#ping">ping operation</a>
 */
@Getter
@Setter
public class DuoPing {
    private Integer time;
}
