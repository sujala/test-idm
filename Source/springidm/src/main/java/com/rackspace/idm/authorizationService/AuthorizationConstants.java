package com.rackspace.idm.authorizationService;


public class AuthorizationConstants {

    // Entities
    public static String SUBJECT = "SUBJECT";
    public static String RESOURCE = "RESOURCE";
    public static String ACTION = "ACTION";
    public static String ENVIRONMENT = "ENVIRONMENT";

    // Types
    public static String TYPE_STRING = "urn:oasis:names:tc:xacml:1.0:string";

    // Attributes
    public static String SUBJECT_ID_ATTRIBUTE = "urn:oasis:names:tc:xacml:1.0:subject:subject-id";
    public static String SUBJECT_ID_QUALIFIER = "urn:oasis:names:tc:xacml:1.0:subject:subject-id-qualifier";
    public static String SUBJECT_ROLE_ATTRIBUTE = "role";
    public static String SUBJECT_COMPANY_ID = "companyId";
    public static String SUBJECT_ROLE_COMPANY_ID = "role-for-company";
    public static String RESOURCE_ID_ATTRIBUTE = "urn:oasis:names:tc:xacml:1.0:resource:resource-id";
    public static String ACTION_ID_ATTRIBUTE = "urn:oasis:names:tc:xacml:1.0:action:action-id";

    public static String RESOURCE_COMPANY_ID = "resourceCompanyId";
    public static String RACKSPACE_COMPANY_ID = "RCN-000-000-000"; 
    
    public static String ADMIN_ROLE = "Admin";

}