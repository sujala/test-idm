package com.rackspace.idm.api.resource.cloud;

import org.springframework.stereotype.Component;

@Component
public class JAXBObjectFactories {

    private final org.openstack.docs.common.api.v1.ObjectFactory openStackCommonV1Factory = new org.openstack.docs.common.api.v1.ObjectFactory();
    private final org.openstack.docs.compute.api.v1.ObjectFactory openStackComputeV1Factory = new org.openstack.docs.compute.api.v1.ObjectFactory();
    private final org.openstack.docs.identity.api.v2.ObjectFactory openStackIdentityV2Factory = new org.openstack.docs.identity.api.v2.ObjectFactory();
    
    private final org.openstack.docs.identity.api.ext.os_ksadm.v1.ObjectFactory openStackIdentityExtKsadmnV1Factory = new org.openstack.docs.identity.api.ext.os_ksadm.v1.ObjectFactory();
    private final org.openstack.docs.identity.api.ext.os_kscatalog.v1.ObjectFactory openStackIdentityExtKscatalogV1Factory = new org.openstack.docs.identity.api.ext.os_kscatalog.v1.ObjectFactory();
    private final org.openstack.docs.identity.api.ext.os_ksec2.v1.ObjectFactory openStackIdentityExtKsec2V1Factory = new org.openstack.docs.identity.api.ext.os_ksec2.v1.ObjectFactory();
    
    private final com.rackspace.docs.identity.api.ext.rax_ksadm.v1.ObjectFactory rackspaceIdentityExtKsadmV1Factory = new com.rackspace.docs.identity.api.ext.rax_ksadm.v1.ObjectFactory();
    private final com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.ObjectFactory rackspaceIdentityExtKsgrpV1Factory = new com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.ObjectFactory();
    private final com.rackspace.docs.identity.api.ext.rax_kskey.v1.ObjectFactory rackspaceIdentityExtKskeyV1Factory = new com.rackspace.docs.identity.api.ext.rax_kskey.v1.ObjectFactory();
    
    private final org.w3._2005.atom.ObjectFactory atomObjectFactory = new org.w3._2005.atom.ObjectFactory();
    
    private final com.rackspacecloud.docs.auth.api.v1.ObjectFactory rackspaceCloudV1ObjectFactory = new com.rackspacecloud.docs.auth.api.v1.ObjectFactory();

    public org.openstack.docs.common.api.v1.ObjectFactory getOpenStackCommonV1Factory() {
        return openStackCommonV1Factory;
    }

    public org.openstack.docs.compute.api.v1.ObjectFactory getOpenStackComputeV1Factory() {
        return openStackComputeV1Factory;
    }

    public org.openstack.docs.identity.api.v2.ObjectFactory getOpenStackIdentityV2Factory() {
        return openStackIdentityV2Factory;
    }

    public org.openstack.docs.identity.api.ext.os_ksadm.v1.ObjectFactory getOpenStackIdentityExtKsadmnV1Factory() {
        return openStackIdentityExtKsadmnV1Factory;
    }

    public org.openstack.docs.identity.api.ext.os_kscatalog.v1.ObjectFactory getOpenStackIdentityExtKscatalogV1Factory() {
        return openStackIdentityExtKscatalogV1Factory;
    }

    public org.openstack.docs.identity.api.ext.os_ksec2.v1.ObjectFactory getOpenStackIdentityExtKsec2V1Factory() {
        return openStackIdentityExtKsec2V1Factory;
    }

    public com.rackspace.docs.identity.api.ext.rax_ksadm.v1.ObjectFactory getRackspaceIdentityExtKsadmV1Factory() {
        return rackspaceIdentityExtKsadmV1Factory;
    }

    public com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.ObjectFactory getRackspaceIdentityExtKsgrpV1Factory() {
        return rackspaceIdentityExtKsgrpV1Factory;
    }

    public com.rackspace.docs.identity.api.ext.rax_kskey.v1.ObjectFactory getRackspaceIdentityExtKskeyV1Factory() {
        return rackspaceIdentityExtKskeyV1Factory;
    }

    public org.w3._2005.atom.ObjectFactory getAtomObjectFactory() {
        return atomObjectFactory;
    }

    public com.rackspacecloud.docs.auth.api.v1.ObjectFactory getRackspaceCloudV1ObjectFactory() {
        return rackspaceCloudV1ObjectFactory;
    }
}
