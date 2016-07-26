package com.rackspace.idm.validation.entity;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.Size;

import static com.rackspace.idm.validation.entity.Constants.MAX;
import static com.rackspace.idm.validation.entity.Constants.PASSWORD_MIN;

@Getter
@Setter
public class PasswordResetForValidation {
    @Size(min = PASSWORD_MIN, max = MAX)
    private String password;
}
