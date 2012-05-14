package com.rackspace.idm.domain.service.impl;

import com.rackspace.idm.domain.dao.UserDao;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.exception.BadRequestException;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by IntelliJ IDEA.
 * User: Hector
 * Date: 12/1/11
 * Time: 11:26 AM
 */
public class DefaultUserServiceTest {

    DefaultUserService defaultUserService;
    UserDao userDao = mock(UserDao.class);

    @Before
    public void setUp() throws Exception {
        defaultUserService = new DefaultUserService(userDao,null,null,null,null,null,null);
    }

    @Test
    public void userExistsById_inMigration_returnsFalse() throws Exception {
        User user = new User();
        user.setInMigration(true);
        when(userDao.getUserById("test")).thenReturn(user);
        boolean exists = defaultUserService.userExistsById("test");
        assertThat("exists", exists, equalTo(false));
    }

    @Test
    public void userExistsById_inMigrationFalse_returnsTrue() throws Exception {
        User user = new User();
        user.setInMigration(false);
        when(userDao.getUserById("test")).thenReturn(user);
        boolean exists = defaultUserService.userExistsById("test");
        assertThat("exists", exists, equalTo(true));
    }

    @Test
    public void userExistsById_inMigrationIsNull_returnsTrue() throws Exception {
        User user = new User();
        user.setInMigration(null);
        when(userDao.getUserById("test")).thenReturn(user);
        boolean exists = defaultUserService.userExistsById("test");
        assertThat("exists", exists, equalTo(true));
    }

    @Test
    public void userExistsById_userIsNull_returnsFalse() throws Exception {
        when(userDao.getUserById("test")).thenReturn(null);
        boolean exists = defaultUserService.userExistsById("test");
        assertThat("exists", exists, equalTo(false));
    }

    @Test
    public void userExistsByUsername_inMigration_returnsFalse() throws Exception {
        User user = new User();
        user.setInMigration(true);
        when(userDao.getUserByUsername("test")).thenReturn(user);
        boolean exists = defaultUserService.userExistsByUsername("test");
        assertThat("exists", exists, equalTo(false));
    }

    @Test
    public void userExistsByUsername_inMigrationFalse_returnsTrue() throws Exception {
        User user = new User();
        user.setInMigration(false);
        when(userDao.getUserByUsername("test")).thenReturn(user);
        boolean exists = defaultUserService.userExistsByUsername("test");
        assertThat("exists", exists, equalTo(true));
    }

    @Test
    public void userExistsByUsername_inMigrationIsNull_returnsTrue() throws Exception {
        User user = new User();
        user.setInMigration(null);
        when(userDao.getUserByUsername("test")).thenReturn(user);
        boolean exists = defaultUserService.userExistsByUsername("test");
        assertThat("exists", exists, equalTo(true));
    }

    @Test
    public void userExistsByUsername_userIsNull_returnsFalse() throws Exception {
        when(userDao.getUserByUsername("test")).thenReturn(null);
        boolean exists = defaultUserService.userExistsByUsername("test");
        assertThat("exists", exists, equalTo(false));
    }

    @Test
    public void validateMossoId_callsUserDAO_getUserByMossoId() throws Exception {
        defaultUserService.validateMossoId(1);
        verify(userDao).getUserByMossoId(1);
    }

    @Test(expected = BadRequestException.class)
    public void validateMossoId_withExistingUserWithMossoId_throwsBadRequestException() throws Exception {
        User testUser = new User("testUser");
        testUser.setMossoId(1);
        when(userDao.getUserByMossoId(1)).thenReturn(testUser);
        defaultUserService.validateMossoId(1);
    }

    @Test
    public void validateMossoId_withExistingUserWithMossoId_throwsBadRequestException_returnsCorrectMessage() throws Exception {
        User testUser = new User("testUser");
        testUser.setMossoId(1);
        when(userDao.getUserByMossoId(1)).thenReturn(testUser);
        try{
        defaultUserService.validateMossoId(1);
        }catch (Exception e){
            assertThat("exception message", e.getMessage(), Matchers.equalTo("User with Mosso Account ID: 1 already exists."));
        }
    }

    @Test
    public void userExistsById_callsUserDao_getUserById() throws Exception {
        defaultUserService.userExistsById("id");
        verify(userDao).getUserById("id");
    }

    @Test
    public void userExistsByUsername_callsUserDao_getUserByUsername() throws Exception {
        defaultUserService.userExistsByUsername("id");
        verify(userDao).getUserByUsername("id");
    }

    @Test
    public void updateUserById_callsUserDaoUpdateByUd() throws Exception {
        User user = new User();
        user.setUsername("user");
        user.setId("id");
        defaultUserService.updateUserById(user, false );
        verify(userDao).updateUserById(user,false);
    }
}
