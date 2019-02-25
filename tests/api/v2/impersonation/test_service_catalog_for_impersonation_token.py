# -*- coding: utf-8 -*
from nose.plugins.attrib import attr
from qe_coverage.opencafe_decorators import tags, unless_coverage

from tests.api.utils import func_helper
from tests.api.v2 import base
from tests.api.v2.models import factory

from tests.package.johny import constants as const
from tests.package.johny.v2.models import requests


class TestServiceCatalogForImpersonation(base.TestBaseV2):

    @classmethod
    @unless_coverage
    def setUpClass(cls):
        """Class level set up for the tests
        Create users needed for the tests and generate clients for those users.
        """
        super(TestServiceCatalogForImpersonation, cls).setUpClass()

    @unless_coverage
    def setUp(self):
        super(TestServiceCatalogForImpersonation, self).setUp()
        self.domain_id = func_helper.generate_randomized_domain_id(
            client=self.identity_admin_client)

    @tags('positive', 'p0', 'smoke')
    @attr(type='smoke_alpha')
    def test_display_catalog_for_impersonation_token(self):

        username = self.generate_random_string(pattern='Username[\w]{12}')
        req = factory.get_add_user_one_call_request_object(
            domainid=self.domain_id, username=username)
        resp = self.identity_admin_client.add_user(request_object=req)
        password = resp.json()[const.USER][const.OS_KSADM_PASSWORD]
        self.assertEqual(resp.status_code, 201)

        # Disable domain tenants to create SUSPENDED account scenario
        list_resp = self.identity_admin_client.list_tenants_in_domain(
            domain_id=self.domain_id)
        self.assertEqual(list_resp.status_code, 200)
        tenants = list_resp.json()[const.TENANTS]
        self.tenant_ids = [item[const.ID] for item in tenants]
        for tenant_id in self.tenant_ids:
            req = requests.Tenant(
                tenant_name=tenant_id, tenant_id=tenant_id, enabled=False)
            resp = self.identity_admin_client.update_tenant(
                tenant_id=tenant_id, request_object=req)
            self.assertEqual(resp.status_code, 200)
        auth_req = requests.AuthenticateWithPassword(
            user_name=username, password=password)
        auth_resp = self.identity_admin_client.get_auth_token(
            request_object=auth_req)
        self.assertEqual(auth_resp.status_code, 200)
        self.assertEqual(
            auth_resp.json()[const.ACCESS][const.SERVICE_CATALOG], [])

        # Get an impersonation token. Cannot impersonate with limited access
        # racker account in Staging Hence, impersonating with identity admin.
        imp_req = requests.ImpersonateUser(user_name=username)
        imp_resp = self.identity_admin_client.impersonate_user(
            request_data=imp_req)
        self.assertEqual(imp_resp.status_code, 200)
        impersonation_token = imp_resp.json()[
            const.ACCESS][const.TOKEN][const.ID]

        # Auth with impersonation token & tenant
        auth_req = requests.AuthenticateAsTenantWithToken(
            token_id=impersonation_token, tenant_id=self.tenant_ids[1])
        auth_resp = self.identity_admin_client.get_auth_token(
            request_object=auth_req)
        self.assertEqual(auth_resp.status_code, 200)
        self.assertNotEqual(
            auth_resp.json()[const.ACCESS][const.SERVICE_CATALOG], [])

        # Get endpoints for token
        list_resp = self.identity_admin_client.list_endpoints_for_token(
            token=impersonation_token)
        self.assertEqual(list_resp.status_code, 200)
        self.assertNotEqual(list_resp.json()[const.ENDPOINTS], [])

        # Validate impersonation token - Verify phone PIN is not returned
        resp = self.identity_admin_client.validate_token(
            token_id=impersonation_token)
        user = resp.json()[const.ACCESS][const.USER]
        self.assertNotIn(const.RAX_AUTH_PHONE_PIN, user)

    @base.base.log_tearDown_error
    @unless_coverage
    def tearDown(self):
        super(TestServiceCatalogForImpersonation, self).tearDown()
        users = self.identity_admin_client.list_users_in_domain(self.domain_id)
        user_ids = [user[const.ID] for user in users.json()[const.USERS]]
        for user_id in user_ids:
            resp = self.identity_admin_client.delete_user(user_id=user_id)
            self.assertEqual(resp.status_code, 204, (
                "user with ID {0} failed to delete".format(user_id)))
        for tenant_id in self.tenant_ids:
            resp = self.identity_admin_client.delete_tenant(
                tenant_id=tenant_id)
            self.assertEqual(resp.status_code, 204, (
                'Tenant with ID {0} failed to delete'.format(tenant_id)))
        disable_domain_req = requests.Domain(enabled=False)
        self.identity_admin_client.update_domain(
            domain_id=self.domain_id, request_object=disable_domain_req)
        resp = self.identity_admin_client.delete_domain(self.domain_id)
        self.assertEqual(resp.status_code, 204, (
            'Domain with ID {0} failed to delete'.format(self.domain_id)))

    @classmethod
    @unless_coverage
    def tearDownClass(cls):
        super(TestServiceCatalogForImpersonation, cls).tearDownClass()
