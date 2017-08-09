# -*- coding: utf-8 -*
import ddt
from nose.plugins.attrib import attr
import time

from tests.api.utils import func_helper, header_validation
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
        resp_domain_id = resp.json()[const.USER][const.RAX_AUTH_DOMAIN_ID]
        self.domain_ids.append(resp_domain_id)
        user_resp = responses.User(resp_json=resp.json())
        return {'username': user_resp.user_name,
                'password': user_resp.password,
                'user_id': user_resp.id,
                'domain_id': resp_domain_id}

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
            pattern=const.ROLE_NAME_PATTERN)
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

    def call_second_step_of_mfa(self, first_auth_resp, secret, tenant_id=None,
                                with_tenant=None, negative=False):

        self.assertEqual(first_auth_resp.status_code, 401)
        auth_header = first_auth_resp.headers[const.WWW_AUTHENTICATE]
        session_id = auth_header.split('sessionId=\'')[1].split('\'')[0]

        # This check is added to verify repose removes the header. If this
        # test is run against identity directly, it will fail.
        self.assertHeaders(first_auth_resp, (
            header_validation.validate_username_header_not_present))

        # authenticate with passcode & session ID (2nd mfa auth step)
        code = func_helper.get_oath_from_secret(secret=secret)

        # Making 2nd step of MFA auth fail
        if negative:
            code += 'a'

        kwargs = {'session_id': session_id, 'pass_code': code}
        if with_tenant == 'tenant_id':
            kwargs['tenant_id'] = tenant_id
        elif with_tenant == 'tenant_name':
            kwargs['tenant_name'] = tenant_id
        mfa_resp = self.identity_admin_client.auth_with_mfa_cred(**kwargs)

        if negative:
            self.assertEqual(mfa_resp.status_code, 401)
        else:
            self.assertEqual(mfa_resp.status_code, 200)
        return mfa_resp

    @ddt.data(['user_password', False], ['user_password', True],
              ['user_apikey', False])
    @ddt.unpack
    @attr(type='smoke_alpha')
    def test_auth_not_specify_tenant(self, auth_type, use_mfa):
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

        auth_resp, secret = self.make_auth_call(
            use_mfa=use_mfa, auth_type=auth_type, user_id=user_id,
            username=username, password=password, apikey=apikey)

        self.validate_auth_headers(use_mfa=use_mfa, auth_resp=auth_resp,
                                   secret=secret)

    def make_auth_call(self, use_mfa, user_id, username,
                       auth_type='user_password', password=None, apikey=None,
                       with_tenant=None, tenant_id=None):

        secret = None
        if use_mfa:
            secret = func_helper.setup_mfa_for_user(
                user_id=user_id, client=self.identity_admin_client)

        if auth_type == 'user_password':
            kwargs = dict([('user_name', username),
                           ('password', password)])
            if with_tenant:
                kwargs[with_tenant] = tenant_id
            auth_obj = requests.AuthenticateWithPassword(**kwargs)
        else:
            kwargs = dict([('user_name', username),
                           ('api_key', apikey)])
            if with_tenant:
                kwargs[with_tenant] = tenant_id
            auth_obj = requests.AuthenticateWithApiKey(**kwargs)

        auth_resp = self.identity_admin_client.get_auth_token(
            request_object=auth_obj)
        return auth_resp, secret

    def validate_auth_headers(self, use_mfa, auth_resp, secret=None,
                              with_tenant=None, tenant_id=None):

        if use_mfa:
            self.assertHeaders(
                auth_resp, (
                    header_validation.validate_tenant_id_header_not_present))
            final_auth_resp = self.call_second_step_of_mfa(
                first_auth_resp=auth_resp, secret=secret,
                with_tenant=with_tenant, tenant_id=tenant_id)
        else:
            final_auth_resp = auth_resp

        if tenant_id is None:
            tenant_id = final_auth_resp.json()[const.ACCESS][const.TOKEN][
                const.TENANT][const.ID]
        self.verify_auth_resp_with_tenant(auth_resp=final_auth_resp,
                                          tenant_id=tenant_id)
        # This check is added to verify repose removes the header. If this
        # test is run against identity directly, it will fail.
        self.assertHeaders(final_auth_resp, (
            header_validation.validate_username_header_not_present))

    @ddt.data(('user_password', 'tenant_id', True),
              ('user_password', 'tenant_id', False),
              ('user_apikey', 'tenant_id', False),
              ('user_password', 'tenant_name', True),
              ('user_password', 'tenant_name', False),
              ('user_apikey', 'tenant_name', False))
    @ddt.unpack
    def test_auth_with_mosso_tenant(self, auth_type, with_tenant, use_mfa):

        username = self.one_call_user_info['username']
        password = self.one_call_user_info['password']
        user_id = self.one_call_user_info['user_id']
        apikey = self.get_user_apikey(user_id=user_id)
        tenant_id = self.one_call_user_info['tenant_id']

        auth_resp, secret = self.make_auth_call(
            use_mfa=use_mfa, auth_type=auth_type, user_id=user_id,
            username=username, password=password, apikey=apikey,
            with_tenant=with_tenant, tenant_id=tenant_id)

        self.validate_auth_headers(use_mfa=use_mfa, auth_resp=auth_resp,
                                   with_tenant=with_tenant, secret=secret,
                                   tenant_id=tenant_id)

    @ddt.data(('user_password', 'tenant_id', True),
              ('user_password', 'tenant_id', False),
              ('user_apikey', 'tenant_id', False),
              ('user_password', 'tenant_name', True),
              ('user_password', 'tenant_name', False),
              ('user_apikey', 'tenant_name', False))
    @ddt.unpack
    @attr(type='smoke_alpha')
    def test_auth_with_nast_tenant(self, auth_type, with_tenant, use_mfa):
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

        auth_resp, secret = self.make_auth_call(
            use_mfa=use_mfa, auth_type=auth_type, user_id=user_id,
            username=username, password=password, apikey=apikey,
            with_tenant=with_tenant, tenant_id=tenant_id)

        self.validate_auth_headers(use_mfa=use_mfa, auth_resp=auth_resp,
                                   with_tenant=with_tenant, secret=secret,
                                   tenant_id=tenant_id)

    @ddt.data(('user_password', 'tenant_id', True),
              ('user_password', 'tenant_id', False),
              ('user_apikey', 'tenant_id', False),
              ('user_password', 'tenant_name', True),
              ('user_password', 'tenant_name', False),
              ('user_apikey', 'tenant_name', False))
    @ddt.unpack
    @attr(type='smoke_alpha')
    def test_auth_with_other_tenant(self, auth_type, with_tenant, use_mfa):
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

        auth_resp, secret = self.make_auth_call(
            use_mfa=use_mfa, auth_type=auth_type, user_id=user_id,
            username=username, password=password, apikey=apikey,
            with_tenant=with_tenant, tenant_id=tenant_id)

        self.validate_auth_headers(use_mfa=use_mfa, auth_resp=auth_resp,
                                   secret=secret, with_tenant=with_tenant,
                                   tenant_id=tenant_id)

    @ddt.data(['tenant_id', False],
              ['tenant_name', False],
              ['tenant_id', True],
              ['tenant_name', True])
    @ddt.unpack
    def test_auth_w_tenant_and_token(self, with_tenant, use_mfa):
        username = self.one_call_user_info['username']
        password = self.one_call_user_info['password']
        tenant_id = self.one_call_user_info['tenant_id']
        user_id = self.one_call_user_info['user_id']

        if use_mfa:
            secret = func_helper.setup_mfa_for_user(
                user_id=user_id, client=self.identity_admin_client)
            # This is to avoid session id getting revoked which can happen
            # if it was generated the same second MFA was enabled. This can
            # removed once we will have sub-second precision(CID-615).
            time.sleep(2)

        # auth
        auth_obj = requests.AuthenticateWithPassword(user_name=username,
                                                     password=password)
        auth_resp = self.identity_admin_client.get_auth_token(
            request_object=auth_obj)

        if use_mfa:
            self.assertHeaders(
                auth_resp, (
                    header_validation.validate_tenant_id_header_not_present))
            auth_resp = self.call_second_step_of_mfa(
                first_auth_resp=auth_resp, secret=secret,
                with_tenant=with_tenant, tenant_id=tenant_id)

        self.assertEqual(auth_resp.status_code, 200)

        # get token
        token_id = auth_resp.json()[const.ACCESS][const.TOKEN][const.ID]

        # auth with tenant and token
        kwargs = {'token_id': token_id,
                  with_tenant: tenant_id}
        auth_obj = requests.AuthenticateAsTenantWithToken(**kwargs)
        final_auth_resp = self.identity_admin_client.get_auth_token(
            request_object=auth_obj)
        self.verify_auth_resp_with_tenant(auth_resp=final_auth_resp,
                                          tenant_id=tenant_id)
        # This check is added to verify repose removes the header. If this
        # test is run against identity directly, it will fail.
        self.assertHeaders(final_auth_resp, (
            header_validation.validate_username_header_not_present))

    @ddt.data(['user_password', True], ['user_password', True],
              ['user_apikey', False])
    @ddt.unpack
    def test_auth_w_user_single_tenant_not_specify_tenant(self, auth_type,
                                                          use_mfa):
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

        auth_resp, secret = self.make_auth_call(
            use_mfa=use_mfa, auth_type=auth_type, user_id=user_id,
            username=username, password=password, apikey=apikey)

        self.validate_auth_headers(use_mfa=use_mfa, auth_resp=auth_resp,
                                   secret=secret, tenant_id=tenant_id)

    @ddt.data(['user_password', True], ['user_password', False],
              ['user_apikey', False])
    @ddt.unpack
    def test_auth_w_user_multi_tenants_not_specify_tenant(self, auth_type,
                                                          use_mfa):
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

        auth_resp, secret = self.make_auth_call(
            use_mfa=use_mfa, auth_type=auth_type, user_id=user_id,
            username=username, password=password, apikey=apikey)

        if use_mfa:
            self.assertHeaders(
                auth_resp, (
                    header_validation.validate_tenant_id_header_not_present))
            final_auth_resp = self.call_second_step_of_mfa(
                first_auth_resp=auth_resp, secret=secret)
        else:
            final_auth_resp = auth_resp
            self.assertEqual(auth_resp.status_code, 200)

        header_validation.basic_header_validations(response=final_auth_resp,
                                                   header=const.X_TENANT_ID)
        self.assertIn(final_auth_resp.headers[const.X_TENANT_ID],
                      [tenant_id, tenant_id2])
        # This check is added to verify repose removes the header. If this
        # test is run against identity directly, it will fail.
        self.assertHeaders(final_auth_resp, (
            header_validation.validate_username_header_not_present))

    @ddt.data(True, False)
    @attr(type='smoke_alpha')
    def test_auth_w_user_no_tenant(self, use_mfa):
        username = self.user_info['username']
        password = self.user_info['password']
        user_id = self.user_info['user_id']

        auth_resp, secret = self.make_auth_call(
            use_mfa=use_mfa, user_id=user_id, username=username,
            password=password)

        if use_mfa:
            self.assertHeaders(
                auth_resp, (
                    header_validation.validate_tenant_id_header_not_present))
            final_auth_resp = self.call_second_step_of_mfa(
                first_auth_resp=auth_resp, secret=secret)
        else:
            final_auth_resp = auth_resp
            self.assertEqual(auth_resp.status_code, 200)

        header_validation.validate_header_not_present(
            unexpected_headers=const.X_TENANT_ID)(final_auth_resp)

        # This check is added to verify repose removes the header. If this
        # test is run against identity directly, it will fail.
        self.assertHeaders(final_auth_resp, (
            header_validation.validate_username_header_not_present))

    @ddt.data(['tenant_id', True], ['tenant_name', True],
              ['tenant_id', False], ['tenant_name', False])
    @ddt.unpack
    def test_auth_fail_401(self, with_tenant, use_mfa):
        """
        Verify X-Tenant-Id not included in headers when failed auth
        """
        username = self.one_call_user_info['username']
        password = self.one_call_user_info['password']
        user_id = self.one_call_user_info['user_id']
        invalid_pwd = self.generate_random_string(
            pattern=const.PASSWORD_PATTERN)
        tenant_id = self.one_call_user_info['tenant_id']

        # auth with tenant
        kwargs = {'user_name': username,
                  with_tenant: tenant_id}

        if use_mfa:
            # This scenario is when 1st step of auth is successful, but
            # 2nd step fails.
            secret = func_helper.setup_mfa_for_user(
                user_id=user_id, client=self.identity_admin_client)
            # This is to avoid session id getting revoked which can happen
            # if it was generated the same second MFA was enabled. This can
            # removed once we will have sub-second precision(CID-615).
            time.sleep(2)

            kwargs['password'] = password
            auth_obj = requests.AuthenticateWithPassword(**kwargs)
            auth_resp = self.identity_admin_client.get_auth_token(
                request_object=auth_obj)

            self.assertHeaders(
                auth_resp, (
                    header_validation.validate_tenant_id_header_not_present))
            final_auth_resp = self.call_second_step_of_mfa(
                first_auth_resp=auth_resp, secret=secret,
                with_tenant=with_tenant, tenant_id=tenant_id, negative=True)
        else:
            kwargs['password'] = invalid_pwd
            auth_obj = requests.AuthenticateWithPassword(**kwargs)
            final_auth_resp = self.identity_admin_client.get_auth_token(
                request_object=auth_obj)

        self.assertEqual(final_auth_resp.status_code, 401)
        self.assertHeaders(
            final_auth_resp, (
                header_validation.validate_tenant_id_header_not_present))

        # This check is added to verify repose removes the header. If this
        # test is run against identity directly, it will fail.
        self.assertHeaders(final_auth_resp, (
            header_validation.validate_username_header_not_present))

    @base.base.log_tearDown_error
    def tearDown(self):
        # Delete all resources created in the tests
        for id_ in self.user_ids:
            resp = self.identity_admin_client.delete_user(user_id=id_)
            self.assertEqual(
                resp.status_code, 204,
                msg='User with ID {0} failed to delete'.format(id_))
        for id_ in self.tenant_ids:
            resp = self.identity_admin_client.delete_tenant(tenant_id=id_)
            self.assertEqual(
                resp.status_code, 204,
                msg='Tenant with ID {0} failed to delete'.format(id_))
        for id_ in self.domain_ids:
            domain_object = requests.Domain(
                domain_name=id_, enabled=False)
            self.identity_admin_client.update_domain(
                domain_id=id_, request_object=domain_object)
            resp = self.identity_admin_client.delete_domain(domain_id=id_)
            self.assertEqual(
                resp.status_code, 204,
                msg='Domain with ID {0} failed to delete'.format(id_))
        for id_ in self.role_ids:
            resp = self.identity_admin_client.delete_role(role_id=id_)
            self.assertEqual(
                resp.status_code, 204,
                msg='Role with ID {0} failed to delete'.format(id_))
        super(TestAuthResponseHeaders, self).tearDown()
