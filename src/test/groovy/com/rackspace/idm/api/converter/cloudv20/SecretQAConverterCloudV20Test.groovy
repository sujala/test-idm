package com.rackspace.idm.api.converter.cloudv20;


import spock.lang.Specification
import spock.lang.Shared
import com.rackspace.idm.api.resource.cloud.JAXBObjectFactories
import org.dozer.DozerBeanMapper
import com.rackspace.idm.domain.entity.SecretQA

/**
 * Created by IntelliJ IDEA.
 * User: jorge
 * Date: 11/14/12
 * Time: 1:45 PM
 * To change this template use File | Settings | File Templates.
 */
class SecretQAConverterCloudV20Test extends Specification {
    @Shared SecretQAConverterCloudV20 converterCloudV20

    def setupSpec() {
        converterCloudV20 = new SecretQAConverterCloudV20().with {
            it.objFactories = new JAXBObjectFactories()
            it.mapper = new DozerBeanMapper()
            return it
        }
    }

    def cleanupSpec() {
    }

    def "convert secretQA from ldap to jersey object"() {
        given:
        SecretQA secretQA = secretQA("id", "question", "answer")

        when:
        com.rackspace.docs.identity.api.ext.rax_auth.v1.SecretQA secretQAEntity = converterCloudV20.toSecretQA(secretQA).value

        then:
        secretQA.question == secretQAEntity.question
        secretQA.id == secretQAEntity.id
        secretQA.answer == secretQAEntity.answer
    }

    def "convert secretQA from jersey object to ldap"() {
        given:
        com.rackspace.docs.identity.api.ext.rax_auth.v1.SecretQA secretQAEntity = secretQAEntity("id", "question","anwser")

        when:
        SecretQA secretQA = converterCloudV20.fromSecretQA(secretQAEntity)

        then:
        secretQAEntity.id == secretQA.id
        secretQAEntity.question == secretQA.question
        secretQAEntity.answer == secretQA.answer
    }

    def "convert secretQAs from ldap to jersey object" () {
        given:
        SecretQA secretQA = secretQA("id", "question", "answer")
        List<SecretQA> secretQAs = new ArrayList<SecretQA>();
        secretQAs.add(secretQA)

        when:
        com.rackspace.docs.identity.api.ext.rax_auth.v1.SecretQAs secretQAsEntity = converterCloudV20.toSecretQAs(secretQAs).value

        then:
        secretQAs.size() == secretQAsEntity.secretqa.size();
        com.rackspace.docs.identity.api.ext.rax_auth.v1.SecretQA secretQAEntity = secretQAsEntity.secretqa.get(0)
        secretQA.id == secretQAEntity.id
        secretQA.question == secretQAEntity.question
    }

    def secretQA(String id, String question, String answer) {
        new SecretQA().with {
            it.id = id
            it.question = question
            it.answer = answer
            return it
        }
    }

    def secretQAEntity(String id, String question, String answer) {
        new com.rackspace.docs.identity.api.ext.rax_auth.v1.SecretQA().with {
            it.id = id
            it.question = question
            it.answer = answer
            return it
        }
    }
}
