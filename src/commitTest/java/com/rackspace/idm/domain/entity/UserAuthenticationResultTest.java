package com.rackspace.idm.domain.entity;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Created with IntelliJ IDEA.
 * User: ryan5034
 * Date: 7/10/12
 * Time: 1:52 PM
 * To change this template use File | Settings | File Templates.
 */
public class UserAuthenticationResultTest {

    UserAuthenticationResult userAuthenticationResult;
    User user = new User();

    @Before
    public void setUp() throws Exception {
        userAuthenticationResult = new UserAuthenticationResult(user,true);
    }

    @Test
    public void equals_objectClassesNotEqual_returnsFalse() throws Exception {
        assertThat("equals",userAuthenticationResult.equals(new ClientAuthenticationResult(null,true)),equalTo(false));
    }

    @Test
    public void equals_nullObject_returnsFalse() throws Exception {
        assertThat("equals",userAuthenticationResult.equals(null),equalTo(false));
    }

    @Test
    public void equals_nullUserButOtherUserNotNull_returnsFalse() throws Exception {
        UserAuthenticationResult userAuthenticationResult1 = new UserAuthenticationResult(null,true);
        assertThat("equals",userAuthenticationResult1.equals(userAuthenticationResult),equalTo(false));
    }

    @Test
    public void equals_bothUsersNull_returnsTrue() throws Exception {
        UserAuthenticationResult userAuthenticationResult1 = new UserAuthenticationResult(null,true);
        UserAuthenticationResult userAuthenticationResult2 = new UserAuthenticationResult(null,true);
        assertThat("equals",userAuthenticationResult1.equals(userAuthenticationResult2),equalTo(true));
    }

    @Test
    public void equals_usersExistButNotTheSame_returnsFalse() throws Exception {
        User user1 = new User();
        user1.setId("123");
        UserAuthenticationResult userAuthenticationResult1 = new UserAuthenticationResult(user1,true);
        assertThat("equals",userAuthenticationResult.equals(userAuthenticationResult1),equalTo(false));
    }

    @Test
    public void equals_objectsEqual_returnsTrue() throws Exception {
      UserAuthenticationResult userAuthenticationResult1 = new UserAuthenticationResult(user,true);
      assertThat("equals",userAuthenticationResult.equals(userAuthenticationResult1),equalTo(true));
    }

    @Test
    public void toString_containsAuthenticated() throws Exception {
        userAuthenticationResult = new UserAuthenticationResult(user, false);
        assertThat("authenticationResult toString", userAuthenticationResult.toString(), containsString("false"));
    }
}
