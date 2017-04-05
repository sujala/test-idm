package com.rackspace.idm.api.converter.cloudv20;

import com.rackspace.idm.api.resource.cloud.JAXBObjectFactories;
import com.rackspace.idm.domain.config.IdentityConfig;
import com.rackspace.idm.domain.entity.Domain;
import com.rackspace.idm.domain.entity.Domains;
import org.dozer.Mapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;

@Component
public class DomainConverterCloudV20 {
    @Autowired
    private Mapper mapper;

    @Autowired
    private IdentityConfig identityConfig;

    @Autowired
    private JAXBObjectFactories objFactories;

    public com.rackspace.docs.identity.api.ext.rax_auth.v1.Domain toDomain(Domain domain) {
        com.rackspace.docs.identity.api.ext.rax_auth.v1.Domain jaxbDomain = mapper.map(domain, com.rackspace.docs.identity.api.ext.rax_auth.v1.Domain.class);

        Duration duration;
        if (jaxbDomain.getSessionInactivityTimeout() == null){
            try {
                DatatypeFactory factory = DatatypeFactory.newInstance();
                duration = factory.newDuration(identityConfig.getReloadableConfig().getDomainDefaultSessionInactivityTimeout().toString());
            } catch (DatatypeConfigurationException e) {
                throw new IllegalStateException("Unable to set default session inactivity timeout for domain.");
            }
            jaxbDomain.setSessionInactivityTimeout(duration);
        }

        return jaxbDomain;
    }

    public Domain fromDomain(com.rackspace.docs.identity.api.ext.rax_auth.v1.Domain domainEntity) {
        Domain domain = mapper.map(domainEntity, Domain.class);
        domain.setEnabled(domainEntity.isEnabled());

        if (domain.getSessionInactivityTimeout() == null) {
            domain.setSessionInactivityTimeout(identityConfig.getReloadableConfig().getDomainDefaultSessionInactivityTimeout().toString());
        }

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
