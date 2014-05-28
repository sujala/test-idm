package com.rackspace.idm.validation;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.ImpersonationRequest;
import com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Group;
import com.rackspace.docs.identity.api.ext.rax_kskey.v1.ApiKeyCredentials;
import com.rackspace.idm.domain.entity.TenantRole;
import com.rackspace.idm.domain.service.TenantService;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspace.idm.exception.NotFoundException;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.openstack.docs.identity.api.v2.PasswordCredentialsBase;
import org.openstack.docs.identity.api.v2.User;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

/**
 * Created with IntelliJ IDEA.
 * User: ryan5034
 * Date: 8/3/12
 * Time: 4:05 PM
 * To change this template use File | Settings | File Templates.
 */
public class Validator20Test {

    TenantService tenantService = mock(TenantService.class);
    Validator20 validator20;

    @Before
    public void setUp() throws Exception {
        validator20 = new Validator20();
        validator20.setTenantService(tenantService);
    }

    @Test
    public void validateUsername_withEmptyString_throwBadRequestException() throws Exception {
        try{
            validator20.validateUsername("");
            assertTrue("should throw exception",false);
        } catch (BadRequestException ex){
            assertThat("exception message",ex.getMessage(),equalTo("Expecting username"));
        }
    }

    @Test
    public void validateUsername_withNullString_throwBadRequestException() throws Exception {
        try{
            validator20.validateUsername(null);
            assertTrue("should throw exception",false);
        } catch (BadRequestException ex){
            assertThat("exception message",ex.getMessage(),equalTo("Expecting username"));
        }
    }

    @Test
    public void validateUsername_withWhiteSpaceContainingString_throwBadRequestException() throws Exception {
        try{
            validator20.validateUsername("first last");
            assertTrue("should throw exception",false);
        }catch (BadRequestException ex){
            assertThat("exception message",ex.getMessage(),equalTo("Username should not contain white spaces"));
        }
    }

    @Test
    public void validateUsername_withWhiteSpaceContainingString2_throwBadRequestException() throws Exception {
        try{
            validator20.validateUsername(" firstlast");
            assertTrue("should throw exception",false);
        }catch (BadRequestException ex){
            assertThat("exception message", ex.getMessage(), equalTo("Username should not contain white spaces"));
        }
    }

    @Test
    public void validateUsername_withWhiteSpaceContainingString3_throwBadRequestException() throws Exception {
        try{
            validator20.validateUsername("firstlast ");
            assertTrue("should throw exception",false);
        }catch (BadRequestException ex){
            assertThat("exception message",ex.getMessage(),equalTo("Username should not contain white spaces"));
        }
    }

    @Test
    public void validateUsername_withTabContainingString_throwBadRequestException() throws Exception {
        try{
            validator20.validateUsername("first   last");
            assertTrue("should throw exception",false);
        } catch (BadRequestException ex){
            assertThat("exception message", ex.getMessage(),equalTo("Username should not contain white spaces"));
        }
    }

    @Ignore // allowed for release (1.0.12) 12/03/2012
    @Test
    public void validateUsernameForUpdateOrCreate_withNonAlphChara_throwBadRequestException() throws Exception {
        try{
            validator20.validateUsernameForUpdateOrCreate("12nogood");
            assertTrue("should throw exception",false);
        } catch (BadRequestException ex){
            assertThat("exception message", ex.getMessage(), equalTo("Username must begin with an alphabetic character."));
        }
    }

    @Test
    public void validateUsernameForUpdateOrCreate_withSpecialChara_throwBadRequestException() throws Exception {
        try{
            validator20.validateUsernameForUpdateOrCreate("jorgenogood!");
            assertTrue("should throw exception",false);
        } catch (BadRequestException ex){
            assertThat("exception message", ex.getMessage(),equalTo("Username has invalid characters."));
        }
    }

    @Test
    public void validateUsernameForUpdateOrCreate_validUserName() throws Exception {
        validator20.validateUsernameForUpdateOrCreate("jorgegood");
    }

    @Test
    public void validateEmail_missingEmail_throwsBadRequestException() throws Exception {
        try{
            validator20.validateEmail(null);
            assertTrue("should throw exception",false);
        } catch (BadRequestException ex){
            assertThat("exception message",ex.getMessage(),equalTo("Expecting valid email address"));
        }
    }

    @Test
    public void validateEmail_blankEmail_throwsBadRequestException() throws Exception {
        try{
            validator20.validateEmail("");
            assertTrue("should throw exception",false);
        } catch (BadRequestException ex){
            assertThat("exception message",ex.getMessage(),equalTo("Expecting valid email address"));
        }
    }

    @Test
    public void validateEmail_withInvalidEmail_throwsBadRequestException() throws Exception {
        try{
            validator20.validateEmail("foo");
            assertTrue("should throw exception",false);
        } catch (BadRequestException ex){
            assertThat("exception message", ex.getMessage(), equalTo("Expecting valid email address"));
        }
    }

    @Test
    public void validateEmail_withInvalidEmail2_throwsBadRequestException() throws Exception {
        try{
            validator20.validateEmail("foo@");
            assertTrue("should throw exception",false);
        } catch (BadRequestException ex){
            assertThat("exception message",ex.getMessage(),equalTo("Expecting valid email address"));
        }
    }

    @Test
    public void validateEmail_withInvalidEmail3_throwsBadRequestException() throws Exception {
        try{
            validator20.validateEmail("foo.com");
            assertTrue("should throw exception",false);
        } catch (BadRequestException ex){
            assertThat("exception message",ex.getMessage(),equalTo("Expecting valid email address"));
        }
    }

    @Test
    public void validateEmail_withInvalidEmail4_throwsBadRequestException() throws Exception {
        try{
            validator20.validateEmail("foo@.com");
            assertTrue("should throw exception",false);
        } catch (BadRequestException ex){
            assertThat("exception message",ex.getMessage(),equalTo("Expecting valid email address"));
        }
    }

    @Test
    public void validateUser_withValidEmail_succeeds() throws Exception {
        validator20.validateEmail("foo@bar.com");
    }

    @Test
    public void validateUser_withValidEmail2_succeeds() throws Exception {
        validator20.validateEmail("racker@rackspace.com");
    }

    @Test
    public void validateEmail_withValidEmail3_succeeds() throws Exception {
        validator20.validateEmail("john.smith@rackspace.com");
    }

    @Test
    public void validateEmail_withValidEmail4_succeeds() throws Exception {
        validator20.validateEmail("john.elGuapo.smith@rackspace.com");
    }

    @Test
    public void validateEmail_withValidEmail5_succeeds() throws Exception {
        validator20.validateEmail("1@rackspace.com");
    }

    @Test
    public void validateEmail_withValidEmail6_succeeds() throws Exception {
        validator20.validateEmail("1@1.net");
    }

    @Test
    public void validateEmail_withValidEmail7_succeeds() throws Exception {
        validator20.validateEmail("1@1.rackspace.com");
    }

    @Test
    public void validateEmail_withValidEmail8_succeeds() throws Exception {
        validator20.validateEmail("R_a_c_K_e_r_4000@rackspace.com");
    }

    @Test
    public void validatePasswordForCreateOrUpdate_ValidPassword_succeeds() throws Exception {
        validator20.validatePasswordForCreateOrUpdate("Ab345678");
    }

    @Test
    public void validatePasswordCForCreateOrUpdateForCreateOrUpdate_LessThan8CharactersLong_throwsException() throws Exception {
        try{
            validator20.validatePasswordForCreateOrUpdate("123");
            assertTrue("should throw exception",false);
        } catch (BadRequestException ex)
        {
            assertThat("exception message", ex.getMessage(), equalTo("Password must be at least 8 characters in length, must contain at least one uppercase letter, one lowercase letter, and one numeric character."));
        }
    }

    @Test
    public void validatePasswordForCreateOrUpdate_DoesNotContainUpperCaseLetter_throwsException() throws Exception {
        try{
            validator20.validatePasswordForCreateOrUpdate("ab345678");
            assertTrue("should throw exception",false);
        } catch (BadRequestException ex)
        {
            assertThat("exception message", ex.getMessage(), equalTo("Password must be at least 8 characters in length, must contain at least one uppercase letter, one lowercase letter, and one numeric character."));
        }
    }

    @Test
    public void validatePasswordForCreateOrUpdate_DoesNotContainLowerCaseLetter_throwsException() throws Exception {
        try{
            validator20.validatePasswordForCreateOrUpdate("AB345678");
            assertTrue("should throw exception",false);
        } catch (BadRequestException ex)
        {
            assertThat("exception message", ex.getMessage(), equalTo("Password must be at least 8 characters in length, must contain at least one uppercase letter, one lowercase letter, and one numeric character."));
        }
    }

    @Test
    public void validatePasswordForCreateOrUpdate_DoesNotContainNumericCharacter_throwsException() throws Exception {
        try{
            validator20.validatePasswordForCreateOrUpdate("Abcdefghik");
            assertTrue("should throw exception",false);
        } catch (BadRequestException ex)
        {
            assertThat("exception message", ex.getMessage(), equalTo("Password must be at least 8 characters in length, must contain at least one uppercase letter, one lowercase letter, and one numeric character."));
        }
    }

    @Test
    public void validateKsGroup_validGroup_succeeds() {
        Group groupKs = new Group();
        groupKs.setName("name");
        groupKs.setDescription("description");
        validator20.validateKsGroup(groupKs);
    }

    @Test
    public void validateKsGroup_groupNameIsNull_throwsBadRequestException() {
        try{
            validator20.validateKsGroup(new Group());
            assertTrue("should throw exception",false);
        } catch (BadRequestException ex){
            assertThat("exception message",ex.getMessage(),equalTo("Missing group name"));
        }
    }

    @Test
    public void validateKsGroup_groupDescriptionMoreThan1000Characters_throwsBadRequest() {
        try{
            Group groupKs = new Group();
            groupKs.setName("valid");
            String moreThan1000Chars = org.apache.commons.lang.StringUtils.repeat("a", 1001);
            groupKs.setDescription(moreThan1000Chars);
            validator20.validateKsGroup(groupKs);
            assertTrue("should throw exception",false);
        }catch (BadRequestException ex){
            assertThat("exception message", ex.getMessage(), equalTo("Group description length cannot exceed 1000 characters"));
        }
    }

    @Test
    public void validateKsGroup_emptyName_throwsBadRequest() {
        try{
            Group groupKs = new Group();
            groupKs.setName("");
            validator20.validateKsGroup(groupKs);
            assertTrue("should throw exception",false);
        }catch (BadRequestException ex){
           assertThat("exception message", ex.getMessage(), equalTo("Missing group name"));
        }
    }

    @Test
    public void validateKsGroup_invalidGroupLength_throwsBadRequestMessage() {
        Group groupKs = new Group();
        groupKs.setName("Invalidnamellllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllll");
        try {
            validator20.validateKsGroup(groupKs);
            assertTrue("should throw exception",false);
        } catch (BadRequestException e) {
            assertThat("Exception", e.getMessage(), equalTo("Group name length cannot exceed 200 characters"));
        }
    }

    @Test
    public void validateTenantIdInRoles_tenantIdBlankAndContainedInTenantRoles_succeeds() throws Exception {
        List<TenantRole> roles = new ArrayList<TenantRole>();
        when(tenantService.isTenantIdContainedInTenantRoles("",roles)).thenReturn(true);
        validator20.validateTenantIdInRoles("", roles);
    }

    @Test
    public void validateTenantIdInRoles_tenantIdBlankAndNotContainedInTenantRoles_success() throws Exception {
        List<TenantRole> roles = new ArrayList<TenantRole>();
        when(tenantService.isTenantIdContainedInTenantRoles("",roles)).thenReturn(false);
        validator20.validateTenantIdInRoles("", null);
    }

    @Test
    public void validateTenantIdInRoles_tenantIdNotBlankAndContainedInTenantRoles_success() throws Exception {
        List<TenantRole> roles = new ArrayList<TenantRole>();
        when(tenantService.isTenantIdContainedInTenantRoles("tenantId",roles)).thenReturn(true);
        validator20.validateTenantIdInRoles("tenantId", roles);
    }

    @Test
    public void validateTenantIdInRoles_tenantIdNotBlankAndNotContainedInTenantRoles_throwsNotFoundException() throws Exception {
        try{
            List<TenantRole> roles = new ArrayList<TenantRole>();
            when(tenantService.isTenantIdContainedInTenantRoles("tenantId",roles)).thenReturn(false);
            validator20.validateTenantIdInRoles("tenantId", roles);
            assertTrue("should throw exception", false);
        }catch (NotFoundException ex){
            assertThat("exception message",ex.getMessage(), equalTo("Token doesn't belong to Tenant with Id/Name: 'tenantId'"));
        }
    }

    @Test
    public void validateToken_tokenInvalid_throwsBadRequestException() throws Exception {
        try{
            validator20.validateToken("token$");
            assertTrue("should throw exception",false);
        }catch (BadRequestException ex){
            assertThat("exception message", ex.getMessage(), equalTo("Invalid token"));
        }
    }

    @Test
    public void validateToken_tokenIsValid_throwsBadRequestException() throws Exception {
        validator20.validateToken("token");
        assertTrue("should not throw exception", true);
    }
}
