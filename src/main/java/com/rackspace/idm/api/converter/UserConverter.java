package com.rackspace.idm.api.converter;

import com.rackspace.api.idm.v1.ObjectFactory;
import com.rackspace.api.idm.v1.Role;
import com.rackspace.idm.domain.entity.Racker;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.entity.Users;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.bind.JAXBElement;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import java.util.List;

@Component
public class UserConverter {

    private final ObjectFactory objectFactory = new ObjectFactory();

    @Autowired
    private RolesConverter rolesConverter;

    private Logger logger = LoggerFactory.getLogger(UserConverter.class);

    public User toUserDO(com.rackspace.api.idm.v1.User jaxbUser) {
        User user = new User();

        user.setId(jaxbUser.getId());

        user.setCountry(jaxbUser.getCountry());
        user.setTimeZoneId(jaxbUser.getTimeZone());
        user.setCustomerId(jaxbUser.getCustomerId());
        user.setDisplayName(jaxbUser.getDisplayName());
        user.setEmail(jaxbUser.getEmail());
        user.setFirstname(jaxbUser.getFirstName());
        user.setEnabled(jaxbUser.isEnabled());
        user.setLastname(jaxbUser.getLastName());
        user.setMiddlename(jaxbUser.getMiddleName());
        user.setPersonId(jaxbUser.getPersonId());
        user.setPreferredLang(jaxbUser.getPrefLanguage());
        user.setRegion(jaxbUser.getRegion());
        user.setMaxLoginFailuresExceeded(jaxbUser.isMaxLoginFailuresExceded());
        user.setUsername(jaxbUser.getUsername());

        if (jaxbUser.getPasswordCredentials() != null
            && jaxbUser.getPasswordCredentials().getCurrentPassword() != null
            && jaxbUser.getPasswordCredentials().getCurrentPassword()
                .getPassword() != null
            && !StringUtils.isBlank(jaxbUser.getPasswordCredentials()
                .getCurrentPassword().getPassword())) {
            user.setPassword(jaxbUser.getPasswordCredentials()
                .getCurrentPassword().getPassword());
            user.setUserPassword(jaxbUser.getPasswordCredentials().getCurrentPassword().getPassword());
        }

        if (jaxbUser.getSecret() != null
            && !StringUtils.isBlank(jaxbUser.getSecret().getSecretQuestion())
            && !StringUtils.isBlank(jaxbUser.getSecret().getSecretAnswer())) {
            user.setSecretQuestion(jaxbUser.getSecret().getSecretQuestion());
            user.setSecretAnswer(jaxbUser.getSecret().getSecretAnswer());
        }

        return user;
    }

    public JAXBElement<com.rackspace.api.idm.v1.UserList> toUserListJaxb(
        Users users) {

        if (users == null || users.getUsers() == null) {
            return null;
        }

        com.rackspace.api.idm.v1.UserList userlist = objectFactory
            .createUserList();

        for (User user : users.getUsers()) {
            userlist.getUser().add(
                toUserJaxbWithoutAnyAdditionalElements(user).getValue());
        }

        userlist.setLimit(users.getLimit());
        userlist.setOffset(users.getOffset());
        userlist.setTotalRecords(users.getTotalRecords());

        return objectFactory.createUsers(userlist);
    }

    public com.rackspace.api.idm.v1.Racker toRackerJaxb(String rackerId) {
        com.rackspace.api.idm.v1.Racker returnedRacker = objectFactory
            .createRacker();
        returnedRacker.setUsername(rackerId);
        return returnedRacker;
    }

    public JAXBElement<com.rackspace.api.idm.v1.Racker> toRackerJaxb(
        Racker racker) {
        com.rackspace.api.idm.v1.Racker returnedRacker = objectFactory
            .createRacker();
        returnedRacker.setId(racker.getRackerId()); //TODO: is this correct or should it set username?
        if (racker.getRackerRoles() != null
            && racker.getRackerRoles().size() > 0) {
            returnedRacker.setRoles(toRackerRolesJaxb(racker.getRackerRoles())
                .getValue());
        }
        return objectFactory.createRacker(returnedRacker);
    }

    public JAXBElement<com.rackspace.api.idm.v1.RoleList> toRackerRolesJaxb(
        List<String> roles) {
        com.rackspace.api.idm.v1.RoleList returnedRoles = objectFactory
            .createRoleList();
        for (String role : roles) {
            if (!StringUtils.isEmpty(role)) {
                Role rackerRole = objectFactory.createRole();
                rackerRole.setName(role);
                returnedRoles.getRole().add(rackerRole);
            }
        }
        return objectFactory.createRoles(returnedRoles);
    }

    public JAXBElement<com.rackspace.api.idm.v1.User> toUserJaxb(User user) {
        return toUserJaxb(user, true, true);
    }

    public JAXBElement<com.rackspace.api.idm.v1.User> toUserJaxbWithoutAnyAdditionalElements(
        User user) {
        return toUserJaxb(user, false, false);
    }

    JAXBElement<com.rackspace.api.idm.v1.User> toUserJaxb(User user,
                                                          boolean includePassword, boolean includeSecret) {
        com.rackspace.api.idm.v1.User returnedUser = objectFactory.createUser();
        returnedUser.setId(user.getId());
        returnedUser.setCountry(user.getCountry());
        returnedUser.setTimeZone(user.getTimeZoneId());
        returnedUser.setCustomerId(user.getCustomerId());
        returnedUser.setDisplayName(user.getDisplayName());
        returnedUser.setEmail(user.getEmail());
        returnedUser.setEnabled(user.getEnabled());
        returnedUser.setFirstName(user.getFirstname());
        returnedUser.setLastName(user.getLastname());
        returnedUser.setMiddleName(user.getMiddlename());
        returnedUser.setPersonId(user.getPersonId());
        returnedUser.setPrefLanguage(user.getPreferredLang());
        returnedUser.setRegion(user.getRegion());
        returnedUser.setUsername(user.getUsername());
        returnedUser.setMaxLoginFailuresExceded(user.getMaxLoginFailuresExceeded());

        try {
            if (user.getCreated() != null) {
                returnedUser.setCreated(DatatypeFactory.newInstance()
                    .newXMLGregorianCalendar(new DateTime(user.getCreated()).toGregorianCalendar()));
            }

            if (user.getUpdated() != null) {
                returnedUser.setUpdated(DatatypeFactory.newInstance()
                    .newXMLGregorianCalendar(new DateTime(user.getUpdated()).toGregorianCalendar()));
            }

        } catch (DatatypeConfigurationException e) {
            logger.info("failed to create XMLGregorianCalendar: " + e.getMessage());
        }

        if (includePassword && user.getPassword() != null
            && !StringUtils.isBlank(user.getPassword())) {

            com.rackspace.api.idm.v1.UserPasswordCredentials pc = objectFactory
                .createUserPasswordCredentials();
            com.rackspace.api.idm.v1.UserPassword password = objectFactory
                .createUserPassword();

            password.setPassword(user.getPassword());
            pc.setCurrentPassword(password);

            returnedUser.setPasswordCredentials(pc);
        }

        if (includeSecret && !StringUtils.isBlank(user.getSecretAnswer())
            && !StringUtils.isBlank(user.getSecretQuestion())) {

            com.rackspace.api.idm.v1.UserSecret secret = objectFactory
                .createUserSecret();

            secret.setSecretAnswer(user.getSecretAnswer());
            secret.setSecretQuestion(user.getSecretQuestion());
            returnedUser.setSecret(secret);
        }

        return objectFactory.createUser(returnedUser);
    }

    public JAXBElement<com.rackspace.api.idm.v1.User> toUserJaxbFromUser(
        String username, String customerId) {
        com.rackspace.api.idm.v1.User returnedUser = objectFactory.createUser();
        returnedUser.setUsername(username);
        returnedUser.setCustomerId(customerId);

        return objectFactory.createUser(returnedUser);
    }

    public JAXBElement<com.rackspace.api.idm.v1.User> toUserJaxbFromUser(
        User user) {
        if (user == null) {
            return null;
        }

        com.rackspace.api.idm.v1.User jaxbUser = objectFactory.createUser();
        jaxbUser.setUsername(user.getUsername());
        jaxbUser.setCustomerId(user.getCustomerId());
        jaxbUser.setRoles(rolesConverter.toRoleJaxbFromTenantRole(
            user.getRoles()).getValue());

        return objectFactory.createUser(jaxbUser);
    }

    public JAXBElement<com.rackspace.api.idm.v1.Racker> toRackerJaxbFromRacker(
        Racker racker) {
        if (racker == null) {
            return null;
        }

        com.rackspace.api.idm.v1.Racker jaxbRacker = objectFactory
            .createRacker();
        jaxbRacker.setUsername(racker.getRackerId());
        jaxbRacker.setRoles(rolesConverter.toRoleJaxbFromRoleString(
            racker.getRackerRoles()).getValue());

        return objectFactory.createRacker(jaxbRacker);
    }
}
