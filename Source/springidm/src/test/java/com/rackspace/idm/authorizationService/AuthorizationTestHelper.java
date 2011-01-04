package com.rackspace.idm.authorizationService;

import java.util.List;
import java.util.Vector;

import junit.framework.TestCase;

import org.junit.Ignore;

import com.rackspace.idm.exceptions.ForbiddenException;
import com.rackspace.idm.exceptions.XACMLRequestCreationException;

@Ignore
public class AuthorizationTestHelper {

    AuthorizationService authorizationService;
    Entity subject = null;
    Entity resource = null;
    Entity action = null;
    List<Entity> entities = null;

    public AuthorizationTestHelper(AuthorizationService authService) {

        this.authorizationService = authService;
    }

    public void testAllAuthorizations() {

        // FIXME re-enable this once the test gremlins are worked out on Bamboo.
        // testAdminAuthorization();
        testAdminAuthorizationWithAnotherRole();
        testNotAnAdminAuthorization();
        testNonCompanyAuthorization();
        testNullInputToCreateRequest();
        testNullInputToDoAuthorization();
        testUserAuthorization();
        testNonUserAuthorization();

        testCompanyAuthorization();

        testNonRackspaceClientAuthorization();

        testRackspaceClientAuthorization();
    }

    private void testAdminAuthorization() {

        String userCompanyId = "rackspace";
        List<String> resourceCompany = new Vector<String>();
        resourceCompany.add(userCompanyId);

        List<String> subjectRoles = new Vector<String>();
        List<String> subjectCompany = new Vector<String>();

        String methodName = "addRole";

        Entity subject;
        subjectRoles.add("admin");
        subjectCompany.add("rackspace");

        subject = new Entity(AuthorizationConstants.SUBJECT);

        subject.addAttribute(AuthorizationConstants.SUBJECT_ROLE_ATTRIBUTE,
            AuthorizationConstants.TYPE_STRING, subjectRoles);
        subject.addAttribute(AuthorizationConstants.SUBJECT_ROLE_COMPANY_ID,
            AuthorizationConstants.TYPE_STRING, subjectCompany);

        Entity resource = new Entity(AuthorizationConstants.RESOURCE);
        resource.addAttribute(AuthorizationConstants.RESOURCE_COMPANY_ID,
            AuthorizationConstants.TYPE_STRING, resourceCompany);

        Entity action = new Entity(AuthorizationConstants.ACTION);
        action.addAttribute(AuthorizationConstants.ACTION_ID_ATTRIBUTE,
            AuthorizationConstants.TYPE_STRING, methodName);

        List<Entity> entities = new Vector<Entity>();
        entities.add(subject);
        entities.add(resource);
        entities.add(action);

        AuthorizationRequest authRequest = null;

        try {
            authRequest = authorizationService.createRequest(entities);
        } catch (XACMLRequestCreationException exp) {
            exp.printStackTrace();
        }

        boolean result = authorizationService.doAuthorization(authRequest);
        TestCase.assertEquals(true, result);
    }

    private void testAdminAuthorizationWithAnotherRole() {

        String userCompanyId = "rackspace";
        List<String> resourceCompany = new Vector<String>();
        resourceCompany.add(userCompanyId);

        List<String> subjectRoles = new Vector<String>();
        List<String> subjectCompany = new Vector<String>();

        String methodName = "addRole";

        Entity subject;
        subjectRoles.add("admin");
        subjectRoles.add("racker");
        subjectCompany.add("rackspace");
        subjectCompany.add("target");

        subject = new Entity(AuthorizationConstants.SUBJECT);

        subject.addAttribute(AuthorizationConstants.SUBJECT_ROLE_ATTRIBUTE,
            AuthorizationConstants.TYPE_STRING, subjectRoles);
        subject.addAttribute(AuthorizationConstants.SUBJECT_ROLE_COMPANY_ID,
            AuthorizationConstants.TYPE_STRING, subjectCompany);

        Entity resource = new Entity(AuthorizationConstants.RESOURCE);
        resource.addAttribute(AuthorizationConstants.RESOURCE_COMPANY_ID,
            AuthorizationConstants.TYPE_STRING, resourceCompany);

        Entity action = new Entity(AuthorizationConstants.ACTION);
        action.addAttribute(AuthorizationConstants.ACTION_ID_ATTRIBUTE,
            AuthorizationConstants.TYPE_STRING, methodName);

        List<Entity> entities = new Vector<Entity>();
        entities.add(subject);
        entities.add(resource);
        entities.add(action);

        AuthorizationRequest authRequest = null;

        try {
            authRequest = authorizationService.createRequest(entities);
        } catch (XACMLRequestCreationException exp) {
            exp.printStackTrace();
        }

        boolean result = authorizationService.doAuthorization(authRequest);
        TestCase.assertEquals(true, result);
    }

    private void testNotAnAdminAuthorization() {

        String userCompanyId = "rackspace";
        List<String> resourceCompany = new Vector<String>();
        resourceCompany.add(userCompanyId);

        List<String> subjectRoles = new Vector<String>();
        List<String> subjectCompany = new Vector<String>();

        String methodName = "addRole";

        Entity subject;
        subjectRoles.add("racker");
        subjectCompany.add("rackspace");

        subject = new Entity(AuthorizationConstants.SUBJECT);

        subject.addAttribute(AuthorizationConstants.SUBJECT_ROLE_ATTRIBUTE,
            AuthorizationConstants.TYPE_STRING, subjectRoles);
        subject.addAttribute(AuthorizationConstants.SUBJECT_ROLE_COMPANY_ID,
            AuthorizationConstants.TYPE_STRING, subjectCompany);

        Entity resource = new Entity(AuthorizationConstants.RESOURCE);
        resource.addAttribute(AuthorizationConstants.RESOURCE_COMPANY_ID,
            AuthorizationConstants.TYPE_STRING, resourceCompany);

        Entity action = new Entity(AuthorizationConstants.ACTION);
        action.addAttribute(AuthorizationConstants.ACTION_ID_ATTRIBUTE,
            AuthorizationConstants.TYPE_STRING, methodName);

        List<Entity> entities = new Vector<Entity>();
        entities.add(subject);
        entities.add(resource);
        entities.add(action);

        AuthorizationRequest authRequest = null;

        try {
            authRequest = authorizationService.createRequest(entities);
        } catch (XACMLRequestCreationException exp) {
            exp.printStackTrace();
        }

        boolean result = authorizationService.doAuthorization(authRequest);
        TestCase.assertEquals(false, result);
    }

    private void testRackspaceClientAuthorization() {

        List<String> companyName = new Vector<String>();
        companyName.add("RCN-000-000-000");

        String methodName = "addUser";

        Entity subject = new Entity(AuthorizationConstants.SUBJECT);

        subject.addAttribute(AuthorizationConstants.SUBJECT_COMPANY_ID,
            AuthorizationConstants.TYPE_STRING, companyName);

        Entity resource = new Entity(AuthorizationConstants.RESOURCE);

        // resource.addAttribute(AuthorizationConstants.RESOURCE_COMPANY_ID,
        // AuthorizationConstants.TYPE_STRING, resourceName);

        Entity action = new Entity(AuthorizationConstants.ACTION);

        // action.addAttribute(AuthorizationConstants.ACTION_ID_ATTRIBUTE,
        // AuthorizationConstants.TYPE_STRING, methodName);

        List<Entity> entities = new Vector<Entity>();
        entities.add(subject);
        entities.add(resource);
        entities.add(action);

        List<String> subjectCompany = new Vector<String>();

        // String methodName = "addUser";

        // Entity subject;
        // subjectCompany.add("rcn-000-000-000");

        // subject = new Entity(AuthorizationConstants.SUBJECT);

        // subject.addAttribute(AuthorizationConstants.SUBJECT_COMPANY_ID,
        // AuthorizationConstants.TYPE_STRING, subjectCompany);

        // Entity resource = new Entity(AuthorizationConstants.RESOURCE);

        // String resourceName = "dummyCompany";

        // resource.addAttribute(AuthorizationConstants.RESOURCE_COMPANY_ID,
        // AuthorizationConstants.TYPE_STRING, resourceName);

        // Entity action = new Entity(AuthorizationConstants.ACTION);
        // action.addAttribute(AuthorizationConstants.ACTION_ID_ATTRIBUTE,
        // AuthorizationConstants.TYPE_STRING, methodName);

        // List<Entity> entities = new Vector<Entity>();
        // entities.add(subject);
        // entities.add(resource);
        // entities.add(action);

        AuthorizationRequest authRequest = null;

        try {
            authRequest = authorizationService.createRequest(entities);
        } catch (XACMLRequestCreationException exp) {
            exp.printStackTrace();
        }

        boolean result = authorizationService.doAuthorization(authRequest);
        TestCase.assertEquals(true, result);
    }

    private void testNonRackspaceClientAuthorization() {

        List<String> subjectCompany = new Vector<String>();

        String methodName = "addUser";

        Entity subject;
        subjectCompany.add("RCN-111-000-000");

        subject = new Entity(AuthorizationConstants.SUBJECT);

        subject.addAttribute(AuthorizationConstants.SUBJECT_COMPANY_ID,
            AuthorizationConstants.TYPE_STRING, subjectCompany);

        Entity resource = new Entity(AuthorizationConstants.RESOURCE);

        Entity action = new Entity(AuthorizationConstants.ACTION);
        action.addAttribute(AuthorizationConstants.ACTION_ID_ATTRIBUTE,
            AuthorizationConstants.TYPE_STRING, methodName);

        List<Entity> entities = new Vector<Entity>();
        entities.add(subject);
        entities.add(resource);
        entities.add(action);

        AuthorizationRequest authRequest = null;

        try {
            authRequest = authorizationService.createRequest(entities);
        } catch (XACMLRequestCreationException exp) {
            exp.printStackTrace();
        }

        boolean result = authorizationService.doAuthorization(authRequest);
        TestCase.assertEquals(false, result);
    }

    private void testCompanyAuthorization() {

        List<String> companyName = new Vector<String>();
        companyName.add("Target");

        String methodName = "addUser";

        String resourceName = "Target";

        Entity subject = new Entity(AuthorizationConstants.SUBJECT);

        subject.addAttribute(AuthorizationConstants.SUBJECT_COMPANY_ID,
            AuthorizationConstants.TYPE_STRING, companyName);

        Entity resource = new Entity(AuthorizationConstants.RESOURCE);

        resource.addAttribute(AuthorizationConstants.RESOURCE_COMPANY_ID,
            AuthorizationConstants.TYPE_STRING, resourceName);

        Entity action = new Entity(AuthorizationConstants.ACTION);
        action.addAttribute(AuthorizationConstants.ACTION_ID_ATTRIBUTE,
            AuthorizationConstants.TYPE_STRING, methodName);

        List<Entity> entities = new Vector<Entity>();
        entities.add(subject);
        entities.add(resource);
        entities.add(action);

        AuthorizationRequest authRequest = null;

        try {
            authRequest = authorizationService.createRequest(entities);
        } catch (XACMLRequestCreationException exp) {
            exp.printStackTrace();
        }

        boolean result = authorizationService.doAuthorization(authRequest);
        TestCase.assertEquals(true, result);
    }

    private void testNonCompanyAuthorization() {

        List<String> companyName = new Vector<String>();
        companyName.add("Target");

        String methodName = "addUser";

        String resourceName = "Walmart";

        Entity subject = new Entity(AuthorizationConstants.SUBJECT);

        subject.addAttribute(AuthorizationConstants.SUBJECT_COMPANY_ID,
            AuthorizationConstants.TYPE_STRING, companyName);

        Entity resource = new Entity(AuthorizationConstants.RESOURCE);

        resource.addAttribute(AuthorizationConstants.RESOURCE_COMPANY_ID,
            AuthorizationConstants.TYPE_STRING, resourceName);

        Entity action = new Entity(AuthorizationConstants.ACTION);
        action.addAttribute(AuthorizationConstants.ACTION_ID_ATTRIBUTE,
            AuthorizationConstants.TYPE_STRING, methodName);

        List<Entity> entities = new Vector<Entity>();
        entities.add(subject);
        entities.add(resource);
        entities.add(action);

        AuthorizationRequest authRequest = null;

        try {
            authRequest = authorizationService.createRequest(entities);
        } catch (XACMLRequestCreationException exp) {
            exp.printStackTrace();
        }

        boolean result = authorizationService.doAuthorization(authRequest);
        TestCase.assertEquals(false, result);
    }

    private void testUserAuthorization() {

        List<String> subjectName = new Vector<String>();
        subjectName.add("devdatta");

        String methodName = "addUser";

        String resourceName = "devdatta";

        Entity subject = new Entity(AuthorizationConstants.SUBJECT);

        subject.addAttribute(AuthorizationConstants.SUBJECT_ID_ATTRIBUTE,
            AuthorizationConstants.TYPE_STRING, subjectName);

        Entity resource = new Entity(AuthorizationConstants.RESOURCE);

        resource.addAttribute(AuthorizationConstants.RESOURCE_ID_ATTRIBUTE,
            AuthorizationConstants.TYPE_STRING, resourceName);

        Entity action = new Entity(AuthorizationConstants.ACTION);
        action.addAttribute(AuthorizationConstants.ACTION_ID_ATTRIBUTE,
            AuthorizationConstants.TYPE_STRING, methodName);

        List<Entity> entities = new Vector<Entity>();
        entities.add(subject);
        entities.add(resource);
        entities.add(action);

        AuthorizationRequest authRequest = null;

        try {
            authRequest = authorizationService.createRequest(entities);
        } catch (XACMLRequestCreationException exp) {
            exp.printStackTrace();
        }

        boolean result = authorizationService.doAuthorization(authRequest);
        TestCase.assertEquals(true, result);
    }

    private void testNonUserAuthorization() {

        List<String> subjectName = new Vector<String>();
        subjectName.add("devdatta");

        String methodName = "addUser";

        String resourceName = "kulkarni";

        Entity subject = new Entity(AuthorizationConstants.SUBJECT);

        subject.addAttribute(AuthorizationConstants.SUBJECT_ID_ATTRIBUTE,
            AuthorizationConstants.TYPE_STRING, subjectName);

        Entity resource = new Entity(AuthorizationConstants.RESOURCE);

        resource.addAttribute(AuthorizationConstants.RESOURCE_ID_ATTRIBUTE,
            AuthorizationConstants.TYPE_STRING, resourceName);

        Entity action = new Entity(AuthorizationConstants.ACTION);
        action.addAttribute(AuthorizationConstants.ACTION_ID_ATTRIBUTE,
            AuthorizationConstants.TYPE_STRING, methodName);

        List<Entity> entities = new Vector<Entity>();
        entities.add(subject);
        entities.add(resource);
        entities.add(action);

        AuthorizationRequest authRequest = null;

        try {
            authRequest = authorizationService.createRequest(entities);
        } catch (XACMLRequestCreationException exp) {
            exp.printStackTrace();
        }

        boolean result = authorizationService.doAuthorization(authRequest);
        TestCase.assertEquals(false, result);
    }

    private void testNullInputToCreateRequest() {
        try {
            AuthorizationRequest authRequest = authorizationService
                .createRequest(null);
            TestCase
                .fail("AuthorizationService should have thrown an exception for null input!");
        } catch (XACMLRequestCreationException exp) {

        }
    }

    private void testNullInputToDoAuthorization() {
        try {
            boolean result = authorizationService.doAuthorization(null);
            TestCase.assertEquals(false, result);
        } catch (ForbiddenException exp) {
            TestCase.assertEquals("Authorization request is null.", exp
                .getMessage());
        }
    }

    public List<Entity> prepareRequest(String subName, String resName,
        String method) {

        String subjectUsername = subName;
        String methodName = method;
        String resourceUsername = resName;

        List<Entity> entities = new Vector<Entity>();

        subject = new Entity(AuthorizationConstants.SUBJECT);
        subject.addAttribute(AuthorizationConstants.SUBJECT_ID_ATTRIBUTE,
            AuthorizationConstants.TYPE_STRING, subjectUsername);

        resource = new Entity(AuthorizationConstants.RESOURCE);
        resource.addAttribute(AuthorizationConstants.RESOURCE_ID_ATTRIBUTE,
            AuthorizationConstants.TYPE_STRING, resourceUsername);

        Entity action = new Entity(AuthorizationConstants.ACTION);
        action.addAttribute(AuthorizationConstants.ACTION_ID_ATTRIBUTE,
            AuthorizationConstants.TYPE_STRING, methodName);

        entities.add(subject);
        entities.add(resource);
        entities.add(action);

        return entities;
    }
}
