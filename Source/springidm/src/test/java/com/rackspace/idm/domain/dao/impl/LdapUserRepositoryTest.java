package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.audit.Audit;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.exception.DuplicateUsernameException;
import com.rackspace.idm.exception.NotFoundException;
import com.rackspace.idm.exception.StalePasswordException;
import com.rackspace.idm.exception.UserDisabledException;
import com.unboundid.ldap.sdk.*;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.migrate.ldapjdk.*;
import com.unboundid.ldap.sdk.migrate.ldapjdk.LDAPException;
import org.apache.commons.configuration.Configuration;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.powermock.api.mockito.PowerMockito;

import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.any;
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

    @Test (expected = IllegalArgumentException.class)
    public void addUser_userIsNull_throwsIllegalArgument() throws Exception {
        ldapUserRepository.addUser(null);
    }

    @Test (expected = IllegalStateException.class)
    public void addUser_getAddAttributes_throwsGeneralSecurityException() throws Exception {
        User user = new User();
        user.setId("id");
        doThrow(new GeneralSecurityException()).when(spy).getAddAttributes(any(User.class));
        spy.addUser(user);
    }

    @Test (expected = IllegalStateException.class)
    public void addUser_getAddAttributes_throwsInvalidCipherTextException() throws Exception {
        User user = new User();
        user.setId("id");
        doThrow(new InvalidCipherTextException()).when(spy).getAddAttributes(any(User.class));
        spy.addUser(user);
    }

    @Test
    public void addUser_callsAddEntry() throws Exception {
        User user = new User();
        user.setId("id");
        doReturn(new Attribute[0]).when(spy).getAddAttributes(user);
        doNothing().when(spy).addEntry(anyString(), any(Attribute[].class), any(Audit.class));
        spy.addUser(user);
        verify(spy).addEntry(anyString(), any(Attribute[].class), any(Audit.class));
    }

    @Test (expected = IllegalArgumentException.class)
    public void authenticate_usernameIsBlank_throwsIllegalArgument() throws Exception {
        ldapUserRepository.authenticate("", null);
    }

    @Test
    public void authenticate_callsGetUserByUsername() throws Exception {
        doReturn(new User()).when(spy).getUserByUsername("username");
        doReturn(new UserAuthenticationResult(new User(), false)).when(spy).authenticateByPassword(any(User.class), anyString());
        spy.authenticate("username", "password");
        verify(spy).getUserByUsername("username");
    }

    @Test
    public void authenticate_callsAuthenticateByPassword() throws Exception {
        User user = new User();
        doReturn(user).when(spy).getUserByUsername("username");
        doReturn(new UserAuthenticationResult(new User(), false)).when(spy).authenticateByPassword(user, "password");
        spy.authenticate("username", "password");
        verify(spy).authenticateByPassword(user, "password");
    }

    @Test (expected = IllegalArgumentException.class)
    public void authenticateByAPIKey_usernameIsBlank_throwsIllegalArgument() throws Exception {
        ldapUserRepository.authenticateByAPIKey("", "apiKey");
    }

    @Test
    public void authenticateByAPIKey_callsGetUserByUsername() throws Exception {
        doReturn(new User()).when(spy).getUserByUsername("username");
        doReturn(new UserAuthenticationResult(new User(), false)).when(spy).authenticateUserByApiKey(any(User.class), anyString());
        spy.authenticateByAPIKey("username", "apiKey");
        verify(spy).getUserByUsername("username");
    }

    @Test
    public void authenticateByAPIKey_callsAuthenticateUserByApiKey() throws Exception {
        User user = new User();
        doReturn(user).when(spy).getUserByUsername("username");
        doReturn(new UserAuthenticationResult(new User(), false)).when(spy).authenticateUserByApiKey(user, "apiKey");
        spy.authenticateByAPIKey("username", "apiKey");
        verify(spy).authenticateUserByApiKey(user, "apiKey");
    }

    @Test (expected = IllegalArgumentException.class)
    public void deleteRacker_rackerIdIsBlank_throwsIllegalArgumentException() throws Exception {
        ldapUserRepository.deleteRacker(null);
    }

    @Test (expected = NotFoundException.class)
    public void deleteRacker_rackerIsNull_throwsNotFoundException() throws Exception {
        doReturn(null).when(spy).getRackerByRackerId("rackerId");
        spy.deleteRacker("rackerId");
    }

    @Test
    public void deleteRacker_callsDeleteEntryAndSubtree() throws Exception {
        Racker racker = new Racker();
        racker.setUniqueId("uniqueId");
        doReturn(racker).when(spy).getRackerByRackerId("rackerId");
        doNothing().when(spy).deleteEntryAndSubtree(eq("uniqueId"), any(Audit.class));
        spy.deleteRacker("rackerId");
        verify(spy).deleteEntryAndSubtree(eq("uniqueId"), any(Audit.class));
    }

    @Test
    public void deleteUser_withUserParameter_callsDeleteEntryAndSubtree() throws Exception {
        User user = new User();
        user.setUsername("username");
        user.setUniqueId("uniqueId");
        doNothing().when(spy).deleteEntryAndSubtree(eq("uniqueId"), any(Audit.class));
        spy.deleteUser(user);
        verify(spy).deleteEntryAndSubtree(eq("uniqueId"), any(Audit.class));
    }

    @Test (expected = IllegalArgumentException.class)
    public void deleteUser_usernameIsBlank_throwsIllegalArgument() throws Exception {
        ldapUserRepository.deleteUser("");
    }

    @Test (expected = NotFoundException.class)
    public void deleteUser_userNotFound_throwsNotFoundException() throws Exception {
        doReturn(null).when(spy).getUserByUsername("username");
        spy.deleteUser("username");
    }

    @Test
    public void deleteUser_withUsernameParameter_callsDeleteEntryAndSubtree() throws Exception {
        User user = new User();
        user.setUniqueId("uniqueId");
        doReturn(user).when(spy).getUserByUsername("username");
        doNothing().when(spy).deleteEntryAndSubtree(eq("uniqueId"), any(Audit.class));
        spy.deleteUser("username");
        verify(spy).deleteEntryAndSubtree(eq("uniqueId"), any(Audit.class));
    }

    @Test (expected = IllegalArgumentException.class)
    public void getGroupIdsForUser_usernameIsBlank_throwsIllegalArgument() throws Exception {
        ldapUserRepository.getGroupIdsForUser("");
    }

    @Test
    public void getGroupIdsForUser_callsGetSingleEntry() throws Exception {
        doReturn(null).when(spy).getSingleEntry(anyString(), any(SearchScope.class), any(Filter.class), anyString());
        spy.getGroupIdsForUser("username");
        verify(spy).getSingleEntry(anyString(), any(SearchScope.class), any(Filter.class), anyString());
    }

    @Test
    public void getGroupIdsForUser_searchResultEntryNotNull_returnsGroupIds() throws Exception {
        Attribute attribute = new Attribute("rsGroupDN", "groupId");
        Attribute[] attributeArray = {attribute};
        SearchResultEntry searchResultEntry = new SearchResultEntry("1", attributeArray, new Control[0]);
        doReturn(searchResultEntry).when(spy).getSingleEntry(anyString(), any(SearchScope.class), any(Filter.class), anyString());
        String[] result = spy.getGroupIdsForUser("username");
        assertThat("group id", result[0], equalTo("groupId"));
    }

    @Test
    public void getGroupIdsForUser_searchResultEntryIsNull_returnsNull() throws Exception {
        Attribute attribute = new Attribute("notFound", "groupId");
        Attribute[] attributeArray = {attribute};
        SearchResultEntry searchResultEntry = new SearchResultEntry("1", attributeArray, new Control[0]);
        doReturn(searchResultEntry).when(spy).getSingleEntry(anyString(), any(SearchScope.class), any(Filter.class), anyString());
        String[] result = spy.getGroupIdsForUser("username");
        assertThat("group id", result, equalTo(null));
    }

    @Test (expected = IllegalArgumentException.class)
    public void getRackerByRackerId_rackerIdIsBlank_throwsIllegalArgument() throws Exception {
        ldapUserRepository.getRackerByRackerId("   ");
    }

    @Test
    public void getRackerByRackerId_callsGetSingleEntry() throws Exception {
        doReturn(null).when(spy).getSingleEntry(anyString(), any(SearchScope.class), any(Filter.class));
        spy.getRackerByRackerId("rackerId");
        verify(spy).getSingleEntry(anyString(), any(SearchScope.class), any(Filter.class));
    }

    @Test
    public void getRackerByRackerId_searchResultEntryNotNull_returnsRacker() throws Exception {
        Attribute attribute = new Attribute("rackerId", "rackerId");
        Attribute[] attributeArray = {attribute};
        SearchResultEntry searchResultEntry = new SearchResultEntry("uniqueId", attributeArray, new Control[0]);
        doReturn(searchResultEntry).when(spy).getSingleEntry(anyString(), any(SearchScope.class), any(Filter.class));
        Racker result = spy.getRackerByRackerId("rackerId");
        assertThat("unique id", result.getUniqueId(), equalTo("uniqueId"));
        assertThat("racker id", result.getRackerId(), equalTo("rackerId"));
    }

    @Test
    public void getRackerByRackerId_searchResultIsNull_returnsNull() throws Exception {
        doReturn(null).when(spy).getSingleEntry(anyString(), any(SearchScope.class), any(Filter.class));
        Racker result = spy.getRackerByRackerId("rackerId");
        assertThat("racker", result, equalTo(null));
    }

    @Test (expected = IllegalArgumentException.class)
    public void getUserByCustomerIdAndUsername_customerIdIsBlank_throwsIllegalArgument() throws Exception {
        ldapUserRepository.getUserByCustomerIdAndUsername("   ", "username");
    }

    @Test (expected = IllegalArgumentException.class)
    public void getUserByCustomerIdAndUsername_usernameIsBlank_throwsIllegalArgument() throws Exception {
        ldapUserRepository.getUserByCustomerIdAndUsername("customerId", "     ");
    }

    @Test
    public void getUserByCustomerIdAndUsername_callsGetSingleUser() throws Exception {
        doReturn(null).when(spy).getSingleUser(any(Filter.class), any(String[].class));
        spy.getUserByCustomerIdAndUsername("customerId", "username");
        verify(spy).getSingleUser(any(Filter.class), any(String[].class));
    }

    @Test
    public void getUserByCustomerIdAndUsername_foundUser_returnsUser() throws Exception {
        User user = new User();
        doReturn(user).when(spy).getSingleUser(any(Filter.class), any(String[].class));
        User result = spy.getUserByCustomerIdAndUsername("customerId", "username");
        assertThat("user", result, equalTo(user));
    }

    @Test (expected = IllegalArgumentException.class)
    public void getUserById_idIsBlank_throwsIllegalArgument() throws Exception {
        ldapUserRepository.getUserById("");
    }

    @Test
    public void getUserById_callsGetSingleUser() throws Exception {
        doReturn(null).when(spy).getSingleUser(any(Filter.class), any(String[].class));
        spy.getUserById("id");
        verify(spy).getSingleUser(any(Filter.class), any(String[].class));
    }

    @Test
    public void getUserById_foundUser_returnsUser() throws Exception {
        User user = new User();
        doReturn(user).when(spy).getSingleUser(any(Filter.class), any(String[].class));
        User result = spy.getUserById("id");
        assertThat("user", result, equalTo(user));
    }

    @Test
    public void getUsersByMossoId_callsGetMultipleUsers() throws Exception {
        doReturn(null).when(spy).getMultipleUsers(any(Filter.class), any(String[].class), anyInt(), anyInt());
        spy.getUsersByMossoId(1);
        verify(spy).getMultipleUsers(any(Filter.class), any(String[].class), anyInt(), anyInt());
    }

    @Test
    public void getUsersByMossoId_foundUsers_returnUsers() throws Exception {
        Users users = new Users();
        doReturn(users).when(spy).getMultipleUsers(any(Filter.class), any(String[].class), anyInt(), anyInt());
        Users result = spy.getUsersByMossoId(1);
        assertThat("users", result, equalTo(users));
    }

    @Test (expected = IllegalArgumentException.class)
    public void getUsersByNastId_nastIdIsBlank_throwsIllegalArgument() throws Exception {
        ldapUserRepository.getUsersByNastId("");
    }

    @Test
    public void getUsersByNastId_callsGetMultipleUsers() throws Exception {
        doReturn(null).when(spy).getMultipleUsers(any(Filter.class), any(String[].class), anyInt(), anyInt());
        spy.getUsersByNastId("nastId");
        verify(spy).getMultipleUsers(any(Filter.class), any(String[].class), anyInt(), anyInt());
    }

    @Test
    public void getUsersByNastId_foundUsers_returnUsers() throws Exception {
        Users users = new Users();
        doReturn(users).when(spy).getMultipleUsers(any(Filter.class), any(String[].class), anyInt(), anyInt());
        Users result = spy.getUsersByNastId("nastId");
        assertThat("users", result, equalTo(users));
    }

    @Test (expected = IllegalArgumentException.class)
    public void getUsersByDomainId_domainIdIsBlank_throwsIllegalArgument() throws Exception {
        ldapUserRepository.getUsersByDomainId("");
    }

    @Test
    public void getUsersByDomainId_callsGetMultipleUsers() throws Exception {
        doReturn(null).when(spy).getMultipleUsers(any(Filter.class), any(String[].class), anyInt(), anyInt());
        spy.getUsersByDomainId("domainId");
        verify(spy).getMultipleUsers(any(Filter.class), any(String[].class), anyInt(), anyInt());
    }

    @Test
    public void getUsersByDomainId_foundUsers_returnUsers() throws Exception {
        Users users = new Users();
        doReturn(users).when(spy).getMultipleUsers(any(Filter.class), any(String[].class), anyInt(), anyInt());
        Users result = spy.getUsersByDomainId("domainId");
        assertThat("users", result, equalTo(users));
    }

    @Test (expected = IllegalArgumentException.class)
    public void getUserByRPN_rpnIsBlank_throwsIllegalArgument() throws Exception {
        ldapUserRepository.getUserByRPN("");
    }

    @Test
    public void getUserByRPN_callsGetSingleUser() throws Exception {
        doReturn(null).when(spy).getSingleUser(any(Filter.class), any(String[].class));
        spy.getUserByRPN("rpn");
        verify(spy).getSingleUser(any(Filter.class), any(String[].class));
    }

    @Test
    public void getUserByRPN_foundUser_returnsUser() throws Exception {
        User user = new User();
        doReturn(user).when(spy).getSingleUser(any(Filter.class), any(String[].class));
        User result = spy.getUserByRPN("rpn");
        assertThat("user", result, equalTo(user));
    }

    @Test (expected = IllegalArgumentException.class)
    public void getUserBySecureId_secureIdIsBlank_throwsIllegalArgument() throws Exception {
        ldapUserRepository.getUserBySecureId(null);
    }

    @Test
    public void getUserBySecureId_callsGetSingleUser() throws Exception {
        doReturn(null).when(spy).getSingleUser(any(Filter.class), any(String[].class));
        spy.getUserBySecureId("secureId");
        verify(spy).getSingleUser(any(Filter.class), any(String[].class));
    }

    @Test
    public void getUserBySecureId_foundUser_returnsUser() throws Exception {
        User user = new User();
        doReturn(user).when(spy).getSingleUser(any(Filter.class), any(String[].class));
        User result = spy.getUserBySecureId("secureId");
        assertThat("user", result, equalTo(user));
    }

    @Test (expected = IllegalArgumentException.class)
    public void getUserByUsername_usernameIsBlank_throwsIllegalArgument() throws Exception {
        ldapUserRepository.getUserByUsername("   ");
    }

    @Test
    public void getUserByUsername_callsGetSingleUser() throws Exception {
        doReturn(null).when(spy).getSingleUser(any(Filter.class), any(String[].class));
        spy.getUserByUsername("username");
        verify(spy).getSingleUser(any(Filter.class), any(String[].class));
    }

    @Test
    public void getUserByUsername_foundUser_returnsUser() throws Exception {
        User user = new User();
        doReturn(user).when(spy).getSingleUser(any(Filter.class), any(String[].class));
        User result = spy.getUserByUsername("username");
        assertThat("user", result, equalTo(user));
    }

    @Test
    public void getAllUsers_filterParamsIsNull_callsGetMultipleUsers() throws Exception {
        doReturn(null).when(spy).getMultipleUsers(any(Filter.class), any(String[].class), anyInt(), anyInt());
        spy.getAllUsers(null, 1, 1);
        verify(spy).getMultipleUsers(any(Filter.class), any(String[].class), anyInt(), anyInt());
    }

    @Test
    public void getAllUsers_filterParamHasRCN_addsAttribute() throws Exception {
        ArgumentCaptor<Filter> argumentCaptor = ArgumentCaptor.forClass(Filter.class);
        FilterParam filterParam = new FilterParam();
        filterParam.setParam(FilterParam.FilterParamName.RCN);
        filterParam.setValue("rcn");
        FilterParam[] filterParamArray = {filterParam};
        doReturn(null).when(spy).getMultipleUsers(any(Filter.class), any(String[].class), anyInt(), anyInt());
        spy.getAllUsers(filterParamArray, 1, 1);
        verify(spy).getMultipleUsers(argumentCaptor.capture(), any(String[].class), anyInt(), anyInt());
        assertThat("atttribute", argumentCaptor.getValue().getComponents()[1].toNormalizedString(), containsString("=rcn"));
    }

    @Test
    public void getAllUsers_filterParamHasUSERNAME_addsAttribute() throws Exception {
        ArgumentCaptor<Filter> argumentCaptor = ArgumentCaptor.forClass(Filter.class);
        FilterParam filterParam = new FilterParam();
        filterParam.setParam(FilterParam.FilterParamName.USERNAME);
        filterParam.setValue("username");
        FilterParam[] filterParamArray = {filterParam};
        doReturn(null).when(spy).getMultipleUsers(any(Filter.class), any(String[].class), anyInt(), anyInt());
        spy.getAllUsers(filterParamArray, 1, 1);
        verify(spy).getMultipleUsers(argumentCaptor.capture(), any(String[].class), anyInt(), anyInt());
        assertThat("atttribute", argumentCaptor.getValue().getComponents()[1].toNormalizedString(), containsString("=username"));
    }

    @Test
    public void getAllUsers_filterParamHasDOMAINID_addsAttribute() throws Exception {
        ArgumentCaptor<Filter> argumentCaptor = ArgumentCaptor.forClass(Filter.class);
        FilterParam filterParam = new FilterParam();
        filterParam.setParam(FilterParam.FilterParamName.DOMAIN_ID);
        filterParam.setValue("domainid");
        FilterParam[] filterParamArray = {filterParam};
        doReturn(null).when(spy).getMultipleUsers(any(Filter.class), any(String[].class), anyInt(), anyInt());
        spy.getAllUsers(filterParamArray, 1, 1);
        verify(spy).getMultipleUsers(argumentCaptor.capture(), any(String[].class), anyInt(), anyInt());
        assertThat("atttribute", argumentCaptor.getValue().getComponents()[1].toNormalizedString(), containsString("=domainid"));
    }

    @Test
    public void getAllUsers_filterParamHasGROUPID_addsAttribute() throws Exception {
        ArgumentCaptor<Filter> argumentCaptor = ArgumentCaptor.forClass(Filter.class);
        FilterParam filterParam = new FilterParam();
        filterParam.setParam(FilterParam.FilterParamName.GROUP_ID);
        filterParam.setValue("groupid");
        FilterParam[] filterParamArray = {filterParam};
        doReturn(null).when(spy).getMultipleUsers(any(Filter.class), any(String[].class), anyInt(), anyInt());
        spy.getAllUsers(filterParamArray, 1, 1);
        verify(spy).getMultipleUsers(argumentCaptor.capture(), any(String[].class), anyInt(), anyInt());
        assertThat("atttribute", argumentCaptor.getValue().getComponents()[1].toNormalizedString(), containsString("=groupid"));
    }

    @Test
    public void getAllUsers_filterParamHasINMIGRATION_addsAttribute() throws Exception {
        ArgumentCaptor<Filter> argumentCaptor = ArgumentCaptor.forClass(Filter.class);
        FilterParam filterParam = new FilterParam();
        filterParam.setParam(FilterParam.FilterParamName.IN_MIGRATION);
        FilterParam[] filterParamArray = {filterParam};
        doReturn(null).when(spy).getMultipleUsers(any(Filter.class), any(String[].class), anyInt(), anyInt());
        spy.getAllUsers(filterParamArray, 1, 1);
        verify(spy).getMultipleUsers(argumentCaptor.capture(), any(String[].class), anyInt(), anyInt());
        assertThat("atttribute", argumentCaptor.getValue().getComponents()[1].toNormalizedString(), containsString("=true"));
    }

    @Test
    public void getAllUsers_filterParamHasMIGRATED_addsAttribute() throws Exception {
        ArgumentCaptor<Filter> argumentCaptor = ArgumentCaptor.forClass(Filter.class);
        FilterParam filterParam = new FilterParam();
        filterParam.setParam(FilterParam.FilterParamName.MIGRATED);
        FilterParam[] filterParamArray = {filterParam};
        doReturn(null).when(spy).getMultipleUsers(any(Filter.class), any(String[].class), anyInt(), anyInt());
        spy.getAllUsers(filterParamArray, 1, 1);
        verify(spy).getMultipleUsers(argumentCaptor.capture(), any(String[].class), anyInt(), anyInt());
        assertThat("atttribute", argumentCaptor.getValue().getComponents()[1].toNormalizedString(), containsString("=false"));
    }

    @Test
    public void getAllUsers_filterParamDoesNotMatchAny_doesNotAddAttribute() throws Exception {
        ArgumentCaptor<Filter> argumentCaptor = ArgumentCaptor.forClass(Filter.class);
        FilterParam filterParam = new FilterParam();
        filterParam.setParam(FilterParam.FilterParamName.ROLE_NAME);
        filterParam.setValue("role");
        FilterParam[] filterParamArray = {filterParam};
        doReturn(null).when(spy).getMultipleUsers(any(Filter.class), any(String[].class), anyInt(), anyInt());
        spy.getAllUsers(filterParamArray, 1, 1);
        verify(spy).getMultipleUsers(argumentCaptor.capture(), any(String[].class), anyInt(), anyInt());
        assertThat("atttribute", argumentCaptor.getValue().getComponents().length, equalTo(0));
    }

    @Test
    public void isUsernameUnique_callsSingleEntry() throws Exception {
        doReturn(null).when(spy).getSingleEntry(anyString(), any(SearchScope.class), any(Filter.class));
        spy.isUsernameUnique("username");
        verify(spy).getSingleEntry(anyString(), any(SearchScope.class), any(Filter.class));
    }

    @Test
    public void isUsernameUnique_callsGetUser() throws Exception {
        User user = new User();
        SearchResultEntry searchResultEntry = new SearchResultEntry("user", new Attribute[0], new Control[0]);
        doReturn(user).when(spy).getUser(searchResultEntry);
        doReturn(searchResultEntry).when(spy).getSingleEntry(anyString(), any(SearchScope.class), any(Filter.class));
        spy.isUsernameUnique("username");
        verify(spy).getUser(searchResultEntry);
    }

    @Test (expected = IllegalStateException.class)
    public void isUsernameUnique_getUser_throwsGeneralSecurity() throws Exception {
        SearchResultEntry searchResultEntry = new SearchResultEntry("user", new Attribute[0], new Control[0]);
        doReturn(searchResultEntry).when(spy).getSingleEntry(anyString(), any(SearchScope.class), any(Filter.class));
        doThrow(new GeneralSecurityException()).when(spy).getUser(searchResultEntry);
        spy.isUsernameUnique("username");
    }

    @Test (expected = IllegalStateException.class)
    public void isUsernameUnique_getUser_throwsInvalidCipherText() throws Exception {
        SearchResultEntry searchResultEntry = new SearchResultEntry("user", new Attribute[0], new Control[0]);
        doReturn(searchResultEntry).when(spy).getSingleEntry(anyString(), any(SearchScope.class), any(Filter.class));
        doThrow(new InvalidCipherTextException()).when(spy).getUser(searchResultEntry);
        spy.isUsernameUnique("username");
    }

    @Test
    public void isUsernameUnique_foundUser_returnsFalse() throws Exception {
        User user = new User();
        SearchResultEntry searchResultEntry = new SearchResultEntry("user", new Attribute[0], new Control[0]);
        doReturn(user).when(spy).getUser(searchResultEntry);
        doReturn(searchResultEntry).when(spy).getSingleEntry(anyString(), any(SearchScope.class), any(Filter.class));
        boolean result = spy.isUsernameUnique("username");
        assertThat("boolean", result, equalTo(false));
    }

    @Test
    public void isUsernameUnique_userIsNull_returnsTrue() throws Exception {
        SearchResultEntry searchResultEntry = new SearchResultEntry("user", new Attribute[0], new Control[0]);
        doReturn(null).when(spy).getUser(searchResultEntry);
        doReturn(searchResultEntry).when(spy).getSingleEntry(anyString(), any(SearchScope.class), any(Filter.class));
        boolean result = spy.isUsernameUnique("username");
        assertThat("boolean", result, equalTo(true));
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

    @Test (expected = DuplicateUsernameException.class)
    public void updateUserById_usernameNotMatchAndNotUniqueUsername_throwsDuplicateUsername() throws Exception {
        User newUser = new User();
        newUser.setUsername("newUser");
        newUser.setId("id");
        User oldUser = new User();
        oldUser.setUsername("oldUser");
        doReturn(oldUser).when(spy).getUserById("id");
        doReturn(false).when(spy).isUsernameUnique("newUser");
        spy.updateUserById(newUser, false);
    }

    @Test
    public void updateUserById_usernameMatchAndNotUniqueUsername_callsUpdateUser() throws Exception {
        User newUser = new User();
        newUser.setUsername("newUser");
        newUser.setId("id");
        User oldUser = new User();
        oldUser.setUsername("newUser");
        doReturn(oldUser).when(spy).getUserById("id");
        doReturn(false).when(spy).isUsernameUnique("newUser");
        spy.updateUserById(newUser, false);
        verify(spy).updateUser(newUser, oldUser, false);
    }

    @Test
    public void updateUserById_usernameNotMatchAndUniqueUsername_callsUpdateUser() throws Exception {
        User newUser = new User();
        newUser.setUsername("newUser");
        newUser.setId("id");
        User oldUser = new User();
        oldUser.setUsername("oldUser");
        doReturn(oldUser).when(spy).getUserById("id");
        doReturn(true).when(spy).isUsernameUnique("newUser");
        doNothing().when(spy).updateUser(newUser, oldUser, false);
        spy.updateUserById(newUser, false);
        verify(spy).updateUser(newUser, oldUser, false);
    }

    @Test
    public void updateUserById_usernameMatchAndUniqueUsername_callsUpdateUser() throws Exception {
        User newUser = new User();
        newUser.setUsername("newUser");
        newUser.setId("id");
        User oldUser = new User();
        oldUser.setUsername("newUser");
        doReturn(oldUser).when(spy).getUserById("id");
        doReturn(true).when(spy).isUsernameUnique("newUser");
        spy.updateUserById(newUser, false);
        verify(spy).updateUser(newUser, oldUser, false);
    }

    @Test
    public void updateUser_callsUpdateUser() throws Exception {
        User newUser = new User();
        newUser.setUsername("username");
        User oldUser = new User();
        oldUser.setUsername("username");
        doReturn(oldUser).when(spy).getUserByUsername("username");
        spy.updateUser(newUser, false);
        verify(spy).updateUser(newUser, oldUser, false);
    }

    @Test
    public void updateUser_callsGetModification() throws Exception {
        User newUser = new User();
        newUser.setUsername("username");
        User oldUser = new User();
        oldUser.setUsername("username");
        spy.updateUser(newUser, oldUser, false);
        verify(spy).getModifications(oldUser, newUser, false);
    }

    @Test (expected = IllegalStateException.class)
    public void updateUser_getModifications_throwsGeneralSecurity() throws Exception {
        User newUser = new User();
        newUser.setUsername("newUser");
        User oldUser = new User();
        oldUser.setUsername("oldUser");
        doThrow(new GeneralSecurityException()).when(spy).getModifications(oldUser, newUser, false);
        spy.updateUser(newUser, oldUser, false);
    }

    @Test (expected = IllegalStateException.class)
    public void updateUser_getModifications_thrwosInvalidCipherText() throws Exception {
        User newUser = new User();
        newUser.setUsername("newUser");
        User oldUser = new User();
        oldUser.setUsername("oldUser");
        doThrow(new InvalidCipherTextException()).when(spy).getModifications(oldUser, newUser, false);
        spy.updateUser(newUser, oldUser, false);
    }

    @Test (expected = IllegalArgumentException.class)
    public void removeUsersFromClientGroup_groupIsNull_throwsIllegalArgument() throws Exception {
        ldapUserRepository.removeUsersFromClientGroup(null);
    }

    @Test (expected = IllegalArgumentException.class)
    public void throwIfEmptyOldUser_oldUserIsNull_throwsIllegalArgument() throws Exception {
        User user = new User();
        user.setUsername("username");
        ldapUserRepository.throwIfEmptyOldUser(null, user);
    }

    @Test
    public void throwIfEmptyOldUser_oldUserNotNull_doesNothing() throws Exception {
        ldapUserRepository.throwIfEmptyOldUser(new User(), new User());
    }

    @Test (expected = IllegalArgumentException.class)
    public void throwIfEmptyUsername_userIsNull_throwsIllegalArgument() throws Exception {
        ldapUserRepository.throwIfEmptyUsername(null);
    }

    @Test (expected = IllegalArgumentException.class)
    public void throwIfEmptyUsername_usernameIsBlank_throwsIllegalArgument() throws Exception {
        ldapUserRepository.throwIfEmptyUsername(new User());
    }

    @Test (expected = StalePasswordException.class)
    public void throwIfStalePassword_ldapExMatchesViolationAndStalePasswordMessageMatchesLdapMessage_throwsStalePassword() throws Exception {
        com.unboundid.ldap.sdk.LDAPException ldapException = new com.unboundid.ldap.sdk.LDAPException(ResultCode.CONSTRAINT_VIOLATION, "Password match in history");
        spy.throwIfStalePassword(ldapException, mock(Audit.class));
    }

    @Test
    public void throwIfStalePassword_ldapExNotViolationAndStalePasswordMessageMatchesLdapMessage_doesNothing() throws Exception {
        com.unboundid.ldap.sdk.LDAPException ldapException = new com.unboundid.ldap.sdk.LDAPException(ResultCode.ADMIN_LIMIT_EXCEEDED, "Password match in history");
        spy.throwIfStalePassword(ldapException, mock(Audit.class));
    }

    @Test
    public void throwIfStalePassword_ldapExNotViolationAndStalePasswordMessageNotMatchLdapMessage_doesNothing() throws Exception {
        com.unboundid.ldap.sdk.LDAPException ldapException = new com.unboundid.ldap.sdk.LDAPException(ResultCode.ADMIN_LIMIT_EXCEEDED, "different");
        spy.throwIfStalePassword(ldapException, mock(Audit.class));
    }

    @Test
    public void throwIfStalePassword_ldapExMatchesViolationAndStalePasswordMessageNotMatchLdapMessage_doesNothing() throws Exception {
        com.unboundid.ldap.sdk.LDAPException ldapException = new com.unboundid.ldap.sdk.LDAPException(ResultCode.CONSTRAINT_VIOLATION, "different");
        spy.throwIfStalePassword(ldapException, mock(Audit.class));
    }

    @Test
    public void authenticateByPassword_userIsNull_returnsNewUserAuthenticationResult() throws Exception {
        UserAuthenticationResult result = ldapUserRepository.authenticateByPassword(null, null);
        assertThat("user authentication result", result.getUser(), equalTo(null));
    }

    @Test
    public void authenticateByPassword_returnsAuthResult() throws Exception {
        User user = new User();
        doReturn(false).when(spy).bindUser(user, "password");
        UserAuthenticationResult userAuthenticationResult = new UserAuthenticationResult(user, false);
        doReturn(userAuthenticationResult).when(spy).validateUserStatus(user, false);
        doNothing().when(spy).addAuditLogForAuthentication(user, false);
        UserAuthenticationResult result = spy.authenticateByPassword(user, "password");
        assertThat("user authentication result", result, equalTo(userAuthenticationResult));
    }

    @Test
    public void authenticateUserByApiKey_userIsNull_returnsNewUserAuthenticationResult() throws Exception {
        UserAuthenticationResult result = ldapUserRepository.authenticateUserByApiKey(null, null);
        assertThat("user authentication result", result.getUser(), equalTo(null));
    }

    @Test
    public void authenticateUserByApiKey_apiKeyNotBlankAndMatchesUserApiKey_setsAuthenticatedTrue() throws Exception {
        User user = new User();
        user.setApiKey("apiKey");
        doReturn(new UserAuthenticationResult(user, false)).when(spy).validateUserStatus(user, true);
        doNothing().when(spy).addAuditLogForAuthentication(user, true);
        spy.authenticateUserByApiKey(user, "apiKey");
        verify(spy).validateUserStatus(user, true);
    }

    @Test
    public void authenticateUserByApiKey_apiKeyIsBlankAndMatchesUserApiKey_setAuthenticatedFalse() throws Exception {
        User user = new User();
        user.setApiKey("");
        doReturn(new UserAuthenticationResult(user, false)).when(spy).validateUserStatus(user, false);
        doNothing().when(spy).addAuditLogForAuthentication(user, false);
        spy.authenticateUserByApiKey(user, "");
        verify(spy).validateUserStatus(user, false);
    }

    @Test
    public void authenticateUserByApiKey_apiKeyIsBlankAndNotMatchUserApiKey_setAuthenticatedFalse() throws Exception {
        User user = new User();
        user.setApiKey("");
        doReturn(new UserAuthenticationResult(user, false)).when(spy).validateUserStatus(user, false);
        doNothing().when(spy).addAuditLogForAuthentication(user, false);
        spy.authenticateUserByApiKey(user, "notMatch");
        verify(spy).validateUserStatus(user, false);
    }

    @Test
    public void authenticateUserByApiKey_apiKeyIsNotBlankAndNotMatchUserApiKey_setAuthenticatedFalse() throws Exception {
        User user = new User();
        user.setApiKey("notBlank");
        doReturn(new UserAuthenticationResult(user, false)).when(spy).validateUserStatus(user, false);
        doNothing().when(spy).addAuditLogForAuthentication(user, false);
        spy.authenticateUserByApiKey(user, "notMatch");
        verify(spy).validateUserStatus(user, false);
    }

    @Test (expected = IllegalStateException.class)
    public void bindUser_userIsNull_throwsIllegalState() throws Exception {
        ldapUserRepository.bindUser(null, null);
    }

    @Test (expected = IllegalStateException.class)
    public void bindUser_uniqueIdIsNull_throwsIllegalState() throws Exception {
        ldapUserRepository.bindUser(new User(), null);
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

    @Test (expected = IllegalArgumentException.class)
    public void getSoftDeletedUserById_idIsBlank_throwsIllegalArgument() throws Exception {
        ldapUserRepository.getSoftDeletedUserById("           ");
    }

    @Test
    public void getSoftDeletedUserById_callsGetSingleSoftDeletedUser() throws Exception {
        doReturn(new User()).when(spy).getSingleSoftDeletedUser(any(Filter.class), any(String[].class));
        spy.getSoftDeletedUserById("id");
        verify(spy).getSingleSoftDeletedUser(any(Filter.class), any(String[].class));
    }

    @Test
    public void getSoftDeletedUserById_foundUser_returnsUser() throws Exception {
        User user = new User();
        doReturn(user).when(spy).getSingleSoftDeletedUser(any(Filter.class), any(String[].class));
        User result = spy.getSoftDeletedUserById("id");
        assertThat("user", result, equalTo(user));
    }

    @Test (expected = IllegalArgumentException.class)
    public void getSoftDeletedUserByUsername_usernameIsBlank_throwsIllegalArgument() throws Exception {
        ldapUserRepository.getSoftDeletedUserByUsername("           ");
    }

    @Test
    public void getSoftDeletedUserByUsername_callsGetSingleSoftDeletedUser() throws Exception {
        doReturn(new User()).when(spy).getSingleSoftDeletedUser(any(Filter.class), any(String[].class));
        spy.getSoftDeletedUserByUsername("username");
        verify(spy).getSingleSoftDeletedUser(any(Filter.class), any(String[].class));
    }

    @Test
    public void getSoftDeletedUserByUsername_foundUser_returnsUser() throws Exception {
        User user = new User();
        doReturn(user).when(spy).getSingleSoftDeletedUser(any(Filter.class), any(String[].class));
        User result = spy.getSoftDeletedUserByUsername("username");
        assertThat("user", result, equalTo(user));
    }

}
