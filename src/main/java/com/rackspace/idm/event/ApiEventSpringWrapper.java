package com.rackspace.idm.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * A simple wrapper around an API event to allow it to be sent via Spring's event framework
 */
@Getter
public class ApiEventSpringWrapper extends ApplicationEvent {

    @Getter
    private ApiEvent event;

    public ApiEventSpringWrapper(Object source, ApiEvent event) {
        super(source);
        this.event = event;
    }
}
