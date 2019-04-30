# -*- coding: utf-8 -*
from nose.plugins.attrib import attr
from qe_coverage.opencafe_decorators import tags, unless_coverage
from random import randrange

from tests.api.utils import func_helper
from tests.api.v2 import base
from tests.api.v2.models import factory, responses

from tests.api.v2.schema import tokens as tokens_json

from tests.package.johny import constants as const
from tests.package.johny.v2.models import requests


class TestValidateToken(base.TestBaseV2):

    """ Validate Token test"""

    @classmethod
    @unless_coverage
    def setUpClass(cls):
        """Class level set up for the tests
        Create users needed for the tests and generate clients for those users.
        """
        super(TestValidateToken, cls).setUpClass()
        cls.domain_id = func_helper.generate_randomized_domain_id(
            client=cls.identity_admin_client)
        cls.contact_id = randrange(start=const.CONTACT_ID_MIN,
                                   stop=const.CONTACT_ID_MAX)
        cls.user_admin_client = cls.generate_client(
            parent_client=cls.identity_admin_client,
            additional_input_data={'domain_id': cls.domain_id,
                                   'contact_id': cls.contact_id})
        cls.role_ids = []
        cls.tenant_ids = []
        cls.hierarchical_billing_observer_role_id = cls.get_role_id_by_name(
            role_name=const.HIERARCHICAL_BILLING_OBSERVER_ROLE_NAME)

    @unless_coverage
    def setUp(self):
        super(TestValidateToken, self).setUp()
        self.users = []

    @classmethod
    def get_role_id_by_name(cls, role_name):

        option = {
            const.PARAM_ROLE_NAME: role_name
        }
        get_role_resp = cls.identity_admin_client.list_roles(option=option)
        role_id = get_role_resp.json()[const.ROLES][0][const.ID]
        return role_id

    def create_role(self):

        role_obj = factory.get_add_role_request_object(
            administrator_role=const.USER_MANAGE_ROLE_NAME)
        add_role_resp = self.identity_admin_client.add_role(
            request_object=role_obj)
        self.assertEqual(add_role_resp.status_code, 201)
        role = responses.Role(add_role_resp.json())
        self.role_ids.append(role.id)
        return role

    def create_tenant(self, domain=None, name=None, tenant_types=None):

        if not domain:
            domain = self.domain_id
        if name:
            tenant_req = factory.get_add_tenant_object(
                domain_id=domain, tenant_name=name, tenant_types=tenant_types)
        else:
            tenant_req = factory.get_add_tenant_object(
                domain_id=domain, tenant_types=tenant_types)
        add_tenant_resp = self.identity_admin_client.add_tenant(
            tenant=tenant_req)
        self.assertEqual(add_tenant_resp.status_code, 201)
        tenant = responses.Tenant(add_tenant_resp.json())
        self.tenant_ids.append(tenant.id)
        return tenant

    def set_up_role_tenant(self):
        # create role
        role_1 = self.create_role()

        # create tenant with type for which there is a whitelist filter
        tenant_name = ":".join([
            self.test_config.mpc_whitelist_tenant_type,
            self.generate_random_string(pattern=const.TENANT_NAME_PATTERN)])
        tenant_1 = self.create_tenant(
            name=tenant_name,
            tenant_types=[self.test_config.mpc_whitelist_tenant_type])
        return role_1, tenant_1

    @attr(type='smoke_alpha')
    @tags('positive', 'p0', 'smoke')
    def test_validate_token_reports_contact_id(self):
        """Check for contact id in validate token response
        """
        uac = self.user_admin_client
        resp = self.user_admin_client.validate_token(
            token_id=uac.default_headers[const.X_AUTH_TOKEN])
        self.assertEqual(resp.status_code, 200)
        self.assertSchema(response=resp,
                          json_schema=tokens_json.validate_token)
        self.assertEqual(resp.json()[const.ACCESS][
            const.USER][const.RAX_AUTH_CONTACTID], str(self.contact_id))

    @tags('positive', 'p0')
    def test_validate_token_mpc(self):
        role_1, tenant_1 = self.set_up_role_tenant()

        # Add Sub user
        self.sub_user_client = self.generate_client(
            parent_client=self.user_admin_client,
            additional_input_data={'domain_id': self.domain_id})
        self.users.append(
            self.sub_user_client.default_headers[const.X_USER_ID])

        # Add Role to user on tenant
        resp = self.identity_admin_client.add_role_to_user_for_tenant(
            tenant_id=tenant_1.id,
            user_id=self.sub_user_client.default_headers[const.X_USER_ID],
            role_id=role_1.id)
        self.assertEqual(resp.status_code, 200)

        # Add white listed Role to user on tenant
        resp = self.identity_admin_client.add_role_to_user_for_tenant(
            tenant_id=tenant_1.id,
            user_id=self.sub_user_client.default_headers[const.X_USER_ID],
            role_id=self.hierarchical_billing_observer_role_id)
        self.assertEqual(resp.status_code, 200)

        # Authenticates as tenant with token
        tenant_token_req = requests.AuthenticateAsTenantWithToken(
            token_id=self.sub_user_client.default_headers[const.X_AUTH_TOKEN],
            tenant_id=tenant_1.id)
        resp = self.sub_user_client.get_auth_token(
            request_object=tenant_token_req)
        tenant_token = resp.json()[const.ACCESS][const.TOKEN][const.ID]
        resp = self.sub_user_client.validate_token(token_id=tenant_token)
        self.assertEqual(resp.status_code, 200)

        roles = resp.json()[const.ACCESS][const.USER][const.ROLES]
        roles_on_wl_tenant = [
            role for role in roles if
            const.TENANT_ID in role.keys() and
            tenant_1.id == role[const.TENANT_ID]]
        self.assertGreater(len(roles_on_wl_tenant), 0)

        # Delete Role from user on tenant
        resp = self.identity_admin_client.delete_role_from_user_for_tenant(
            tenant_id=tenant_1.id,
            user_id=self.sub_user_client.default_headers[const.X_USER_ID],
            role_id=self.hierarchical_billing_observer_role_id)
        self.assertEqual(resp.status_code, 204)

        resp = self.sub_user_client.validate_token(token_id=tenant_token)
        self.assertEqual(resp.status_code, 200)
        roles = resp.json()[const.ACCESS][const.USER][const.ROLES]
        roles_on_wl_tenant = [
            role for role in roles if
            const.TENANT_ID in role.keys() and
            self.test_config.mpc_whitelist_tenant_type
            in role[const.TENANT_ID]]
        self.assertEqual(len(roles_on_wl_tenant), 0)

    @tags('positive', 'p0', 'regression')
    def test_analyze_user_token(self):
        user_token = self.user_admin_client.default_headers[const.X_AUTH_TOKEN]

        # The identity_admin used should have the 'analyze-token' role inorder
        # to use the analyze token endpoint.
        self.identity_admin_client.default_headers[const.X_SUBJECT_TOKEN] = \
            user_token
        analyze_token_resp = self.identity_admin_client.analyze_token()
        self.assertEqual(analyze_token_resp.status_code, 200)
        self.assertSchema(response=analyze_token_resp,
                          json_schema=tokens_json.analyze_valid_token)

    @tags('positive', 'p0', 'regression')
    def test_analyze_invalid_token(self):
        # The identity_admin used should have the 'analyze-token' role inorder
        # to use the analyze token endpoint.
        self.identity_admin_client.default_headers[const.X_SUBJECT_TOKEN] = \
            'apples_bananas'
        analyze_token_resp = self.identity_admin_client.analyze_token()
        self.assertEqual(analyze_token_resp.status_code, 200)
        self.assertSchema(response=analyze_token_resp,
                          json_schema=tokens_json.analyze_token)

    @unless_coverage
    def tearDown(self):
        super(TestValidateToken, self).tearDown()
        for user_id in self.users:
            self.user_admin_client.delete_user(user_id=user_id)

    @classmethod
    @unless_coverage
    def tearDownClass(cls):
        # Delete all users created in the setUpClass
        cls.delete_client(client=cls.user_admin_client,
                          parent_client=cls.identity_admin_client)
        super(TestValidateToken, cls).tearDownClass()
