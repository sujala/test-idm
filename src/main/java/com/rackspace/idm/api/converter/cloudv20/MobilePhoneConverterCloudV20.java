package com.rackspace.idm.api.converter.cloudv20;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.MobilePhone;
import com.rackspace.idm.api.resource.cloud.JAXBObjectFactories;
import org.apache.commons.configuration.Configuration;
import org.dozer.Mapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Converts between the MobilePhone "LDAP Entity" and the MobilePhone "REST request/response" entity
 */
@Component
public class MobilePhoneConverterCloudV20 {
    @Autowired
    private Mapper mapper;

    @Autowired
    private JAXBObjectFactories objFactories;

    @Autowired
    private Configuration config;

    /**
     * Converts from the request/response web service representation of a mobile phone to the LDAP based representation.
     *
     * @param mobilePhoneWeb
     * @return
     */
    public com.rackspace.idm.domain.entity.MobilePhone fromMobilePhoneWeb(MobilePhone mobilePhoneWeb) {
        com.rackspace.idm.domain.entity.MobilePhone phone = mapper.map(mobilePhoneWeb, com.rackspace.idm.domain.entity.MobilePhone.class);
        return phone;
    }

    /**
     * Converts from the LDAP representation of a mobile phone to the request/response web service based representation.
     *
     * @param mobilePhoneEntity
     * @return
     */
    public MobilePhone toMobilePhoneWeb(com.rackspace.idm.domain.entity.MobilePhone mobilePhoneEntity) {
        MobilePhone phone = mapper.map(mobilePhoneEntity, MobilePhone.class);
        return phone;
    }
}
