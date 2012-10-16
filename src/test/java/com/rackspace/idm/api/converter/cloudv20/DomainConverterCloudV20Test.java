package com.rackspace.idm.api.converter.cloudv20;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.Domain;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.Domains;
import com.rackspace.idm.api.resource.cloud.JAXBObjectFactories;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Created by IntelliJ IDEA.
 * User: matt.colton
 * Date: 8/13/12
 * Time: 3:10 PM
 * To change this template use File | Settings | File Templates.
 */
public class DomainConverterCloudV20Test {
    private DomainConverterCloudV20 domainConverterCloudV20;
    private DomainsConverterCloudV20 domainsConverterCloudV20;
    private JAXBObjectFactories jaxbObjectFactories;

    @Before
    public void setUp() throws Exception {
        domainConverterCloudV20 = new DomainConverterCloudV20();
        domainsConverterCloudV20 = new DomainsConverterCloudV20();
        jaxbObjectFactories = new JAXBObjectFactories();
        domainConverterCloudV20.setObjFactories(jaxbObjectFactories);
        domainsConverterCloudV20.setObjFactories(jaxbObjectFactories);
    }

    @Test
    public void toDomain_returnsDomain() throws Exception {
        com.rackspace.idm.domain.entity.Domain domain = new com.rackspace.idm.domain.entity.Domain();
        domain.setDomainId("domainId");
        domain.setName("name");
        domain.setDescription("description");
        domain.setEnabled(true);
        Domain jaxbDomain = domainConverterCloudV20.toDomain(domain);
        assertThat("id", jaxbDomain.getId(), equalTo("domainId"));
        assertThat("name", jaxbDomain.getName(), equalTo("name"));
        assertThat("description", jaxbDomain.getDescription(), equalTo("description"));
        assertThat("enabled", jaxbDomain.isEnabled(), equalTo(true));
    }

    @Test
    public void toDomainDO_returnsDomainDO() throws Exception {
        Domain domain = new Domain();
        domain.setId("domainId");
        domain.setName("name");
        domain.setDescription("description");
        domain.setEnabled(true);
        com.rackspace.idm.domain.entity.Domain domainDO = domainConverterCloudV20.toDomainDO(domain);
        assertThat("name", domainDO.getName(), equalTo("name"));
        assertThat("id", domainDO.getDomainId(), equalTo("domainId"));
        assertThat("description", domainDO.getDescription(), equalTo("description"));
        assertThat("enabled", domainDO.isEnabled(), equalTo(true));
    }

    @Test
    public void toDomainsDO_returnsDomainsDO() throws Exception {
        domainsConverterCloudV20.setDomainConverter(domainConverterCloudV20);
        Domains domains = new Domains();
        Domain domain = new Domain();
        domain.setId("domainId");
        domain.setName("name");
        domain.setDescription("description");
        domain.setEnabled(true);
        domains.getDomain().add(domain);
        com.rackspace.idm.domain.entity.Domains domainsDO = domainsConverterCloudV20.toDomainsDO(domains);
        assertThat("size", domainsDO.getDomain().size(), equalTo(1));
    }
}
