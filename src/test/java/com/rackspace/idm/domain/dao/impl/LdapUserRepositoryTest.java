package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.api.resource.pagination.DefaultPaginator;
import com.rackspace.idm.api.resource.pagination.PaginatorContext;
import com.sun.jersey.api.ConflictException;
import org.junit.runner.RunWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;

import org.mockito.runners.MockitoJUnitRunner;

import com.rackspace.idm.audit.Audit;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.exception.*;
import com.rackspace.idm.util.CryptHelper;
import com.unboundid.ldap.sdk.*;
import com.unboundid.util.StaticUtils;
import org.apache.commons.configuration.Configuration;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.joda.time.DateTime;
import org.joda.time.tz.FixedDateTimeZone;
import org.junit.Before;
import org.junit.Test;

import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.*;

/**
 * Created by IntelliJ IDEA.
 * User: Hector
 * Date: 3/20/12
 * Time: 3:29 PM
 */
@RunWith(MockitoJUnitRunner.class)
public class LdapUserRepositoryTest extends InMemoryLdapIntegrationTest{

    @InjectMocks
    LdapUserRepository ldapUserRepository = new LdapUserRepository();
    @Mock
    LdapConnectionPools ldapConnectionPools;
    @Mock
    Configuration configuration;
    @Mock
    PaginatorContext<User> paginatorContext;
    @Mock
    DefaultPaginator<User> paginator;

    LdapUserRepository spy;
    LDAPInterface ldapInterface;
    CryptHelper cryptHelper;

    @Before
    public void setUp() throws Exception {
        spy = spy(ldapUserRepository);

        ldapInterface = mock(LDAPInterface.class);

        cryptHelper = new CryptHelper();
        cryptHelper.setConfiguration(configuration);
        when(configuration.getString("crypto.password")).thenReturn("password");
        when(configuration.getString("crypto.salt")).thenReturn("a1 b1");

        doReturn(ldapInterface).when(spy).getAppInterface();
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
        verify(spy).getRackerAddAttributes(racker);
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
        user.setUsername("username");
        doReturn(true).when(spy).isUsernameUnique(user.getUsername());
        doReturn(new Attribute[0]).when(spy).getAddAttributes(user);
        doNothing().when(spy).addEntry(anyString(), any(Attribute[].class), any(Audit.class));
        spy.addUser(user);
        verify(spy).addEntry(anyString(), any(Attribute[].class), any(Audit.class));
    }

    @Test(expected = DuplicateUsernameException.class)
    public void addUser_FailsUniqueCheck() throws Exception {
        User user = new User();
        user.setId("id");
        user.setUsername("username");
        doReturn(false).when(spy).isUsernameUnique(user.getUsername());
        doReturn(new Attribute[0]).when(spy).getAddAttributes(user);
        doNothing().when(spy).addEntry(anyString(), any(Attribute[].class), any(Audit.class));
        spy.addUser(user);
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

    @Test
    public void getGroupIdsForUser_usernameIsBlank_returnsNull() throws Exception {
        String[] result = ldapUserRepository.getGroupIdsForUser("");
        assertThat("list", result, equalTo(null));
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

    @Test
    public void getRackerByRackerId_rackerIdIsBlank_returnsNull() throws Exception {
        Racker result = ldapUserRepository.getRackerByRackerId("   ");
        assertThat("racker", result, equalTo(null));
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

    @Test
    public void getUserByCustomerIdAndUsername_customerIdIsBlank_returnsNull() throws Exception {
        User result = ldapUserRepository.getUserByCustomerIdAndUsername("   ", "username");
        assertThat("user", result, equalTo(null));
    }

    @Test
    public void getUserByCustomerIdAndUsername_usernameIsBlank_returnsNull() throws Exception {
        User result = ldapUserRepository.getUserByCustomerIdAndUsername("customerId", "     ");
        assertThat("user", result, equalTo(null));
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

    @Test
    public void getUserById_idIsBlank_returnsNull() throws Exception {
        User result = ldapUserRepository.getUserById("");
        assertThat("user", result, equalTo(null));
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

    @Test
    public void getUsersByNastId_nastIdIsBlank_returnsNull() throws Exception {
        Users result = ldapUserRepository.getUsersByNastId("");
        assertThat("users", result, equalTo(null));
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

    @Test
    public void getUsersByDomainId_domainIdIsBlank_returnsNull() throws Exception {
        Users result = ldapUserRepository.getUsersByDomainId("");
        assertThat("users", result, equalTo(null));
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

    @Test
    public void getUserByRPN_rpnIsBlank_returnsNull() throws Exception {
        User result = ldapUserRepository.getUserByRPN("");
        assertThat("user", result, equalTo(null));
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

    @Test
    public void getUserBySecureId_secureIdIsBlank_returnsNull() throws Exception {
        User result = ldapUserRepository.getUserBySecureId(null);
        assertThat("user", result, equalTo(null));
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

    @Test
    public void getUserByUsername_usernameIsBlank_returnsNull() throws Exception {
        User userByUsername = ldapUserRepository.getUserByUsername("   ");
        assertThat("user", userByUsername, equalTo(null));
    }

    @Test
    public void getUserByUsername_usernameIsNull_returnsNull() throws Exception {
        User userByUsername = ldapUserRepository.getUserByUsername(null);
        assertThat("user", userByUsername, equalTo(null));
    }

    @Test
    public void getUserByUsername_usernameIsEmptyString_returnsNull() throws Exception {
        User userByUsername = ldapUserRepository.getUserByUsername("");
        assertThat("user", userByUsername, equalTo(null));
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
    public void getAllUsersPaged_callsCreateSearchFilter() throws Exception {
        FilterParam filterParam = new FilterParam();
        filterParam.setParam(FilterParam.FilterParamName.RCN);
        filterParam.setValue("rcn");
        FilterParam[] filterParamArray = {filterParam};
        LdapRepository.LdapSearchBuilder searchBuilder = new LdapRepository.LdapSearchBuilder();
        searchBuilder.addEqualAttribute(LdapUserRepository.ATTR_OBJECT_CLASS, LdapUserRepository.OBJECTCLASS_TENANT_ROLE);

        doReturn(paginatorContext).when(spy).getMultipleUsersPaged(any(Filter.class), any(String[].class), anyInt(), anyInt());
        spy.getAllUsersPaged(filterParamArray, 0, 10);
        verify(spy).createSearchFilter(filterParamArray);
    }

    @Test
    public void getAllUsersPaged_callsGetMultipleUsersPaginated() throws Exception {
        FilterParam filterParam = new FilterParam();
        filterParam.setParam(FilterParam.FilterParamName.RCN);
        filterParam.setValue("rcn");
        FilterParam[] filterParamArray = {filterParam};
        LdapRepository.LdapSearchBuilder searchBuilder = new LdapRepository.LdapSearchBuilder();
        searchBuilder.addEqualAttribute(LdapUserRepository.ATTR_OBJECT_CLASS, LdapUserRepository.OBJECTCLASS_RACKSPACEPERSON);
        searchBuilder.addEqualAttribute(LdapUserRepository.ATTR_RACKSPACE_CUSTOMER_NUMBER, filterParam.getStrValue());
        Filter filter = searchBuilder.build();

        doReturn(paginatorContext).when(spy).getMultipleUsersPaged(any(Filter.class), any(String[].class), anyInt(), anyInt());
        spy.getAllUsersPaged(filterParamArray, 0, 10);
        verify(spy).getMultipleUsersPaged(filter, LdapUserRepository.ATTR_USER_SEARCH_ATTRIBUTES, 0, 10);
    }

    @Test
    public void getAllUsers_filterParamsIsNull_callsGetMultipleUsers() throws Exception {
        doReturn(null).when(spy).getMultipleUsers(any(Filter.class), any(String[].class), anyInt(), anyInt());
        spy.getAllUsers(null, 1, 1);
        verify(spy).getMultipleUsers(any(Filter.class), any(String[].class), anyInt(), anyInt());
    }

    @Test
    public void getAllUsers_filterParamNotNull_callsGetMultipleUsers() throws Exception {
        FilterParam filterParam = new FilterParam();
        filterParam.setParam(FilterParam.FilterParamName.RCN);
        filterParam.setValue("rcn");
        FilterParam[] filterParamArray = {filterParam};
        LdapRepository.LdapSearchBuilder searchBuilder = new LdapRepository.LdapSearchBuilder();
        searchBuilder.addEqualAttribute(LdapUserRepository.ATTR_OBJECT_CLASS, LdapUserRepository.OBJECTCLASS_RACKSPACEPERSON);
        searchBuilder.addEqualAttribute(LdapUserRepository.ATTR_RACKSPACE_CUSTOMER_NUMBER, filterParam.getStrValue());
        Filter filter = searchBuilder.build();

        doReturn(null).when(spy).getMultipleUsers(any(Filter.class), any(String[].class), anyInt(), anyInt());
        spy.getAllUsers(filterParamArray, 0, 10);
        verify(spy).getMultipleUsers(filter, LdapUserRepository.ATTR_USER_SEARCH_ATTRIBUTES, 0, 10);
    }

    @Test
    public void getAllUsersNoLimit_callsCreateSearchFilter() throws Exception {
        FilterParam[] filters = makeFilterParamArray("123456789", "123456879");

        doReturn(new Users()).when(spy).getMultipleUsers(any(Filter.class), any(String[].class));

        spy.getAllUsersNoLimit(filters);
        verify(spy).createSearchFilter(filters);
    }

    private FilterParam[] makeFilterParamArray(String role, String domain) {
        return new FilterParam[]{new FilterParam(FilterParam.FilterParamName.ROLE_ID, role),
                                                new FilterParam(FilterParam.FilterParamName.DOMAIN_ID, domain)};
    }

    @Test
    public void getAllUsersNoLit_callsGetMultipleUsers() throws Exception {
        FilterParam[] filterParams = makeFilterParamArray("123456789", "123456789");
        Filter filter = spy.createSearchFilter(filterParams);
        String[] attributes = new String[]{"*", "createTimestamp", "modifyTimestamp", "dxPwdFailedTime"};

        doReturn(new Users()).when(spy).getMultipleUsers(any(Filter.class), any(String[].class));

        spy.getAllUsersNoLimit(filterParams);
        verify(spy).getMultipleUsers(filter, attributes);
    }

    @Test
    public void createSearchFilter_filterParamHasRCN_addsAttribute() throws Exception {
        FilterParam filterParam = new FilterParam();
        filterParam.setParam(FilterParam.FilterParamName.RCN);
        filterParam.setValue("rcn");
        FilterParam[] filterParamArray = {filterParam};
        LdapRepository.LdapSearchBuilder searchBuilder = new LdapRepository.LdapSearchBuilder();

        Filter filter = spy.createSearchFilter(filterParamArray);
        assertThat("attribute", filter.getComponents()[1].toNormalizedString(), containsString("=rcn"));
    }

    @Test
    public void createSearchFilter_filterParamHasUSERNAME_addsAttribute() throws Exception {
        FilterParam filterParam = new FilterParam();
        filterParam.setParam(FilterParam.FilterParamName.USERNAME);
        filterParam.setValue("username");
        FilterParam[] filterParamArray = {filterParam};
        LdapRepository.LdapSearchBuilder searchBuilder = new LdapRepository.LdapSearchBuilder();

        Filter filter = spy.createSearchFilter(filterParamArray);
        assertThat("atttribute", filter.getComponents()[1].toNormalizedString(), containsString("=username"));
    }

    @Test
    public void createSearchFilter_filterParamHasDOMAINID_addsAttribute() throws Exception {
        FilterParam filterParam = new FilterParam();
        filterParam.setParam(FilterParam.FilterParamName.DOMAIN_ID);
        filterParam.setValue("domainid");
        FilterParam[] filterParamArray = {filterParam};
        LdapRepository.LdapSearchBuilder searchBuilder = new LdapRepository.LdapSearchBuilder();

        Filter filter = spy.createSearchFilter(filterParamArray);
        assertThat("atttribute", filter.getComponents()[1].toNormalizedString(), containsString("=domainid"));
    }

    @Test
    public void createSearchFilter_filterParamHasGROUPID_addsAttribute() throws Exception {
        FilterParam filterParam = new FilterParam();
        filterParam.setParam(FilterParam.FilterParamName.GROUP_ID);
        filterParam.setValue("groupid");
        FilterParam[] filterParamArray = {filterParam};
        LdapRepository.LdapSearchBuilder searchBuilder = new LdapRepository.LdapSearchBuilder();

        Filter filter = spy.createSearchFilter(filterParamArray);
        assertThat("atttribute", filter.getComponents()[1].toNormalizedString(), containsString("=groupid"));
    }

    @Test
    public void createSearchFilter_filterParamHasINMIGRATION_addsAttribute() throws Exception {
        FilterParam filterParam = new FilterParam();
        filterParam.setParam(FilterParam.FilterParamName.IN_MIGRATION);
        FilterParam[] filterParamArray = {filterParam};
        LdapRepository.LdapSearchBuilder searchBuilder = new LdapRepository.LdapSearchBuilder();

        Filter filter = spy.createSearchFilter(filterParamArray);
        assertThat("atttribute", filter.getComponents()[1].toNormalizedString(), containsString("=true"));
    }

    @Test
    public void createSearchFilter_filterParamHasMIGRATED_addsAttribute() throws Exception {
        FilterParam filterParam = new FilterParam();
        filterParam.setParam(FilterParam.FilterParamName.MIGRATED);
        FilterParam[] filterParamArray = {filterParam};
        LdapRepository.LdapSearchBuilder searchBuilder = new LdapRepository.LdapSearchBuilder();

        Filter filter = spy.createSearchFilter(filterParamArray);
        assertThat("atttribute", filter.getComponents()[1].toNormalizedString(), containsString("=false"));
    }

    @Test
    public void createSearchFilter_filterParamDoesNotMatchAny_doesNotAddAttribute() throws Exception {
        FilterParam filterParam = new FilterParam();
        filterParam.setParam(FilterParam.FilterParamName.ROLE_NAME);
        filterParam.setValue("role");
        FilterParam[] filterParamArray = {filterParam};
        LdapRepository.LdapSearchBuilder searchBuilder = new LdapRepository.LdapSearchBuilder();

        Filter filter = spy.createSearchFilter(filterParamArray);
        assertThat("atttribute", filter.getComponents().length, equalTo(0));
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

    @Test (expected = IllegalStateException.class)
    public void updateUser_callsLDAPInterfaceModify_throwsLDAPException() throws Exception {
        User newUser = new User();
        newUser.setUsername("newUser");
        User oldUser = new User();
        oldUser.setUsername("oldUser");
        oldUser.setUniqueId("uniqueId");
        List<Modification> mods = new ArrayList<Modification>();
        mods.add(new Modification(ModificationType.ADD, "add"));
        doReturn(mods).when(spy).getModifications(oldUser, newUser, false);
        doThrow(new LDAPException(ResultCode.LOCAL_ERROR)).when(ldapInterface).modify("uniqueId", mods);
        spy.updateUser(newUser, oldUser, false);
    }

    @Test
    public void updateUser_updatesPasswordObj_toExisting() throws Exception {
        Password password = new Password();
        User newUser = new User();
        newUser.setUsername("newUser");
        newUser.setPasswordObj(password);
        User oldUser = new User();
        oldUser.setUsername("oldUser");
        oldUser.setUniqueId("uniqueId");
        List<Modification> mods = new ArrayList<Modification>();
        mods.add(new Modification(ModificationType.ADD, "add"));
        doReturn(mods).when(spy).getModifications(oldUser, newUser, false);
        when(ldapInterface.modify("uniqueId", mods)).thenReturn(new LDAPResult(1, ResultCode.SUCCESS));
        spy.updateUser(newUser, oldUser, false);
        assertThat("password object", newUser.getPasswordObj().isNew(), equalTo(false));
    }

    @Test (expected = IllegalArgumentException.class)
    public void removeUsersFromClientGroup_groupIsNull_throwsIllegalArgument() throws Exception {
        ldapUserRepository.removeUsersFromClientGroup(null);
    }

    @Test (expected = IllegalStateException.class)
    public void removeUsersFromClientGroup_callsLDAPInterfaceSearch_throwsLDAPException() throws Exception {
        ClientGroup clientGroup = new ClientGroup();
        clientGroup.setUniqueId("uniqueId");
        doThrow(new LDAPSearchException(ResultCode.LOCAL_ERROR, "error")).when(ldapInterface).search(any(SearchRequest.class));
        spy.removeUsersFromClientGroup(clientGroup);
    }

    @Test (expected = IllegalStateException.class)
    public void removeUsersFromClientGroup_callsGetUser_throwsGeneralSecurityException() throws Exception {
        ClientGroup clientGroup = new ClientGroup();
        clientGroup.setUniqueId("uniqueId");
        SearchResultEntry searchResultEntry = new SearchResultEntry("", new Attribute[0]);
        List<SearchResultEntry> searchEntries = new ArrayList<SearchResultEntry>();
        searchEntries.add(searchResultEntry);
        SearchResult searchResult = new SearchResult(1, ResultCode.SUCCESS, "diag", "matchDN", null, searchEntries, null, 1, 0, null);
        when(ldapInterface.search(any(SearchRequest.class))).thenReturn(searchResult);
        doThrow(new GeneralSecurityException()).when(spy).getUser(searchResultEntry);
        spy.removeUsersFromClientGroup(clientGroup);
    }

    @Test (expected = IllegalStateException.class)
    public void removeUsersFromClientGroup_callsGetUser_throwsInvalidCipherTextException() throws Exception {
        ClientGroup clientGroup = new ClientGroup();
        clientGroup.setUniqueId("uniqueId");
        SearchResultEntry searchResultEntry = new SearchResultEntry("", new Attribute[0]);
        List<SearchResultEntry> searchEntries = new ArrayList<SearchResultEntry>();
        searchEntries.add(searchResultEntry);
        SearchResult searchResult = new SearchResult(1, ResultCode.SUCCESS, "diag", "matchDN", null, searchEntries, null, 1, 0, null);
        when(ldapInterface.search(any(SearchRequest.class))).thenReturn(searchResult);
        doThrow(new InvalidCipherTextException()).when(spy).getUser(searchResultEntry);
        spy.removeUsersFromClientGroup(clientGroup);
    }

    @Test
    public void removeUsersFromClientGroup_userListIsEmpty() throws Exception {
        ClientGroup clientGroup = new ClientGroup();
        clientGroup.setUniqueId("uniqueId");
        List<SearchResultEntry> searchEntries = new ArrayList<SearchResultEntry>();
        SearchResult searchResult = new SearchResult(1, ResultCode.SUCCESS, "diag", "matchDN", null, searchEntries, null, 1, 0, null);
        when(ldapInterface.search(any(SearchRequest.class))).thenReturn(searchResult);
        spy.removeUsersFromClientGroup(clientGroup);
    }

    @Test (expected = IllegalStateException.class)
    public void removeUsersFromClientGroup_callsLDAPInterfaceModify_throwsLDAPException() throws Exception {
        ClientGroup clientGroup = new ClientGroup();
        clientGroup.setUniqueId("uniqueId");
        SearchResultEntry searchResultEntry = new SearchResultEntry("", new Attribute[0]);
        List<SearchResultEntry> searchEntries = new ArrayList<SearchResultEntry>();
        searchEntries.add(searchResultEntry);
        User user = new User();
        user.setUniqueId("uniqueId");
        SearchResult searchResult = new SearchResult(1, ResultCode.SUCCESS, "diag", "matchDN", null, searchEntries, null, 1, 0, null);
        when(ldapInterface.search(any(SearchRequest.class))).thenReturn(searchResult);
        doReturn(user).when(spy).getUser(searchResultEntry);
        doThrow(new LDAPException(ResultCode.LOCAL_ERROR)).when(ldapInterface).modify(anyString(), any(List.class));
        spy.removeUsersFromClientGroup(clientGroup);
    }

    @Test
    public void removeUsersFromClientGroup_removeSucceeds() throws Exception {
        ClientGroup clientGroup = new ClientGroup();
        clientGroup.setUniqueId("uniqueId");
        SearchResultEntry searchResultEntry = new SearchResultEntry("", new Attribute[0]);
        List<SearchResultEntry> searchEntries = new ArrayList<SearchResultEntry>();
        searchEntries.add(searchResultEntry);
        User user = new User();
        user.setUniqueId("uniqueId");
        SearchResult searchResult = new SearchResult(1, ResultCode.SUCCESS, "diag", "matchDN", null, searchEntries, null, 1, 0, null);
        when(ldapInterface.search(any(SearchRequest.class))).thenReturn(searchResult);
        doReturn(user).when(spy).getUser(searchResultEntry);
        when(ldapInterface.modify(anyString(), any(List.class))).thenReturn(new LDAPResult(1, ResultCode.SUCCESS));
        spy.removeUsersFromClientGroup(clientGroup);
        verify(ldapInterface).modify(anyString(), any(List.class));
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

    @Test (expected = BadRequestException.class)
    public void throwIfEmptyUsername_userIsNull_throwsIllegalArgument() throws Exception {
        ldapUserRepository.throwIfEmptyUsername(null);
    }

    @Test (expected = BadRequestException.class)
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
    public void addAuditLogForAuthentication_authenticated_auditSucceed() throws Exception {
         ldapUserRepository.addAuditLogForAuthentication(new User(), true);
    }

    @Test
    public void addAuditLogForAuthentication_maxLoginFailureExceded_auditFail() throws Exception {
        User user = new User();
        user.setMaxLoginFailuresExceded(true);
        spy.addAuditLogForAuthentication(user, false);
    }

    @Test
    public void addAuditLogForAuthentication_userDisabled_auditFail() throws Exception {
        User user = new User();
        user.setMaxLoginFailuresExceded(false);
        user.setEnabled(false);
        spy.addAuditLogForAuthentication(user, false);
    }

    @Test
    public void addAuditLogForAuthentication_incorrectCredentials_auditFail() throws Exception {
        User user = new User();
        user.setMaxLoginFailuresExceded(false);
        user.setEnabled(true);
        spy.addAuditLogForAuthentication(user, false);
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
    public void getAddAttributes_addsAllAttributesOfAUser_returnsArray() throws Exception {
        Password password = new Password();
        password.setValue("secret");
        User user = new User();
        user.setId("123");
        user.setCountry("us");
        user.setDisplayName("test");
        user.setFirstname("john");
        user.setEmail("john.smith@email.com");
        user.setMiddlename("jon");
        user.setLocale(new Locale("en"));
        user.setCustomerId("456");
        user.setPersonId("789");
        user.setApiKey("aaa-bbb-ccc");
        user.setSecretAnswer("pass");
        user.setSecretQuestion("tests");
        user.setLastname("smith");
        user.setTimeZoneObj(new FixedDateTimeZone("UTC", "UTC", 0, 0));
        user.setUsername("jsmith");
        user.setPasswordObj(password);
        user.setRegion("central");
        user.setEnabled(true);
        user.setNastId("012");
        user.setMossoId(123);
        user.setDomainId("345");
        user.setInMigration(true);
        user.setMigrationDate(new DateTime());
        Attribute[] result = ldapUserRepository.getAddAttributes(user);
        assertThat("id", result[1].getValue(), equalTo("123"));
        assertThat("country", result[2].getValue(), equalTo("us"));
        assertThat("display name", cryptHelper.decrypt(result[3].getValueByteArray()), equalTo("test"));
        assertThat("first name", cryptHelper.decrypt(result[4].getValueByteArray()), equalTo("john"));
        assertThat("email", cryptHelper.decrypt(result[5].getValueByteArray()), equalTo("john.smith@email.com"));
        assertThat("middle name", result[6].getValue(), equalTo("jon"));
        assertThat("locale", result[7].getValue(), equalTo("en"));
        assertThat("customer id", result[8].getValue(), equalTo("456"));
        assertThat("person id", result[9].getValue(), equalTo("789"));
        assertThat("api key", cryptHelper.decrypt(result[10].getValueByteArray()), equalTo("aaa-bbb-ccc"));
        assertThat("secret answer", cryptHelper.decrypt(result[11].getValueByteArray()), equalTo("pass"));
        assertThat("secret question", cryptHelper.decrypt(result[12].getValueByteArray()), equalTo("tests"));
        assertThat("last name", cryptHelper.decrypt(result[13].getValueByteArray()), equalTo("smith"));
        assertThat("time zone", result[14].getValue(), equalTo("UTC"));
        assertThat("username", result[15].getValue(), equalTo("jsmith"));
        assertThat("password", result[16].getValue(), equalTo("secret"));
        assertThat("region", result[20].getValue(), equalTo("central"));
        assertThat("enabled", result[21].getValue(), equalTo("true"));
        assertThat("nast id", result[22].getValue(), equalTo("012"));
        assertThat("mosso id", result[23].getValue(), equalTo("123"));
        assertThat("domain id", result[24].getValue(), equalTo("345"));
        assertThat("migration", result[25].getValue(), equalTo("true"));
    }

    @Test
    public void getAddAttributes_onlyAddsUsername_returnsArray() throws Exception {
        User user = new User();
        user.setUsername("jsmith");
        Attribute[] result = ldapUserRepository.getAddAttributes(user);
        assertThat("username", result[1].getValue(), equalTo("jsmith"));
        assertThat("list size", result.length, equalTo(2));
    }

    @Test
    public void getMultipleUsersPaginated_callsCreateSearchRequestWithPaging() throws Exception {
        FilterParam filterParam = new FilterParam();
        filterParam.setParam(FilterParam.FilterParamName.RCN);
        filterParam.setValue("rcn");
        FilterParam[] filterParamArray = {filterParam};
        LdapRepository.LdapSearchBuilder searchBuilder = new LdapRepository.LdapSearchBuilder();
        searchBuilder.addEqualAttribute(LdapUserRepository.ATTR_OBJECT_CLASS, LdapUserRepository.OBJECTCLASS_RACKSPACEPERSON);
        searchBuilder.addEqualAttribute(LdapUserRepository.ATTR_RACKSPACE_CUSTOMER_NUMBER, filterParam.getStrValue());
        Filter filter = searchBuilder.build();

        spy.getMultipleUsersPaged(filter, LdapUserRepository.ATTR_USER_SEARCH_ATTRIBUTES, 0, 10);
        verify(paginator).createSearchRequest(anyString(), any(SearchRequest.class), anyInt(), anyInt());
    }

    @Test
    public void getMultipleUsersPaginated_returnsEmptyPaginator() throws Exception {
        PaginatorContext<User> userContext = new PaginatorContext<User>();
        FilterParam filterParam = new FilterParam();
        filterParam.setParam(FilterParam.FilterParamName.RCN);
        filterParam.setValue("rcn");
        FilterParam[] filterParamArray = {filterParam};
        LdapRepository.LdapSearchBuilder searchBuilder = new LdapRepository.LdapSearchBuilder();
        searchBuilder.addEqualAttribute(LdapUserRepository.ATTR_OBJECT_CLASS, LdapUserRepository.OBJECTCLASS_RACKSPACEPERSON);
        searchBuilder.addEqualAttribute(LdapUserRepository.ATTR_RACKSPACE_CUSTOMER_NUMBER, filterParam.getStrValue());
        Filter filter = searchBuilder.build();

        doReturn(null).when(spy).getMultipleEntries(any(SearchRequest.class));
        doReturn(userContext).when(paginator).createSearchRequest(anyString(), any(SearchRequest.class), anyInt(), anyInt());
        PaginatorContext<User> page = spy.getMultipleUsersPaged(filter, LdapUserRepository.ATTR_USER_SEARCH_ATTRIBUTES, 0, 10);

        assertThat("result length", page.getSearchResultEntryList().size(), equalTo(0));
    }

    @Test
    public void getMultipleUsersPaginated_callsCreatePageFromResult() throws Exception {
        PaginatorContext<User> userContext = new PaginatorContext<User>();
        FilterParam filterParam = new FilterParam();
        filterParam.setParam(FilterParam.FilterParamName.RCN);
        filterParam.setValue("rcn");
        FilterParam[] filterParamArray = {filterParam};
        LdapRepository.LdapSearchBuilder searchBuilder = new LdapRepository.LdapSearchBuilder();
        searchBuilder.addEqualAttribute(LdapUserRepository.ATTR_OBJECT_CLASS, LdapUserRepository.OBJECTCLASS_RACKSPACEPERSON);
        searchBuilder.addEqualAttribute(LdapUserRepository.ATTR_RACKSPACE_CUSTOMER_NUMBER, filterParam.getStrValue());
        Filter filter = searchBuilder.build();
        SearchResult result = new SearchResult(0, ResultCode.SUCCESS, null, null, null, 10, 10, null);

        doReturn(result).when(spy).getMultipleEntries(any(SearchRequest.class));
        doReturn(userContext).when(paginator).createSearchRequest(anyString(), any(SearchRequest.class), anyInt(), anyInt());
        spy.getMultipleUsersPaged(filter, LdapUserRepository.ATTR_USER_SEARCH_ATTRIBUTES, 0, 10);
        verify(paginator).createPage(any(SearchResult.class), any(PaginatorContext.class));
    }

    @Test
    public void getMultipleUsersPaginated_returnsNonEmptyPaginator() throws Exception {
        PaginatorContext<User> userContext = new PaginatorContext<User>();
        FilterParam filterParam = new FilterParam();
        filterParam.setParam(FilterParam.FilterParamName.RCN);
        filterParam.setValue("rcn");
        FilterParam[] filterParamArray = {filterParam};
        LdapRepository.LdapSearchBuilder searchBuilder = new LdapRepository.LdapSearchBuilder();
        searchBuilder.addEqualAttribute(LdapUserRepository.ATTR_OBJECT_CLASS, LdapUserRepository.OBJECTCLASS_RACKSPACEPERSON);
        searchBuilder.addEqualAttribute(LdapUserRepository.ATTR_RACKSPACE_CUSTOMER_NUMBER, filterParam.getStrValue());
        Filter filter = searchBuilder.build();

        List<SearchResultEntry> searchResultEntryList = new ArrayList<SearchResultEntry>();
        SearchResultEntry searchResultEntry = new SearchResultEntry("dn", new Attribute[]{new Attribute("name")}, new Control[]{new Control("123")});
        searchResultEntryList.add(searchResultEntry);

        List<SearchResultReference> searchResultReferenceList = new ArrayList<SearchResultReference>();
        SearchResultReference searchResultReference = new SearchResultReference(new String[]{"123", "456"}, new Control[]{new Control("123")});
        searchResultReferenceList.add(searchResultReference);

        User user = new User();
        List<User> userList = new ArrayList<User>();
        userList.add(user);
        String[] referralUrls = new String[]{"this", "that"};
        SearchResult result = new SearchResult(0, ResultCode.SUCCESS, "ok", "dn", referralUrls, searchResultEntryList, searchResultReferenceList, 1, 1, null);

        userContext.setSearchResultEntryList(searchResultEntryList);
        doReturn(user).when(spy).getUser(any(SearchResultEntry.class));
        doReturn(result).when(spy).getMultipleEntries(any(SearchRequest.class));
        doReturn(userContext).when(paginator).createSearchRequest(anyString(), any(SearchRequest.class), anyInt(), anyInt());

        PaginatorContext<User> context = spy.getMultipleUsersPaged(filter, LdapUserRepository.ATTR_USER_SEARCH_ATTRIBUTES, 0, 10);

        assertThat("userList", context.getValueList().equals(userList));
    }

    @Test
    public void getMultipleUsers_offsetGreaterThanZero_doesNotCallGetLdapPagingOffsetDefault() throws Exception {
        doReturn(1).when(spy).getLdapPagingLimitDefault();
        doReturn(new ArrayList<SearchResultEntry>()).when(spy).getMultipleEntries(LdapRepository.USERS_BASE_DN, SearchScope.SUB, LdapRepository.ATTR_UID, null, null);
        spy.getMultipleUsers(null, null, 1, 0);
        verify(spy,never()).getLdapPagingOffsetDefault();
    }

    @Test
    public void getMultipleUsers_limitGreaterThan0_doesNotCallGetLdapPagingLimitDefault() throws Exception {
        doReturn(1).when(spy).getLdapPagingOffsetDefault();
        doReturn(new ArrayList<SearchResultEntry>()).when(spy).getMultipleEntries(LdapRepository.USERS_BASE_DN, SearchScope.SUB, LdapRepository.ATTR_UID, null, null);
        spy.getMultipleUsers(null, null, -1, 1);
        verify(spy,never()).getLdapPagingLimitDefault();
    }

    @Test
    public void getMultipleUsers_offsetMoreThanContentCount_setsUsersToEmptyList() throws Exception {
        doReturn(1).when(spy).getLdapPagingOffsetDefault();
        doReturn(1).when(spy).getLdapPagingLimitDefault();
        doReturn(new ArrayList<SearchResultEntry>()).when(spy).getMultipleEntries(LdapRepository.USERS_BASE_DN, SearchScope.SUB, LdapRepository.ATTR_UID, null, null);
        Users result = spy.getMultipleUsers(null, null, -1, 0);
        assertThat("user list", result.getUsers().isEmpty(), equalTo(true));
    }

    @Test
    public void getMultipleUsers_emptySubList_setsUsersToEmptyList() throws Exception {
        doReturn(0).when(spy).getLdapPagingOffsetDefault();
        doReturn(0).when(spy).getLdapPagingLimitDefault();
        SearchResultEntry searchResultEntry = new SearchResultEntry("", new Attribute[0]);
        ArrayList<SearchResultEntry> entries = new ArrayList<SearchResultEntry>();
        entries.add(searchResultEntry);
        doReturn(entries).when(spy).getMultipleEntries(LdapRepository.USERS_BASE_DN, SearchScope.SUB, LdapRepository.ATTR_UID, null, null);
        Users result = spy.getMultipleUsers(null, null, -1, 0);
        assertThat("user list", result.getUsers().isEmpty(), equalTo(true));
    }

    @Test (expected = IllegalStateException.class)
    public void getMultipleUsers_callsGetUser_throwsGeneralSecurityException() throws Exception {
        doReturn(0).when(spy).getLdapPagingOffsetDefault();
        doReturn(3).when(spy).getLdapPagingLimitDefault();
        doReturn(5).when(spy).getLdapPagingLimitMax();
        SearchResultEntry searchResultEntry = new SearchResultEntry("", new Attribute[0]);
        ArrayList<SearchResultEntry> entries = new ArrayList<SearchResultEntry>();
        entries.add(searchResultEntry);
        doReturn(entries).when(spy).getMultipleEntries(LdapRepository.USERS_BASE_DN, SearchScope.SUB, LdapRepository.ATTR_UID, null, null);
        doThrow(new GeneralSecurityException()).when(spy).getUser(searchResultEntry);
        spy.getMultipleUsers(null, null, -1, 0);
    }

    @Test (expected = IllegalStateException.class)
    public void getMultipleUsers_addsUserToList_setsUsersToList() throws Exception {
        doReturn(0).when(spy).getLdapPagingOffsetDefault();
        doReturn(3).when(spy).getLdapPagingLimitDefault();
        doReturn(5).when(spy).getLdapPagingLimitMax();
        SearchResultEntry searchResultEntry = new SearchResultEntry("", new Attribute[0]);
        ArrayList<SearchResultEntry> entries = new ArrayList<SearchResultEntry>();
        entries.add(searchResultEntry);
        doReturn(entries).when(spy).getMultipleEntries(LdapRepository.USERS_BASE_DN, SearchScope.SUB, LdapRepository.ATTR_UID, null, null);
        doThrow(new InvalidCipherTextException()).when(spy).getUser(searchResultEntry);
        spy.getMultipleUsers(null, null, -1, 0);

    }

    @Test
    public void getMultipleUsers_callsGetUser_throwsInvalidCipherTextException() throws Exception {
        doReturn(0).when(spy).getLdapPagingOffsetDefault();
        doReturn(3).when(spy).getLdapPagingLimitDefault();
        doReturn(5).when(spy).getLdapPagingLimitMax();
        SearchResultEntry searchResultEntry = new SearchResultEntry("", new Attribute[0]);
        ArrayList<SearchResultEntry> entries = new ArrayList<SearchResultEntry>();
        entries.add(searchResultEntry);
        doReturn(entries).when(spy).getMultipleEntries(LdapRepository.USERS_BASE_DN, SearchScope.SUB, LdapRepository.ATTR_UID, null, null);
        doReturn(new User()).when(spy).getUser(searchResultEntry);
        Users result = spy.getMultipleUsers(null, null, -1, 0);
        assertThat("user list", result.getUsers().size(), equalTo(1));
    }

    @Test
    public void getMultipleUsers_callsGetMultipleEntries() throws Exception {
        SearchResultEntry searchResultEntry = new SearchResultEntry("", new Attribute[0]);
        ArrayList<SearchResultEntry> entries = new ArrayList<SearchResultEntry>();
        entries.add(searchResultEntry);
        doReturn(entries).when(spy).getMultipleEntries(anyString(), any(SearchScope.class), anyString(), any(Filter.class), any(String[].class));
        doReturn(new User()).when(spy).getUser(searchResultEntry);

        Filter searchFilter = spy.createSearchFilter(makeFilterParamArray("123456798", "123456789"));
        spy.getMultipleUsers(searchFilter, new String[]{"*"});
        verify(spy).getMultipleEntries(anyString(), any(SearchScope.class), anyString(), any(Filter.class), any(String[].class));
    }

    @Test
    public void getMultipleUsers_callsGetUser() throws Exception {
        SearchResultEntry searchResultEntry = new SearchResultEntry("", new Attribute[0]);
        ArrayList<SearchResultEntry> entries = new ArrayList<SearchResultEntry>();
        entries.add(searchResultEntry);
        doReturn(entries).when(spy).getMultipleEntries(anyString(), any(SearchScope.class), anyString(), any(Filter.class), any(String[].class));
        doReturn(new User()).when(spy).getUser(searchResultEntry);

        Filter searchFilter = spy.createSearchFilter(makeFilterParamArray("123456798", "123456789"));
        spy.getMultipleUsers(searchFilter, new String[]{"*"});
        verify(spy).getUser(searchResultEntry);
    }

    @Test
    public void getMultipleUsers_returnsUsers() throws Exception {
        SearchResultEntry searchResultEntry = new SearchResultEntry("", new Attribute[0]);
        ArrayList<SearchResultEntry> entries = new ArrayList<SearchResultEntry>();
        entries.add(searchResultEntry);
        doReturn(entries).when(spy).getMultipleEntries(anyString(), any(SearchScope.class), anyString(), any(Filter.class), any(String[].class));
        doReturn(new User()).when(spy).getUser(searchResultEntry);

        Filter searchFilter = spy.createSearchFilter(makeFilterParamArray("123456798", "123456789"));
        Users users = spy.getMultipleUsers(searchFilter, new String[]{"*"});

        assertThat("users contains one user", users.getUsers().size(), equalTo(1));
    }

    @Test (expected = IllegalStateException.class)
    public void getMultipleUsers_throwsIllegalStateFromGeneralSecurityException() throws Exception {
        SearchResultEntry searchResultEntry = new SearchResultEntry("", new Attribute[0]);
        ArrayList<SearchResultEntry> entries = new ArrayList<SearchResultEntry>();
        entries.add(searchResultEntry);
        doReturn(entries).when(spy).getMultipleEntries(anyString(), any(SearchScope.class), anyString(), any(Filter.class), any(String[].class));
        doThrow(new GeneralSecurityException()).when(spy).getUser(searchResultEntry);

        Filter searchFilter = spy.createSearchFilter(makeFilterParamArray("123456798", "123456789"));
        Users users = spy.getMultipleUsers(searchFilter, new String[]{"*"});
    }

    @Test (expected = IllegalStateException.class)
    public void getMultipleUsers_throwsIllegalStateFromInvalidCipherTextException() throws Exception {
        SearchResultEntry searchResultEntry = new SearchResultEntry("", new Attribute[0]);
        ArrayList<SearchResultEntry> entries = new ArrayList<SearchResultEntry>();
        entries.add(searchResultEntry);
        doReturn(entries).when(spy).getMultipleEntries(anyString(), any(SearchScope.class), anyString(), any(Filter.class), any(String[].class));
        doThrow(new InvalidCipherTextException()).when(spy).getUser(searchResultEntry);

        Filter searchFilter = spy.createSearchFilter(makeFilterParamArray("123456798", "123456789"));
        Users users = spy.getMultipleUsers(searchFilter, new String[]{"*"});
    }

    @Test (expected = IllegalStateException.class)
    public void getSingleUser_getUser_throwsGeneralSecurityException() throws Exception {
        String[] searchAttributes = new String[0];
        Filter filter = null;
        SearchResultEntry searchResultEntry = new SearchResultEntry("", new Attribute[0], new Control[0]);
        doReturn(searchResultEntry).when(spy).getSingleEntry("ou=users,o=rackspace,dc=rackspace,dc=com", SearchScope.SUB, null, searchAttributes);
        doThrow(new GeneralSecurityException()).when(spy).getUser(searchResultEntry);
        spy.getSingleUser(filter, searchAttributes);
    }

    @Test (expected = IllegalStateException.class)
    public void getSingleUser_getUser_throwsInvalidCipherTextException() throws Exception {
        String[] searchAttributes = new String[0];
        Filter filter = null;
        SearchResultEntry searchResultEntry = new SearchResultEntry("", new Attribute[0], new Control[0]);
        doReturn(searchResultEntry).when(spy).getSingleEntry("ou=users,o=rackspace,dc=rackspace,dc=com", SearchScope.SUB, null, searchAttributes);
        doThrow(new InvalidCipherTextException()).when(spy).getUser(searchResultEntry);
        spy.getSingleUser(filter, searchAttributes);
    }

    @Test
    public void getSingleUser_entryIsNull_returnsNull() throws Exception {
        String[] searchAttributes = new String[0];
        doReturn(null).when(spy).getSingleEntry("ou=users,o=rackspace,dc=rackspace,dc=com", SearchScope.SUB, null, searchAttributes);
        User result = spy.getSingleUser(null, searchAttributes);
        assertThat("user", result, equalTo(null));
    }

    @Test
    public void getSingleUser_foundUser_returnsUser() throws Exception {
        String[] searchAttributes = new String[0];
        Filter filter = null;
        User user = new User();
        SearchResultEntry searchResultEntry = new SearchResultEntry("", new Attribute[0], new Control[0]);
        doReturn(searchResultEntry).when(spy).getSingleEntry("ou=users,o=rackspace,dc=rackspace,dc=com", SearchScope.SUB, null, searchAttributes);
        doReturn(user).when(spy).getUser(searchResultEntry);
        User result = spy.getSingleUser(filter, searchAttributes);
        assertThat("user", result, equalTo(user));
    }

    @Test
    public void getSingleSoftDeletedUser_entryIsNull_returnsNull() throws Exception {
        String[] searchAttributes = new String[0];
        doReturn(null).when(spy).getSingleEntry("ou=users,o=rackspace,dc=rackspace,dc=com", SearchScope.SUB, null, searchAttributes);
        User result = spy.getSingleSoftDeletedUser(null, searchAttributes);
        assertThat("user", result, equalTo(null));
    }

    @Test (expected = IllegalStateException.class)
    public void getSingleSoftDeletedUser_getUser_throwsGeneralSecurityException() throws Exception {
        String[] searchAttributes = new String[0];
        Filter filter = null;
        SearchResultEntry searchResultEntry = new SearchResultEntry("", new Attribute[0], new Control[0]);
        doReturn(searchResultEntry).when(spy).getSingleEntry("ou=users,ou=softDeleted,o=rackspace,dc=rackspace,dc=com", SearchScope.SUB, null, searchAttributes);
        doThrow(new GeneralSecurityException()).when(spy).getUser(searchResultEntry);
        spy.getSingleSoftDeletedUser(filter, searchAttributes);
    }

    @Test (expected = IllegalStateException.class)
    public void getSingleSoftDeletedUser_getUser_throwsInvalidCipherTextException() throws Exception {
        String[] searchAttributes = new String[0];
        Filter filter = null;
        SearchResultEntry searchResultEntry = new SearchResultEntry("", new Attribute[0], new Control[0]);
        doReturn(searchResultEntry).when(spy).getSingleEntry("ou=users,ou=softDeleted,o=rackspace,dc=rackspace,dc=com", SearchScope.SUB, null, searchAttributes);
        doThrow(new InvalidCipherTextException()).when(spy).getUser(searchResultEntry);
        spy.getSingleSoftDeletedUser(filter, searchAttributes);
    }

    @Test
    public void getSingleSoftDeletedUser_foundUser_returnsUser() throws Exception {
        String[] searchAttributes = new String[0];
        Filter filter = null;
        User user = new User();
        SearchResultEntry searchResultEntry = new SearchResultEntry("", new Attribute[0], new Control[0]);
        doReturn(searchResultEntry).when(spy).getSingleEntry("ou=users,ou=softDeleted,o=rackspace,dc=rackspace,dc=com", SearchScope.SUB, null, searchAttributes);
        doReturn(user).when(spy).getUser(searchResultEntry);
        User result = spy.getSingleSoftDeletedUser(filter, searchAttributes);
        assertThat("user", result, equalTo(user));
    }

    @Test
    public void getUser_setsAllUserAttributes_returnsUser() throws Exception {
        Password password = new Password();
        password.setValue("secret");
        User user = new User();
        user.setId("123");
        user.setCountry("us");
        user.setDisplayName("test");
        user.setFirstname("john");
        user.setEmail("john.smith@email.com");
        user.setMiddlename("jon");
        user.setLocale(new Locale("en"));
        user.setCustomerId("456");
        user.setPersonId("789");
        user.setApiKey("aaa-bbb-ccc");
        user.setSecretAnswer("pass");
        user.setSecretQuestion("tests");
        user.setLastname("smith");
        user.setTimeZoneObj(new FixedDateTimeZone("UTC", "UTC", 0, 0));
        user.setUsername("jsmith");
        user.setPasswordObj(password);
        user.setRegion("central");
        user.setEnabled(true);
        user.setNastId("012");
        user.setMossoId(123);
        user.setDomainId("345");
        user.setInMigration(true);
        user.setMigrationDate(new DateTime());
        user.setSoftDeletedTimestamp(new DateTime());
        user.setCreated(new DateTime());
        user.setUpdated(new DateTime());
        Attribute[] attributes = ldapUserRepository.getAddAttributes(user);
        List<Attribute> attributeList = new ArrayList<Attribute>();
        Collections.addAll(attributeList, attributes);
        attributeList.add(new Attribute("softDeletedTimestamp", StaticUtils.encodeGeneralizedTime(user.getSoftDeleteTimestamp().toDate())));
        attributeList.add(new Attribute("createTimestamp", StaticUtils.encodeGeneralizedTime(user.getCreated().toDate())));
        attributeList.add(new Attribute("modifyTimestamp", StaticUtils.encodeGeneralizedTime(user.getUpdated().toDate())));
        attributeList.add(new Attribute("dxPwdFailedTime", StaticUtils.encodeGeneralizedTime(new DateTime().toDate())));
        doReturn(100).when(spy).getLdapPasswordFailureLockoutMin();
        Attribute[] newAttributes = attributeList.toArray(new Attribute[0]);
        SearchResultEntry searchResultEntry = new SearchResultEntry("uniqueId", newAttributes, new Control[0]);
        User result = spy.getUser(searchResultEntry);
        assertThat("user id",result.getId(),equalTo(user.getId()));
        assertThat("user unique id",result.getUniqueId(),equalTo("uniqueId"));
        assertThat("user username",result.getUsername(),equalTo(user.getUsername()));
        assertThat("user country",result.getCountry(),equalTo(user.getCountry()));
        assertThat("user display name",result.getDisplayName(),equalTo(user.getDisplayName()));
        assertThat("user first name",result.getFirstname(),equalTo(user.getFirstname()));
        assertThat("user email",result.getEmail(),equalTo(user.getEmail()));
        assertThat("user middle name",result.getMiddlename(),equalTo(user.getMiddlename()));
        assertThat("user preferred language",result.getPreferredLang(),equalTo(user.getPreferredLang()));
        assertThat("user customer id",result.getCustomerId(),equalTo(user.getCustomerId()));
        assertThat("user person id",result.getPersonId(),equalTo(user.getPersonId()));
        assertThat("user api key",result.getApiKey(),equalTo(user.getApiKey()));
        assertThat("user secret question",result.getSecretQuestion(),equalTo(user.getSecretQuestion()));
        assertThat("user secret answer",result.getSecretAnswer(),equalTo(user.getSecretAnswer()));
        assertThat("user last name",result.getLastname(),equalTo(user.getLastname()));
        assertThat("user time zone", result.getTimeZone(),equalTo(user.getTimeZone()));
        assertThat("user domain id", result.getDomainId(),equalTo(user.getDomainId()));
        assertThat("user in migration",result.getInMigration(),equalTo(user.getInMigration()));
        assertThat("user migration date", result.getMigrationDate(),equalTo(user.getMigrationDate()));
        assertThat("user password object",result.getPasswordObj(),equalTo(user.getPasswordObj()));
        assertThat("user region", result.getRegion(),equalTo(user.getRegion()));
        assertThat("user soft delete time stamp",result.getSoftDeleteTimestamp(),equalTo(user.getSoftDeleteTimestamp()));
        assertThat("user enabled",result.isEnabled(),equalTo(user.isEnabled()));
        assertThat("user mosso id",result.getMossoId(),equalTo(user.getMossoId()));
        assertThat("user nast id",result.getNastId(),equalTo(user.getNastId()));
        assertThat("user created date",result.getCreated(),equalTo(user.getCreated()));
        assertThat("user updated date",result.getUpdated(),equalTo(user.getUpdated()));
        assertThat("user max login failures exceeded",result.isMaxLoginFailuresExceded(),equalTo(true));
        assertThat("user secure id",result.getSecureId(),equalTo(user.getSecureId()));
    }


    @Test
    public void getUser_userMigrationNull_returnsUserWithNullMigrationDate() throws Exception {
        Password password = new Password();
        password.setValue("secret");
        User user = new User();
        user.setId("123");
        user.setCountry("us");
        user.setDisplayName("test");
        user.setFirstname("john");
        user.setEmail("john.smith@email.com");
        user.setMiddlename("jon");
        user.setLocale(new Locale("en"));
        user.setCustomerId("456");
        user.setPersonId("789");
        user.setApiKey("aaa-bbb-ccc");
        user.setSecretAnswer("pass");
        user.setSecretQuestion("tests");
        user.setLastname("smith");
        user.setTimeZoneObj(new FixedDateTimeZone("UTC", "UTC", 0, 0));
        user.setUsername("jsmith");
        user.setPasswordObj(password);
        user.setRegion("central");
        user.setEnabled(true);
        user.setNastId("012");
        user.setMossoId(123);
        user.setDomainId("345");
        user.setMigrationDate(new DateTime());
        user.setSoftDeletedTimestamp(new DateTime());
        user.setCreated(new DateTime());
        user.setUpdated(new DateTime());
        Attribute[] attributes = ldapUserRepository.getAddAttributes(user);
        List<Attribute> attributeList = new ArrayList<Attribute>();
        Collections.addAll(attributeList, attributes);
        attributeList.add(new Attribute("softDeletedTimestamp", StaticUtils.encodeGeneralizedTime(user.getSoftDeleteTimestamp().toDate())));
        attributeList.add(new Attribute("createTimestamp", StaticUtils.encodeGeneralizedTime(user.getCreated().toDate())));
        attributeList.add(new Attribute("modifyTimestamp", StaticUtils.encodeGeneralizedTime(user.getUpdated().toDate())));
        attributeList.add(new Attribute("dxPwdFailedTime", StaticUtils.encodeGeneralizedTime(new DateTime().toDate())));
        doReturn(100).when(spy).getLdapPasswordFailureLockoutMin();
        Attribute[] newAttributes = attributeList.toArray(new Attribute[0]);
        SearchResultEntry searchResultEntry = new SearchResultEntry("uniqueId", newAttributes, new Control[0]);
        User result = spy.getUser(searchResultEntry);
        assertThat("user id",result.getId(),equalTo(user.getId()));
        assertThat("user unique id",result.getUniqueId(),equalTo("uniqueId"));
        assertThat("user username",result.getUsername(),equalTo(user.getUsername()));
        assertThat("user country",result.getCountry(),equalTo(user.getCountry()));
        assertThat("user display name",result.getDisplayName(),equalTo(user.getDisplayName()));
        assertThat("user first name",result.getFirstname(),equalTo(user.getFirstname()));
        assertThat("user email",result.getEmail(),equalTo(user.getEmail()));
        assertThat("user middle name",result.getMiddlename(),equalTo(user.getMiddlename()));
        assertThat("user preferred language",result.getPreferredLang(),equalTo(user.getPreferredLang()));
        assertThat("user customer id",result.getCustomerId(),equalTo(user.getCustomerId()));
        assertThat("user person id",result.getPersonId(),equalTo(user.getPersonId()));
        assertThat("user api key",result.getApiKey(),equalTo(user.getApiKey()));
        assertThat("user secret question",result.getSecretQuestion(),equalTo(user.getSecretQuestion()));
        assertThat("user secret answer",result.getSecretAnswer(),equalTo(user.getSecretAnswer()));
        assertThat("user last name",result.getLastname(),equalTo(user.getLastname()));
        assertThat("user time zone", result.getTimeZone(),equalTo(user.getTimeZone()));
        assertThat("user domain id", result.getDomainId(),equalTo(user.getDomainId()));
        assertThat("user in migration",result.getInMigration(),equalTo(user.getInMigration()));
        assertThat("user migration date", result.getMigrationDate(),equalTo(null));
        assertThat("user password object",result.getPasswordObj(),equalTo(user.getPasswordObj()));
        assertThat("user region", result.getRegion(),equalTo(user.getRegion()));
        assertThat("user soft delete time stamp",result.getSoftDeleteTimestamp(),equalTo(user.getSoftDeleteTimestamp()));
        assertThat("user enabled",result.isEnabled(),equalTo(user.isEnabled()));
        assertThat("user mosso id",result.getMossoId(),equalTo(user.getMossoId()));
        assertThat("user nast id",result.getNastId(),equalTo(user.getNastId()));
        assertThat("user created date",result.getCreated(),equalTo(user.getCreated()));
        assertThat("user updated date",result.getUpdated(),equalTo(user.getUpdated()));
        assertThat("user max login failures exceeded",result.isMaxLoginFailuresExceded(),equalTo(true));
        assertThat("user secure id",result.getSecureId(),equalTo(user.getSecureId()));
    }

    @Test
    public void getUser_nullSoftDeleteTimeStamp_returnsUserWithNullSoftDeleteTimeStamp() throws Exception {
        Password password = new Password();
        password.setValue("secret");
        User user = new User();
        user.setId("123");
        user.setCountry("us");
        user.setDisplayName("test");
        user.setFirstname("john");
        user.setEmail("john.smith@email.com");
        user.setMiddlename("jon");
        user.setLocale(new Locale("en"));
        user.setCustomerId("456");
        user.setPersonId("789");
        user.setApiKey("aaa-bbb-ccc");
        user.setSecretAnswer("pass");
        user.setSecretQuestion("tests");
        user.setLastname("smith");
        user.setTimeZoneObj(new FixedDateTimeZone("UTC", "UTC", 0, 0));
        user.setUsername("jsmith");
        user.setPasswordObj(password);
        user.setRegion("central");
        user.setEnabled(true);
        user.setNastId("012");
        user.setMossoId(123);
        user.setDomainId("345");
        user.setInMigration(true);
        user.setMigrationDate(new DateTime());
        user.setCreated(new DateTime());
        user.setUpdated(new DateTime());
        Attribute[] attributes = ldapUserRepository.getAddAttributes(user);
        List<Attribute> attributeList = new ArrayList<Attribute>();
        Collections.addAll(attributeList, attributes);
        attributeList.add(new Attribute("createTimestamp", StaticUtils.encodeGeneralizedTime(user.getCreated().toDate())));
        attributeList.add(new Attribute("modifyTimestamp", StaticUtils.encodeGeneralizedTime(user.getUpdated().toDate())));
        attributeList.add(new Attribute("dxPwdFailedTime", StaticUtils.encodeGeneralizedTime(new DateTime().toDate())));
        doReturn(100).when(spy).getLdapPasswordFailureLockoutMin();
        Attribute[] newAttributes = attributeList.toArray(new Attribute[0]);
        SearchResultEntry searchResultEntry = new SearchResultEntry("uniqueId", newAttributes, new Control[0]);
        User result = spy.getUser(searchResultEntry);
        assertThat("user id",result.getId(),equalTo(user.getId()));
        assertThat("user unique id",result.getUniqueId(),equalTo("uniqueId"));
        assertThat("user username",result.getUsername(),equalTo(user.getUsername()));
        assertThat("user country",result.getCountry(),equalTo(user.getCountry()));
        assertThat("user display name",result.getDisplayName(),equalTo(user.getDisplayName()));
        assertThat("user first name",result.getFirstname(),equalTo(user.getFirstname()));
        assertThat("user email",result.getEmail(),equalTo(user.getEmail()));
        assertThat("user middle name",result.getMiddlename(),equalTo(user.getMiddlename()));
        assertThat("user preferred language",result.getPreferredLang(),equalTo(user.getPreferredLang()));
        assertThat("user customer id",result.getCustomerId(),equalTo(user.getCustomerId()));
        assertThat("user person id",result.getPersonId(),equalTo(user.getPersonId()));
        assertThat("user api key",result.getApiKey(),equalTo(user.getApiKey()));
        assertThat("user secret question",result.getSecretQuestion(),equalTo(user.getSecretQuestion()));
        assertThat("user secret answer",result.getSecretAnswer(),equalTo(user.getSecretAnswer()));
        assertThat("user last name",result.getLastname(),equalTo(user.getLastname()));
        assertThat("user time zone", result.getTimeZone(),equalTo(user.getTimeZone()));
        assertThat("user domain id", result.getDomainId(),equalTo(user.getDomainId()));
        assertThat("user in migration",result.getInMigration(),equalTo(user.getInMigration()));
        assertThat("user migration date", result.getMigrationDate(),equalTo(user.getMigrationDate()));
        assertThat("user password object",result.getPasswordObj(),equalTo(user.getPasswordObj()));
        assertThat("user region", result.getRegion(),equalTo(user.getRegion()));
        assertThat("user soft delete time stamp",result.getSoftDeleteTimestamp(),equalTo(user.getSoftDeleteTimestamp()));
        assertThat("user enabled",result.isEnabled(),equalTo(user.isEnabled()));
        assertThat("user mosso id",result.getMossoId(),equalTo(user.getMossoId()));
        assertThat("user nast id",result.getNastId(),equalTo(user.getNastId()));
        assertThat("user created date",result.getCreated(),equalTo(user.getCreated()));
        assertThat("user updated date",result.getUpdated(),equalTo(user.getUpdated()));
        assertThat("user max login failures exceeded",result.isMaxLoginFailuresExceded(),equalTo(true));
        assertThat("user secure id",result.getSecureId(),equalTo(user.getSecureId()));
    }

    @Test
    public void getUser_nullCreatedDate_returnsUserWithNullCreatedDate() throws Exception {
        Password password = new Password();
        password.setValue("secret");
        User user = new User();
        user.setId("123");
        user.setCountry("us");
        user.setDisplayName("test");
        user.setFirstname("john");
        user.setEmail("john.smith@email.com");
        user.setMiddlename("jon");
        user.setLocale(new Locale("en"));
        user.setCustomerId("456");
        user.setPersonId("789");
        user.setApiKey("aaa-bbb-ccc");
        user.setSecretAnswer("pass");
        user.setSecretQuestion("tests");
        user.setLastname("smith");
        user.setTimeZoneObj(new FixedDateTimeZone("UTC", "UTC", 0, 0));
        user.setUsername("jsmith");
        user.setPasswordObj(password);
        user.setRegion("central");
        user.setEnabled(true);
        user.setNastId("012");
        user.setMossoId(123);
        user.setDomainId("345");
        user.setInMigration(true);
        user.setMigrationDate(new DateTime());
        user.setSoftDeletedTimestamp(new DateTime());
        user.setUpdated(new DateTime());
        Attribute[] attributes = ldapUserRepository.getAddAttributes(user);
        List<Attribute> attributeList = new ArrayList<Attribute>();
        Collections.addAll(attributeList, attributes);
        attributeList.add(new Attribute("softDeletedTimestamp", StaticUtils.encodeGeneralizedTime(user.getSoftDeleteTimestamp().toDate())));
        attributeList.add(new Attribute("modifyTimestamp", StaticUtils.encodeGeneralizedTime(user.getUpdated().toDate())));
        attributeList.add(new Attribute("dxPwdFailedTime", StaticUtils.encodeGeneralizedTime(new DateTime().toDate())));
        doReturn(100).when(spy).getLdapPasswordFailureLockoutMin();
        Attribute[] newAttributes = attributeList.toArray(new Attribute[0]);
        SearchResultEntry searchResultEntry = new SearchResultEntry("uniqueId", newAttributes, new Control[0]);
        User result = spy.getUser(searchResultEntry);
        assertThat("user id",result.getId(),equalTo(user.getId()));
        assertThat("user unique id",result.getUniqueId(),equalTo("uniqueId"));
        assertThat("user username",result.getUsername(),equalTo(user.getUsername()));
        assertThat("user country",result.getCountry(),equalTo(user.getCountry()));
        assertThat("user display name",result.getDisplayName(),equalTo(user.getDisplayName()));
        assertThat("user first name",result.getFirstname(),equalTo(user.getFirstname()));
        assertThat("user email",result.getEmail(),equalTo(user.getEmail()));
        assertThat("user middle name",result.getMiddlename(),equalTo(user.getMiddlename()));
        assertThat("user preferred language",result.getPreferredLang(),equalTo(user.getPreferredLang()));
        assertThat("user customer id",result.getCustomerId(),equalTo(user.getCustomerId()));
        assertThat("user person id",result.getPersonId(),equalTo(user.getPersonId()));
        assertThat("user api key",result.getApiKey(),equalTo(user.getApiKey()));
        assertThat("user secret question",result.getSecretQuestion(),equalTo(user.getSecretQuestion()));
        assertThat("user secret answer",result.getSecretAnswer(),equalTo(user.getSecretAnswer()));
        assertThat("user last name",result.getLastname(),equalTo(user.getLastname()));
        assertThat("user time zone", result.getTimeZone(),equalTo(user.getTimeZone()));
        assertThat("user domain id", result.getDomainId(),equalTo(user.getDomainId()));
        assertThat("user in migration",result.getInMigration(),equalTo(user.getInMigration()));
        assertThat("user migration date", result.getMigrationDate(),equalTo(user.getMigrationDate()));
        assertThat("user password object",result.getPasswordObj(),equalTo(user.getPasswordObj()));
        assertThat("user region", result.getRegion(),equalTo(user.getRegion()));
        assertThat("user soft delete time stamp",result.getSoftDeleteTimestamp(),equalTo(user.getSoftDeleteTimestamp()));
        assertThat("user enabled",result.isEnabled(),equalTo(user.isEnabled()));
        assertThat("user mosso id",result.getMossoId(),equalTo(user.getMossoId()));
        assertThat("user nast id",result.getNastId(),equalTo(user.getNastId()));
        assertThat("user created date",result.getCreated(),equalTo(user.getCreated()));
        assertThat("user updated date",result.getUpdated(),equalTo(user.getUpdated()));
        assertThat("user max login failures exceeded",result.isMaxLoginFailuresExceded(),equalTo(true));
        assertThat("user secure id",result.getSecureId(),equalTo(user.getSecureId()));
    }

    @Test
    public void getUser_nullUpdatedDate_returnsUserWithNullUpdatedDate() throws Exception {
        Password password = new Password();
        password.setValue("secret");
        User user = new User();
        user.setId("123");
        user.setCountry("us");
        user.setDisplayName("test");
        user.setFirstname("john");
        user.setEmail("john.smith@email.com");
        user.setMiddlename("jon");
        user.setLocale(new Locale("en"));
        user.setCustomerId("456");
        user.setPersonId("789");
        user.setApiKey("aaa-bbb-ccc");
        user.setSecretAnswer("pass");
        user.setSecretQuestion("tests");
        user.setLastname("smith");
        user.setTimeZoneObj(new FixedDateTimeZone("UTC", "UTC", 0, 0));
        user.setUsername("jsmith");
        user.setPasswordObj(password);
        user.setRegion("central");
        user.setEnabled(true);
        user.setNastId("012");
        user.setMossoId(123);
        user.setDomainId("345");
        user.setInMigration(true);
        user.setMigrationDate(new DateTime());
        user.setSoftDeletedTimestamp(new DateTime());
        user.setCreated(new DateTime());
        Attribute[] attributes = ldapUserRepository.getAddAttributes(user);
        List<Attribute> attributeList = new ArrayList<Attribute>();
        Collections.addAll(attributeList, attributes);
        attributeList.add(new Attribute("softDeletedTimestamp", StaticUtils.encodeGeneralizedTime(user.getSoftDeleteTimestamp().toDate())));
        attributeList.add(new Attribute("createTimestamp", StaticUtils.encodeGeneralizedTime(user.getCreated().toDate())));
        attributeList.add(new Attribute("dxPwdFailedTime", StaticUtils.encodeGeneralizedTime(new DateTime().toDate())));
        doReturn(100).when(spy).getLdapPasswordFailureLockoutMin();
        Attribute[] newAttributes = attributeList.toArray(new Attribute[0]);
        SearchResultEntry searchResultEntry = new SearchResultEntry("uniqueId", newAttributes, new Control[0]);
        User result = spy.getUser(searchResultEntry);
        assertThat("user id",result.getId(),equalTo(user.getId()));
        assertThat("user unique id",result.getUniqueId(),equalTo("uniqueId"));
        assertThat("user username",result.getUsername(),equalTo(user.getUsername()));
        assertThat("user country",result.getCountry(),equalTo(user.getCountry()));
        assertThat("user display name",result.getDisplayName(),equalTo(user.getDisplayName()));
        assertThat("user first name",result.getFirstname(),equalTo(user.getFirstname()));
        assertThat("user email",result.getEmail(),equalTo(user.getEmail()));
        assertThat("user middle name",result.getMiddlename(),equalTo(user.getMiddlename()));
        assertThat("user preferred language",result.getPreferredLang(),equalTo(user.getPreferredLang()));
        assertThat("user customer id",result.getCustomerId(),equalTo(user.getCustomerId()));
        assertThat("user person id",result.getPersonId(),equalTo(user.getPersonId()));
        assertThat("user api key",result.getApiKey(),equalTo(user.getApiKey()));
        assertThat("user secret question",result.getSecretQuestion(),equalTo(user.getSecretQuestion()));
        assertThat("user secret answer",result.getSecretAnswer(),equalTo(user.getSecretAnswer()));
        assertThat("user last name",result.getLastname(),equalTo(user.getLastname()));
        assertThat("user time zone", result.getTimeZone(),equalTo(user.getTimeZone()));
        assertThat("user domain id", result.getDomainId(),equalTo(user.getDomainId()));
        assertThat("user in migration",result.getInMigration(),equalTo(user.getInMigration()));
        assertThat("user migration date", result.getMigrationDate(),equalTo(user.getMigrationDate()));
        assertThat("user password object",result.getPasswordObj(),equalTo(user.getPasswordObj()));
        assertThat("user region", result.getRegion(),equalTo(user.getRegion()));
        assertThat("user soft delete time stamp",result.getSoftDeleteTimestamp(),equalTo(user.getSoftDeleteTimestamp()));
        assertThat("user enabled",result.isEnabled(),equalTo(user.isEnabled()));
        assertThat("user mosso id",result.getMossoId(),equalTo(user.getMossoId()));
        assertThat("user nast id",result.getNastId(),equalTo(user.getNastId()));
        assertThat("user created date",result.getCreated(),equalTo(user.getCreated()));
        assertThat("user updated date",result.getUpdated(),equalTo(user.getUpdated()));
        assertThat("user max login failures exceeded",result.isMaxLoginFailuresExceded(),equalTo(true));
        assertThat("user secure id",result.getSecureId(),equalTo(user.getSecureId()));
    }

    @Test
    public void getUser_nullPasswordFailureDate_returnsUserWithMaxLoginFailuresExceededFalse() throws Exception {
        Password password = new Password();
        password.setValue("secret");
        User user = new User();
        user.setId("123");
        user.setCountry("us");
        user.setDisplayName("test");
        user.setFirstname("john");
        user.setEmail("john.smith@email.com");
        user.setMiddlename("jon");
        user.setLocale(new Locale("en"));
        user.setCustomerId("456");
        user.setPersonId("789");
        user.setApiKey("aaa-bbb-ccc");
        user.setSecretAnswer("pass");
        user.setSecretQuestion("tests");
        user.setLastname("smith");
        user.setTimeZoneObj(new FixedDateTimeZone("UTC", "UTC", 0, 0));
        user.setUsername("jsmith");
        user.setPasswordObj(password);
        user.setRegion("central");
        user.setEnabled(true);
        user.setNastId("012");
        user.setMossoId(123);
        user.setDomainId("345");
        user.setInMigration(true);
        user.setMigrationDate(new DateTime());
        user.setSoftDeletedTimestamp(new DateTime());
        user.setCreated(new DateTime());
        user.setUpdated(new DateTime());
        Attribute[] attributes = ldapUserRepository.getAddAttributes(user);
        List<Attribute> attributeList = new ArrayList<Attribute>();
        Collections.addAll(attributeList, attributes);
        attributeList.add(new Attribute("softDeletedTimestamp", StaticUtils.encodeGeneralizedTime(user.getSoftDeleteTimestamp().toDate())));
        attributeList.add(new Attribute("createTimestamp", StaticUtils.encodeGeneralizedTime(user.getCreated().toDate())));
        attributeList.add(new Attribute("modifyTimestamp", StaticUtils.encodeGeneralizedTime(user.getUpdated().toDate())));
        doReturn(100).when(spy).getLdapPasswordFailureLockoutMin();
        Attribute[] newAttributes = attributeList.toArray(new Attribute[0]);
        SearchResultEntry searchResultEntry = new SearchResultEntry("uniqueId", newAttributes, new Control[0]);
        User result = spy.getUser(searchResultEntry);
        assertThat("user id",result.getId(),equalTo(user.getId()));
        assertThat("user unique id",result.getUniqueId(),equalTo("uniqueId"));
        assertThat("user username",result.getUsername(),equalTo(user.getUsername()));
        assertThat("user country",result.getCountry(),equalTo(user.getCountry()));
        assertThat("user display name",result.getDisplayName(),equalTo(user.getDisplayName()));
        assertThat("user first name",result.getFirstname(),equalTo(user.getFirstname()));
        assertThat("user email",result.getEmail(),equalTo(user.getEmail()));
        assertThat("user middle name",result.getMiddlename(),equalTo(user.getMiddlename()));
        assertThat("user preferred language",result.getPreferredLang(),equalTo(user.getPreferredLang()));
        assertThat("user customer id",result.getCustomerId(),equalTo(user.getCustomerId()));
        assertThat("user person id",result.getPersonId(),equalTo(user.getPersonId()));
        assertThat("user api key",result.getApiKey(),equalTo(user.getApiKey()));
        assertThat("user secret question",result.getSecretQuestion(),equalTo(user.getSecretQuestion()));
        assertThat("user secret answer",result.getSecretAnswer(),equalTo(user.getSecretAnswer()));
        assertThat("user last name",result.getLastname(),equalTo(user.getLastname()));
        assertThat("user time zone", result.getTimeZone(),equalTo(user.getTimeZone()));
        assertThat("user domain id", result.getDomainId(),equalTo(user.getDomainId()));
        assertThat("user in migration",result.getInMigration(),equalTo(user.getInMigration()));
        assertThat("user migration date", result.getMigrationDate(),equalTo(user.getMigrationDate()));
        assertThat("user password object",result.getPasswordObj(),equalTo(user.getPasswordObj()));
        assertThat("user region", result.getRegion(),equalTo(user.getRegion()));
        assertThat("user soft delete time stamp",result.getSoftDeleteTimestamp(),equalTo(user.getSoftDeleteTimestamp()));
        assertThat("user enabled",result.isEnabled(),equalTo(user.isEnabled()));
        assertThat("user mosso id",result.getMossoId(),equalTo(user.getMossoId()));
        assertThat("user nast id",result.getNastId(),equalTo(user.getNastId()));
        assertThat("user created date",result.getCreated(),equalTo(user.getCreated()));
        assertThat("user updated date",result.getUpdated(),equalTo(user.getUpdated()));
        assertThat("user max login failures exceeded",result.isMaxLoginFailuresExceded(),equalTo(false));
        assertThat("user secure id",result.getSecureId(),equalTo(user.getSecureId()));
    }

    @Test
    public void getNextUserId_callsGetNextId() throws Exception {
        doReturn("").when(spy).getNextId(anyString());
        spy.getNextUserId();
        verify(spy).getNextId(anyString());
    }

    @Test (expected = IllegalStateException.class)
    public void softDeleteUser_callsLDAPInterfaceModify_throwsLDAPException() throws Exception {
        User user = new User();
        user.setUniqueId("uniqueId");
        user.setId("id");
        doThrow(new LDAPException(ResultCode.LOCAL_ERROR)).when(ldapInterface).modify(anyString(), any(Modification.class));
        spy.softDeleteUser(user);
    }

    @Test
    public void softDeleteUser_callsLDAPInterface_modify() throws Exception {
        User user = new User();
        user.setUniqueId("uniqueId");
        user.setId("id");
        doReturn(new LDAPResult(1, ResultCode.SUCCESS)).when(ldapInterface).modify(anyString(), any(Modification.class));
        spy.softDeleteUser(user);
        verify(ldapInterface).modify(anyString(), any(Modification.class));
    }

    @Test (expected = IllegalStateException.class)
    public void unSoftDeleteUser_callsLDAPInterfaceModify_throwsLDAPException() throws Exception {
        User user = new User();
        user.setUniqueId("uniqueId");
        user.setId("id");
        doThrow(new LDAPException(ResultCode.LOCAL_ERROR)).when(ldapInterface).modify(anyString(), any(Modification.class));
        spy.unSoftDeleteUser(user);
    }

    @Test
    public void unSoftDeleteUser_callsLDAPInterface_modify() throws Exception {
        User user = new User();
        user.setUniqueId("uniqueId");
        user.setId("id");
        doReturn(new LDAPResult(1, ResultCode.SUCCESS)).when(ldapInterface).modify(anyString(), any(Modification.class));
        spy.unSoftDeleteUser(user);
        verify(ldapInterface).modify(anyString(), any(Modification.class));
    }

    @Test
    public void validateUserStatus_userIsDisabledAndNotAuthenticated_throwsUserDisabledException() throws Exception {
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
    public void validateUserStatus_notAuthenticated_returnsUserAuthenticationResult() throws Exception {
        User user = new User();
        user.setEnabled(true);
        UserAuthenticationResult result = ldapUserRepository.validateUserStatus(user, false);
        assertThat("user authentication result", result.getUser(), equalTo(user));
    }

    @Test
    public void validateUserStatus_userIsDisabled_returnsUserAuthenticationResult() throws Exception {
        User user = new User();
        user.setEnabled(false);
        UserAuthenticationResult result = ldapUserRepository.validateUserStatus(user, false);
        assertThat("user authentication result", result.getUser(), equalTo(user));
    }

    @Test
    public void validateUserStatus_userIsEnabledAndNotAuthenticated_returnsUserAuthenticationResult() throws Exception {
        User user = new User();
        user.setEnabled(true);
        UserAuthenticationResult result = ldapUserRepository.validateUserStatus(user, false);
        assertThat("user authentication result", result.getUser(), equalTo(user));
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

    @Test
    public void checkForUserNameModification_addsDeleteMod() throws Exception {
        User newUser = new User();
        User oldUser = new User();
        newUser.setUsername("");
        List<Modification> modificationList = new ArrayList<Modification>();
        ldapUserRepository.checkForUserNameModification(oldUser, newUser, modificationList);
        assertThat("mod list", modificationList.get(0).getModificationType().toString(), equalTo("DELETE"));
    }

    @Test
    public void checkForMossoIdModification_addsDeleteMod() throws Exception {
        User newUser = new User();
        User oldUser = new User();
        newUser.setMossoId(-1);
        List<Modification> modificationList = new ArrayList<Modification>();
        ldapUserRepository.checkForMossoIdModification(oldUser, newUser, modificationList);
        assertThat("mod list", modificationList.get(0).getModificationType().toString(), equalTo("DELETE"));
    }

    @Test
    public void checkForMossoIdModification_addsReplaceMod() throws Exception {
        User newUser = new User();
        User oldUser = new User();
        newUser.setMossoId(1);
        oldUser.setMossoId(2);
        List<Modification> modificationList = new ArrayList<Modification>();
        ldapUserRepository.checkForMossoIdModification(oldUser, newUser, modificationList);
        assertThat("mod list", modificationList.get(0).getModificationType().toString(), equalTo("REPLACE"));
    }

    @Test
    public void checkForMossoIdModification_doesNotAddMod() throws Exception {
        User newUser = new User();
        User oldUser = new User();
        newUser.setMossoId(1);
        oldUser.setMossoId(1);
        List<Modification> modificationList = new ArrayList<Modification>();
        ldapUserRepository.checkForMossoIdModification(oldUser, newUser, modificationList);
        assertThat("mod list", modificationList.isEmpty(), equalTo(true));
    }

    @Test
    public void checkForNastIdModification_addsDeleteMod() throws Exception {
        User newUser = new User();
        User oldUser = new User();
        newUser.setNastId("");
        List<Modification> modificationList = new ArrayList<Modification>();
        ldapUserRepository.checkForNastIdModification(oldUser, newUser, modificationList);
        assertThat("mod list", modificationList.get(0).getModificationType().toString(), equalTo("DELETE"));
    }

    @Test
    public void checkForNastIdModification_addsReplaceMod() throws Exception {
        User newUser = new User();
        User oldUser = new User();
        newUser.setNastId("1");
        oldUser.setNastId("2");
        List<Modification> modificationList = new ArrayList<Modification>();
        ldapUserRepository.checkForNastIdModification(oldUser, newUser, modificationList);
        assertThat("mod list", modificationList.get(0).getModificationType().toString(), equalTo("REPLACE"));
    }

    @Test
    public void checkForNastIdModification_doesNotAddMod() throws Exception {
        User newUser = new User();
        User oldUser = new User();
        newUser.setNastId("1");
        oldUser.setNastId("1");
        List<Modification> modificationList = new ArrayList<Modification>();
        ldapUserRepository.checkForNastIdModification(oldUser, newUser, modificationList);
        assertThat("mod list", modificationList.isEmpty(), equalTo(true));
    }

    @Test
    public void checkForEnabledStatusModification_enabledNotNullAndNewEnabled_addsReplaceMod() throws Exception {
        User newUser = new User();
        User oldUser = new User();
        newUser.setEnabled(true);
        oldUser.setEnabled(false);
        List<Modification> modificationList = new ArrayList<Modification>();
        ldapUserRepository.checkForEnabledStatusModification(oldUser, newUser, modificationList);
        assertThat("mod list", modificationList.get(0).getModificationType().toString(), equalTo("REPLACE"));
    }

    @Test
    public void checkForEnabledStatusModification_enabledNull_doesNotAddReplaceMod() throws Exception {
        User newUser = new User();
        User oldUser = new User();
        oldUser.setEnabled(false);
        List<Modification> modificationList = new ArrayList<Modification>();
        ldapUserRepository.checkForEnabledStatusModification(oldUser, newUser, modificationList);
        assertThat("mod list", modificationList.isEmpty(), equalTo(true));
    }

    @Test
    public void checkForEnabledStatusModfication_enabledNotNullAndNewEnabledEqualsOldEnabled_doesNotAddReplaceMod() throws Exception {
        User newUser = new User();
        User oldUser = new User();
        newUser.setEnabled(false);
        oldUser.setEnabled(false);
        List<Modification> modificationList = new ArrayList<Modification>();
        ldapUserRepository.checkForEnabledStatusModification(oldUser, newUser, modificationList);
        assertThat("mod list", modificationList.isEmpty(), equalTo(true));
    }

    @Test
    public void checkForTimeZoneModification_timeZoneNotNullAndNotEqual_addsReplaceMod() throws Exception {
        User newUser = new User();
        User oldUser = new User();
        List<Modification> modificationList = new ArrayList<Modification>();
        newUser.setTimeZoneObj(new FixedDateTimeZone("UTC", "UTC", 0, 0));
        newUser.setTimeZone("UTC");
        oldUser.setTimeZone("EST");
        ldapUserRepository.checkForTimeZoneModification(oldUser, newUser, modificationList);
        assertThat("mod list", modificationList.get(0).getModificationType().toString(), equalTo("REPLACE"));
    }

    @Test
    public void checkForTimeZoneModification_timeZoneNull_doesNotAddMod() throws Exception {
        User newUser = new User();
        User oldUser = new User();
        List<Modification> modificationList = new ArrayList<Modification>();
        ldapUserRepository.checkForTimeZoneModification(oldUser, newUser, modificationList);
        assertThat("mod list", modificationList.isEmpty(), equalTo(true));
    }

    @Test
    public void checkForTimeZoneModification_timeZoneNotNullAndEqual_doesNotAddMod() throws Exception {
        User newUser = new User();
        User oldUser = new User();
        List<Modification> modificationList = new ArrayList<Modification>();
        newUser.setTimeZoneObj(new FixedDateTimeZone("UTC", "UTC", 0, 0));
        newUser.setTimeZone("UTC");
        oldUser.setTimeZone("UTC");
        ldapUserRepository.checkForTimeZoneModification(oldUser, newUser, modificationList);
        assertThat("mod list", modificationList.isEmpty(), equalTo(true));
    }

    @Test
    public void checkForLocaleModification_localeNotNullAndNotEqual_addsReplaceMod() throws Exception {
        User newUser = new User();
        User oldUser = new User();
        List<Modification> modificationList = new ArrayList<Modification>();
        newUser.setLocale(new Locale("en"));
        oldUser.setLocale(new Locale("chi"));
        ldapUserRepository.checkForLocaleModification(oldUser, newUser, modificationList);
        assertThat("mod list", modificationList.get(0).getModificationType().toString(), equalTo("REPLACE"));
    }

    @Test
    public void checkForLocalModification_localeNull_doesNotAddMod() throws Exception {
        User newUser = new User();
        User oldUser = new User();
        List<Modification> modificationList = new ArrayList<Modification>();
        ldapUserRepository.checkForLocaleModification(oldUser, newUser, modificationList);
        assertThat("mod list", modificationList.isEmpty(), equalTo(true));
    }

    @Test
    public void checkForLocaleModification_localeNotNUllAndEqual_doesNotAddMod() throws Exception {
        User newUser = new User();
        User oldUser = new User();
        List<Modification> modificationList = new ArrayList<Modification>();
        newUser.setLocale(new Locale("en"));
        oldUser.setLocale(new Locale("en"));
        ldapUserRepository.checkForLocaleModification(oldUser, newUser, modificationList);
        assertThat("mod list", modificationList.isEmpty(), equalTo(true));
    }

    @Test
    public void checkForPersonIdModification_addsDeleteMod() throws Exception {
        User user = new User();
        User oldUser = new User();
        List<Modification> modificationList = new ArrayList<Modification>();
        user.setPersonId("");
        ldapUserRepository.checkForPersonIdModification(oldUser, user, modificationList);
        assertThat("mod list", modificationList.get(0).getModificationType().toString(), equalTo("DELETE"));
    }

    @Test
    public void checkForPersonIdModification_addsReplaceMod() throws Exception {
        User user = new User();
        User oldUser = new User();
        List<Modification> modificationList = new ArrayList<Modification>();
        user.setPersonId("1");
        oldUser.setPersonId("2");
        ldapUserRepository.checkForPersonIdModification(oldUser, user, modificationList);
        assertThat("mod list", modificationList.get(0).getModificationType().toString(), equalTo("REPLACE"));
    }

    @Test
    public void checkForPersonIdModification_doesNotAddMod() throws Exception {
        User user = new User();
        User oldUser = new User();
        List<Modification> modificationList = new ArrayList<Modification>();
        user.setPersonId("1");
        oldUser.setPersonId("1");
        ldapUserRepository.checkForPersonIdModification(oldUser, user, modificationList);
        assertThat("mod list", modificationList.isEmpty(), equalTo(true));
    }

    @Test
    public void checkForRegionModification_addsDeleteMod() throws Exception {
        User user = new User();
        User oldUser = new User();
        List<Modification> modificationList = new ArrayList<Modification>();
        user.setRegion("");
        ldapUserRepository.checkForRegionModification(oldUser, user, modificationList);
        assertThat("mod list", modificationList.get(0).getModificationType().toString(), equalTo("DELETE"));
    }

    @Test
    public void checkForRegionModification_addsReplaceMod() throws Exception {
        User user = new User();
        User oldUser = new User();
        List<Modification> modificationList = new ArrayList<Modification>();
        user.setRegion("cst");
        oldUser.setRegion("est");
        ldapUserRepository.checkForRegionModification(oldUser, user, modificationList);
        assertThat("mod list", modificationList.get(0).getModificationType().toString(), equalTo("REPLACE"));
    }

    @Test
    public void checkForRegionModification_doesNotAddMod() throws Exception {
        User user = new User();
        User oldUser = new User();
        List<Modification> modificationList = new ArrayList<Modification>();
        user.setRegion("cst");
        oldUser.setRegion("cst");
        ldapUserRepository.checkForRegionModification(oldUser, user, modificationList);
        assertThat("mod list", modificationList.isEmpty(), equalTo(true));
    }

    @Test
    public void checkForLastNameModification_addsDeleteMod() throws Exception {
        User user = new User();
        User oldUser = new User();
        List<Modification> modificationList = new ArrayList<Modification>();
        user.setLastname("");
        ldapUserRepository.checkForLastNameModification(oldUser, user, new CryptHelper(), modificationList);
        assertThat("mod list", modificationList.get(0).getModificationType().toString(), equalTo("DELETE"));
    }

    @Test
    public void checkForLastNameModification_addsReplaceMod() throws Exception {
        User user = new User();
        User oldUser = new User();
        List<Modification> modificationList = new ArrayList<Modification>();
        user.setLastname("abc");
        oldUser.setLastname("def");
        ldapUserRepository.checkForLastNameModification(oldUser, user, new CryptHelper(), modificationList);
        assertThat("mod list", modificationList.get(0).getModificationType().toString(), equalTo("REPLACE"));
    }

    @Test
    public void checkForLastNameModification_doesNotAddMod() throws Exception {
        User user = new User();
        User oldUser = new User();
        List<Modification> modificationList = new ArrayList<Modification>();
        user.setLastname("abc");
        oldUser.setLastname("abc");
        ldapUserRepository.checkForLastNameModification(oldUser, user, new CryptHelper(), modificationList);
        assertThat("mod list", modificationList.isEmpty(), equalTo(true));
    }

    @Test
    public void checkForSecretQuestionModification_addsDeleteMod() throws Exception {
        User user = new User();
        User oldUser = new User();
        List<Modification> modificationList = new ArrayList<Modification>();
        user.setSecretQuestion("");
        ldapUserRepository.checkForSecretQuestionModification(oldUser, user, new CryptHelper(), modificationList);
        assertThat("mod list", modificationList.get(0).getModificationType().toString(), equalTo("DELETE"));
    }

    @Test
    public void checkForSecretQuestionModification_addsReplaceMod() throws Exception {
        User user = new User();
        User oldUser = new User();
        List<Modification> modificationList = new ArrayList<Modification>();
        user.setSecretQuestion("abc");
        oldUser.setSecretQuestion("def");
        ldapUserRepository.checkForSecretQuestionModification(oldUser, user, new CryptHelper(), modificationList);
        assertThat("mod list", modificationList.get(0).getModificationType().toString(), equalTo("REPLACE"));
    }

    @Test
    public void checkForSecretQuestionModification_doesNotAddMod() throws Exception {
        User user = new User();
        User oldUser = new User();
        List<Modification> modificationList = new ArrayList<Modification>();
        user.setSecretQuestion("abc");
        oldUser.setSecretQuestion("abc");
        ldapUserRepository.checkForSecretQuestionModification(oldUser, user, new CryptHelper(), modificationList);
        assertThat("mod list", modificationList.isEmpty(), equalTo(true));
    }

    @Test
    public void checkForSecretAnswerModification_addsDeleteMod() throws Exception {
        User user = new User();
        User oldUser = new User();
        List<Modification> modificationList = new ArrayList<Modification>();
        user.setSecretAnswer("");
        ldapUserRepository.checkForSecretAnswerModification(oldUser, user, new CryptHelper(), modificationList);
        assertThat("mod list", modificationList.get(0).getModificationType().toString(), equalTo("DELETE"));
    }

    @Test
    public void checkForSecretAnswerModification_addsReplaceMod() throws Exception {
        User user = new User();
        User oldUser = new User();
        List<Modification> modificationList = new ArrayList<Modification>();
        user.setSecretAnswer("abc");
        oldUser.setSecretAnswer("def");
        ldapUserRepository.checkForSecretAnswerModification(oldUser, user, new CryptHelper(), modificationList);
        assertThat("mod list", modificationList.get(0).getModificationType().toString(), equalTo("REPLACE"));
    }

    @Test
    public void checkForSecretAnswerModification_doesNotAddMod() throws Exception {
        User user = new User();
        User oldUser = new User();
        List<Modification> modificationList = new ArrayList<Modification>();
        user.setSecretAnswer("abc");
        oldUser.setSecretAnswer("abc");
        ldapUserRepository.checkForSecretAnswerModification(oldUser, user, new CryptHelper(), modificationList);
        assertThat("mod list", modificationList.isEmpty(), equalTo(true));
    }

    @Test
    public void checkForApiKeyModification_newKeyBlank_addsDeleteKeyModification() throws Exception {
        User oldUser = new User();
        oldUser.setApiKey("hello!");
        User newUser = new User();
        newUser.setApiKey("");
        List<Modification> mod = new ArrayList<Modification>();
        ldapUserRepository.checkForApiKeyModification(oldUser, newUser, null, mod);
        assertThat("modification type",mod.get(0).getModificationType().getName(),equalTo("DELETE"));
    }

    @Test
    public void checkForApiKeyModification_addsReplaceMod() throws Exception {
        User user = new User();
        User oldUser = new User();
        List<Modification> modificationList = new ArrayList<Modification>();
        user.setApiKey("abc");
        oldUser.setApiKey("def");
        ldapUserRepository.checkForApiKeyModification(oldUser, user, new CryptHelper(), modificationList);
        assertThat("mod list", modificationList.get(0).getModificationType().toString(), equalTo("REPLACE"));
    }

    @Test
    public void checkForApiKeyModification_doesNotAddMod() throws Exception {
        User user = new User();
        User oldUser = new User();
        List<Modification> modificationList = new ArrayList<Modification>();
        user.setApiKey("abc");
        oldUser.setApiKey("abc");
        ldapUserRepository.checkForApiKeyModification(oldUser, user, new CryptHelper(), modificationList);
        assertThat("mod list", modificationList.isEmpty(), equalTo(true));
    }

    @Test
    public void checkForMiddleNameModification_addsDeleteMod() throws Exception {
        User user = new User();
        User oldUser = new User();
        List<Modification> modificationList = new ArrayList<Modification>();
        user.setMiddlename("");
        ldapUserRepository.checkForMiddleNameModification(oldUser, user, modificationList);
        assertThat("mod list", modificationList.get(0).getModificationType().toString(), equalTo("DELETE"));
    }

    @Test
    public void checkForMiddleNameModification_addsReplaceMod() throws Exception {
        User user = new User();
        User oldUser = new User();
        List<Modification> modificationList = new ArrayList<Modification>();
        user.setMiddlename("abc");
        oldUser.setMiddlename("def");
        ldapUserRepository.checkForMiddleNameModification(oldUser, user, modificationList);
        assertThat("mod list", modificationList.get(0).getModificationType().toString(), equalTo("REPLACE"));
    }

    @Test
    public void checkForMiddleNameModification_doesNotAddMod() throws Exception {
        User user = new User();
        User oldUser = new User();
        List<Modification> modificationList = new ArrayList<Modification>();
        user.setMiddlename("abc");
        oldUser.setMiddlename("abc");
        ldapUserRepository.checkForMiddleNameModification(oldUser, user, modificationList);
        assertThat("mod list", modificationList.isEmpty(), equalTo(true));
    }

    @Test
    public void checkForEmailModification_addsDeleteMod() throws Exception {
        User user = new User();
        User oldUser = new User();
        List<Modification> modificationList = new ArrayList<Modification>();
        user.setEmail("");
        ldapUserRepository.checkForEmailModification(oldUser, user, new CryptHelper(), modificationList);
        assertThat("mod list", modificationList.get(0).getModificationType().toString(), equalTo("DELETE"));
    }

    @Test
    public void checkForEmailModification_addsReplaceMod() throws Exception {
        User user = new User();
        User oldUser = new User();
        List<Modification> modificationList = new ArrayList<Modification>();
        user.setEmail("abc");
        oldUser.setEmail("def");
        ldapUserRepository.checkForEmailModification(oldUser, user, new CryptHelper(), modificationList);
        assertThat("mod list", modificationList.get(0).getModificationType().toString(), equalTo("REPLACE"));
    }

    @Test
    public void checkForEmailModification_doesNotAddMod() throws Exception {
        User user = new User();
        User oldUser = new User();
        List<Modification> modificationList = new ArrayList<Modification>();
        user.setEmail("abc");
        oldUser.setEmail("abc");
        ldapUserRepository.checkForEmailModification(oldUser, user, new CryptHelper(), modificationList);
        assertThat("mod list", modificationList.isEmpty(), equalTo(true));
    }

    @Test
    public void checkForFirstNameModification_addsDeleteMod() throws Exception {
        User user = new User();
        User oldUser = new User();
        List<Modification> modificationList = new ArrayList<Modification>();
        user.setFirstname("");
        ldapUserRepository.checkForFirstNameModification(oldUser, user, new CryptHelper(), modificationList);
        assertThat("mod list", modificationList.get(0).getModificationType().toString(), equalTo("DELETE"));
    }

    @Test
    public void checkForFirstNameModification_addsReplaceMod() throws Exception {
        User user = new User();
        User oldUser = new User();
        List<Modification> modificationList = new ArrayList<Modification>();
        user.setFirstname("abc");
        oldUser.setFirstname("def");
        ldapUserRepository.checkForFirstNameModification(oldUser, user, new CryptHelper(), modificationList);
        assertThat("mod list", modificationList.get(0).getModificationType().toString(), equalTo("REPLACE"));
    }

    @Test
    public void checkForFirstNameModification_doesNotAddMod() throws Exception {
        User user = new User();
        User oldUser = new User();
        List<Modification> modificationList = new ArrayList<Modification>();
        user.setFirstname("abc");
        oldUser.setFirstname("abc");
        ldapUserRepository.checkForFirstNameModification(oldUser, user, new CryptHelper(), modificationList);
        assertThat("mod list", modificationList.isEmpty(), equalTo(true));
    }

    @Test
    public void checkForSecureIdModification_addDeleteMod() throws Exception {
        User user = new User();
        User oldUser = new User();
        List<Modification> modificationList = new ArrayList<Modification>();
        user.setSecureId("");
        ldapUserRepository.checkForSecureIdModification(oldUser, user, modificationList);
        assertThat("mod list", modificationList.get(0).getModificationType().toString(), equalTo("DELETE"));
    }

    @Test
    public void checkForSecureIdModification_addReplaceMod() throws Exception {
        User user = new User();
        User oldUser = new User();
        List<Modification> modificationList = new ArrayList<Modification>();
        user.setSecureId("abc");
        oldUser.setSecureId("def");
        ldapUserRepository.checkForSecureIdModification(oldUser, user, modificationList);
        assertThat("mod list", modificationList.get(0).getModificationType().toString(), equalTo("REPLACE"));
    }

    @Test
    public void checkForSecureIdModification_doesNotAddMod() throws Exception {
        User user = new User();
        User oldUser = new User();
        List<Modification> modificationList = new ArrayList<Modification>();
        user.setSecureId("abc");
        oldUser.setSecureId("abc");
        ldapUserRepository.checkForSecureIdModification(oldUser, user, modificationList);
        assertThat("mod list", modificationList.isEmpty(), equalTo(true));
    }

    @Test
    public void checkForDisplayNameModification_addsDeleteMod() throws Exception {
        User user = new User();
        User oldUser = new User();
        List<Modification> modificationList = new ArrayList<Modification>();
        user.setDisplayName("");
        ldapUserRepository.checkForDisplayNameModification(oldUser, user, new CryptHelper(), modificationList);
        assertThat("mod list", modificationList.get(0).getModificationType().toString(), equalTo("DELETE"));
    }

    @Test
    public void checkForDisplayNameModification_addsReplaceMod() throws Exception {
        User user = new User();
        User oldUser = new User();
        List<Modification> modificationList = new ArrayList<Modification>();
        user.setDisplayName("abc");
        oldUser.setDisplayName("def");
        ldapUserRepository.checkForDisplayNameModification(oldUser, user, new CryptHelper(), modificationList);
        assertThat("mod list", modificationList.get(0).getModificationType().toString(), equalTo("REPLACE"));
    }

    @Test
    public void checkForDisplayNameModification_doesNotAddMod() throws Exception {
        User user = new User();
        User oldUser = new User();
        List<Modification> modificationList = new ArrayList<Modification>();
        user.setDisplayName("abc");
        oldUser.setDisplayName("abc");
        ldapUserRepository.checkForDisplayNameModification(oldUser, user, new CryptHelper(), modificationList);
        assertThat("mod list", modificationList.isEmpty(), equalTo(true));
    }

    @Test
    public void checkForCountryModification_addsDeleteMod() throws Exception {
        User user = new User();
        User oldUser = new User();
        List<Modification> modificationList = new ArrayList<Modification>();
        user.setCountry("");
        ldapUserRepository.checkForCountryModification(oldUser, user, modificationList);
        assertThat("mod list", modificationList.get(0).getModificationType().toString(), equalTo("DELETE"));
    }

    @Test
    public void checkForCountryModification_addsReplaceMod() throws Exception {
        User user = new User();
        User oldUser = new User();
        List<Modification> modificationList = new ArrayList<Modification>();
        user.setCountry("abc");
        oldUser.setCountry("def");
        ldapUserRepository.checkForCountryModification(oldUser, user, modificationList);
        assertThat("mod list", modificationList.get(0).getModificationType().toString(), equalTo("REPLACE"));
    }

    @Test
    public void checkForCountryModification_doesNotAddMod() throws Exception {
        User user = new User();
        User oldUser = new User();
        List<Modification> modificationList = new ArrayList<Modification>();
        user.setCountry("abc");
        oldUser.setCountry("abc");
        ldapUserRepository.checkForCountryModification(oldUser, user, modificationList);
        assertThat("mod list", modificationList.isEmpty(), equalTo(true));
    }

    @Test
    public void checkForCustomerIdModification_addsDeleteMod() throws Exception {
        User user = new User();
        User oldUser = new User();
        List<Modification> modificationList = new ArrayList<Modification>();
        user.setCustomerId("");
        ldapUserRepository.checkForCustomerIdModfication(oldUser, user, modificationList);
        assertThat("mod list", modificationList.get(0).getModificationType().toString(), equalTo("DELETE"));
    }

    @Test
    public void checkForCustomerIdModification_addsReplaceMod() throws Exception {
        User user = new User();
        User oldUser = new User();
        List<Modification> modificationList = new ArrayList<Modification>();
        user.setCustomerId("abc");
        oldUser.setCustomerId("def");
        ldapUserRepository.checkForCustomerIdModfication(oldUser, user, modificationList);
        assertThat("mod list", modificationList.get(0).getModificationType().toString(), equalTo("REPLACE"));
    }

    @Test
    public void checkForCustomerIdModification_doesNotAddMod() throws Exception {
        User user = new User();
        User oldUser = new User();
        List<Modification> modificationList = new ArrayList<Modification>();
        user.setCustomerId("abc");
        oldUser.setCustomerId("abc");
        ldapUserRepository.checkForCustomerIdModfication(oldUser, user, modificationList);
        assertThat("mod list", modificationList.isEmpty(), equalTo(true));
    }

    @Test (expected = PasswordSelfUpdateTooSoonException.class)
    public void checkForPasswordModification_selfUpdated_throwsPasswordSelfUpdateTooSoon() throws Exception {
        User user = new User();
        User oldUser = new User();
        List<Modification> modificationList = new ArrayList<Modification>();
        Password oldPassword = Password.existingInstance("abc", new DateTime(), true);
        oldUser.setPasswordObj(oldPassword);
        user.setPasswordObj(Password.newInstance("abc"));
        ldapUserRepository.checkForPasswordModification(oldUser, user, true, new CryptHelper(), modificationList);
    }

    @Test
    public void checkForPasswordModification_notSelfUpdated_addsMod() throws Exception {
        User user = new User();
        User oldUser = new User();
        List<Modification> modificationList = new ArrayList<Modification>();
        Password oldPassword = Password.existingInstance("abc", new DateTime(), true);
        oldUser.setPasswordObj(oldPassword);
        user.setPasswordObj(Password.newInstance("abc"));
        ldapUserRepository.checkForPasswordModification(oldUser, user, false, new CryptHelper(), modificationList);
        assertThat("mod list", modificationList.isEmpty(), equalTo(false));
    }

    @Test
    public void checkForMigrationStatusModification_addsReplaceMod() throws Exception {
        User user = new User();
        User oldUser = new User();
        List<Modification> modificationList = new ArrayList<Modification>();
        user.setInMigration(true);
        ldapUserRepository.checkForMigrationStatusModification(oldUser, user, modificationList);
        assertThat("mod list", modificationList.get(0).getModificationType().toString(), equalTo("REPLACE"));
    }

    @Test
    public void getSoftDeletedUserById_idIsBlank_returnNull() throws Exception {
        User result = ldapUserRepository.getSoftDeletedUserById("           ");
        assertThat("user", result, equalTo(null));
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

    @Test
    public void getSoftDeletedUserByUsername_usernameIsBlank_returnsNull() throws Exception {
        User result = ldapUserRepository.getSoftDeletedUserByUsername("           ");
        assertThat("user", result, equalTo(null));
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
