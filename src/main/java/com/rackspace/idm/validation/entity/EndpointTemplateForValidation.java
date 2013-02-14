package com.rackspace.idm.validation.entity;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.Size;

/**
 * Created with IntelliJ IDEA.
 * User: jorge
 * Date: 2/13/13
 * Time: 4:05 PM
 * To change this template use File | Settings | File Templates.
 */
@Data
public class EndpointTemplateForValidation {
    @Valid
    protected VersionForServiceForValidation version;
    @Size(max = 100)
    protected String type;
    @Size(max = 100)
    protected String name;
    @Size(max = 100)
    protected String region;
    @Size(max = 1000)
    protected String publicURL;
    @Size(max = 1000)
    protected String internalURL;
    @Size(max = 1000)
    protected String adminURL;
}
