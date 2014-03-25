package com.rackspace.idm.domain.dao.impl

import com.rackspace.idm.domain.entity.Application
import com.rackspace.idm.domain.entity.ClientSecret
import com.rackspace.idm.domain.entity.User
import com.rackspace.idm.domain.entity.UserScopeAccess
import com.rackspace.idm.exception.DuplicateException
import com.rackspace.test.SingleTestConfiguration
import com.unboundid.ldap.sdk.Attribute
import com.unboundid.ldap.sdk.DeleteRequest
import com.unboundid.ldap.sdk.Filter
import com.unboundid.ldap.sdk.LDAPInterface
import com.unboundid.ldap.sdk.controls.SubtreeDeleteRequestControl
import com.unboundid.ldap.sdk.persist.LDAPPersister
import org.apache.commons.lang.NotImplementedException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.test.context.ContextConfiguration
import spock.lang.Shared
import spock.lang.Specification
import testHelpers.Cloud20Utils
import testHelpers.ConcurrentStageTaskRunner
import testHelpers.MultiStageTask
import testHelpers.MultiStageTaskFactory

import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when

/**
 * This test is used to test the update functions of the LdapGenericRepository/LdapRepository and the integration with the unboundid sdk. It uses the Application object as the
 * persisted object for tests. Tests attempt to clean up after themselves.
 *
 */
@ContextConfiguration(locations = ["classpath:app-config.xml", "classpath:com/rackspace/idm/domain/dao/impl/LdapGenericRepositoryUpdateIntegrationTest-context.xml"])
class LdapGenericRepositoryUpdateIntegrationTest extends Specification {
    /**
     * Wire in the generic repo
     */
    @Autowired
    LdapGenericRepository<Application> genericApplicationRepository

    /**
     * Wire in the overridden generic repo
     */
    @Autowired
    LdapGenericRepository<Application> overriddenGenericApplicationRepository

    @Autowired
    LdapConnectionPools ldapConnectionPools

    def ConcurrentStageTaskRunner concurrentStageTaskRunner = new ConcurrentStageTaskRunner()

    LDAPInterface con
    LDAPPersister<Application> applicationPersister

    def setup() {
        con = ldapConnectionPools.getAppConnPoolInterface()
        applicationPersister = LDAPPersister.getInstance(Application.class)
    }

    /* ###############################################################################
                        updateObjectAsIs(Object) tests
    ############################################################################### */

    /**
     * This test verifies that the only changes persisted to CA are those that were made to this object based on the state
     * of the object when it was initially loaded.
     */
    def "updateObjectAsIs - verify only modified properties are updated even if changed in CA"() {
        setup:
        final Application app = createClient()
        String originalName = app.getName()
        String newName = UUID.randomUUID().toString()
        Boolean originalEnabled = app.getEnabled()
        final Boolean behindTheScenesEnableChange = !originalEnabled
        overriddenGenericApplicationRepository.addObject(app)

        //get a fresh object as persisted in CA and modify it
        Application localApp = overriddenGenericApplicationRepository.getObject(searchFilterGetApplicationByClientId(app.clientId))
        assert localApp.getEnabled() == originalEnabled //verify enabled is still original value

        /*
        now change the enabled property behind the scenes. While probably not required, do this in a completely separate
         thread to completely isolate the update from this thread. Overkill to use the multistagetaskfactor, but it exists
         so...
         */
        def List<ModifyAppEnabledTask> runs = concurrentStageTaskRunner.runConcurrent(1, new MultiStageTaskFactory<ModifyAppEnabledTask>() {
            @Override
            ModifyAppEnabledTask createTask() {
                return new ModifyAppEnabledTask(localApp.clientId, behindTheScenesEnableChange)
            }
        })

        when:
        //now change the name on the previously loaded object, but NOT the enabled flag.
        localApp.setName(newName)
        overriddenGenericApplicationRepository.updateObjectAsIs(localApp)
        Application reloadedApp = overriddenGenericApplicationRepository.getObject(searchFilterGetApplicationByClientId(localApp.clientId))

        then:
        originalEnabled != behindTheScenesEnableChange //just make sure.
        localApp.getEnabled() == originalEnabled //this shouldn't be changed on this entity because we never reloaded it from CA

        //the reloaded entry should contain both the enabled change and the name change
        reloadedApp.getEnabled() == behindTheScenesEnableChange
        reloadedApp.getName() == newName

        cleanup:
        if (localApp != null) overriddenGenericApplicationRepository.deleteObject(localApp)
    }

    /* ###############################################################################
                        updateObject(Object) tests
    ############################################################################### */

    /**
     * This test verifies that the only changes persisted to CA are those that were made to this object based on the state
     * of the object when it was initially loaded.
     */
    def "updateObject - verify only modified properties are updated even if changed in CA"() {
        setup:
        final Application app = createClient()
        String originalName = app.getName()
        String newName = UUID.randomUUID().toString()
        Boolean originalEnabled = app.getEnabled()
        final Boolean behindTheScenesEnableChange = !originalEnabled
        overriddenGenericApplicationRepository.addObject(app)

        //get a fresh object as persisted in CA and modify it
        Application localApp = new Application()
        localApp.setName(newName)
        localApp.setClientId(app.getClientId())
        localApp.setLdapEntry(overriddenGenericApplicationRepository.getObject(searchFilterGetApplicationByClientId(app.clientId)).getLdapEntry())

        /*
        now change the enabled property behind the scenes. While probably not required, do this in a completely separate
         thread to completely isolate the update from this thread. Overkill to use the multistagetaskfactor, but it exists
         so...
         */
        def List<ModifyAppEnabledTask> runs = concurrentStageTaskRunner.runConcurrent(1, new MultiStageTaskFactory<ModifyAppEnabledTask>() {
            @Override
            ModifyAppEnabledTask createTask() {
                return new ModifyAppEnabledTask(localApp.clientId, behindTheScenesEnableChange)
            }
        })

        when:
        overriddenGenericApplicationRepository.updateObject(localApp)
        Application reloadedApp = overriddenGenericApplicationRepository.getObject(searchFilterGetApplicationByClientId(localApp.clientId))

        then:
        originalEnabled != behindTheScenesEnableChange //just make sure.
        localApp.getEnabled() == null //this should be null because we created an entity from scratch

        //the reloaded entry should contain both the enabled change and the name change
        reloadedApp.getEnabled() == behindTheScenesEnableChange
        reloadedApp.getName() == newName

        cleanup:
        if (localApp != null) overriddenGenericApplicationRepository.deleteObject(localApp)
    }

    /* ###############################################################################
                        HELPER METHODS
    ############################################################################### */

    def createClient() {
        def id = Cloud20Utils.createRandomString()
        Application app = new Application("client$id", ClientSecret.newInstance("secret"), "name", "clientRCN$id")
        app.setEnabled(true)
        return app;
    }

    /**
     * This config is used to wire the dependencies into a dummy generic repository for testing. The context file LdapEndpointRepositoryTest-context.xml
     * loads this file.
     */
    @SingleTestConfiguration
    public static class Config {

        /**
         * Raw bean. Nothing overridden.
         * @return
         */
        @Bean
        LdapGenericRepository<Application> genericApplicationRepository() {
            return new LdapGenericRepository<Application>() {
            }
        }

        /**
         * Override the base methods that throw NotImplementedExceptions
         * @return
         */
        @Bean
        LdapGenericRepository<Application> overriddenGenericApplicationRepository() {
            return new LdapGenericRepository<Application>() {
                @Override
                public String getBaseDn() {
                    return APPLICATIONS_BASE_DN;
                }
            }
        }
    }

    private class ModifyAppEnabledTask implements MultiStageTask {
        String clientId;
        boolean newEnabled = false;

        ModifyAppEnabledTask(String clientId, boolean newEnabled) {
            this.clientId = clientId;
            this.newEnabled = newEnabled
        }

        @Override
        int getNumberConcurrentStages() {
            return 1
        }

        @Override
        void setup() {
        }

        @Override
        void runStage(int i) {
            switch (i) {
                case 1:
                    updateEnabled()
                    break;
                default:
                    throw new IllegalStateException("This task does not support stage '" + i + "'")
            }
        }

        void updateEnabled() {
                Application app = overriddenGenericApplicationRepository.getObject(searchFilterGetApplicationByClientId(clientId))
                app.setEnabled(newEnabled)
                overriddenGenericApplicationRepository.updateObject(app)
        }

        Throwable getExceptionEncountered() {
            return exceptionEncountered
        }

        void cleanup() {
        }


    }

    private Filter searchFilterGetApplicationByClientId(String clientId) {
        return new LdapRepository.LdapSearchBuilder()
                .addEqualAttribute(LdapRepository.ATTR_CLIENT_ID, clientId)
                .addEqualAttribute(LdapRepository.ATTR_OBJECT_CLASS, LdapRepository.OBJECTCLASS_RACKSPACEAPPLICATION)
                .build();
    }

}
