package com.rackspace.idm.api.converter.cloudv20;

import com.rackspace.idm.domain.entity.IdentityProperty;
import org.dozer.Mapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class IdentityPropertyConverter {

    @Autowired
    private Mapper mapper;

    public IdentityProperty toIdentityProperty(com.rackspace.docs.identity.api.ext.rax_auth.v1.IdentityProperty idmProp) {
        IdentityProperty prop = mapper.map(idmProp, IdentityProperty.class);

        return prop;
    }

    public com.rackspace.docs.identity.api.ext.rax_auth.v1.IdentityProperty fromIdentityProperty(IdentityProperty identityProperty) {
        com.rackspace.docs.identity.api.ext.rax_auth.v1.IdentityProperty prop =
                mapper.map(identityProperty, com.rackspace.docs.identity.api.ext.rax_auth.v1.IdentityProperty.class);

        return prop;
    }

}
