"""
  JIRA CID-507
  A porting of Jmeter scripts v2.0 token related smoke tests to Johny

  *  IdentityAdmin validates token of Useradmin from password login.
  *  IdentityAdmin lists the groups of the userAdmin.
  *  Make sure Useradmin can login via username+apikey
  *  Make sure useradmin can login via tenantID+username+password and that
     identityAdmin validates the resulting token
"""

import ddt
from nose.plugins.attrib import attr

from tests.api.v2 import base
from tests.api.v2.models import responses
from tests.api.v2.schema import tokens as tokens_json
from tests.api.v2.schema import groups as groups_json

from tests.package.johny import constants as const
from tests.package.johny.v2.models import requests


@ddt.ddt
class AuthAndValidateTokens(base.TestBaseV2):
    """
    AuthAndValidateTokens
    """
    @classmethod
    def setUpClass(cls):
        super(AuthAndValidateTokens, cls).setUpClass()

    def setUp(self):
        super(AuthAndValidateTokens, self).setUp()
        self.user_ids = []
        self.tenant_ids = []
        self.domain_ids = []
        self.acct_info = self.create_user_with_tenant_id()

    def create_user_with_tenant_id(self):
        # Create a userAdmin with tenantId
        username = self.generate_random_string(
            pattern=const.USER_ADMIN_PATTERN)
        domain_id = self.generate_random_string(
            pattern=const.NUMERIC_DOMAIN_ID_PATTERN)
        input_data = {'email': const.EMAIL_RANDOM,
                      'secret_qa': {
                          const.SECRET_QUESTION: self.generate_random_string(
                              pattern=const.LOWER_CASE_LETTERS),
                          const.SECRET_ANSWER: self.generate_random_string(
                              pattern=const.UPPER_CASE_LETTERS)},
                      'domain_id': domain_id}
        req_obj = requests.UserAdd(user_name=username, **input_data)
        resp = self.identity_admin_client.add_user(request_object=req_obj)
        self.assertEqual(resp.status_code, 201)
        create_user_with_tenant_resp = responses.User(resp.json())
        self.user_ids.append(create_user_with_tenant_resp.id)
        self.domain_ids.append(domain_id)
        password = resp.json()[const.USER][const.NS_PASSWORD]

        # Get user's tenant ID
        auth_obj = requests.AuthenticateWithPassword(user_name=username,
                                                     password=password)
        resp = self.identity_admin_client.get_auth_token(
            request_object=auth_obj)
        self.assertEqual(resp.status_code, 200)
        auth_user_pass_resp = responses.Access(resp.json())
        self.tenant_ids.append(auth_user_pass_resp.access.token.tenant.id)
        return {'token': auth_user_pass_resp.access.token.id,
                'user_id': create_user_with_tenant_resp.id,
                'tenant_name': auth_user_pass_resp.access.token.tenant.name,
                'username': username,
                'password': create_user_with_tenant_resp.password}

    @attr(type='smoke')
    def test_validate_useradmin_token_from_userpass_auth(self):
        token = self.acct_info['token']
        resp = self.identity_admin_client.validate_token(token)
        self.assertEqual(resp.status_code, 200)
        self.assertSchema(response=resp,
                          json_schema=tokens_json.validate_token)

    @attr(type='smoke')
    def test_list_groups_of_useradmin(self):
        user_id = self.acct_info['user_id']
        resp = self.identity_admin_client.list_groups(user_id=user_id)
        self.assertEqual(resp.status_code, 200)
        self.assertSchema(response=resp,
                          json_schema=groups_json.list_groups)

    @attr(type='smoke')
    def test_auth_with_api_key(self):
        username = self.acct_info['username']
        user_id = self.acct_info['user_id']

        # Get apikey
        resp = self.identity_admin_client.get_api_key(user_id)
        self.assertEqual(resp.status_code, 200)
        api_key = (resp.json()[const.NS_API_KEY_CREDENTIALS]
                   [const.API_KEY])

        # Auth with apikey
        auth_obj = requests.AuthenticateWithApiKey(user_name=username,
                                                   api_key=api_key)
        resp = self.identity_admin_client.get_auth_token(
            request_object=auth_obj)
        self.assertEqual(resp.status_code, 200)
        access_resp = responses.Access(resp.json())
        self.assertIsNotNone(access_resp.access.token.id)

    @attr(type='smoke')
    def test_validate_token_from_auth_tenant_user_pass(self):
        tenant_name = self.acct_info['tenant_name']
        username = self.acct_info['username']
        password = self.acct_info['password']

        # do auth with Tenant+User+Password
        request_object = requests.AuthenticateWithPassword(
            user_name=username,
            password=password,
            tenant_id=tenant_name)
        resp = self.identity_admin_client.get_auth_token(
            request_object=request_object)
        self.assertEqual(resp.status_code, 200)
        access_resp = responses.Access(resp.json())

        # get token from auth
        token = access_resp.access.token.id

        # Validate token
        resp = self.identity_admin_client.validate_token(token)
        self.assertEqual(resp.status_code, 200)
        self.assertSchema(response=resp,
                          json_schema=tokens_json.validate_token)

    @attr(type='smoke')
    def test_validate_token_from_auth_tenant_user_api_key(self):
        tenant_name = self.acct_info['tenant_name']
        username = self.acct_info['username']
        user_id = self.acct_info['user_id']

        # Get apikey
        resp = self.identity_admin_client.get_api_key(user_id)
        self.assertEqual(resp.status_code, 200)
        api_key = (resp.json()[const.NS_API_KEY_CREDENTIALS]
                   [const.API_KEY])

        # do auth with Tenant+User+Apikey
        request_object = requests.AuthenticateWithApiKey(
            user_name=username,
            api_key=api_key,
            tenant_name=tenant_name)
        resp = self.identity_admin_client.get_auth_token(
            request_object=request_object)
        self.assertEqual(resp.status_code, 200)
        access_resp = responses.Access(resp.json())

        # get token from auth
        token = access_resp.access.token.id

        # Validate token
        resp = self.identity_admin_client.validate_token(token)
        self.assertEqual(resp.status_code, 200)
        self.assertSchema(response=resp,
                          json_schema=tokens_json.validate_token)

    def tearDown(self):
        for user_id in self.user_ids:
            self.identity_admin_client.delete_user(user_id=user_id)
        for tenant_id in self.tenant_ids:
            self.identity_admin_client.delete_tenant(tenant_id=tenant_id)
        for domain_id in self.domain_ids:
            self.identity_admin_client.delete_domain(domain_id=domain_id)

    @classmethod
    def tearDownClass(cls):
        super(AuthAndValidateTokens, cls).tearDownClass()
