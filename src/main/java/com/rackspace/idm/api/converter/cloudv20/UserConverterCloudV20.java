package com.rackspace.idm.api.converter.cloudv20;

import com.rackspace.idm.domain.entity.Racker;
import com.rackspace.idm.domain.entity.TenantRole;
import org.apache.commons.lang.StringUtils;
import org.dozer.Mapper;
import org.joda.time.DateTime;
import org.openstack.docs.identity.api.ext.os_ksadm.v1.UserForCreate;
import org.openstack.docs.identity.api.v2.ObjectFactory;
import org.openstack.docs.identity.api.v2.User;
import org.openstack.docs.identity.api.v2.UserForAuthenticateResponse;
import org.openstack.docs.identity.api.v2.UserList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import java.util.List;

@Component
public class UserConverterCloudV20 {
    private Logger logger = LoggerFactory.getLogger(UserConverterCloudV20.class);

    private ObjectFactory objectFactory = new ObjectFactory();

    @Autowired
    private Mapper mapper;

    @Autowired
    RoleConverterCloudV20 roleConverterCloudV20;

    @Autowired
    SecretQAConverterCloudV20 secretQAConverterCloudV20;

    @Autowired
    GroupConverterCloudV20 groupConverterCloudV20;

    public void setMapper(Mapper mapper) {
        this.mapper = mapper;
    }

    public com.rackspace.idm.domain.entity.User fromUser(org.openstack.docs.identity.api.v2.User user) {
        com.rackspace.idm.domain.entity.User userEntity = mapper.map(user, com.rackspace.idm.domain.entity.User.class);

        //This is being set manually, and not relying on dozer to do this,
        //if not the password that is saved will not work.
        userEntity.setPassword(user.getPassword());
        userEntity.setUserPassword(user.getPassword());

        if (user.getSecretQA() != null) {
            userEntity.setSecretQuestion(user.getSecretQA().getQuestion());
            userEntity.setSecretAnswer(user.getSecretQA().getAnswer());
        }

        userEntity.setRoles(roleConverterCloudV20.toTenantRoles(user.getRoles()));
        userEntity.setRsGroupId(groupConverterCloudV20.toSetOfGroupIds(user.getGroups()));

        return userEntity;
    }

    public UserForAuthenticateResponse toUserForAuthenticateResponse(com.rackspace.idm.domain.entity.User user) {
        return toUserForAuthenticateResponse(user, user.getRoles());
    }

    public UserForAuthenticateResponse toUserForAuthenticateResponse(com.rackspace.idm.domain.entity.User user, List<TenantRole> roles) {
        UserForAuthenticateResponse jaxbUser = objectFactory.createUserForAuthenticateResponse();

        jaxbUser.setId(user.getId());
        jaxbUser.setName(user.getUsername());
        String region = user.getRegion();
        if(org.apache.commons.lang.StringUtils.isBlank(region) ){
            region = "";
        }
        jaxbUser.setDefaultRegion(region);

        if(roles != null){
            jaxbUser.setRoles(this.roleConverterCloudV20.toRoleListJaxb(roles));
        }

        jaxbUser.setFederated(user.isFederated());

        if (StringUtils.isNotBlank(user.getFederatedIdp())) {
            jaxbUser.setFederatedIdp(user.getFederatedIdp());
        }

        return jaxbUser;
    }

    public UserForAuthenticateResponse toRackerForAuthenticateResponse(Racker racker, List<TenantRole> roles) {
        UserForAuthenticateResponse userForAuthenticateResponse = objectFactory.createUserForAuthenticateResponse();
        userForAuthenticateResponse.setName(racker.getUsername());
        userForAuthenticateResponse.setId(racker.getRackerId());
        if(roles != null){
            userForAuthenticateResponse.setRoles(this.roleConverterCloudV20.toRoleListJaxb(roles));
        }
        return userForAuthenticateResponse;
    }

    public UserForCreate toUserForCreate(com.rackspace.idm.domain.entity.User user) {
        org.openstack.docs.identity.api.ext.os_ksadm.v1.ObjectFactory v1ObjectFactory = new org.openstack.docs.identity.api.ext.os_ksadm.v1.ObjectFactory();
        UserForCreate jaxbUser = v1ObjectFactory.createUserForCreate();

        jaxbUser.setDisplayName(user.getDisplayName());
        jaxbUser.setEmail(user.getEmail());
        jaxbUser.setEnabled(user.getEnabled());
        jaxbUser.setId(user.getId());
        jaxbUser.setUsername(user.getUsername());
        jaxbUser.setDomainId(user.getDomainId());
        jaxbUser.setDefaultRegion(user.getRegion());
        if (user.getPassword() != null) {
            jaxbUser.setPassword(user.getPassword());
        }

        if(user.getRegion() != null){
            jaxbUser.setDefaultRegion(user.getRegion());
        }
        if(user.getDomainId() != null){
            jaxbUser.setDomainId(user.getDomainId());
        }

        try {
            if (user.getCreated() != null) {
                jaxbUser.setCreated(DatatypeFactory.newInstance()
                        .newXMLGregorianCalendar(new DateTime(user.getCreated()).toGregorianCalendar()));
            }

            if (user.getUpdated() != null) {
                jaxbUser.setUpdated(DatatypeFactory.newInstance()
                        .newXMLGregorianCalendar(new DateTime(user.getUpdated()).toGregorianCalendar()));
            }

        }   catch (DatatypeConfigurationException e) {
            logger.info("failed to create XMLGregorianCalendar: " + e.getMessage());
        }

        return jaxbUser;
    }

    public User toUser(com.rackspace.idm.domain.entity.User user) {
        User jaxbUser = mapper.map(user, User.class);

        try {
            if (user.getCreated() != null) {
                jaxbUser.setCreated(DatatypeFactory.newInstance()
                        .newXMLGregorianCalendar(new DateTime(user.getCreated()).toGregorianCalendar()));
            }

            if (user.getUpdated() != null) {
                jaxbUser.setUpdated(DatatypeFactory.newInstance()
                        .newXMLGregorianCalendar(new DateTime(user.getUpdated()).toGregorianCalendar()));
            }

            if (user.getSecretQuestion() != null || user.getSecretAnswer() != null) {
                jaxbUser.setSecretQA(this.secretQAConverterCloudV20.toSecretQA(user.getSecretQuestion(), user.getSecretAnswer()));
            }

            if (user.getRoles() != null) {
                jaxbUser.setRoles(this.roleConverterCloudV20.toRoleListJaxb(user.getRoles()));
            }

            if (user.getRsGroupId() != null) {
                jaxbUser.setGroups(this.groupConverterCloudV20.toGroupListJaxb(user.getRsGroupId()));
            }

        } catch (DatatypeConfigurationException e) {
            logger.info("failed to create XMLGregorianCalendar: " + e.getMessage());
        }

        return jaxbUser;
    }

    public UserList toUserList(Iterable<com.rackspace.idm.domain.entity.User> users) {

        UserList list = objectFactory.createUserList();

        for (com.rackspace.idm.domain.entity.User user : users) {
            list.getUser().add(this.toUser(user));
        }

        return list;
    }
}
