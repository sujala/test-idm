package com.rackspace.idm.jython

import com.rackspace.idm.domain.dao.ScopeAccessDao
import com.rackspace.idm.domain.dao.UserDao
import testHelpers.RootIntegrationTest

import static com.rackspace.idm.api.resource.cloud.AbstractAroundClassJerseyTest.startOrRestartGrizzly
import static com.rackspace.idm.api.resource.cloud.AbstractAroundClassJerseyTest.stopGrizzly

class ObjectsTest extends RootIntegrationTest {

    @Override
    public void doSetupSpec(){
        this.resource = startOrRestartGrizzly("classpath:app-config.xml")
    }

    @Override
    public void doCleanupSpec() {
        stopGrizzly();
    }

    def "Test if you can retrieve beans by name or class"() {
        when:
        def obj = Objects.getBean(UserDao.class)

        then:
        obj != null
        obj instanceof UserDao

        when:
        obj = Objects.getBeanByName("scopeAccessDao")

        then:
        obj != null
        obj instanceof ScopeAccessDao
    }

}
