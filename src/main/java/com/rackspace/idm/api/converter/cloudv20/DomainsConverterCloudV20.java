package com.rackspace.idm.api.converter.cloudv20;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.Domain;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.Domains;
import com.rackspace.idm.api.resource.cloud.JAXBObjectFactories;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Created by IntelliJ IDEA.
 * User: jorge
 * Date: 9/25/12
 * Time: 6:53 PM
 * To change this template use File | Settings | File Templates.
 */
@Component
public class DomainsConverterCloudV20 {
    @Autowired
    private JAXBObjectFactories objFactories;
    @Autowired
    private DomainConverterCloudV20 domainConverterCloudv20;

    public Domains toDomains(com.rackspace.idm.domain.entity.Domains domains) {
        Domains jaxbDomains = objFactories.getRackspaceIdentityExtRaxgaV1Factory().createDomains();
        for(com.rackspace.idm.domain.entity.Domain domain : domains.getDomain()){
            Domain jaxbDomain = domainConverterCloudv20.toDomain(domain);
            jaxbDomains.getDomain().add(jaxbDomain);
        }
        return jaxbDomains;
    }

    public com.rackspace.idm.domain.entity.Domains toDomainsDO(Domains domains) {
        com.rackspace.idm.domain.entity.Domains domainsDO = new com.rackspace.idm.domain.entity.Domains();
        for(Domain domain : domains.getDomain()){
            com.rackspace.idm.domain.entity.Domain entityDomain = domainConverterCloudv20.toDomainDO(domain);
            domainsDO.getDomain().add(entityDomain);
        }
        return domainsDO;
    }

    public void setObjFactories(JAXBObjectFactories objFactories) {
        this.objFactories = objFactories;
    }

    public void setDomainConverter(DomainConverterCloudV20 domainConverter) {
        this.domainConverterCloudv20 = domainConverter;
    }
}
