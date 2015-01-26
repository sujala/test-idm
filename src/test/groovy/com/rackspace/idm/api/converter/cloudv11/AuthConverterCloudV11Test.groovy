package com.rackspace.idm.api.converter.cloudv11

import com.rackspace.idm.domain.entity.CloudBaseUrl
import com.rackspace.idm.domain.entity.OpenstackEndpoint
import com.rackspace.idm.domain.entity.User
import com.rackspace.idm.domain.entity.UserScopeAccess
import com.rackspacecloud.docs.auth.api.v1.FullToken
import org.joda.time.DateTime
import spock.lang.Shared
import testHelpers.RootServiceTest

import javax.xml.datatype.DatatypeFactory

/**
 * Created with IntelliJ IDEA.
 * User: jorge
 * Date: 7/3/13
 * Time: 5:50 PM
 * To change this template use File | Settings | File Templates.
 */
class AuthConverterCloudV11Test extends RootServiceTest{

    @Shared
    AuthConverterCloudV11 converter

    def setupSpec(){
        converter = new AuthConverterCloudV11()
    }

    def setup(){
        mockConfiguration(converter)
    }

    def "Verify that converter displays correct created date for scopeAccess"(){
        given:
        def date = new Date()
        def user = new User()
        def sa = new UserScopeAccess().with {
            it.accessTokenString = "token"
            it.accessTokenExp = new Date().plus(1)
            it.username = "username"
            it.createTimestamp = date
            return it
        }

        when:
        FullToken result = converter.toCloudV11TokenJaxb(sa, "requestUrl", user)

        then:
        result.created != null
        result.created == DatatypeFactory.newInstance()
                .newXMLGregorianCalendar(new DateTime(date).toGregorianCalendar());
    }

}
