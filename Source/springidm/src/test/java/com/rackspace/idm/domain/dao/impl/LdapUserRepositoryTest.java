package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.audit.Audit;
import com.rackspace.idm.domain.entity.Racker;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.exception.UserDisabledException;
import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.Modification;
import org.apache.commons.configuration.Configuration;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.*;

/**
 * Created by IntelliJ IDEA.
 * User: Hector
 * Date: 3/20/12
 * Time: 3:29 PM
 */
public class LdapUserRepositoryTest {

    LdapUserRepository ldapUserRepository;
    LdapUserRepository spy;

    @Before
    public void setUp() throws Exception {
        ldapUserRepository = new LdapUserRepository(mock(LdapConnectionPools.class),mock(Configuration.class));
        spy = spy(ldapUserRepository);
    }

    @Test (expected = IllegalArgumentException.class)
    public void addRacker_userIsNull_throwsIllegalArgument() throws Exception {
        ldapUserRepository.addRacker(null);
    }

    @Test
    public void addRacker_callsGetRackerAddAttributes() throws Exception {
        Racker racker = new Racker();
        racker.setRackerId("rackerId");
        doNothing().when(spy).addEntry(anyString(), any(Attribute[].class), any(Audit.class));
        spy.addRacker(racker);
        verify(spy).getRackerAddAtrributes(racker);
    }

    @Test
    public void addRacker_callsAddEntry() throws Exception {
        Racker racker = new Racker();
        racker.setRackerId("rackerId");
        doNothing().when(spy).addEntry(anyString(), any(Attribute[].class), any(Audit.class));
        spy.addRacker(racker);
        verify(spy).addEntry(anyString(), any(Attribute[].class), any(Audit.class));
    }

    @Test
    public void updateUserById_callsGetUserById() throws Exception {
        User user = new User();
        user.setUsername("user");
        user.setId("id");
        doNothing().when(spy).updateUser(any(User.class),any(User.class),anyBoolean());
        doReturn(user).when(spy).getUserById("id");
        spy.updateUserById(user, false);
        verify(spy).getUserById("id");
    }

    @Test
    public void validateUserStatus_userIsDisabled_throwsUserDisabledException() throws Exception {
        try{
            User user = new User();
            user.setEnabled(false);
            user.setUsername("rclements");
            ldapUserRepository.validateUserStatus(user,true);
        }catch(UserDisabledException ex){
            assertThat("message",ex.getMessage(),equalTo("User 'rclements' is disabled."));
        }
    }

    @Test
    public void checkForApiKeyModification_newKeyBlank_addsDeleteKeyModification() throws Exception {
        User oldUser = new User();
        oldUser.setApiKey("hello!");
        User newUser = new User();
        newUser.setApiKey("");
        List<Modification> mod = new ArrayList<Modification>();
        ldapUserRepository.checkForApiKeyModification(oldUser,newUser,null,mod);
        assertThat("modification type",mod.get(0).getModificationType().getName(),equalTo("DELETE"));
    }

    @Test
    public void getModifications_ListsUserName() throws Exception {
        User oldUser = new User();
        oldUser.setUsername("orignal");
        User newUser = new User();
        newUser.setUsername("innovation");
        List<Modification> mod = ldapUserRepository.getModifications(oldUser, newUser, false);
        assertThat("modified attribute", mod.get(0).getAttributeName(), equalTo("uid"));
    }

}
