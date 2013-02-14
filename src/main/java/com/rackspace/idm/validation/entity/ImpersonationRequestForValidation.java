package com.rackspace.idm.validation.entity;

import lombok.Data;

import javax.validation.Valid;

/**
 * Created with IntelliJ IDEA.
 * User: jorge
 * Date: 2/13/13
 * Time: 6:03 PM
 * To change this template use File | Settings | File Templates.
 */
@Data
public class ImpersonationRequestForValidation {
    @Valid
    protected UserForValidation user;
}
