package com.rackspace.idm.validation.entity;

import lombok.Getter;
import lombok.Setter;

import javax.validation.Valid;
import javax.validation.constraints.Size;
import java.util.List;

import static com.rackspace.idm.validation.entity.Constants.MAX;

/**
 * Created with IntelliJ IDEA.
 * User: jorge
 * Date: 2/13/13
 * Time: 5:23 PM
 * To change this template use File | Settings | File Templates.
 */
@Getter
@Setter
public class PoliciesForValidation {
    @Valid
    protected List<PolicyForValidation> policy;

    @Size(max = MAX)
    protected String algorithm;
}
