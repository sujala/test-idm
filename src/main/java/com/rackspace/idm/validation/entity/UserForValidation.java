package com.rackspace.idm.validation.entity;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.Size;

import static com.rackspace.idm.validation.entity.Constants.*;

/**
 * Created with IntelliJ IDEA.
 * User: jorge
 * Date: 2/13/13
 * Time: 5:51 PM
 * To change this template use File | Settings | File Templates.
 */
@Data
public class UserForValidation {
    @Size(max = MAX)
    protected String id;
    @Size(max = MAX)
    protected String username;
    @Size(max = MAX)
    protected String email;
    @Size(max = MAX)
    protected String displayName;
    @Size(max = MAX)
    protected String password;
    @Size(max = MAX)
    protected String nastId;
    @Size(max = MAX)
    protected String key;
    @Valid
    protected BaseUrlRefListForValidation baseURLRefs;

}
