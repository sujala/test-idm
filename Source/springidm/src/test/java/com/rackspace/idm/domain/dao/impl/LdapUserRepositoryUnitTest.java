package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.exception.UserDisabledException;
import org.apache.commons.configuration.Configuration;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.*;

/**
 * Created by IntelliJ IDEA.
 * User: Hector
 * Date: 3/20/12
 * Time: 3:29 PM
 */
public class LdapUserRepositoryUnitTest {

    LdapUserRepository ldapUserRepository;
    LdapUserRepository ldapUserRepositorySpy;

    @Before
    public void setUp() throws Exception {
        ldapUserRepository = new LdapUserRepository(mock(LdapConnectionPools.class),mock(Configuration.class));
        ldapUserRepositorySpy = spy(ldapUserRepository);
    }

    @Test
    public void updateUserById_callsGetUserById() throws Exception {
        User user = new User();
        user.setUsername("user");
        user.setId("id");
        doNothing().when(ldapUserRepositorySpy).updateUser(any(User.class),any(User.class),anyBoolean());
        doReturn(user).when(ldapUserRepositorySpy).getUserById("id");
        ldapUserRepositorySpy.updateUserById(user, false);
        verify(ldapUserRepositorySpy).getUserById("id");
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
}
