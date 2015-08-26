package com.rackspace.idm.domain.migration

import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.dao.UniqueId
import com.rackspace.idm.domain.migration.event.MigrationChangeApplicationEvent
import com.rackspace.idm.domain.migration.event.MigrationChangeEventListener
import com.rackspace.idm.domain.migration.ldap.event.LdapMigrationChangeApplicationEvent
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Unroll
import testHelpers.RootIntegrationTest

/**
 * Only extending root in order to get the IdentityConfig
 */
class MigrationChangeEventListenerTest extends RootIntegrationTest {
    String listenerName = "testListener"

    MyListener listener

    @Autowired
    IdentityConfig identityConfig;

    MigrationChangeEventListener mockListener

    def setup() {
        reloadableConfiguration.reset()

        mockListener = Mock();
        mockListener.getListenerName() >> listenerName

        listener = MyListener.createListener(identityConfig, mockListener)
    }


    def "When default prop configured to enable all, ignore nothing all events are handled"() {
        given:
        def recordEntity = createEntity()
        reloadableConfiguration.setProperty(IdentityConfig.MIGRATION_LISTENER_DEFAULT_HANDLES_CHANGE_EVENTS_PROP, true)
        reloadableConfiguration.setProperty(IdentityConfig.MIGRATION_LISTENER_DEFAULT_IGNORES_CHANGE_EVENTS_OF_TYPE_PROP, "")

        when: "add"
        listener.onApplicationEvent(createEvent(ChangeType.ADD, recordEntity))

        then:
        1 * mockListener.handleEvent(_)

        when: "modify"
        listener.onApplicationEvent(createEvent(ChangeType.MODIFY, recordEntity))

        then:
        1 * mockListener.handleEvent(_)

        when: "delete"
        listener.onApplicationEvent(createEvent(ChangeType.DELETE, recordEntity))

        then:
        1 * mockListener.handleEvent(_)
    }

    def "When default prop configured to disable all w/ no specific overrides, nothing recorded"() {
        given:
        def recordEntity = createEntity()
        reloadableConfiguration.setProperty(IdentityConfig.MIGRATION_LISTENER_DEFAULT_HANDLES_CHANGE_EVENTS_PROP, false)
        reloadableConfiguration.setProperty(IdentityConfig.MIGRATION_LISTENER_DEFAULT_IGNORES_CHANGE_EVENTS_OF_TYPE_PROP, "")

        when: "add"
        listener.onApplicationEvent(createEvent(ChangeType.ADD, recordEntity))

        then:
        0 * mockListener.handleEvent(_)

        when: "modify"
        listener.onApplicationEvent(createEvent(ChangeType.MODIFY, recordEntity))

        then:
        0 * mockListener.handleEvent(_)

        when: "delete"
        listener.onApplicationEvent(createEvent(ChangeType.DELETE, recordEntity))

        then:
        0 * mockListener.handleEvent(_)
    }

    /**
     * Change Types that are processed can be set at the default level and on the specific listener. When set on the
     * listener, the defaults are overridden (it's not a union)
     * @return
     */
    @Unroll
    def "Appropriate event processing when using ignore types: #ignoreTypes, listenerSpecific: #listenerSpecific"() {
        given:
        def recordEntity = createEntity()
        reloadableConfiguration.setProperty(IdentityConfig.MIGRATION_LISTENER_DEFAULT_HANDLES_CHANGE_EVENTS_PROP, true)
        if (listenerSpecific) {
            //if testing listener specific settings, set the default to ignore everything to verify it's overridden
            reloadableConfiguration.setProperty(IdentityConfig.MIGRATION_LISTENER_DEFAULT_IGNORES_CHANGE_EVENTS_OF_TYPE_PROP, "ADD, DELETE, MODIFY") //ignore all default
            reloadableConfiguration.setProperty(String.format(IdentityConfig.MIGRATION_LISTENER_IGNORES_CHANGE_EVENTS_OF_TYPE_PROP_REG, listenerName), ignoreTypes)
        } else {
            reloadableConfiguration.setProperty(IdentityConfig.MIGRATION_LISTENER_DEFAULT_IGNORES_CHANGE_EVENTS_OF_TYPE_PROP, ignoreTypes)
        }

        when: "add"
        listener.onApplicationEvent(createEvent(ChangeType.ADD, recordEntity))

        then:
        interaction {
            if (ignoreTypes.contains(ChangeType.ADD.name())) {
                0 * mockListener.handleEvent(_)
            } else {
                1 * mockListener.handleEvent(_)
            }
        }

        when: "modify"
        listener.onApplicationEvent(createEvent(ChangeType.MODIFY, recordEntity))

        then:
        interaction {
            if (ignoreTypes.contains(ChangeType.MODIFY.name())) {
                0 * mockListener.handleEvent(_)
            } else {
                1 * mockListener.handleEvent(_)
            }
        }

        when: "delete"
        listener.onApplicationEvent(createEvent(ChangeType.DELETE, recordEntity))

        then:
        interaction {
            if (ignoreTypes.contains(ChangeType.DELETE.name())) {
                0 * mockListener.handleEvent(_)
            } else {
                1 * mockListener.handleEvent(_)
            }
        }

        where:
        ignoreTypes             | listenerSpecific
        "ADD"                   | false
        "ADD, DELETE"           | false
        "ADD, DELETE, MODIFY"   | false
        ""                      | false
        "MODIFY"                | false
        "MODIFY, ADD"           | false
        "DELETE"                | false
        "ADD"                   | true
        "ADD, DELETE"           | true
        "ADD, DELETE, MODIFY"   | true
        ""                      | true
        "MODIFY"                | true
        "MODIFY, ADD"           | true
        "DELETE"                | true
    }

    def MigrationChangeApplicationEvent createEvent(ChangeType type, UniqueId entity) {
        return new LdapMigrationChangeApplicationEvent(this, type, entity.getUniqueId(), null)
    }

    def UniqueId createEntity() {
        return new UniqueId() {
            @Override
            String getUniqueId() {
                return String.format("rsId=%s;dn=blah", UUID.randomUUID().toString())
            }

            @Override
            void setUniqueId(String uniqueId) {

            }
        }
    }

    public static class MyListener extends MigrationChangeEventListener<MigrationChangeApplicationEvent> {
        MigrationChangeEventListener inner

        static MyListener createListener(IdentityConfig identityConfig, MigrationChangeEventListener mockListener) {
            MyListener listener = new MyListener()
            listener.identityConfig = identityConfig
            listener.inner = mockListener
            return listener
        }

        @Override
        String getListenerName() {
            return inner.getListenerName()
        }

        @Override
        protected void handleEvent(MigrationChangeApplicationEvent event) {
            inner.handleEvent(event)
        }
    }
}
