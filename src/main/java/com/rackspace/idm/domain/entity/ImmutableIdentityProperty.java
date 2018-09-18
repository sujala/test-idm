package com.rackspace.idm.domain.entity;

import lombok.experimental.Delegate;

public class ImmutableIdentityProperty implements ReadableIdentityProperty {
   @Delegate(types = ReadableIdentityProperty.class)
   IdentityProperty innerClone;

   public ImmutableIdentityProperty(IdentityProperty toClone) {
        this.innerClone = toClone;
    }
}
