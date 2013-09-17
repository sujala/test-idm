package com.rackspace.idm.api.converter.cloudv20;

import com.rackspace.idm.api.resource.cloud.JAXBObjectFactories;
import org.dozer.Mapper;
import org.openstack.docs.identity.api.v2.Tenant;
import org.openstack.docs.identity.api.v2.Tenants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.util.GregorianCalendar;
import java.util.List;

@Component
public class TenantConverterCloudV20 {
    @Autowired
    private Mapper mapper;

    @Autowired
    private JAXBObjectFactories objFactories;

    public Tenant toTenant(com.rackspace.idm.domain.entity.Tenant tenantEntity) {
        return mapper.map(tenantEntity, Tenant.class);
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
        return tenant;
    }
}
