package com.rackspace.idm.validation.entity;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.Size;

import static com.rackspace.idm.validation.entity.Constants.*;

@Getter
@Setter
public class DomainForValidation {
    @Size(max = MAX)
    private String id;

    @Size(max = MAX)
    private String name;

    @Size(max = LONG_MAX)
    private String description;
}
