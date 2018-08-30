# -*- coding: utf-8 -*
from nose.plugins.attrib import attr
from qe_coverage.opencafe_decorators import tags, unless_coverage

from tests.api.utils import func_helper
from tests.api.v2 import base

from tests.package.johny import constants as const
from tests.package.johny.v2.models import requests
import urlparse
from tests.api.v2.models import factory
from tests.api.v1_1 import client
import base64


class TestAuthWithDisabledTenant(base.TestBaseV2):

    @classmethod
    @unless_coverage
    def setUpClass(cls):
        """Class level set up for the tests
        """
        super(TestAuthWithDisabledTenant, cls).setUpClass()
        VERSION = 'v1.1'
        url = urlparse.urljoin(
            cls.identity_config.base_url, cls.identity_config.cloud_url)
        url = urlparse.urljoin(url, VERSION)
        cls.identity_admin_client_v1_1 = client.IdentityAPIClient(
            url=url,
            serialize_format=cls.test_config.serialize_format,
            deserialize_format=cls.test_config.deserialize_format)
        username = cls.identity_config.identity_admin_user_name
        password = cls.identity_config.identity_admin_password
        encrypted_password = (
            base64.encodestring('{0}:{1}'.format(username, password))[:-1])
        cls.identity_admin_client_v1_1.default_headers['Authorization'] = \
            'Basic {0}'.format(encrypted_password)

    @unless_coverage
    def setUp(self):
        super(TestAuthWithDisabledTenant, self).setUp()
        self.domain_id = func_helper.generate_randomized_domain_id(
            client=self.identity_admin_client)
        self.username = self.generate_random_string(pattern='Username[\w]{12}')
        req = factory.get_add_user_one_call_request_object(
            domainid=self.domain_id, username=self.username)
        resp = self.identity_admin_client.add_user(request_object=req)
        self.password = resp.json()[const.USER][const.OS_KSADM_PASSWORD]
        self.user_id = resp.json()[const.USER][const.ID]
        # Get apikey
        resp = self.identity_admin_client.get_api_key(self.user_id)
        self.assertEqual(resp.status_code, 200)
        self.api_key = resp.json()[const.NS_API_KEY_CREDENTIALS][const.API_KEY]
        # Get Tenants in Domain
        list_resp = self.identity_admin_client.list_tenants_in_domain(
            domain_id=self.domain_id)
        self.assertEqual(list_resp.status_code, 200)
        tenants = list_resp.json()[const.TENANTS]
        self.tenant_ids = [item[const.ID] for item in tenants]

    @tags('positive', 'p0', 'smoke')
    @attr(type='smoke_alpha')
    def test_auth_with_all_tenants_disabled(self):
        """
        Given a user has associated tenants, When all tenants are disabled
        And the user cannot authenticate via Identity API version 1.1
        And the user has limited authentication ability via API version 2.0 -
        no service catalog is returned
        """
        for tenant_id in self.tenant_ids:
            req = requests.Tenant(
                tenant_name=tenant_id, tenant_id=tenant_id, enabled=False)
            resp = self.identity_admin_client.update_tenant(
                tenant_id=tenant_id, request_object=req)
            self.assertEqual(resp.status_code, 200)

        auth_resp = self.validate_v2_0_auth()
        self.assertEqual(
            auth_resp.json()[const.ACCESS][const.SERVICE_CATALOG], [])
        self.validate_v1_1_auth(expected_response=403)
        self.validate_mosso_auth(expected_response=403)
        self.validate_nast_auth(expected_response=403)

    @tags('positive', 'p0', 'smoke')
    @attr(type='smoke_alpha')
    def test_auth_with_one_tenant_disabled(self):
        """
        Given a user has associated tenants And 1 or more of the tenants is
        enabled When the user attempts to authenticate via Identity API
        version 1.1 or 2.0 Then the user can successfully authenticate
        """
        req = requests.Tenant(
            tenant_name=self.tenant_ids[0],
            tenant_id=self.tenant_ids[0], enabled=False)
        resp = self.identity_admin_client.update_tenant(
            tenant_id=self.tenant_ids[0], request_object=req)
        self.assertEqual(resp.status_code, 200)

        auth_resp = self.validate_v2_0_auth()
        self.assertNotEqual(
            auth_resp.json()[const.ACCESS][const.SERVICE_CATALOG], [])

        self.validate_v1_1_auth(expected_response=200)
        self.validate_mosso_auth(expected_response=200)
        self.validate_nast_auth(expected_response=200)

    def validate_v2_0_auth(self):
        """
        Authenticate in v2.0 using username and password
        """
        auth_req = requests.AuthenticateWithPassword(
            user_name=self.username, password=self.password)
        auth_resp = self.identity_admin_client.get_auth_token(
            request_object=auth_req)
        self.assertEqual(auth_resp.status_code, 200)
        self.assertEqual(
            auth_resp.json()
            [const.ACCESS][const.TOKEN][const.TENANT][const.NAME],
            self.domain_id)
        self.assertEqual(
            auth_resp.json()
            [const.ACCESS][const.TOKEN][const.TENANT][const.ID],
            self.domain_id)
        return auth_resp

    def validate_v1_1_auth(self, expected_response):
        """
        Authenticate in v1.1 using username and password
        """
        auth_resp = self.identity_admin_client_v1_1.auth_user_password(
            user_name=self.username, password=self.password)
        self.assertEqual(auth_resp.status_code, expected_response)

        if expected_response == 200:
            self.assertIsNotNone(
                auth_resp.json()[const.AUTH][const.TOKEN][const.ID])
        else:
            self.assertEqual(
                auth_resp.json()[const.FORBIDDEN][const.MESSAGE],
                "Forbidden: not allowed for suspended account")

    def validate_mosso_auth(self, expected_response):
        """
        Authenticate in v1.1 using mosso id and api key
        """
        auth_resp = self.identity_admin_client_v1_1.auth_mosso_key(
            mosso_id=self.domain_id, key=self.api_key)
        self.assertEqual(auth_resp.status_code, expected_response)

        if expected_response == 200:
            self.assertIsNotNone(
                auth_resp.json()[const.AUTH][const.TOKEN][const.ID])
        else:
            self.assertEqual(
                auth_resp.json()[const.FORBIDDEN][const.MESSAGE],
                "Forbidden: not allowed for suspended account")

    def validate_nast_auth(self, expected_response):
        """
        Authenticate in v1.1 using nast id and api key
        """
        auth_resp = self.identity_admin_client_v1_1.auth_nast_key(
            nast_id=self.domain_id, key=self.api_key)
        self.assertEqual(auth_resp.status_code, expected_response)

        if expected_response == 200:
            self.assertIsNotNone(
                auth_resp.json()[const.AUTH][const.TOKEN][const.ID])
        else:
            self.assertEqual(
                auth_resp.json()[const.FORBIDDEN][const.MESSAGE],
                "Forbidden: not allowed for suspended account")

    @base.base.log_tearDown_error
    @unless_coverage
    def tearDown(self):
        super(TestAuthWithDisabledTenant, self).tearDown()
        users = self.identity_admin_client.list_users_in_domain(self.domain_id)
        user_ids = [user[const.ID] for user in users.json()[const.USERS]]
        for user_id in user_ids:
            resp = self.identity_admin_client.delete_user(user_id=user_id)
            self.assertEqual(
                resp.status_code, 204,
                msg="user with ID {0} failed to delete".format(user_id))
        for tenant_id in self.tenant_ids:
            resp = self.identity_admin_client.delete_tenant(
                tenant_id=tenant_id)
            self.assertEqual(
                resp.status_code, 204,
                msg='Tenant with ID {0} failed to delete'.format(tenant_id))

    @classmethod
    @unless_coverage
    def tearDownClass(cls):
        super(TestAuthWithDisabledTenant, cls).tearDownClass()
