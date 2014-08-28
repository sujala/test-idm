package com.rackspace.idm.api.security;

import com.rackspace.idm.domain.entity.EndUser;
import com.rackspace.idm.domain.entity.ImpersonatedScopeAccess;
import com.rackspace.idm.domain.entity.ScopeAccess;
import lombok.Data;
import lombok.Getter;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;

@Data
@Component
@Scope(value = "request", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class RequestContext {
    private boolean isImpersonated;
    private EndUser endUser;
    private ScopeAccess callerScopeAccess;
}
