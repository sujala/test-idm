package com.rackspace.idm.api.converter.cloudv20;

import com.rackspace.idm.api.resource.cloud.JAXBObjectFactories;
import com.rackspace.idm.domain.entity.SecretQA;
import org.dozer.Mapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.bind.JAXBElement;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: jorge
 * Date: 11/13/12
 * Time: 4:09 PM
 * To change this template use File | Settings | File Templates.
 */
@Component
public class SecretQAConverterCloudV20 {
    @Autowired
    Mapper mapper;

    @Autowired
    private JAXBObjectFactories objFactories;

    public JAXBElement<com.rackspace.docs.identity.api.ext.rax_auth.v1.SecretQA> toSecretQA(SecretQA secretQA) {
        com.rackspace.docs.identity.api.ext.rax_auth.v1.SecretQA secretQAEntity  = mapper.map(
                secretQA, com.rackspace.docs.identity.api.ext.rax_auth.v1.SecretQA.class
        );

        return objFactories.getRackspaceIdentityExtRaxgaV1Factory().createSecretqa(secretQAEntity);
    }

    public SecretQA fromSecretQA(com.rackspace.docs.identity.api.ext.rax_auth.v1.SecretQA secretQAEntity) {
        return mapper.map(secretQAEntity, SecretQA.class);
    }

    public JAXBElement<com.rackspace.docs.identity.api.ext.rax_auth.v1.SecretQAs> toSecretQAs(List<SecretQA> secretQAs) {
        com.rackspace.docs.identity.api.ext.rax_auth.v1.SecretQAs secretQAsEntity = objFactories.getRackspaceIdentityExtRaxgaV1Factory().createSecretQAs();

        for(SecretQA secretQA : secretQAs) {
            secretQAsEntity.getSecretqa().add(toSecretQA(secretQA).getValue());
        }

        return objFactories.getRackspaceIdentityExtRaxgaV1Factory().createSecretqas(secretQAsEntity);
    }
}
