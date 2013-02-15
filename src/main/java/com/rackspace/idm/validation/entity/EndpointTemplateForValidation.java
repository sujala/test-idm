package com.rackspace.idm.validation.entity;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.Size;

import static com.rackspace.idm.validation.entity.Constants.*;

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
    @Size(max = MAX)
    protected String type;
    @Size(max = MAX)
    protected String name;
    @Size(max = MAX)
    protected String region;
    @Size(max = LONG_MAX)
    protected String publicURL;
    @Size(max = LONG_MAX)
    protected String internalURL;
    @Size(max = LONG_MAX)
    protected String adminURL;
}
