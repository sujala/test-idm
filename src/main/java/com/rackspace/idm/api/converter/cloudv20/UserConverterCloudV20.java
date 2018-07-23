package com.rackspace.idm.api.converter.cloudv20;

import com.rackspace.idm.api.security.AuthenticationContext;
import com.rackspace.idm.domain.config.IdentityConfig;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.multifactor.service.BasicMultiFactorService;
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
import org.springframework.util.CollectionUtils;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;
import java.util.HashSet;
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

    @Autowired
    AuthorizationService authorizationService;

    @Autowired
    IdentityConfig identityConfig;

    @Autowired
    private BasicMultiFactorService basicMultiFactorService;

    @Autowired
    AuthenticationContext authenticationContext;

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

    public UserForAuthenticateResponse toUserForAuthenticateResponse(EndUser user, List<TenantRole> roles) {
        UserForAuthenticateResponse jaxbUser = objectFactory.createUserForAuthenticateResponse();

        jaxbUser.setId(user.getId());
        jaxbUser.setName(user.getUsername());
        jaxbUser.setDomainId(user.getDomainId());
        jaxbUser.setContactId(user.getContactId());

        String region = user.getRegion();
        if(org.apache.commons.lang.StringUtils.isBlank(region)) {
            region = "";
        }
        jaxbUser.setDefaultRegion(region);

        if(roles != null) {
            jaxbUser.setRoles(this.roleConverterCloudV20.toRoleListJaxb(roles));
        }

        // Federated specific information
        if (user instanceof FederatedBaseUser) {
            String idp = ((FederatedBaseUser)user).getFederatedIdpUri();
            if (StringUtils.isNotBlank(idp)) {
                jaxbUser.setFederatedIdp(idp);
            }
        }

        // Delegate specific information
        if (user instanceof EndUserDelegate) {
            EndUserDelegate endUserDelegate = (EndUserDelegate) user;
            if (endUserDelegate.getDelegationAgreement() != null) {
                jaxbUser.setDelegationAgreementId(endUserDelegate.getDelegationAgreement().getId());
            }
        }

        if (authenticationContext.getDomain() != null) {
            Domain domain = authenticationContext.getDomain();
            String timeout = domain.getSessionInactivityTimeout() != null ?
                    domain.getSessionInactivityTimeout() : identityConfig.getReloadableConfig().getDomainDefaultSessionInactivityTimeout().toString();
            try {
                Duration  userDuration = DatatypeFactory.newInstance().newDuration(timeout);
                jaxbUser.setSessionInactivityTimeout(userDuration);
            } catch (DatatypeConfigurationException e) {
                logger.warn("Unable to parse session timeout duration {}.", domain.getSessionInactivityTimeout());
            }
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
        if (racker.isFederatedRacker()) {
            userForAuthenticateResponse.setFederatedIdp(racker.getFederatedIdpUri());
            userForAuthenticateResponse.setId(racker.getUsername());
            userForAuthenticateResponse.setName(racker.getUsername());
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

    public User toUser(com.rackspace.idm.domain.entity.EndUser user) {
        if (user instanceof com.rackspace.idm.domain.entity.User) {
            return toUser((com.rackspace.idm.domain.entity.User)user, false);
        } else if (user instanceof FederatedUser) {
            return toFederatedUser((FederatedUser) user);
        } else if (user instanceof ProvisionedUserDelegate) {
            return toDelegateUser((com.rackspace.idm.domain.entity.ProvisionedUserDelegate)user);
        }
        throw new IllegalArgumentException("Unrecognized end user");
    }

    private User toDelegateUser(ProvisionedUserDelegate user) {
        if (user.getOriginalEndUser() instanceof com.rackspace.idm.domain.entity.User) {
            User jaxbUser = toUser((com.rackspace.idm.domain.entity.User)user.getOriginalEndUser(), false);
            jaxbUser.setDomainId(user.getDomainId());
            jaxbUser.setDefaultRegion(user.getRegion());
            jaxbUser.setDelegationAgreementId(user.getDelegationAgreement().getId());
            return jaxbUser;
        } else {
            throw new IllegalArgumentException("Unrecognized end user");
        }
    }

    public User toUser(com.rackspace.idm.domain.entity.User user, boolean includeOtherAttributes) {
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
            /*
                The initial mapper.map call to create the initial jaxbUser will call user.getRoles(). This
                call has the side effect of creating an empty ArrayList, which is then set on the jaxbUser object
                and added to the resultant json as '"roles": {}'. This is not desired. Instead we do not
                want it included in the json, so null it out.
            */
            jaxbUser.setRoles(null);

            jaxbUser.setMultiFactorState(basicMultiFactorService.getLogicalUserMultiFactorState(user));
            if (user.isMultiFactorEnabled()) {
                jaxbUser.setFactorType(user.getMultiFactorTypeAsEnum());
            }

            if (!user.isUnverified()) {
                //if the user has null for mfa enabled the set the value to false
                //this is done to match the json logic to write the user to the logic used to write the user in xml
                jaxbUser.setMultiFactorEnabled(user.getMultifactorEnabled() == null ? false : user.getMultifactorEnabled());
            }

            if(includeOtherAttributes) {
                if (user.getSecretQuestion() != null || user.getSecretAnswer() != null) {
                    jaxbUser.setSecretQA(this.secretQAConverterCloudV20.toSecretQA(user.getSecretQuestion(), user.getSecretAnswer()));
                }

                if (!CollectionUtils.isEmpty(user.getRoles())) {
                    jaxbUser.setRoles(this.roleConverterCloudV20.toRoleListJaxb(user.getRoles()));
                }

                if (!CollectionUtils.isEmpty(user.getRsGroupId())) {
                    jaxbUser.setGroups(this.groupConverterCloudV20.toGroupListJaxb(new HashSet<String>(user.getRsGroupId())));
                }

            }
        } catch (DatatypeConfigurationException e) {
            logger.info("failed to create XMLGregorianCalendar: " + e.getMessage());
        }

        return jaxbUser;
    }

    public User toFederatedUser(FederatedUser user) {
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

            if(user.getContactId() != null) {
                jaxbUser.setContactId(user.getContactId());
            }
            /*
                The initial mapper.map call to create the initial jaxbUser will call user.getRoles(). This
                call has the side effect of creating an empty ArrayList, which is then set on the jaxbUser object
                and added to the resultant json as '"roles": {}'. This is not desired. Instead we do not
                want it included in the json, so null it out.
            */
            jaxbUser.setRoles(null);

            if(StringUtils.isNotBlank(user.getFederatedIdpUri())){
                jaxbUser.setFederatedIdp(user.getFederatedIdpUri());
            }

            jaxbUser.setEnabled(true); //fed users that exist are always enabled

        } catch (DatatypeConfigurationException e) {
            logger.info("failed to create XMLGregorianCalendar: " + e.getMessage());
        }

        return jaxbUser;
    }

    public UserList toUserList(Iterable<? extends EndUser> users) {

        UserList list = objectFactory.createUserList();

        for (EndUser user : users) {
            list.getUser().add(this.toUser(user));
        }

        return list;
    }
}
