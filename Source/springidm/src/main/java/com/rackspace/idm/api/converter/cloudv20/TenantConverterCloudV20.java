package com.rackspace.idm.api.converter.cloudv20;

import java.util.GregorianCalendar;
import java.util.List;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.openstack.docs.identity.api.v2.Tenant;
import org.openstack.docs.identity.api.v2.Tenants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.rackspace.idm.api.resource.cloud.JAXBObjectFactories;

@Component
public class TenantConverterCloudV20 {

    @Autowired
    private JAXBObjectFactories OBJ_FACTORIES;

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
                e.printStackTrace();
            }
        }
        
        if (tenant.getUpdated() != null) {
            GregorianCalendar gc = new GregorianCalendar();
            gc.setTime(tenant.getUpdated());

            XMLGregorianCalendar updatedDate = null;
            try {
                updatedDate = DatatypeFactory.newInstance()
                    .newXMLGregorianCalendar(gc);
                jaxbTenant.setCreated(updatedDate);
            } catch (DatatypeConfigurationException e) {
                e.printStackTrace();
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
}
