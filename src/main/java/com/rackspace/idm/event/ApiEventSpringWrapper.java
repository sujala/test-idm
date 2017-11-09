package com.rackspace.idm.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class ApiEventSpringWrapper extends ApplicationEvent {

    @Getter
    private ApiEvent event;

    public ApiEventSpringWrapper(Object source, ApiEvent event) {
        super(source);
        this.event = event;
    }
}
