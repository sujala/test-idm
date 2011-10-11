package com.rackspace.idm.api.converter;

import java.util.List;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;

import org.apache.commons.lang.StringUtils;

import com.rackspace.api.idm.v1.ObjectFactory;
import com.rackspace.api.idm.v1.Role;
import com.rackspace.idm.domain.entity.Racker;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.entity.Users;

public class UserConverter {

    private final ObjectFactory objectFactory = new ObjectFactory();

    private final RolesConverter rolesConverter;
    
    public UserConverter(RolesConverter rolesConverter) {
    	this.rolesConverter = rolesConverter;
    }
    
    public User toUserDO(com.rackspace.api.idm.v1.User jaxbUser) {
        User user = new User();
        
        user.setId(jaxbUser.getId());

        user.setCountry(jaxbUser.getCountry());
        user.setTimeZone(jaxbUser.getTimeZone());
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
        user.setSoftDeleted(jaxbUser.isSoftDeleted());
        user.setMaxLoginFailuresExceded(jaxbUser.isMaxLoginFailuresExceded());
        user.setUsername(jaxbUser.getUsername());

        if (jaxbUser.getPasswordCredentials()!= null
            && !StringUtils.isBlank(jaxbUser.getPasswordCredentials().getCurrentPassword().getPassword())) {
            user.setPassword(jaxbUser.getPasswordCredentials().getCurrentPassword().getPassword());
        }

        if (jaxbUser.getSecret() != null
            && !StringUtils.isBlank(jaxbUser.getSecret().getSecretQuestion())
            && !StringUtils.isBlank(jaxbUser.getSecret().getSecretAnswer())) {
            user.setSecretQuestion(jaxbUser.getSecret().getSecretQuestion());
            user.setSecretAnswer(jaxbUser.getSecret().getSecretAnswer());
        }

        return user;
    }

    public com.rackspace.api.idm.v1.Users toUserListJaxb(Users users) {

        if (users == null || users.getUsers() == null) {
            return null;
        }

        com.rackspace.api.idm.v1.Users userlist = objectFactory.createUsers();

        for (User user : users.getUsers()) {
            userlist.getUsers().add(
                toUserJaxbWithoutAnyAdditionalElements(user));
        }

        userlist.setLimit(users.getLimit());
        userlist.setOffset(users.getOffset());
        userlist.setTotalRecords(users.getTotalRecords());

        return userlist;
    }

    public com.rackspace.api.idm.v1.Racker toRackerJaxb(String rackerId) {
        com.rackspace.api.idm.v1.Racker returnedRacker = objectFactory.createRacker();
        returnedRacker.setUsername(rackerId);
        return returnedRacker;
    }
    
    public com.rackspace.api.idm.v1.Racker toRackerJaxb(Racker racker) {
        com.rackspace.api.idm.v1.Racker returnedRacker = objectFactory.createRacker();
        returnedRacker.setId(racker.getRackerId());
        if (racker.getRackerRoles() != null && racker.getRackerRoles().size() > 0) {
            returnedRacker.setRoles(toRackerRolesJaxb(racker.getRackerRoles()));
        }
        return returnedRacker;
    }

    public com.rackspace.api.idm.v1.Roles toRackerRolesJaxb(
        List<String> roles) {
        com.rackspace.api.idm.v1.Roles returnedRoles = objectFactory
            .createRoles();
        for (String role : roles) {
            if (!StringUtils.isEmpty(role)) {
                Role rackerRole = objectFactory.createRole();
                rackerRole.setName(role);
                returnedRoles.getRoles().add(rackerRole);
            }
        }
        return returnedRoles;
    }

    public com.rackspace.api.idm.v1.User toUserJaxb(User user) {
        return toUserJaxb(user, true, true);
    }

    public com.rackspace.api.idm.v1.User toUserWithOnlySoftDeletedJaxb(User user) {
        com.rackspace.api.idm.v1.User returnedUser = objectFactory.createUser();
        returnedUser.setSoftDeleted(user.isSoftDeleted());
        return returnedUser;
    }

    public com.rackspace.api.idm.v1.User toUserJaxbWithoutAnyAdditionalElements(
        User user) {
        return toUserJaxb(user, false, false);
    }

    private com.rackspace.api.idm.v1.User toUserJaxb(User user,
        boolean includePassword, boolean includeSecret) {
        com.rackspace.api.idm.v1.User returnedUser = objectFactory.createUser();
        returnedUser.setId(user.getId());
        returnedUser.setCountry(user.getCountry());
        returnedUser.setTimeZone(user.getTimeZone());
        returnedUser.setCustomerId(user.getCustomerId());
        returnedUser.setDisplayName(user.getDisplayName());
        returnedUser.setEmail(user.getEmail());
        returnedUser.setEnabled(user.isEnabled());
        returnedUser.setFirstName(user.getFirstname());
        returnedUser.setLastName(user.getLastname());
        returnedUser.setMiddleName(user.getMiddlename());
        returnedUser.setPersonId(user.getPersonId());
        returnedUser.setPrefLanguage(user.getPreferredLang());
        returnedUser.setRegion(user.getRegion());
        returnedUser.setUsername(user.getUsername());
        returnedUser.setSoftDeleted(user.isSoftDeleted());
        returnedUser.setMaxLoginFailuresExceded(user.isMaxLoginFailuresExceded());

        try {
            if (user.getCreated() != null) {

                returnedUser.setCreated(DatatypeFactory.newInstance()
                    .newXMLGregorianCalendar(
                        user.getCreated().toGregorianCalendar()));
            }

            if (user.getUpdated() != null) {
                returnedUser.setUpdated(DatatypeFactory.newInstance()
                    .newXMLGregorianCalendar(
                        user.getUpdated().toGregorianCalendar()));
            }

        } catch (DatatypeConfigurationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        if (includePassword && user.getPasswordObj() != null
            && !StringUtils.isBlank(user.getPasswordObj().getValue())) {
            com.rackspace.api.idm.v1.UserPassword password = objectFactory.createUserPassword();
            com.rackspace.api.idm.v1.PasswordCredentials passwordCredentials = objectFactory.createPasswordCredentials();
            
            password.setPassword(user.getPasswordObj().getValue());
            passwordCredentials.setCurrentPassword(password);
            returnedUser.setPasswordCredentials(passwordCredentials);
        }

        if (includeSecret && !StringUtils.isBlank(user.getSecretAnswer())
            && !StringUtils.isBlank(user.getSecretQuestion())) {

            com.rackspace.api.idm.v1.Secret secret = objectFactory.createSecret();

            secret.setSecretAnswer(user.getSecretAnswer());
            secret.setSecretQuestion(user.getSecretQuestion());
            returnedUser.setSecret(secret);
        }

        return returnedUser;
    }

    public com.rackspace.api.idm.v1.User toUserJaxbFromUser(String username,
        String customerId) {
        com.rackspace.api.idm.v1.User returnedUser = objectFactory.createUser();
        returnedUser.setUsername(username);
        returnedUser.setCustomerId(customerId);

        return returnedUser;
    }
    
    public com.rackspace.api.idm.v1.User toUserJaxbFromUser(User user) {
    	if (user == null) {
    		return null;
    	}
    	
        com.rackspace.api.idm.v1.User jaxbUser = objectFactory.createUser();
        jaxbUser.setUsername(user.getUsername());
        jaxbUser.setCustomerId(user.getCustomerId());
        jaxbUser.setRoles(rolesConverter.toRoleJaxbFromTenantRole(user.getRoles()));

        return jaxbUser;
    }

    public com.rackspace.api.idm.v1.Racker toRackerJaxbFromRacker(Racker racker) {
    	if (racker == null) {
    		return null;
    	}
    	
        com.rackspace.api.idm.v1.Racker jaxbRacker = objectFactory.createRacker();
        jaxbRacker.setUsername(racker.getRackerId());
        jaxbRacker.setRoles(rolesConverter.toRoleJaxbFromRoleString(racker.getRackerRoles()));

        return jaxbRacker;
    }
}
