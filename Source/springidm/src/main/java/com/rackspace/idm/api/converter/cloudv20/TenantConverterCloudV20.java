package com.rackspace.idm.api.converter.cloudv20;

import java.util.GregorianCalendar;
import java.util.List;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.openstack.docs.identity.api.v2.Tenant;
import org.openstack.docs.identity.api.v2.Tenants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.rackspace.idm.api.resource.cloud.JAXBObjectFactories;

@Component
public class TenantConverterCloudV20 {

    @Autowired
    private JAXBObjectFactories OBJ_FACTORIES;
    private Logger logger = LoggerFactory.getLogger(TenantConverterCloudV20.class);

    public Tenant toTenant(com.rackspace.idm.domain.entity.Tenant tenant) {
        Tenant jaxbTenant = OBJ_FACTORIES.getOpenStackIdentityV2Factory()
            .createTenant();
        jaxbTenant.setDescription(tenant.getDescription());
        jaxbTenant.setDisplayName(tenant.getDisplayName());
        jaxbTenant.setEnabled(tenant.isEnabled());
        jaxbTenant.setId(tenant.getTenantId());
        jaxbTenant.setName(tenant.getName());

        if (tenant.getCreated() != null) {
            GregorianCalendar gc = new GregorianCalendar();
            gc.setTime(tenant.getCreated());

            XMLGregorianCalendar createdDate = null;
            try {
                createdDate = DatatypeFactory.newInstance()
                    .newXMLGregorianCalendar(gc);
                jaxbTenant.setCreated(createdDate);
            } catch (DatatypeConfigurationException e) {
                logger.info("failed to create XMLGregorianCalendar: " + e.getMessage());
            }
        }
        
        if (tenant.getUpdated() != null) {
            GregorianCalendar gc = new GregorianCalendar();
            gc.setTime(tenant.getUpdated());

            XMLGregorianCalendar updatedDate = null;
            try {
                updatedDate = DatatypeFactory.newInstance()
                    .newXMLGregorianCalendar(gc);
                jaxbTenant.setUpdated(updatedDate);
            } catch (DatatypeConfigurationException e) {
                logger.info("failed to create XMLGregorianCalendar: " + e.getMessage());
            }
        }
        return jaxbTenant;
    }
    
    public Tenants toTenantList(List<com.rackspace.idm.domain.entity.Tenant> tenants) {
        
        Tenants jaxbTenants = OBJ_FACTORIES.getOpenStackIdentityV2Factory().createTenants();
        
        for (com.rackspace.idm.domain.entity.Tenant tenant : tenants) {
            jaxbTenants.getTenant().add(toTenant(tenant));
        }
        
        return jaxbTenants;
    }

    public com.rackspace.idm.domain.entity.Tenant toTenantDO(
        org.openstack.docs.identity.api.v2.Tenant jaxbTenant) {
        
        com.rackspace.idm.domain.entity.Tenant tenant = new com.rackspace.idm.domain.entity.Tenant();
        
        tenant.setDescription(jaxbTenant.getDescription());
        tenant.setDisplayName(jaxbTenant.getDisplayName());
        tenant.setEnabled(jaxbTenant.isEnabled());
        tenant.setName(jaxbTenant.getName());
        tenant.setTenantId(jaxbTenant.getId());
        
        return tenant;
    }

    public void setObjFactories(JAXBObjectFactories OBJ_FACTORIES) {
        this.OBJ_FACTORIES = OBJ_FACTORIES;
    }
}
