package com.rackspace.idm.domain.config;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class IdmPropertyList {

    public String configPath;
    public List<IdmProperty> properties;

}
