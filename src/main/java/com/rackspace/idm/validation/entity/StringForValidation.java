package com.rackspace.idm.validation.entity;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.Size;

import static com.rackspace.idm.validation.entity.Constants.MAX;

/**
 * Created with IntelliJ IDEA.
 * User: jorge
 * Date: 2/13/13
 * Time: 9:23 AM
 * To change this template use File | Settings | File Templates.
 */
@Getter
@Setter
public class StringForValidation {

    StringForValidation() {
    }

    StringForValidation(String value) {
        this.value = value;
    }

    @Size(max = MAX)
    private String value;
}
