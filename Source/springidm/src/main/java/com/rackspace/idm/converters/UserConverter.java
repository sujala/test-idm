package com.rackspace.idm.converters;

import org.apache.commons.lang.StringUtils;

import com.rackspace.idm.entities.User;
import com.rackspace.idm.entities.UserStatus;
import com.rackspace.idm.entities.Users;
import com.rackspace.idm.jaxb.ObjectFactory;

public class UserConverter {

    private RoleConverter roleConverter;
    protected ObjectFactory of = new ObjectFactory();

    public UserConverter(RoleConverter roleConverter) {
        this.roleConverter = roleConverter;
    }

    public User toUserDO(com.rackspace.idm.jaxb.User jaxbUser) {
        User user = new User();

        user.setCountry(jaxbUser.getCountry());
        user.setTimeZone(jaxbUser.getTimeZone());
        user.setCustomerId(jaxbUser.getCustomerId());
        user.setDisplayName(jaxbUser.getDisplayName());
        user.setEmail(jaxbUser.getEmail());
        user.setFirstname(jaxbUser.getFirstName());
        user.setIname(jaxbUser.getIname());
        user.setInum(jaxbUser.getInum());

        if (jaxbUser.isLocked() != null) {
            user.setIsLocked(jaxbUser.isLocked());
        }

        user.setLastname(jaxbUser.getLastName());
        user.setMiddlename(jaxbUser.getMiddleName());
        user.setOrgInum(jaxbUser.getCustomerInum());
        user.setPersonId(jaxbUser.getPersonId());
        user.setPrefferedLang(jaxbUser.getPrefLanguage());
        user.setRegion(jaxbUser.getRegion());

        if (jaxbUser.isSoftDeleted() != null) {
            user.setSoftDeleted(jaxbUser.isSoftDeleted());
        }

        if (jaxbUser.getStatus() != null) {
            user.setStatus(Enum.valueOf(UserStatus.class, jaxbUser.getStatus()
                .value().toUpperCase()));
        }

        // user.setTimeZone(jaxbUser.getTimeZone());
        user.setUsername(jaxbUser.getUsername());

        if (jaxbUser.getPassword() != null
            && !StringUtils.isBlank(jaxbUser.getPassword().getPassword())) {
            user.setPassword(jaxbUser.getPassword().getPassword());
        }

        if (jaxbUser.getSecret() != null
            && !StringUtils.isBlank(jaxbUser.getSecret().getSecretQuestion())
            && !StringUtils.isBlank(jaxbUser.getSecret().getSecretAnswer())) {
            user.setSecretQuestion(jaxbUser.getSecret().getSecretQuestion());
            user.setSecretAnswer(jaxbUser.getSecret().getSecretAnswer());
        }

        if (jaxbUser.getApiKey() != null
            && !StringUtils.isBlank(jaxbUser.getApiKey().getApiKey())) {
            user.setApiKey(jaxbUser.getApiKey().getApiKey());
        }

        if (jaxbUser.getRoles() != null
            && jaxbUser.getRoles().getRoles().size() > 0) {
            user.setRoles(roleConverter.toRoleListDO((jaxbUser.getRoles())));
        }

        return user;
    }

    public com.rackspace.idm.jaxb.Users toUserListJaxb(Users users) {
        com.rackspace.idm.jaxb.Users userlist = of.createUsers();

        for (User user : users.getUsers()) {
            userlist.getUsers().add(
                toUserJaxbWithoutAnyAdditionalElements(user));
        }

        userlist.setLimit(users.getLimit());
        userlist.setOffset(users.getOffset());
        userlist.setTotalRecords(users.getTotalRecords());

        return userlist;
    }

    public com.rackspace.idm.jaxb.User toUserJaxb(User user) {
        return toUserJaxb(user, true, true, true);
    }

    public com.rackspace.idm.jaxb.User toUserWithOnlyRolesJaxb(User user) {
        return toUserJaxb(user, false, false, true);
    }

    public com.rackspace.idm.jaxb.User toUserWithOnlyStatusJaxb(User user) {
        com.rackspace.idm.jaxb.User returnedUser = of.createUser();
        String status = user.getStatus().toString().toUpperCase();
        returnedUser.setStatus(Enum.valueOf(
            com.rackspace.idm.jaxb.UserStatus.class, status));
        return returnedUser;
    }

    public com.rackspace.idm.jaxb.User toUserWithOnlyLockJaxb(User user) {
        com.rackspace.idm.jaxb.User returnedUser = of.createUser();
        returnedUser.setLocked(user.getIsLocked());
        return returnedUser;
    }

    public com.rackspace.idm.jaxb.User toUserWithOnlySoftDeletedJaxb(User user) {
        com.rackspace.idm.jaxb.User returnedUser = of.createUser();
        returnedUser.setSoftDeleted(user.getSoftDeleted());
        return returnedUser;
    }

    public com.rackspace.idm.jaxb.User toUserJaxbWithoutAnyAdditionalElements(
        User user) {
        return toUserJaxb(user, false, false, false);
    }

    private com.rackspace.idm.jaxb.User toUserJaxb(User user,
        boolean includePassword, boolean includeSecret, boolean includeRoles) {
        com.rackspace.idm.jaxb.User returnedUser = of.createUser();
        returnedUser.setCountry(user.getCountry());
        returnedUser.setTimeZone(user.getTimeZone());
        returnedUser.setCustomerId(user.getCustomerId());
        returnedUser.setCustomerInum(user.getOrgInum());
        returnedUser.setDisplayName(user.getDisplayName());
        returnedUser.setEmail(user.getEmail());
        returnedUser.setIname(user.getIname());
        returnedUser.setInum(user.getInum());
        returnedUser.setLocked(user.getIsLocked());
        returnedUser.setFirstName(user.getFirstname());
        returnedUser.setLastName(user.getLastname());
        returnedUser.setMiddleName(user.getMiddlename());
        returnedUser.setPersonId(user.getPersonId());
        returnedUser.setPrefLanguage(user.getPreferredLang());
        returnedUser.setRegion(user.getRegion());
        String status = user.getStatus().toString().toUpperCase();
        returnedUser.setStatus(Enum.valueOf(
            com.rackspace.idm.jaxb.UserStatus.class, status));
        returnedUser.setUsername(user.getUsername());
        returnedUser.setSoftDeleted(user.getSoftDeleted());

        if (includePassword && user.getPasswordObj() != null
            && !StringUtils.isBlank(user.getPasswordObj().getValue())) {
            com.rackspace.idm.jaxb.UserPassword password = of
                .createUserPassword();

            password.setPassword(user.getPasswordObj().getValueNoPrefix());
            returnedUser.setPassword(password);
        }

        if (includeSecret && !StringUtils.isBlank(user.getSecretAnswer())
            && !StringUtils.isBlank(user.getSecretQuestion())) {

            com.rackspace.idm.jaxb.UserSecret secret = of.createUserSecret();

            secret.setSecretAnswer(user.getSecretAnswer());
            secret.setSecretQuestion(user.getSecretQuestion());
            returnedUser.setSecret(secret);
        }

        if (includeRoles && user.getRoles() != null
            && user.getRoles().size() > 0) {

            com.rackspace.idm.jaxb.Roles roles = roleConverter.toRolesJaxb(user
                .getRoles());

            returnedUser.setRoles(roles);
        }

        return returnedUser;
    }

}
