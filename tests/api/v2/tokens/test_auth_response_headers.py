# -*- coding: utf-8 -*
import ddt
from tests.api.utils import header_validation
from tests.api.v2 import base
from tests.api.v2.models import responses
from tests.api.v2.models import factory

from tests.package.johny import constants as const
from tests.package.johny.v2.models import requests


@ddt.ddt
class TestAuthResponseHeaders(base.TestBaseV2):
    """
    1. Return 'X-Tenant-Id' header for authentication requests.
    1.1. If user specifies tenantId / tenantName in request set value to
    request tenantId.
    1.2. If not specified and user has a single tenant id then use that value.
    1.3. If not specified and user has multiple tenant ids then use the mosso
    tenant id.
    1.4. If not specified and user has multiple tenant ids and none are mosso
    pick arbitrarily tenant.
    1.5. If user does not have any tenants then header is not added.
    """

    @classmethod
    def setUpClass(cls):
        super(TestAuthResponseHeaders, cls).setUpClass()

    def setUp(self):
        super(TestAuthResponseHeaders, self).setUp()
        self.user_ids = []
        self.tenant_ids = []
        self.domain_ids = []
        self.role_ids = []
        self.one_call_user_info = self.create_user_one_call_logic()
        self.user_info = self.create_user()

    def create_user_one_call_logic(self):
        username = self.generate_random_string(
            pattern=const.USER_ADMIN_PATTERN)
        domain_id = self.generate_random_string(
            pattern=const.NUMERIC_DOMAIN_ID_PATTERN)
        add_user_obj = factory.get_add_user_request_object_pull(
            user_name=username, domain_id=domain_id)
        resp = self.identity_admin_client.add_user(
            request_object=add_user_obj)
        self.assertEqual(resp.status_code, 201)
        user_id = resp.json()[const.USER][const.ID]
        self.user_ids.append(user_id)
        self.domain_ids.append(domain_id)
        self.tenant_ids.append(domain_id)
        user_resp = responses.User(resp_json=resp.json())
        return {'username': user_resp.user_name,
                'password': user_resp.password,
                'user_id': user_resp.id,
                'domain_id': user_resp.domain_id,
                'tenant_id': user_resp.domain_id,
                'roles': user_resp.roles}

    def create_user(self):
        """regular"""
        username = self.generate_random_string(
            pattern=const.USER_NAME_PATTERN)
        domain_id = self.generate_random_string(pattern=const.NUMBERS_PATTERN)
        request_object = requests.UserAdd(user_name=username,
                                          domain_id=domain_id)
        resp = self.identity_admin_client.add_user(
            request_object=request_object)
        self.assertEqual(resp.status_code, 201)
        user_id = resp.json()[const.USER][const.ID]
        self.user_ids.append(user_id)
        user_resp = responses.User(resp_json=resp.json())
        return {'username': user_resp.user_name,
                'password': user_resp.password,
                'user_id': user_resp.id,
                'domain_id': domain_id}

    def create_tenant(self, domain_id=None):
        tenant_name = self.generate_random_string(
            pattern=const.NUMBERS_PATTERN)
        tenant_req = factory.get_add_tenant_request_object(
            tenant_name=tenant_name, domain_id=domain_id)
        resp = self.identity_admin_client.add_tenant(tenant=tenant_req)
        self.assertEqual(resp.status_code, 201)
        tenant_id = resp.json()[const.TENANT][const.ID]
        self.tenant_ids.append(tenant_id)
        return tenant_id

    def create_role(self, client=None):
        if not client:
            client = self.identity_admin_client
        role_name = self.generate_random_string(
            pattern=const.IDENTITY_PRODUCT_ROLE_NAME_PATTERN)
        role_object = factory.get_add_role_request_object(role_name=role_name)
        resp = client.add_role(request_object=role_object)
        self.assertEqual(resp.status_code, 201)
        role_id = resp.json()[const.ROLE][const.ID]
        self.role_ids.append(role_id)
        return role_id

    def get_user_apikey(self, user_id):
        resp = self.identity_admin_client.get_api_key(user_id=user_id)
        self.assertEqual(resp.status_code, 200)
        api_key = resp.json()[const.NS_API_KEY_CREDENTIALS][const.API_KEY]
        return api_key

    def verify_auth_resp_with_tenant(self, auth_resp, tenant_id):
        self.assertEqual(auth_resp.status_code, 200)
        header_validation.validate_header_tenant_id(value=tenant_id)(auth_resp)

    @ddt.data('user_password', 'user_apikey')
    def test_auth_not_specify_tenant(self, auth_type):
        """
        Create user with one call logic
        - return user object with mosso and nast tenant
        - auth with usernane/password
        - tenant_id is default tenant
        """
        username = self.one_call_user_info['username']
        password = self.one_call_user_info['password']
        user_id = self.one_call_user_info['user_id']
        apikey = self.get_user_apikey(user_id=user_id)

        # auth
        if auth_type == 'user_password':
            auth_obj = requests.AuthenticateWithPassword(user_name=username,
                                                         password=password)
        else:
            auth_obj = requests.AuthenticateWithApiKey(user_name=username,
                                                       api_key=apikey)
        auth_resp = self.identity_admin_client.get_auth_token(
            request_object=auth_obj)
        self.assertEqual(auth_resp.status_code, 200)
        tenant_id = auth_resp.json()[const.ACCESS][const.TOKEN][
            const.TENANT][const.ID]
        self.verify_auth_resp_with_tenant(auth_resp=auth_resp,
                                          tenant_id=tenant_id)

    @ddt.data(('user_password', 'tenant_id'), ('user_apikey', 'tenant_id'),
              ('user_password', 'tenant_name'), ('user_apikey', 'tenant_name'))
    @ddt.unpack
    def test_auth_with_mosso_tenant(self, auth_type, with_tenant):
        username = self.one_call_user_info['username']
        password = self.one_call_user_info['password']
        user_id = self.one_call_user_info['user_id']
        apikey = self.get_user_apikey(user_id=user_id)
        tenant_id = self.one_call_user_info['tenant_id']
        # auth with tenant
        if auth_type == 'user_password':
            kwargs = {'user_name': username,
                      'password': password,
                      with_tenant: tenant_id}
            auth_obj = requests.AuthenticateWithPassword(**kwargs)
        else:
            kwargs = {'user_name': username,
                      'api_key': apikey,
                      with_tenant: tenant_id}
            auth_obj = requests.AuthenticateWithApiKey(**kwargs)

        auth_resp = self.identity_admin_client.get_auth_token(
            request_object=auth_obj)
        self.verify_auth_resp_with_tenant(auth_resp=auth_resp,
                                          tenant_id=tenant_id)

    @ddt.data(('user_password', 'tenant_id'), ('user_apikey', 'tenant_id'),
              ('user_password', 'tenant_name'), ('user_apikey', 'tenant_name'))
    @ddt.unpack
    def test_auth_with_nast_tenant(self, auth_type, with_tenant):
        username = self.one_call_user_info['username']
        password = self.one_call_user_info['password']
        user_id = self.one_call_user_info['user_id']
        apikey = self.get_user_apikey(user_id=user_id)
        roles = self.one_call_user_info['roles']
        tenant_id = ''

        # get nast tenant
        for role in roles:
            if role[0] == const.OBJECT_STORE_ROLE_NAME:
                tenant_id = role[1]
        self.assertIsNotNone(tenant_id)

        # auth with tenant
        if auth_type == 'user_password':
            kwargs = {'user_name': username,
                      'password': password,
                      with_tenant: tenant_id}
            auth_obj = requests.AuthenticateWithPassword(**kwargs)
        else:
            kwargs = {'user_name': username,
                      'api_key': apikey,
                      with_tenant: tenant_id}
            auth_obj = requests.AuthenticateWithApiKey(**kwargs)
        auth_resp = self.identity_admin_client.get_auth_token(
            request_object=auth_obj)
        self.verify_auth_resp_with_tenant(auth_resp=auth_resp,
                                          tenant_id=tenant_id)

    @ddt.data(('user_password', 'tenant_id'), ('user_apikey', 'tenant_id'),
              ('user_password', 'tenant_name'), ('user_apikey', 'tenant_name'))
    @ddt.unpack
    def test_auth_with_other_tenant(self, auth_type, with_tenant):
        """
        User with multi tenants
        Auth with other tenant not mosso or nast
        """
        username = self.one_call_user_info['username']
        password = self.one_call_user_info['password']
        user_id = self.one_call_user_info['user_id']
        apikey = self.get_user_apikey(user_id=user_id)

        # create tenant
        tenant_id = self.create_tenant()

        # create identity product role
        role_id = self.create_role()

        # add role to user on tenant
        add_resp = self.identity_admin_client.add_role_to_user_for_tenant(
            tenant_id=tenant_id, user_id=user_id, role_id=role_id
        )
        self.assertEqual(add_resp.status_code, 200)

        # auth with tenant other than nast and mosso
        if auth_type == 'user_password':
            kwargs = {'user_name': username,
                      'password': password,
                      with_tenant: tenant_id}
            auth_obj = requests.AuthenticateWithPassword(**kwargs)
        else:
            kwargs = {'user_name': username,
                      'api_key': apikey,
                      with_tenant: tenant_id}
            auth_obj = requests.AuthenticateWithApiKey(**kwargs)
        auth_resp = self.identity_admin_client.get_auth_token(
            request_object=auth_obj)
        self.verify_auth_resp_with_tenant(auth_resp=auth_resp,
                                          tenant_id=tenant_id)

    @ddt.data('tenant_id', 'tenant_name')
    def test_auth_w_tenant_and_token(self, with_tenant):
        username = self.one_call_user_info['username']
        password = self.one_call_user_info['password']
        tenant_id = self.one_call_user_info['tenant_id']
        # auth
        auth_obj = requests.AuthenticateWithPassword(user_name=username,
                                                     password=password)
        auth_resp = self.identity_admin_client.get_auth_token(
            request_object=auth_obj)
        self.assertEqual(auth_resp.status_code, 200)
        # get token
        token_id = auth_resp.json()[const.ACCESS][const.TOKEN][const.ID]
        # auth with tenant and token
        kwargs = {'token_id': token_id,
                  with_tenant: tenant_id}
        auth_obj = requests.AuthenticateAsTenantWithToken(**kwargs)
        auth_resp = self.identity_admin_client.get_auth_token(
            request_object=auth_obj)
        self.verify_auth_resp_with_tenant(auth_resp=auth_resp,
                                          tenant_id=tenant_id)

    @ddt.data('user_password', 'user_apikey')
    def test_auth_w_user_single_tenant_not_specify_tenant(self, auth_type):
        username = self.user_info['username']
        password = self.user_info['password']
        domain_id = self.user_info['domain_id']
        user_id = self.user_info['user_id']
        apikey = self.get_user_apikey(user_id=user_id)

        # create tenant
        tenant_id = self.create_tenant(domain_id=domain_id)

        # create identity product role
        role_id = self.create_role()

        # add role to user on tenant
        add_resp = self.identity_admin_client.add_role_to_user_for_tenant(
            tenant_id=tenant_id, user_id=user_id, role_id=role_id
        )
        self.assertEqual(add_resp.status_code, 200)

        # auth
        if auth_type == 'user_password':
            auth_obj = requests.AuthenticateWithPassword(user_name=username,
                                                         password=password)
        else:
            auth_obj = requests.AuthenticateWithApiKey(user_name=username,
                                                       api_key=apikey)
        auth_resp = self.identity_admin_client.get_auth_token(
            request_object=auth_obj)
        self.verify_auth_resp_with_tenant(auth_resp=auth_resp,
                                          tenant_id=tenant_id)

    @ddt.data('user_password', 'user_apikey')
    def test_auth_w_user_multi_tenants_not_specify_tenant(self, auth_type):
        username = self.user_info['username']
        password = self.user_info['password']
        domain_id = self.user_info['domain_id']
        user_id = self.user_info['user_id']
        apikey = self.get_user_apikey(user_id=user_id)

        # create tenant
        tenant_id = self.create_tenant(domain_id=domain_id)
        tenant_id2 = self.create_tenant(domain_id=domain_id)

        # create identity product role
        role_id = self.create_role()

        # create identity product role
        role_id2 = self.create_role()

        # add role to user on tenant
        add_resp = self.identity_admin_client.add_role_to_user_for_tenant(
            tenant_id=tenant_id, user_id=user_id, role_id=role_id
        )
        self.assertEqual(add_resp.status_code, 200)

        # add role to user on tenant2
        add_resp = self.identity_admin_client.add_role_to_user_for_tenant(
            tenant_id=tenant_id2, user_id=user_id, role_id=role_id2
        )
        self.assertEqual(add_resp.status_code, 200)

        # auth
        if auth_type == 'user_password':
            auth_obj = requests.AuthenticateWithPassword(user_name=username,
                                                         password=password)
        else:
            auth_obj = requests.AuthenticateWithApiKey(user_name=username,
                                                       api_key=apikey)
        auth_resp = self.identity_admin_client.get_auth_token(
            request_object=auth_obj)
        self.assertEqual(auth_resp.status_code, 200)
        header_validation.basic_header_validations(response=auth_resp,
                                                   header=const.X_TENANT_ID)
        self.assertIn(auth_resp.headers[const.X_TENANT_ID],
                      [tenant_id, tenant_id2])

    def test_auth_w_user_no_tenant(self):
        username = self.user_info['username']
        password = self.user_info['password']
        # auth
        auth_obj = requests.AuthenticateWithPassword(user_name=username,
                                                     password=password)
        auth_resp = self.identity_admin_client.get_auth_token(
            request_object=auth_obj)
        self.assertEqual(auth_resp.status_code, 200)
        header_validation.validate_header_not_present(
            unexpected_headers=const.X_TENANT_ID)(auth_resp)

    @ddt.data('tenant_id', 'tenant_name')
    def test_auth_fail_401(self, with_tenant):
        """
        Verify X-Tenant-Id not included in headers when failed auth
        """
        username = self.one_call_user_info['username']
        invalid_pwd = self.generate_random_string(
            pattern=const.PASSWORD_PATTERN)
        tenant_id = self.one_call_user_info['tenant_id']
        # auth with tenant
        kwargs = {'user_name': username,
                  'password': invalid_pwd,
                  with_tenant: tenant_id}
        auth_obj = requests.AuthenticateWithPassword(**kwargs)
        auth_resp = self.identity_admin_client.get_auth_token(
            request_object=auth_obj)
        self.assertEqual(auth_resp.status_code, 401)
        header_validation.validate_header_not_present(
            unexpected_headers=const.X_TENANT_ID)(auth_resp)

    def tearDown(self):
        # Delete all resources created in the tests
        for id_ in self.user_ids:
            self.identity_admin_client.delete_user(user_id=id_)
        for id_ in self.tenant_ids:
            self.identity_admin_client.delete_tenant(tenant_id=id_)
        for id_ in self.domain_ids:
            self.identity_admin_client.delete_domain(domain_id=id_)
        for id_ in self.role_ids:
            self.identity_admin_client.delete_role(role_id=id_)
        super(TestAuthResponseHeaders, self).tearDown()
