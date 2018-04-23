package com.rackspace.idm.domain.entity;

public enum DelegateType {
    USER_GROUP(com.rackspace.docs.identity.api.ext.rax_auth.v1.DelegateType.USER_GROUP),
    USER(com.rackspace.docs.identity.api.ext.rax_auth.v1.DelegateType.USER);

    /**
     * The web representation ot this delegate type. // TODO: May want to just replace this enum with
     * com.rackspace.docs.identity.api.ext.rax_auth.v1.DelegateType.
     */
    private com.rackspace.docs.identity.api.ext.rax_auth.v1.DelegateType webType;

    DelegateType(com.rackspace.docs.identity.api.ext.rax_auth.v1.DelegateType webType) {
        this.webType = webType;
    }

    public String value() {
        return name();
    }

    public static DelegateType fromValue(String v) {
        return valueOf(v);
    }

    public com.rackspace.docs.identity.api.ext.rax_auth.v1.DelegateType toWebType() {
        return webType;
    }
}
