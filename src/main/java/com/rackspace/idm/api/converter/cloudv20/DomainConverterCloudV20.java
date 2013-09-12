package com.rackspace.idm.api.converter.cloudv20;

import com.rackspace.idm.api.resource.cloud.JAXBObjectFactories;
import com.rackspace.idm.domain.entity.Domain;
import com.rackspace.idm.domain.entity.Domains;
import org.dozer.Mapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.bind.JAXBElement;

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
    private Mapper mapper;

    @Autowired
    private JAXBObjectFactories objFactories;

    public com.rackspace.docs.identity.api.ext.rax_auth.v1.Domain toDomain(Domain domain) {
        return mapper.map(domain, com.rackspace.docs.identity.api.ext.rax_auth.v1.Domain.class);
    }

    public Domain fromDomain(com.rackspace.docs.identity.api.ext.rax_auth.v1.Domain domainEntity) {
        Domain domain = mapper.map(domainEntity, Domain.class);
        domain.setEnabled(domainEntity.isEnabled());
        return domain;
    }

    public com.rackspace.docs.identity.api.ext.rax_auth.v1.Domains toDomains(Domains domains) {
        com.rackspace.docs.identity.api.ext.rax_auth.v1.Domains jaxbDomains = objFactories.getRackspaceIdentityExtRaxgaV1Factory().createDomains();
        for(com.rackspace.idm.domain.entity.Domain domain : domains.getDomain()){
            com.rackspace.docs.identity.api.ext.rax_auth.v1.Domain jaxbDomain = toDomain(domain);
            jaxbDomains.getDomain().add(jaxbDomain);
        }
        return jaxbDomains;
    }

    public Domains fromDomains(com.rackspace.docs.identity.api.ext.rax_auth.v1.Domains domains) {
        com.rackspace.idm.domain.entity.Domains domainsEntity = new com.rackspace.idm.domain.entity.Domains();
        for(com.rackspace.docs.identity.api.ext.rax_auth.v1.Domain domain : domains.getDomain()){
            com.rackspace.idm.domain.entity.Domain entityDomain = fromDomain(domain);
            domainsEntity.getDomain().add(entityDomain);
        }
        return domainsEntity;
    }

    public void setObjFactories(JAXBObjectFactories objFactories) {
        this.objFactories = objFactories;
    }
}
