package com.rackspace.idm.validation.entity;

import lombok.Data;

import javax.validation.constraints.Size;

/**
 * Created with IntelliJ IDEA.
 * User: jorge
 * Date: 2/13/13
 * Time: 3:14 PM
 * To change this template use File | Settings | File Templates.
 */
@Data
public class RoleForValidation {
    @Size(max = 100)
    protected String id;
    @Size(max = 100)
    protected String name;
    @Size(max = 100)
    protected String description;
    @Size(max = 100)
    protected String serviceId;
    @Size(max = 100)
    protected String tenantId;
}
