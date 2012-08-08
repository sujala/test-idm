package com.rackspace.idm.api.converter.cloudv20;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.Domain;
import com.rackspace.idm.api.resource.cloud.JAXBObjectFactories;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Created by IntelliJ IDEA.
 * User: matt.colton
 * Date: 8/7/12
 * Time: 7:51 AM
 * To change this template use File | Settings | File Templates.
 */
@Component
public class DomainConverterCloudV20 {
    @Autowired
    private JAXBObjectFactories objFactories;
    private Logger logger = LoggerFactory.getLogger(TenantConverterCloudV20.class);

    public Domain toDomain(com.rackspace.idm.domain.entity.Domain domain) {
        Domain jaxbDomain = objFactories.getRackspaceIdentityExtRaxgaV1Factory().createDomain();
        jaxbDomain.setId(domain.getDomainId());
        jaxbDomain.setName(domain.getName());
        jaxbDomain.setDescription(domain.getDescription());
        jaxbDomain.setEnabled(domain.isEnabled());
        return jaxbDomain;
    }

    public com.rackspace.idm.domain.entity.Domain toDomainDO(Domain domain) {
        com.rackspace.idm.domain.entity.Domain domainDO = new com.rackspace.idm.domain.entity.Domain();
        domainDO.setDomainId(domain.getId());
        domainDO.setName(domain.getName());
        domainDO.setDescription(domain.getDescription());
        domainDO.setEnabled(domain.isEnabled());
        return domainDO;
    }

    public void setObjFactories(JAXBObjectFactories objFactories) {
        this.objFactories = objFactories;
    }
}
