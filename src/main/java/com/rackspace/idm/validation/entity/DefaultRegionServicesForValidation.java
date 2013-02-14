package com.rackspace.idm.validation.entity;

import lombok.Data;

import javax.validation.Valid;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: jorge
 * Date: 2/13/13
 * Time: 5:41 PM
 * To change this template use File | Settings | File Templates.
 */
@Data
public class DefaultRegionServicesForValidation {
    @Valid
    protected List<StringForValidation> serviceName;
}
