package com.rackspace.idm.validation.entity;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.Size;

import static com.rackspace.idm.validation.entity.Constants.MAX;

/**
 * Created with IntelliJ IDEA.
 * User: jorge
 * Date: 4/2/13
 * Time: 1:39 PM
 * To change this template use File | Settings | File Templates.
 */
@Getter
@Setter
public class BaseUrlForValidation {
    @Size(max = MAX)
    protected String serviceName;
    @Size(max = MAX)
    protected String region;
    @Size(max = MAX)
    protected String publicURL;
    @Size(max = MAX)
    protected String internalURL;
    @Size(max = MAX)
    protected String adminURL;
}
