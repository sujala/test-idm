package com.rackspace.idm.api.converter.cloudv20;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.TenantType;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.TenantTypes;
import com.rackspace.idm.api.resource.cloud.JAXBObjectFactories;
import org.dozer.Mapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TenantTypeConverterCloudV20 {

    @Autowired
    private Mapper mapper;

    @Autowired
    private JAXBObjectFactories objFactories;

    public com.rackspace.idm.domain.entity.TenantType fromTenantType(TenantType tenantType) {
        com.rackspace.idm.domain.entity.TenantType tenantTypeEntity = mapper.map(tenantType, com.rackspace.idm.domain.entity.TenantType.class);
        return tenantTypeEntity;
    }

    public TenantType toTenantType(com.rackspace.idm.domain.entity.TenantType tenantTypeEntity) {
        TenantType tenant = mapper.map(tenantTypeEntity, TenantType.class);
        return tenant;
    }

    public TenantTypes toTenantType(List<com.rackspace.idm.domain.entity.TenantType> tenantTypeList) {
        TenantTypes tenantTypes = objFactories.getRackspaceIdentityExtRaxgaV1Factory().createTenantTypes();
        for (com.rackspace.idm.domain.entity.TenantType tenantTypeEntity : tenantTypeList) {
            tenantTypes.getTenantType().add(toTenantType(tenantTypeEntity));
        }
        return tenantTypes;
    }
}
