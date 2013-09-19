package com.rackspace.idm.validation.entity;

import lombok.Data;

import javax.validation.constraints.Size;

import static com.rackspace.idm.validation.entity.Constants.MAX;

/**
 * Created with IntelliJ IDEA.
 * User: jorge
 * Date: 4/2/13
 * Time: 1:39 PM
 * To change this template use File | Settings | File Templates.
 */
@Data
public class BaseUrlForValidation {
    @Size(max = MAX)
    private String serviceName;
    @Size(max = MAX)
    private String region;
    @Size(max = MAX)
    private String publicURL;
    @Size(max = MAX)
    private String internalURL;
    @Size(max = MAX)
    private String adminURL;
}
