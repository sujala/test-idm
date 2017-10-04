# -*- coding: utf-8 -*
import ddt
import copy

from tests.api.v2 import base
from tests.api.v2.schema import roles as roles_json
from tests.api.v2.schema import tokens as token_json
from tests.api.v2.models import factory, responses


from tests.package.johny import constants as const
from tests.package.johny.v2.models import requests


@ddt.ddt
class TestListRoleWTypeTenantTypesNAssignment(base.TestBaseV2):
    """
    1. For each returned role from List Roles (http://docs-internal.rackspace.
    com/auth/api/v2.0/auth-admin-devguide/content/GET_listRoles__v2.0_OS
    -KSADM_roles_Role_Calls.html) include the following attributes as
    applicable:
    - assignment (added in CID-438)
    - type (CID-481)
    - tenant type (CID-481)
    1.1 Assignment must always be returned. When not set on the role, must
    return whatever value is considered 'both' by CID-438
    1.2 type must always be returned. When not set on the role, must return
    whatever value is considered 'standard' by CID-481
    1.3 Tenant Type must only be returned when type is 'rcn'

    2. Do not return assignment, type, or tenant type attributes on roles in
    responses for:
    2.1 List User Global Roles (
    http://docs-internal.rackspace.com/auth/api/v2.0/auth-admin-devguide
    /content/GET_listUserGlobalRoles__v2.0_users__userId__roles_
    Role_Calls.html)
    2.2 List Roles for User on Tenant (
    http://docs-internal.rackspace.com/auth/api/v2.0/auth-admin-devguide
    /content/GET_listRolesForUserOnTenant_v2.0_tenants__tenantId__users__
    userId__roles_Tenant_Calls.html)
    2.3 Authenticate
    2.4 Validate

    3. Changes to the above services as specified must be enabled via a
    feature flag 'feature.list.support.additional.role.properties'
    3.1 Hardcoded default value set to true
    3.2 Default docker config set to true
    """

    @classmethod
    def setUpClass(cls):
        super(TestListRoleWTypeTenantTypesNAssignment, cls).setUpClass()
        cls.feature_flag_role_properties = (
            const.FEATURE_LIST_SUPPORT_ADDITIONAL_ROLE_PROPERTIES)
        cls.user_ids = []
        cls.tenant_ids = []
        cls.domain_ids = []
        cls.tenant_type_ids = []
        cls.role_ids = []

        # create user
        user_resp = cls.create_user()
        cls.user_id = user_resp.json()[const.USER][const.ID]
        cls.user_ids.append(cls.user_id)
        cls.user_name = user_resp.json()[const.USER][const.USERNAME]
        cls.password = user_resp.json()[const.USER][const.NS_PASSWORD]
        domain_id = user_resp.json()[const.USER][const.RAX_AUTH_DOMAIN_ID]
        cls.domain_ids.append(domain_id)
        cls.tenant_id = domain_id

    def setUp(self):
        super(TestListRoleWTypeTenantTypesNAssignment, self).setUp()
        self.add_schema_fields = [const.RAX_AUTH_ROLE_TYPE,
                                  const.RAX_AUTH_ASSIGNMENT]

    @classmethod
    def create_user(self):
        """one call logic"""
        user_name = self.generate_random_string(
            pattern=const.USER_NAME_PATTERN)
        domain_id = self.generate_random_string(pattern=const.NUMBERS_PATTERN)
        add_obj = factory.get_add_user_request_object_pull(
            user_name=user_name, domain_id=domain_id)
        resp = self.identity_admin_client.add_user(request_object=add_obj)
        return resp

    def create_tenant_type(self, name):
        request_object = requests.TenantType(name, 'description')
        self.service_admin_client.add_tenant_type(tenant_type=request_object)
        self.tenant_type_ids.append(name.lower())

    def verify_role_from_auth_and_token_validation_resp(self, resp):
        self.assertEqual(resp.status_code, 200)
        self.assertSchema(response=resp,
                          json_schema=token_json.validate_token)
        for role in resp.json()[const.ACCESS][const.USER][const.ROLES]:
            self.assertNotIn(const.NS_TYPES, role)
            self.assertNotIn(const.RAX_AUTH_ROLE_TYPE, role)
            self.assertNotIn(const.RAX_AUTH_ASSIGNMENT, role)

    def test_list_roles(self):
        # list role

        if not self.test_config.run_service_admin_tests:
            self.skipTest('Skipping Service Admin Tests per config value')
        param = {
            'limit': 200
        }
        list_resp = self.identity_admin_client.list_roles(option=param)
        self.assertEqual(list_resp.status_code, 200)
        if self.devops_client.get_feature_flag(
                flag_name=self.feature_flag_role_properties):
            cp_item_schema = copy.deepcopy(roles_json.role_item)
            cp_item_schema[const.REQUIRED] = (
                roles_json.role_item[const.REQUIRED] + self.add_schema_fields)
            cp_list_schema = copy.deepcopy(roles_json.list_roles)
            cp_list_schema[const.PROPERTIES][const.ROLES][const.ITEMS] = (
                cp_item_schema
            )

            self.assertSchema(response=list_resp, json_schema=cp_list_schema)

            for role in list_resp.json()[const.ROLES]:
                if role[const.RAX_AUTH_ROLE_TYPE] == const.RCN:
                    self.assertIn(const.NS_TYPES, role)
                    self.assertEqual(role[const.RAX_AUTH_ASSIGNMENT],
                                     const.ROLE_ASSIGNMENT_TYPE_GLOBAL)
                else:
                    self.assertNotIn(const.NS_TYPES, role)
                    self.assertIn(role[const.RAX_AUTH_ASSIGNMENT],
                                  [const.ROLE_ASSIGNMENT_TYPE_BOTH,
                                   const.ROLE_ASSIGNMENT_TYPE_TENANT,
                                   const.ROLE_ASSIGNMENT_TYPE_GLOBAL])
        else:
            self.assertSchema(response=list_resp,
                              json_schema=roles_json.list_roles)
            for role in list_resp.json()[const.ROLES]:
                self.assertNotIn(const.NS_TYPES, role)
                self.assertNotIn(const.RAX_AUTH_ROLE_TYPE, role)
                self.assertNotIn(const.RAX_AUTH_ASSIGNMENT, role)

    def test_list_user_global_role(self):
        # list user global roles
        user_id = self.identity_admin_client.default_headers[const.X_USER_ID]
        resp = self.identity_admin_client.list_roles_for_user(
            user_id=user_id)
        self.assertEqual(resp.status_code, 200)
        self.assertSchema(response=resp, json_schema=roles_json.list_roles)
        for role in resp.json()[const.ROLES]:
            self.assertNotIn(const.NS_TYPES, role)
            self.assertNotIn(const.RAX_AUTH_ROLE_TYPE, role)
            self.assertNotIn(const.RAX_AUTH_ASSIGNMENT, role)

    @ddt.data(True, False)
    def test_list_global_roles_for_user_with_query_param(self, apply_rcn):

        if not self.test_config.run_service_admin_tests:
            self.skipTest('Skipping Service Admin Tests per config value')
        tenant_type = self.generate_random_string(
            pattern=const.TENANT_TYPE_PATTERN)
        self.create_tenant_type(name=tenant_type)

        # create an RCN role
        role_obj = requests.RoleAdd(
            role_name=self.generate_random_string(
                pattern=const.ROLE_NAME_PATTERN), role_type=const.RCN,
            tenant_types=[tenant_type])
        add_role_resp = self.identity_admin_client.add_role(
            request_object=role_obj)
        self.assertEqual(add_role_resp.status_code, 201)
        rcn_role = responses.Role(add_role_resp.json())
        self.role_ids.append(rcn_role.id)

        # create a user
        create_user_resp = self.create_user()
        self.assertEqual(create_user_resp.status_code, 201)
        created_user = responses.User(create_user_resp.json())
        self.user_ids.append(created_user.id)

        # add the RCN role to the user
        resp = self.identity_admin_client.add_role_to_user(
            role_id=rcn_role.id, user_id=created_user.id
        )
        self.assertEqual(resp.status_code, 200)

        # list global roles for user
        global_roles_resp = self.identity_admin_client.list_roles_for_user(
            user_id=created_user.id, apply_rcn_roles=apply_rcn)
        self.assertEqual(global_roles_resp.status_code, 200)
        list_of_roles = [role[const.ID] for role in global_roles_resp.json()[
            const.ROLES]]

        # Not adding schema validation as of now, because this check is
        # more than sufficient to validate the api response
        if apply_rcn:
            self.assertNotIn(rcn_role.id, list_of_roles)
        else:
            self.assertIn(rcn_role.id, list_of_roles)

    def test_list_role_for_user_on_tenant(self):
        # list role for user on tenant
        roles_tenant_resp = (
            self.identity_admin_client.list_roles_for_user_on_tenant(
                tenant_id=self.tenant_id, user_id=self.user_id)
        )
        self.assertEqual(roles_tenant_resp.status_code, 200)
        for role in roles_tenant_resp.json()[const.ROLES]:
            self.assertNotIn(const.NS_TYPES, role)
            self.assertNotIn(const.RAX_AUTH_ROLE_TYPE, role)
            self.assertNotIn(const.RAX_AUTH_ASSIGNMENT, role)

    def test_auth_and_validation(self):
        # verify authenticate
        auth_obj = requests.AuthenticateWithPassword(user_name=self.user_name,
                                                     password=self.password)
        auth_resp = self.identity_admin_client.get_auth_token(
            request_object=auth_obj)
        self.verify_role_from_auth_and_token_validation_resp(auth_resp)
        token_id = auth_resp.json()[const.ACCESS][const.TOKEN][const.ID]

        # verify validate token
        validate_resp = self.identity_admin_client.validate_token(
            token_id=token_id)
        self.verify_role_from_auth_and_token_validation_resp(validate_resp)

    @classmethod
    def tearDownClass(cls):
        # Delete all resources created in the tests
        for id_ in cls.user_ids:
            cls.identity_admin_client.delete_user(user_id=id_)
        for id_ in cls.tenant_ids:
            cls.identity_admin_client.delete_tenant(tenant_id=id_)
        for id_ in cls.role_ids:
            # Using identity admin here, because the role being cleaned up
            # has administrative-role as 'identity:admin'. But, if other
            # roles are to be cleaned up, we may need to update the client
            cls.identity_admin_client.delete_role(role_id=id_)
        for name in cls.tenant_type_ids:
            cls.service_admin_client.delete_tenant_type(name=name)
        for id_ in cls.domain_ids:
            disable_domain_req = requests.Domain(enabled=False)
            cls.identity_admin_client.update_domain(
                domain_id=id_, request_object=disable_domain_req)
            cls.identity_admin_client.delete_domain(domain_id=id_)
        super(TestListRoleWTypeTenantTypesNAssignment, cls).tearDownClass()
