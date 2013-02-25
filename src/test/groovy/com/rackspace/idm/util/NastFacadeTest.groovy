package com.rackspace.idm.util

import com.rackspace.idm.exception.ApiException
import org.apache.xmlrpc.XmlRpcException
import org.slf4j.Logger
import spock.lang.Shared
import testHelpers.RootServiceTest

class NastFacadeTest extends RootServiceTest {
    @Shared NastFacade nastFacade
    @Shared NastXmlRpcClientWrapper clientWrapper
    @Shared NastConfiguration configuration
    @Shared Logger logger;

    def setupSpec() {
        nastFacade = new NastFacade()
    }

    def setup() {
        configuration = Mock()
        nastFacade.configuration = configuration
        clientWrapper = Mock()
        nastFacade.clientWrapper = clientWrapper
        logger = Mock()
        nastFacade.logger = logger

    }

    def "adding a nast user should not throw an exception"() {
        given:
        def user = v1Factory.createUser()

        when:
        nastFacade.addNastUser(user)

        then:
        2 * configuration.isNastXmlRpcEnabled() >> true
        clientWrapper.addResellerStorageAccount(_) >> { throw new XmlRpcException() }

        then:
        notThrown(Exception)
    }

    def "adding a nast user should log not being able to connect"() {
        given:
        def user = v1Factory.createUser()

        when:
        nastFacade.addNastUser(user)

        then:
        2 * configuration.isNastXmlRpcEnabled() >> true
        clientWrapper.addResellerStorageAccount(_) >> { throw new XmlRpcException() }

        then:
        1 * logger.error(_, _)
    }
}
