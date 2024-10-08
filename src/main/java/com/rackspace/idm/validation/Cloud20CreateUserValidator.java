package com.rackspace.idm.validation;

import com.rackspace.idm.ErrorCodes;
import com.rackspace.idm.api.resource.cloud.v20.DefaultCloud20Service;
import com.rackspace.idm.api.security.IdentityRole;
import com.rackspace.idm.domain.config.IdentityConfig;
import com.rackspace.idm.domain.entity.Domain;
import com.rackspace.idm.domain.entity.EndUser;
import com.rackspace.idm.domain.entity.FederatedUser;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.service.*;
import com.rackspace.idm.domain.service.impl.CreateUserUtil;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspace.idm.exception.ForbiddenException;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Transformer;
import org.apache.commons.lang.StringUtils;
import org.openstack.docs.identity.api.v2.Role;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.List;


@Component
public class Cloud20CreateUserValidator {

    @Autowired
    private AuthorizationService authorizationService;

    @Autowired
    private IdentityConfig identityConfig;

    @Autowired
    private RoleService roleService;

    @Autowired
    private DomainService domainService;

    @Autowired
    private PrecedenceValidator precedenceValidator;

    @Autowired
    private Validator20 validator20;

    @Autowired
    private PhonePinService phonePinService;

    public User validateCreateUserAndGetUserForDefaults(org.openstack.docs.identity.api.v2.User user, EndUser caller) {

        if (StringUtils.isNotBlank(user.getContactId())) {
            validator20.validateStringMaxLength("contactId", user.getContactId(), Validator20.MAX_LENGTH_64);
        }

        // Validate phone pin
        if (StringUtils.isNotBlank(user.getPhonePin())) {
            // Verify caller is authorized to set phone pin
            if (!authorizationService.authorizeEffectiveCallerHasAtLeastOneOfIdentityRolesByName(
                    IdentityRole.IDENTITY_PHONE_PIN_ADMIN.getRoleName())) {
                throw new ForbiddenException("Not authorized to set phone pin.", ErrorCodes.ERROR_CODE_FORBIDDEN_ACTION);
            }

            validator20.validatePhonePin(user.getPhonePin());
        }

        if (CreateUserUtil.isCreateUserOneCall(user)) {
            // Only identity:admin should be able to create a user including roles, groups and secret QA.
            if (!authorizationService.authorizeEffectiveCallerHasAtLeastOneOfIdentityRolesByName(Collections.singletonList(IdentityUserTypeEnum.IDENTITY_ADMIN.getRoleName()))) {
                throw new ForbiddenException(DefaultCloud20Service.NOT_AUTHORIZED);
            }

            if (user.getRoles() != null) {
                for (Role role : user.getRoles().getRole()) {

                    if (StringUtils.isBlank(role.getName())) {
                        throw new BadRequestException("Role name cannot be blank");
                    }

                    if (roleService.isIdentityUserTypeRole(role.getName())) {
                        //identity admins can create sub-users and the user defaults are set based on the user admin for the domain
                        if(IdentityUserTypeEnum.DEFAULT_USER.getRoleName().equals(role.getName()) ||
                                IdentityUserTypeEnum.USER_MANAGER.getRoleName().equals(role.getName())) {
                            Domain domain = domainService.getDomain(user.getDomainId());

                            if(domain == null || !domain.getEnabled()) {
                                throw new BadRequestException(DefaultCloud20Service.INVALID_DOMAIN_ERROR);
                            }

                            if(user.getGroups() != null) {
                                throw new BadRequestException(DefaultCloud20Service.CANNOT_SPECIFY_GROUPS_ERROR);
                            }

                            if(CollectionUtils.isEmpty(domainService.getEnabledDomainAdmins(user.getDomainId()))) {
                                throw new BadRequestException(DefaultCloud20Service.INVALID_DOMAIN_ERROR);
                            }

                        } else {
                            throw new ForbiddenException(DefaultCloud20Service.NOT_AUTHORIZED);
                        }
                    }
                }
            }
        }

        if (user.getSecretQA() != null) {
            if (StringUtils.isBlank(user.getSecretQA().getQuestion())) {
                throw new BadRequestException("Missing secret question");
            }
            if (StringUtils.isBlank(user.getSecretQA().getAnswer())) {
                throw new BadRequestException("Missing secret answer");
            }
        }

        User userForDefaults = getUserForDefaults(user, caller);

        Collection<String> roleNames = user.getRoles() == null ? Collections.EMPTY_LIST : CollectionUtils.collect(user.getRoles().getRole(), new Transformer<Role, String>() {
            @Override
            public String transform(Role role) {
                return role != null && StringUtils.isNotBlank(role.getName()) ? role.getName() : null;
            }
        });
        precedenceValidator.verifyCallerRolePrecedenceForAssignment(caller, roleNames);

        return userForDefaults;
    }

    private User getUserForDefaults(org.openstack.docs.identity.api.v2.User usr, EndUser caller) {
        User userForDefaults = null;
        if (caller instanceof FederatedUser) {
            // if caller is a federated user,
            // this means that the user for the defaults should be the enabled user-admin of the domain
            userForDefaults = getDomainUserAdmin(caller.getDomainId());
        } else {
            userForDefaults = (User)caller;

            // if this is a one-user call and we are creating a sub-user,
            // this means that the user for defaults should be the enabled user-admin of the domain
            if(CreateUserUtil.isCreateUserOneCall(usr) && usr.getRoles() != null && CollectionUtils.isNotEmpty(usr.getRoles().getRole())) {
                Collection<IdentityUserTypeEnum> userTypeBeingCreated = CollectionUtils.collect(usr.getRoles().getRole(), new Transformer<Role, IdentityUserTypeEnum>() {
                    @Override
                    public IdentityUserTypeEnum transform(Role role) {
                        return role == null || StringUtils.isBlank(role.getName()) ? null : IdentityUserTypeEnum.fromRoleName(role.getName());
                    }
                });
                if (userTypeBeingCreated.contains(IdentityUserTypeEnum.DEFAULT_USER) || userTypeBeingCreated.contains(IdentityUserTypeEnum.USER_MANAGER)) {
                    userForDefaults = getDomainUserAdmin(usr.getDomainId());
                }
            }
        }

        return userForDefaults;
    }

    private User getDomainUserAdmin(String domainId) {
        User userForDefaults;List<User> userAdmins = domainService.getEnabledDomainAdmins(domainId);
        //the user-admins for the domain have already been validate but checking again just to be safe and not return a 500
        if(CollectionUtils.isEmpty(userAdmins)) {
            throw new BadRequestException(DefaultCloud20Service.INVALID_DOMAIN_ERROR);
        }
        userForDefaults = userAdmins.get(0);
        return userForDefaults;
    }

}
