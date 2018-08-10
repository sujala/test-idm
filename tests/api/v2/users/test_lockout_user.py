# -*- coding: utf-8 -*
from nose.plugins.attrib import attr
from qe_coverage.opencafe_decorators import tags, unless_coverage

from tests.api.utils import func_helper
from tests.api.v2 import base
from tests.api.v2.models import responses, factory

from tests.package.johny import constants as const
from tests.package.johny.v2.models import requests


class TestLockoutUser(base.TestBaseV2):

    """ Lockout user test

        This test will lock out a user and then test the lockout time
    """
    @classmethod
    @unless_coverage
    def setUpClass(cls):
        """Class level set up for the tests

        Create users needed for the tests and generate clients for those users.
        """
        super(TestLockoutUser, cls).setUpClass()

    @unless_coverage
    def setUp(self):
        super(TestLockoutUser, self).setUp()

        self.lockout_count = int(self.get_lockout_pswd_retries())

        # Create a userAdmin with tenantId
        self.username = self.generate_random_string(
            pattern=const.USER_ADMIN_PATTERN)
        self.domain_id = func_helper.generate_randomized_domain_id(
            client=self.identity_admin_client)
        req_obj = factory.get_add_user_one_call_request_object(
            username=self.username, domainid=self.domain_id)
        resp = self.identity_admin_client.add_user(request_object=req_obj)
        self.assertEqual(resp.status_code, 201)
        self.create_user_with_tenant_resp = responses.User(resp.json())

        self.password = resp.json()[const.USER][const.NS_PASSWORD]
        self.user_id = resp.json()[const.USER][const.ID]

    @tags('positive', 'p0', 'smoke')
    @attr(type='smoke_alpha')
    def test_lock_out_user(self):
        """Lock out user
        """

        # lock out the account
        for count in range(0, self.lockout_count):
            # Authenticate with invalid creds
            auth_obj = requests.AuthenticateWithPassword(
                user_name=self.username,
                password='fake-pass-{}'.format(count))
            resp = self.identity_admin_client.get_auth_token(
                request_object=auth_obj)
            self.assertEqual(resp.status_code, 401)

        # Now try with valid creds and fail
        auth_obj = requests.AuthenticateWithPassword(
            user_name=self.username,
            password=self.password)
        resp = self.identity_admin_client.get_auth_token(
            request_object=auth_obj)
        self.assertEqual(resp.status_code, 401)

    @tags('positive', 'p0', 'smoke')
    @attr(type='smoke_alpha')
    def test_almost_lock_out_user(self):
        """Lock out user
        """
        # authenticate n-1 times incorrectly
        for count in range(0, self.lockout_count - 1):
            # Authenticate with invalid creds
            auth_obj = requests.AuthenticateWithPassword(
                user_name=self.username,
                password='fake-pass-{}'.format(count))
            resp = self.identity_admin_client.get_auth_token(
                request_object=auth_obj)
            self.assertEqual(resp.status_code, 401)

        # Now try with valid creds and pass
        auth_obj = requests.AuthenticateWithPassword(
            user_name=self.username,
            password=self.password)
        resp = self.identity_admin_client.get_auth_token(
            request_object=auth_obj)
        self.assertEqual(resp.status_code, 200)

        # the count is reset so we can try to fake auth again
        for count in range(0, self.lockout_count):
            # Authenticate with invalid creds
            auth_obj = requests.AuthenticateWithPassword(
                user_name=self.username,
                password='fake-pass-{}'.format(count))
            resp = self.identity_admin_client.get_auth_token(
                request_object=auth_obj)
            self.assertEqual(resp.status_code, 401)

        # Now try with valid creds and fail
        auth_obj = requests.AuthenticateWithPassword(
            user_name=self.username,
            password=self.password)
        resp = self.identity_admin_client.get_auth_token(
            request_object=auth_obj)
        self.assertEqual(resp.status_code, 401)

    @base.base.log_tearDown_error
    @unless_coverage
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
        super(TestLockoutUser, self).tearDown()

    def get_lockout_pswd_retries(self):
        prop_resp = self.devops_client.get_devops_properties(
            prop_name=const.PASSWORD_LOCKOUT_RETRIES)

        return prop_resp.json()[
            const.PROPERTIES][0][const.VALUE]

    @classmethod
    @unless_coverage
    def tearDownClass(cls):
        super(TestLockoutUser, cls).tearDownClass()
