
import pytest
from qe_coverage.opencafe_decorators import tags, unless_coverage

from tests.api.utils import func_helper
from tests.api.v2 import base
from tests.api.v2.models import responses
from tests.api.v2.schema import tokens as tokens_json

from tests.package.johny import constants as const
from tests.package.johny.v2.models import requests
from tests.api.v2.models import factory


class AuthIntoDomain(base.TestBaseV2):
    """
    AuthIntoDomain
    """
    @classmethod
    @unless_coverage
    def setUpClass(cls):
        super(AuthIntoDomain, cls).setUpClass()

    @unless_coverage
    def setUp(self):
        super(AuthIntoDomain, self).setUp()
        self.user_ids = []
        self.tenant_ids = []
        self.acct_info = self.create_user_with_tenant_id()
        # create tenant in other domain
        tenant_object = factory.get_add_tenant_object()
        resp = self.identity_admin_client.add_tenant(tenant=tenant_object)
        tenant = responses.Tenant(resp.json())
        self.invalid_tenant_id = tenant.id
        # create role
        role_id = self.create_role()
        # add role to user
        self.identity_admin_client.add_role_to_user_for_tenant(
            tenant_id=self.invalid_tenant_id,
            role_id=role_id, user_id=self.acct_info['user_id'])
        self.assertEqual(resp.status_code, 201)

    def create_role(self):
        role_object = factory.get_add_role_request_object()
        resp = self.identity_admin_client.add_role(request_object=role_object)
        self.assertEqual(resp.status_code, 201)
        role_id = resp.json()[const.ROLE][const.ID]
        return role_id

    def create_user_with_tenant_id(self):
        # Create a userAdmin with tenantId
        username = self.generate_random_string(
            pattern=const.USER_ADMIN_PATTERN)
        domain_id = func_helper.generate_randomized_domain_id(
            client=self.identity_admin_client)
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
        self.user_id = create_user_with_tenant_resp.id
        self.user_ids.append(create_user_with_tenant_resp.id)
        self.domain_id = domain_id

        password = resp.json()[const.USER][const.NS_PASSWORD]

        # Get user's tenant ID
        auth_obj = requests.AuthenticateWithPassword(user_name=username,
                                                     password=password,
                                                     domain_id=domain_id)
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

    @tags('positive', 'p0', 'smoke')
    @pytest.mark.smoke_alpha
    def test_auth_with_api_key_when_tenant_belongs_to_user_domain(self):
        tenant_name = self.acct_info['tenant_name']
        username = self.acct_info['username']
        user_id = self.acct_info['user_id']

        # Get apikey
        resp = self.identity_admin_client.get_api_key(user_id)
        self.assertEqual(resp.status_code, 200)
        api_key = (resp.json()[const.NS_API_KEY_CREDENTIALS]
                   [const.API_KEY])

        # do auth with Domain+Tenant+User+Apikey
        request_object = requests.AuthenticateWithApiKey(
            user_name=username,
            api_key=api_key,
            domain_id=self.domain_id,
            tenant_name=tenant_name)
        resp = self.identity_admin_client.get_auth_token(
            request_object=request_object)
        self.assertEqual(resp.status_code, 200)
        if self.test_config.deserialize_format == const.JSON:
            self.assertSchema(response=resp,
                              json_schema=tokens_json.auth)

    @tags('positive', 'p0', 'regression')
    @pytest.mark.regression
    def test_auth_with_api_key_when_tenant_belongs_diff_domain(self):

        username = self.acct_info['username']
        user_id = self.acct_info['user_id']
        # Get apikey
        resp = self.identity_admin_client.get_api_key(user_id)
        self.assertEqual(resp.status_code, 200)
        api_key = (resp.json()[const.NS_API_KEY_CREDENTIALS]
                   [const.API_KEY])
        # Auth with apikey
        # tenant belongs to different domain than user
        # and specify user's domainId.
        auth_obj = \
            requests.AuthenticateWithApiKey(user_name=username,
                                            api_key=api_key,
                                            domain_id=self.domain_id,
                                            tenant_id=self.invalid_tenant_id)
        resp = self.identity_admin_client.get_auth_token(
            request_object=auth_obj)
        self.assertEqual(resp.status_code, 401)
        self.assertEqual(resp.json()[const.UNAUTHORIZED][
                         const.MESSAGE], "Error code: 'AUTH-007';"
                         " Not authorized for the tenant.")

    @tags('positive', 'p0', 'smoke')
    @pytest.mark.smoke_alpha
    def test_auth_with_pwd_when_tenant_belongs_to_user_domain(self):
        tenant_name = self.acct_info['tenant_name']
        username = self.acct_info['username']
        password = self.acct_info['password']
        # do auth with Domain+Tenant+User+Password
        # authorized domain
        # user's tenant
        request_object = requests.AuthenticateWithPassword(
            user_name=username,
            password=password,
            tenant_name=tenant_name, domain_id=self.domain_id)
        resp = self.identity_admin_client.get_auth_token(
            request_object=request_object)

        self.assertEqual(resp.status_code, 200)

    @tags('positive', 'p0', 'regression')
    @pytest.mark.regression
    def test_auth_with_pwd_when_tenant_belongs_diff_domain(self):
        # tenant belongs to different domain than user
        # and specify user's domainId.
        username = self.acct_info['username']
        password = self.acct_info['password']

        # do auth with Domain+Tenant+User+Password
        request_object = requests.AuthenticateWithPassword(
            user_name=username,
            password=password,
            tenant_id=self.invalid_tenant_id, domain_id=self.domain_id)
        resp = self.identity_admin_client.get_auth_token(
            request_object=request_object)

        self.assertEqual(resp.status_code, 401)
        self.assertEqual(resp.json()[const.UNAUTHORIZED][
                         const.MESSAGE], "Error code: 'AUTH-007';"
                         " Not authorized for the tenant.")

    @tags('positive', 'p0', 'regression')
    @pytest.mark.regression
    def test_auth_with_pwd_for_unauthorized_domain(self):
        tenant_name = self.acct_info['tenant_name']
        username = self.acct_info['username']
        password = self.acct_info['password']
        # do auth with Domain+Tenant+User+Password
        # unauthorized domain
        request_object = requests.AuthenticateWithPassword(
            user_name=username,
            password=password,
            tenant_name=tenant_name, domain_id="1334566")
        resp = self.identity_admin_client.get_auth_token(
            request_object=request_object)

        self.assertEqual(resp.status_code, 401)
        self.assertEqual(resp.json()[const.UNAUTHORIZED][
                         const.MESSAGE], "Error code: 'AUTH-006';"
                         " Not authorized for the domain.")

    @tags('positive', 'p0', 'regression')
    @pytest.mark.regression
    def test_mfa_auth_when_tenant_belongs_diff_domain(self):
        username = self.acct_info['username']
        password = self.acct_info['password']

        # setup mfa for user
        secret = func_helper.setup_mfa_for_user(
            user_id=self.user_id, client=self.identity_admin_client)
        auth_obj = requests.AuthenticateWithPassword(
            user_name=username, password=password)
        # first step of MFA
        auth_resp = self.identity_admin_client.get_auth_token(
            request_object=auth_obj)
        # second step of MFA
        mfa_auth_resp = self.call_second_step_of_mfa(
            first_auth_resp=auth_resp, secret=secret)
        self.assertEqual(mfa_auth_resp.status_code, 401)
        self.assertEqual(mfa_auth_resp.json()[const.UNAUTHORIZED][
                         const.MESSAGE], "Error code: 'AUTH-007';"
                         " Not authorized for the tenant.")

    def call_second_step_of_mfa(self, first_auth_resp, secret):
        self.assertEqual(first_auth_resp.status_code, 401)
        auth_header = first_auth_resp.headers[const.WWW_AUTHENTICATE]
        session_id = auth_header.split('sessionId=\'')[1].split('\'')[0]

        # authenticate with passcode & session ID & tenant & domain
        # (2nd mfa auth step)
        code = func_helper.get_oath_from_secret(secret=secret)
        # tenant belongs to different domain than user
        # and specify user's domainId.
        kwargs = {'session_id': session_id, 'pass_code': code,
                  'tenant_id': self.invalid_tenant_id,
                  'domain_id': self.domain_id}
        return self.identity_admin_client.auth_with_mfa_cred(**kwargs)

    @tags('positive', 'p0', 'smoke')
    @pytest.mark.smoke_alpha
    def test_auth_as_tenant_with_token(self):
        token = self.acct_info['token']
        tenant_name = self.acct_info['tenant_name']
        # auth with user's tenant and token
        kwargs = {'token_id': token,
                  'tenant_name': tenant_name,
                  'domain_id': self.domain_id}
        auth_obj = requests.AuthenticateAsTenantWithToken(**kwargs)
        resp = self.identity_admin_client.get_auth_token(
            request_object=auth_obj)
        self.assertEqual(resp.status_code, 200)

        # auth with tenant in other domain
        kwargs = {'token_id': token,
                  'tenant_id': self.invalid_tenant_id,
                  'domain_id': self.domain_id}
        auth_obj = requests.AuthenticateAsTenantWithToken(**kwargs)
        resp = self.identity_admin_client.get_auth_token(
            request_object=auth_obj)
        self.assertEqual(resp.status_code, 401)
        self.assertEqual(resp.json()[const.UNAUTHORIZED][
                         const.MESSAGE], "Error code: 'AUTH-007';"
                         " Not authorized for the tenant.")

    @unless_coverage
    @base.base.log_tearDown_error
    def tearDown(self):
        resp = self.identity_admin_client.list_users_in_domain(
            domain_id=self.domain_id)
        users = resp.json()[const.USERS]
        user_ids = [item[const.ID] for item in users]
        for user_id in user_ids:
            resp = self.identity_admin_client.delete_user(user_id=user_id)
            self.assertEqual(
                resp.status_code, 204,
                msg='User with ID {0} failed to delete'.format(user_id))

        resp = self.identity_admin_client.list_tenants_in_domain(
            domain_id=self.domain_id)

        if resp.status_code == 200:
            tenants = resp.json()[const.TENANTS]
            tenant_ids = [item[const.ID] for item in tenants]
            for tenant_id in tenant_ids:
                resp = self.identity_admin_client.delete_tenant(
                    tenant_id=tenant_id)
                self.assertEqual(
                    resp.status_code, 204,
                    msg='Tenant with ID {0} failed to delete'.format(
                        tenant_id))

        if hasattr(self, 'domain_id'):
            # Disable Domain before delete.
            disable_domain_req = requests.Domain(enabled=False)
            resp = self.identity_admin_client.update_domain(
                domain_id=self.domain_id, request_object=disable_domain_req)

            resp = self.identity_admin_client.delete_domain(
                domain_id=self.domain_id)
            self.assertEqual(
                resp.status_code, 204,
                msg='Domain with ID {0} failed to delete'.format(
                    self.domain_id))

    @classmethod
    @unless_coverage
    def tearDownClass(cls):
        super(AuthIntoDomain, cls).tearDownClass()
