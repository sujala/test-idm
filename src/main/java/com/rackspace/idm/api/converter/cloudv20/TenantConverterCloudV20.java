package com.rackspace.idm.api.converter.cloudv20;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.Types;
import com.rackspace.idm.api.resource.cloud.JAXBObjectFactories;
import org.dozer.Mapper;
import org.openstack.docs.identity.api.v2.Tenant;
import org.openstack.docs.identity.api.v2.Tenants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.bind.JAXBElement;
import java.util.List;

@Component
public class TenantConverterCloudV20 {
    @Autowired
    private Mapper mapper;

    @Autowired
    private JAXBObjectFactories objFactories;

    public Tenant toTenant(com.rackspace.idm.domain.entity.Tenant tenantEntity) {
        Tenant tenant = mapper.map(tenantEntity, Tenant.class);
        // Setting display-name to null in order to remove it from the returned
        // XML and json.
        tenant.setDisplayName(null);
        Types types = new Types();
        for (String type : tenantEntity.getTypes()) {
            types.getType().add(type);
        }
        if (types.getType().size() > 0) {
            tenant.setTypes(types);
        } else {
            tenant.setTypes(null);
        }
        return tenant;
    }
    
    public Tenants toTenantList(List<com.rackspace.idm.domain.entity.Tenant> tenants) {
        
        Tenants jaxbTenants = objFactories.getOpenStackIdentityV2Factory().createTenants();
        
        for (com.rackspace.idm.domain.entity.Tenant tenant : tenants) {
            jaxbTenants.getTenant().add(toTenant(tenant));
        }
        
        return jaxbTenants;
    }

    public com.rackspace.idm.domain.entity.Tenant fromTenant(
            org.openstack.docs.identity.api.v2.Tenant jaxbTenant) {
        com.rackspace.idm.domain.entity.Tenant tenant = mapper.map(jaxbTenant, com.rackspace.idm.domain.entity.Tenant.class);
        tenant.setEnabled(jaxbTenant.isEnabled());

        if (jaxbTenant.getTypes() != null) {
            for (String type : jaxbTenant.getTypes().getType()) {
                tenant.getTypes().add(type);
            }
        }

        return tenant;
    }
}
