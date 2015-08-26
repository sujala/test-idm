package com.rackspace.idm.domain.migration.event;

import com.rackspace.idm.domain.config.IdentityConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;

public abstract class MigrationChangeEventListener<T extends MigrationChangeApplicationEvent> implements ApplicationListener<T> {

    @Autowired
    protected IdentityConfig identityConfig;

    @Override
    public final void onApplicationEvent(T event) {
        String listenerName = getListenerName();

        if (identityConfig.getReloadableConfig().isMigrationListenerEnabled(listenerName)
                && !identityConfig.getReloadableConfig().getIgnoredChangeTypesForMigrationListener(listenerName).contains(event.getChangeType())
                ) {
            handleEvent(event);
        }
    }

    public abstract String getListenerName();

    protected abstract void handleEvent(T event);

}
