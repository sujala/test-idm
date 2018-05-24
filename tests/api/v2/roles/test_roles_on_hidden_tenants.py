from nose.plugins.attrib import attr

from tests.api.utils import func_helper
from tests.api.v2 import base
from tests.api.v2.models import factory, responses
from tests.package.johny import constants as const
from tests.package.johny.v2.models import requests


class RolesOnHiddenTenantsTests(base.TestBaseV2):
    """
    Tests to verify that when apply_rcn_roles=true, the de-normalized roles
    for default users are shown appropriately for hidden & non-hidden tenants
    """

    @classmethod
    def setUpClass(cls):
        """
        SetUpClass() is creating user admin & default user for the tests.
        It also creates two tenants with hidden tenant type & adds a role
        to default user on one of them. This class has tests for services
        affected by CID-1286 like auth & list roles for user on tenant,
        when 'apply_rcn_roles' query param is provided.
        """

        super(RolesOnHiddenTenantsTests, cls).setUpClass()
        cls.domain_id = func_helper.generate_randomized_domain_id(
            client=cls.identity_admin_client, pattern=const.DOMAIN_PATTERN)
        cls.user_admin_client = cls.generate_client(
            parent_client=cls.identity_admin_client,
            additional_input_data={'domain_id': cls.domain_id})

        # Creating sub user in user-admin's domain
        cls.sub_username = cls.generate_random_string(const.SUB_USER_PATTERN)
        cls.sub_user_pwd = cls.generate_random_string(const.PASSWORD_PATTERN)
        user_req = factory.get_add_user_request_object(
            username=cls.sub_username,
            input_data={'password': cls.sub_user_pwd})
        sub_user_create = cls.user_admin_client.add_user(
            request_object=user_req)
        assert sub_user_create.status_code == 201, (
            'default user creation failed')
        cls.sub_user_id = sub_user_create.json()[const.USER][const.ID]

        # Creating two tenants with excluded tenant type
        tenant_1_name = cls.generate_random_string(pattern=':'.join([
            const.TENANT_TYPE_PROTECTED_PREFIX, const.TENANT_NAME_PATTERN]))
        tenant_1_req = factory.get_add_tenant_request_object(
            tenant_name=tenant_1_name, domain_id=cls.domain_id,
            tenant_types=const.EXCLUDED_TENANT_TYPES)
        cls.tenant_1_resp = cls.identity_admin_client.add_tenant(
            tenant=tenant_1_req)
        assert cls.tenant_1_resp.status_code == 201, 'tenant 1 creation failed'
        cls.tenant_1 = responses.Tenant(cls.tenant_1_resp.json())

        tenant_2_name = cls.generate_random_string(pattern=':'.join([
            const.TENANT_TYPE_PROTECTED_PREFIX, const.TENANT_NAME_PATTERN]))
        tenant_2_req = factory.get_add_tenant_request_object(
            tenant_name=tenant_2_name, domain_id=cls.domain_id,
            tenant_types=const.EXCLUDED_TENANT_TYPES)
        cls.tenant_2_resp = cls.identity_admin_client.add_tenant(
            tenant=tenant_2_req)
        assert cls.tenant_2_resp.status_code == 201, 'tenant 2 creation failed'
        cls.tenant_2 = responses.Tenant(cls.tenant_2_resp.json())

        role_req = factory.get_add_role_request_object(
            administrator_role=const.USER_MANAGE_ROLE_NAME)
        role_resp = cls.identity_admin_client.add_role(request_object=role_req)
        assert role_resp.status_code == 201, 'role creation failed'
        cls.role = responses.Role(role_resp.json())

        # add explicit role to sub user on one of the tenant (tenant_1)
        cls.identity_admin_client.add_role_to_user_for_tenant(
            tenant_id=cls.tenant_1.id, user_id=cls.sub_user_id,
            role_id=cls.role.id)

        # Since list roles for user on tenant does not return role names,
        # looking up role ids
        option = {
            'roleName': const.TENANT_ACCESS_ROLE_NAME
        }
        tenant_access_role = cls.identity_admin_client.list_roles(
            option=option)
        cls.tenant_access_role_id = tenant_access_role.json()[
            const.ROLES][0][const.ID]

    @attr(type='smoke_alpha')
    def test_auth_resp_shows_role_on_hidden_tenant(self):
        sub_user_auth_req = requests.AuthenticateWithPassword(
            user_name=self.sub_username, password=self.sub_user_pwd)
        sub_user_auth_resp = self.identity_admin_client.get_auth_token(
            sub_user_auth_req)
        tenant_access_role_present = False
        for role in sub_user_auth_resp.json()[
          const.ACCESS][const.USER][const.ROLES]:
            if role[const.NAME] == const.TENANT_ACCESS_ROLE_NAME:
                tenant_access_role_present = True
            if role[const.NAME] == const.USER_DEFAULT_ROLE_NAME:
                self.assertNotIn(const.TENANT_ID, role)
        self.assertFalse(tenant_access_role_present, (
            'default user got the tenant access role'))

        option = {
            'apply_rcn_roles': 'true'
        }
        sub_user_auth_resp = self.identity_admin_client.get_auth_token(
            sub_user_auth_req, option=option)
        for role in sub_user_auth_resp.json()[
          const.ACCESS][const.USER][const.ROLES]:
            if role[const.NAME] == const.TENANT_ACCESS_ROLE_NAME:
                tenant_access_role_present = True
            if role[const.NAME] == const.USER_DEFAULT_ROLE_NAME:
                self.assertEqual(role[const.TENANT_ID], self.tenant_1.id)
        self.assertFalse(tenant_access_role_present, (
            'default user got the tenant access role'))

    @attr(type='smoke_alpha')
    def test_list_roles_for_user_on_hidden_tenants(self):

        list_resp = self.identity_admin_client.list_roles_for_user_on_tenant(
            tenant_id=self.tenant_1.id, user_id=self.sub_user_id)

        role_ids = [role[const.ID] for role in list_resp.json()[
            const.ROLES]]
        self.assertNotIn(self.tenant_access_role_id, role_ids, (
            'default user got the tenant access role'))

        # Now, with apply_rcn_roles
        option = {
            'apply_rcn_roles': 'true'
        }
        self.verify_roles_for_user_on_given_tenant(
            tenant=self.tenant_1, user_id=self.sub_user_id, option=option,
            role_present=True)

        # Since user does not have an explicit role assigned on tenant 2,
        # de-normalized roles won't show up on that tenant
        self.verify_roles_for_user_on_given_tenant(
            tenant=self.tenant_2, user_id=self.sub_user_id, option=option,
            role_present=False)

    def verify_roles_for_user_on_given_tenant(self, tenant, user_id, option,
                                              role_present=True):

        list_resp = self.identity_admin_client.list_roles_for_user_on_tenant(
            tenant_id=tenant.id, user_id=user_id, option=option)
        tenant_access_role_present = False
        default_user_role_present = False
        for role in list_resp.json()[const.ROLES]:
            if role[const.ID] == self.tenant_access_role_id:
                tenant_access_role_present = True
            if role[const.ID] == const.USER_DEFAULT_ROLE_ID:
                default_user_role_present = True
        self.assertFalse(tenant_access_role_present, (
                'default user got the tenant access role on the tenant'))
        if role_present:
            self.assertTrue(default_user_role_present, (
                'default user did not get the expected role on the tenant'))
        else:
            self.assertFalse(default_user_role_present, (
                'default user got the unexpected role on the tenant'))

    @classmethod
    def tearDownClass(cls):
        super(RolesOnHiddenTenantsTests, cls).tearDownClass()
        cls.identity_admin_client.delete_user(cls.sub_user_id)
        cls.delete_client(client=cls.user_admin_client,
                          parent_client=cls.identity_admin_client)
        cls.identity_admin_client.delete_role(cls.role.id)
