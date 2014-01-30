package com.rackspace.idm.domain.entity;

import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;

@Getter
@Setter
public class AuthorizationContext {
    ScopeAccess scopeAccess;
    HashSet<String> roles;
}
