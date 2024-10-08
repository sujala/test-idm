package com.rackspace.idm.validation.entity;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.Size;

import static com.rackspace.idm.validation.entity.Constants.MAX;

/**
 * Created with IntelliJ IDEA.
 * User: jorge
 * Date: 2/13/13
 * Time: 1:52 PM
 * To change this template use File | Settings | File Templates.
 */
@Getter
@Setter
public class RegionForValidation {
    @Size(max = MAX)
    private String name;
}
