package com.rackspace.idm.api.converter;

import com.rackspace.api.idm.v1.ObjectFactory;
import com.rackspace.api.idm.v1.Tenant;
import com.rackspace.api.idm.v1.Tenants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.util.GregorianCalendar;
import java.util.List;

@Component
public class TenantConverter {


    private final ObjectFactory objectFactory = new ObjectFactory();
    private Logger logger = LoggerFactory.getLogger(TenantConverter.class);

//    @Autowired
//    private JAXBObjectFactories OBJ_FACTORIES;

    /*
     com.rackspace.api.idm.v1.Tenant
     com.rackspace.idm.domain.entity.Tenant
     */

    public Tenant toTenant(com.rackspace.idm.domain.entity.Tenant tenant) {
        Tenant jaxbTenant = objectFactory.createTenant();
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
        
        Tenants jaxbTenants = objectFactory.createTenants();
        
        for (com.rackspace.idm.domain.entity.Tenant tenant : tenants) {
            jaxbTenants.getTenant().add(toTenant(tenant));
        }
        
        return jaxbTenants;
    }

    public com.rackspace.idm.domain.entity.Tenant toTenantDO(
        Tenant jaxbTenant) {
        
        com.rackspace.idm.domain.entity.Tenant tenant = new com.rackspace.idm.domain.entity.Tenant();
        
        tenant.setDescription(jaxbTenant.getDescription());
        tenant.setDisplayName(jaxbTenant.getDisplayName());
        tenant.setEnabled(jaxbTenant.isEnabled());
        tenant.setName(jaxbTenant.getName());
        tenant.setTenantId(jaxbTenant.getId());
        
        return tenant;
    }
}
