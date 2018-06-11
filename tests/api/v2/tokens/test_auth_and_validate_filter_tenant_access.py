"""
  JIRA CID-1239
  User-default does not properly remove all `tenant-access` roles
  from roles list on validate call
"""

from nose.plugins.attrib import attr
from qe_coverage.opencafe_decorators import tags, unless_coverage

from tests.api.utils import func_helper
from tests.api.v2 import base
from tests.api.v2.models import responses, factory
from tests.api.v2.schema import tokens as tokens_json

from tests.package.johny import constants as const
from tests.package.johny.v2.models import requests


class AuthAndValidateTokens(base.TestBaseV2):
    """
    AuthAndValidateTokens
    """
    @classmethod
    @unless_coverage
    def setUpClass(cls):
        super(AuthAndValidateTokens, cls).setUpClass()
        cls.domain_id = func_helper.generate_randomized_domain_id(
            client=cls.identity_admin_client)

        cls.user_admin_client = cls.generate_client(
            parent_client=cls.identity_admin_client,
            additional_input_data={'domain_id': cls.domain_id})

        # user manage client
        cls.user_manage_client = cls.generate_client(
            parent_client=cls.user_admin_client,
            additional_input_data={'domain_id': cls.domain_id,
                                   'is_user_manager': True})

        # user default client
        cls.user_default_client = cls.generate_client(
            parent_client=cls.user_manage_client,
            additional_input_data={'domain_id': cls.domain_id})

        cls.tenant = cls.create_tenant_with_faws_prefix(
            cls.domain_id)

    @unless_coverage
    def setUp(self):
        super(AuthAndValidateTokens, self).setUp()

    @classmethod
    def create_tenant_with_faws_prefix(cls, domain_id=None):
        if domain_id is None:
            domain_id = cls.domain_id
        tenant_name = const.TENANT_TYPE_PROTECTED_PREFIX + ':{}'.format(
            cls.generate_random_string(
                pattern=const.TENANT_NAME_PATTERN))
        tenant_req = factory.get_add_tenant_object(tenant_name=tenant_name,
                                                   domain_id=domain_id)
        add_tenant_resp = cls.identity_admin_client.add_tenant(
            tenant=tenant_req)
        assert add_tenant_resp.status_code == 201
        return responses.Tenant(add_tenant_resp.json())

    @tags('positive', 'p0', 'smoke')
    @attr(type='smoke_alpha')
    def test_auth_useradmin_token(self):
        resp = self.identity_admin_client.validate_token(
            self.user_admin_client.default_headers[const.X_AUTH_TOKEN]
        )
        self.assertEqual(resp.status_code, 200)
        if self.test_config.deserialize_format == const.JSON:
            self.assertSchema(response=resp,
                              json_schema=tokens_json.validate_token)
        roles = resp.json()[const.ACCESS][const.USER][const.ROLES]
        faws_tenant_access_role = next(
            (role for role in roles
             if role[const.NAME] == const.TENANT_ACCESS_ROLE_NAME and
             str(role[const.TENANT_ID]).startswith(
                 const.TENANT_TYPE_PROTECTED_PREFIX + ":")), False)
        self.assertIsNotNone(faws_tenant_access_role)

    @tags('positive', 'p0', 'smoke')
    @attr(type='smoke_alpha')
    def test_auth_usermanage_token(self):
        resp = self.identity_admin_client.validate_token(
            self.user_manage_client.default_headers[const.X_AUTH_TOKEN]
        )
        self.assertEqual(resp.status_code, 200)
        if self.test_config.deserialize_format == const.JSON:
            self.assertSchema(response=resp,
                              json_schema=tokens_json.validate_token)
        roles = resp.json()[const.ACCESS][const.USER][const.ROLES]
        faws_tenant_access_role = next(
            (role for role in roles
             if role[const.NAME] == const.TENANT_ACCESS_ROLE_NAME and
             str(role[const.TENANT_ID]).startswith(
                 const.TENANT_TYPE_PROTECTED_PREFIX + ":")), False)
        self.assertIsNotNone(faws_tenant_access_role)

    @tags('positive', 'p0', 'smoke')
    def test_auth_userdefault_token(self):
        resp = self.identity_admin_client.validate_token(
            self.user_default_client.default_headers[const.X_AUTH_TOKEN]
        )
        self.assertEqual(resp.status_code, 200)
        if self.test_config.deserialize_format == const.JSON:
            self.assertSchema(response=resp,
                              json_schema=tokens_json.validate_token)
        roles = resp.json()[const.ACCESS][const.USER][const.ROLES]
        tenant_access_role = any(
            role[const.NAME] ==
            const.TENANT_ACCESS_ROLE_NAME for role in roles)
        self.assertEqual(False, tenant_access_role,
                         "Tenant access role exists")

    @unless_coverage
    def tearDown(self):
        super(AuthAndValidateTokens, self).tearDown()

    @classmethod
    @unless_coverage
    def tearDownClass(cls):
        super(AuthAndValidateTokens, cls).tearDownClass()
        cls.identity_admin_client.delete_user(
            cls.user_default_client.default_headers[const.X_USER_ID])
        cls.identity_admin_client.delete_user(
            cls.user_manage_client.default_headers[const.X_USER_ID])
        cls.delete_client(cls.user_admin_client)
        cls.identity_admin_client.delete_tenant(
            tenant_id=cls.tenant.id)
        # Disable Domain before delete.
        disable_domain_req = requests.Domain(enabled=False)
        cls.identity_admin_client.update_domain(
            domain_id=cls.domain_id, request_object=disable_domain_req)

        cls.identity_admin_client.delete_domain(
            domain_id=cls.domain_id)
