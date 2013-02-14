package com.rackspace.idm.validation;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.*;
import com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Group;
import com.rackspace.docs.identity.api.ext.rax_ksqa.v1.SecretQA;
import com.rackspace.idm.validation.entity.*;
import org.dozer.DozerBeanMapper;
import org.dozer.Mapper;
import org.openstack.docs.identity.api.ext.os_ksadm.v1.Service;
import org.openstack.docs.identity.api.ext.os_ksadm.v1.UserForCreate;
import org.openstack.docs.identity.api.ext.os_kscatalog.v1.EndpointTemplate;
import org.openstack.docs.identity.api.v2.AuthenticationRequest;
import org.openstack.docs.identity.api.v2.Role;
import org.openstack.docs.identity.api.v2.Tenant;
import org.openstack.docs.identity.api.v2.User;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.lang.reflect.Type;
import java.util.HashMap;

@Component
public class ObjectConverter {
    private static Mapper mapper = new DozerBeanMapper();
    private HashMap<Type, Type> mapping;

    @PostConstruct
    public void setup(){
        mapping = new HashMap<Type, Type>();
        mapping.put(AuthenticationRequest.class, AuthenticationRequestForValidation.class);
        mapping.put(Region.class, RegionForValidation.class);
        mapping.put(Domain.class, DomainForValidation.class);
        mapping.put(Role.class, RoleForValidation.class);
        mapping.put(Group.class, GroupForValidation.class);
        mapping.put(Question.class, QuestionForValidation.class);
        mapping.put(SecretQA.class, SecretQAForValidation.class);
        mapping.put(com.rackspace.docs.identity.api.ext.rax_auth.v1.SecretQA.class, SecretQAForValidation.class);
        mapping.put(Service.class, ServiceForValidation.class);
        mapping.put(Tenant.class, TenantForValidation.class);
        mapping.put(EndpointTemplate.class, EndpointTemplateForValidation.class);
        mapping.put(Policy.class, PolicyForValidation.class);
        mapping.put(Policies.class, PoliciesForValidation.class);
        mapping.put(DefaultRegionServices.class, DefaultRegionServicesForValidation.class);
        mapping.put(User.class, UserForValidation.class);
        mapping.put(UserForCreate.class, UserForValidation.class);
        mapping.put(ImpersonationRequest.class, ImpersonationRequestForValidation.class);
    }

    public Object convert(Object object) {
        Type type = mapping.get(object.getClass());
        return mapper.map(object, (Class) type );
    }

    public boolean isConvertible(Object object){
        return mapping.containsKey(object.getClass());
    }
}
