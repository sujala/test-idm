package com.rackspace.idm.validation.entity;

import lombok.Getter;
import lombok.Setter;

import javax.validation.Valid;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: jorge
 * Date: 2/13/13
 * Time: 5:51 PM
 * To change this template use File | Settings | File Templates.
 */
@Getter
@Setter
public class BaseUrlRefListForValidation {
    @Valid
    private List<BaseUrlRefForValidation> baseURLRef;
}
