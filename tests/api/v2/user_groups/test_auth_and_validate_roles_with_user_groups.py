"""
  JIRA CID-507
  A porting of Jmeter scripts v2.0 token related smoke tests to Johny

  *  IdentityAdmin validates token of Useradmin from password login.
  *  IdentityAdmin lists the groups of the userAdmin.
  *  Make sure Useradmin can login via username+apikey
  *  Make sure useradmin can login via tenantID+username+password and that
     identityAdmin validates the resulting token
"""

import pytest
from qe_coverage.opencafe_decorators import tags, unless_coverage

from tests.api.v2.user_groups import usergroups
from tests.api.v2.models import responses
from tests.api.v2.schema import tokens as tokens_json
from tests.api.v2.schema import user_groups

from tests.package.johny import constants as const
from tests.package.johny.v2.models import requests


class AuthAndValidateRolesWithUserGroups(usergroups.TestUserGroups):
    """
    Auth and validate roles with user groups
    """
    @classmethod
    @unless_coverage
    def setUpClass(cls):
        super(AuthAndValidateRolesWithUserGroups, cls).setUpClass()

    @unless_coverage
    def setUp(self):
        super(AuthAndValidateRolesWithUserGroups, self).setUp()
        self.user_admin_wl_domain_client = None

    @tags('positive', 'p0', 'regression')
    def test_auth_and_validate_no_groups(self):
        self.user_admin_wl_domain_client = self.generate_client(
            parent_client=self.identity_admin_client,
            additional_input_data={
                'domain_id': self.domain_id,
                'password': self.password,
                'user_name': self.user_name
            })

        auth_obj = requests.AuthenticateWithPassword(user_name=self.user_name,
                                                     password=self.password)

        resp = self.identity_admin_client.get_auth_token(
            request_object=auth_obj)
        self.assertEqual(resp.status_code, 200)

        if self.test_config.deserialize_format == const.JSON:
            self.assertSchema(response=resp,
                              json_schema=tokens_json.auth)
        access_resp = responses.Access(resp.json())
        self.assertEqual(len(access_resp.access.user.roles), 1)
        self.assertEqual(access_resp.access.user.roles[0].name,
                         const.USER_ADMIN_ROLE_NAME)

        # get token from auth
        token = access_resp.access.token.id

        # Validate token
        resp = self.identity_admin_client.validate_token(token)
        self.assertEqual(resp.status_code, 200)
        if self.test_config.deserialize_format == const.JSON:
            self.assertSchema(response=resp,
                              json_schema=tokens_json.validate_token)

        # validate only identity:user-admin role is set
        access_resp = responses.Access(resp.json())
        self.assertEqual(len(access_resp.access.user.roles), 1)
        self.assertEqual(access_resp.access.user.roles[0].name,
                         const.USER_ADMIN_ROLE_NAME)

    @tags('positive', 'p0', 'regression')
    def test_auth_and_validate_group_no_roles(self):
        self.user_admin_wl_domain_client = self.generate_client(
            parent_client=self.identity_admin_client,
            additional_input_data={
                'domain_id': self.domain_id,
                'password': self.password,
                'user_name': self.user_name
            })

        self.create_and_add_user_group_to_user(
            self.user_admin_wl_domain_client)
        auth_obj = requests.AuthenticateWithPassword(user_name=self.user_name,
                                                     password=self.password)

        resp = self.identity_admin_client.get_auth_token(
            request_object=auth_obj)
        self.assertEqual(resp.status_code, 200)

        if self.test_config.deserialize_format == const.JSON:
            self.assertSchema(response=resp,
                              json_schema=tokens_json.auth)
        access_resp = responses.Access(resp.json())
        self.assertEqual(len(access_resp.access.user.roles), 1)
        self.assertEqual(access_resp.access.user.roles[0].name,
                         const.USER_ADMIN_ROLE_NAME)

        # get token from auth
        token = access_resp.access.token.id

        # Validate token
        resp = self.identity_admin_client.validate_token(token)
        self.assertEqual(resp.status_code, 200)
        if self.test_config.deserialize_format == const.JSON:
            self.assertSchema(response=resp,
                              json_schema=tokens_json.validate_token)

        # validate only identity:user-admin role is set
        access_resp = responses.Access(resp.json())
        self.assertEqual(len(access_resp.access.user.roles), 1)
        self.assertEqual(access_resp.access.user.roles[0].name,
                         const.USER_ADMIN_ROLE_NAME)

    @tags('positive', 'p0', 'smoke')
    @pytest.mark.smoke_alpha
    def test_auth_and_validate_group_with_roles(self):
        self.user_admin_wl_domain_client = self.generate_client(
            parent_client=self.identity_admin_client,
            additional_input_data={
                'domain_id': self.domain_id,
                'password': self.password,
                'user_name': self.user_name
            })

        group = self.create_and_add_user_group_to_user(
            self.user_admin_wl_domain_client)
        # create roles
        role_1 = self.create_role()
        role_2 = self.create_role()

        # create tenant
        tenant_1 = self.create_tenant()

        # create tenant assignments request dicts
        tenant_assignment_req_1 = self.generate_tenants_assignment_dict(
            role_1.id, tenant_1.id)
        tenant_assignment_req_2 = self.generate_tenants_assignment_dict(
            role_2.id, "*")
        tenants_role_assignment_req = requests.TenantRoleAssignments(
            tenant_assignment_req_1, tenant_assignment_req_2)

        assignment_resp = (self.user_admin_wl_domain_client
                           .add_tenant_role_assignments_to_user_group(
                            domain_id=self.domain_id, group_id=group.id,
                            request_object=tenants_role_assignment_req))
        self.assertEqual(assignment_resp.status_code, 200)
        self.assertSchema(
            assignment_resp,
            json_schema=user_groups.tenants_role_assignments_for_user_group)

        auth_obj = requests.AuthenticateWithPassword(user_name=self.user_name,
                                                     password=self.password)

        resp = self.identity_admin_client.get_auth_token(
            request_object=auth_obj)
        self.assertEqual(resp.status_code, 200)

        if self.test_config.deserialize_format == const.JSON:
            self.assertSchema(response=resp,
                              json_schema=tokens_json.auth)
        access_resp = responses.Access(resp.json())
        role_name_list = [role.name for role in access_resp.access.user.roles]
        self.assertEqual(len(role_name_list), 4)
        self.assertIn(role_1.name, role_name_list)
        self.assertIn(role_2.name, role_name_list)
        self.assertIn(const.TENANT_ACCESS_ROLE_NAME, role_name_list)
        self.assertIn(const.USER_ADMIN_ROLE_NAME, role_name_list)

        # get token from auth
        token = access_resp.access.token.id

        # Validate token
        resp = self.identity_admin_client.validate_token(token)
        self.assertEqual(resp.status_code, 200)
        if self.test_config.deserialize_format == const.JSON:
            self.assertSchema(response=resp,
                              json_schema=tokens_json.validate_token)

        # validate only identity:user-admin role is set
        access_resp = responses.Access(resp.json())
        role_name_list = [role.name for role in access_resp.access.user.roles]
        self.assertEqual(len(role_name_list), 4)
        self.assertIn(role_1.name, role_name_list)
        self.assertIn(role_2.name, role_name_list)
        self.assertIn(const.TENANT_ACCESS_ROLE_NAME, role_name_list)
        self.assertIn(const.USER_ADMIN_ROLE_NAME, role_name_list)

    @tags('positive', 'p0', 'smoke')
    @pytest.mark.smoke_alpha
    def test_auth_and_validate_group_domain_not_whitelisted(self):
        domain_id = self.generate_random_string(
            pattern=const.DOMAIN_PATTERN
        )
        self.user_admin_wl_domain_client = self.generate_client(
            parent_client=self.identity_admin_client,
            additional_input_data={
                'domain_id': domain_id,
                'password': self.password,
                'user_name': self.user_name
            })
        self.create_and_add_user_group_to_user(
            self.user_admin_wl_domain_client, 404)

    @unless_coverage
    def tearDown(self):
        # This deletes the domain which automatically deletes any user groups
        # in that domain. Hence, not explicitly deleting the user groups
        self.delete_client(self.user_admin_wl_domain_client,
                           parent_client=self.identity_admin_client)
        super(AuthAndValidateRolesWithUserGroups, self).tearDown()

    @classmethod
    @unless_coverage
    def tearDownClass(cls):
        super(AuthAndValidateRolesWithUserGroups, cls).tearDownClass()
