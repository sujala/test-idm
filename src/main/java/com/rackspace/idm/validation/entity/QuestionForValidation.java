package com.rackspace.idm.validation.entity;

import lombok.Data;

import javax.validation.constraints.Size;

import static com.rackspace.idm.validation.entity.Constants.LONG_MAX;
import static com.rackspace.idm.validation.entity.Constants.MAX;

/**
 * Created with IntelliJ IDEA.
 * User: jorge
 * Date: 2/13/13
 * Time: 3:31 PM
 * To change this template use File | Settings | File Templates.
 */
@Data
public class QuestionForValidation {
    @Size(max = MAX)
    protected String id;
    @Size(max = LONG_MAX)
    protected String question;
}
