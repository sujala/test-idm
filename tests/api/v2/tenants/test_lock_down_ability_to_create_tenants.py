# -*- coding: utf-8 -*
from qe_coverage.opencafe_decorators import tags, unless_coverage
import pytest

from tests.api.v2 import base
from tests.package.johny import constants as const
from tests.package.johny.v2.models import requests
from tests.api.v2.models import responses


class TestLockDownAbilityToCreateTenants(base.TestBaseV2):

    @classmethod
    @unless_coverage
    def setUpClass(cls):
        super(TestLockDownAbilityToCreateTenants, cls).setUpClass()

        req_obj = requests.Domain(
            domain_name=cls.generate_random_string(const.DOMAIN_PATTERN),
            description=cls.generate_random_string(const.DESC_PATTERN),
            enabled=True)

        resp = cls.identity_admin_client.add_domain(req_obj)
        assert resp.status_code == 201
        cls.domain_id = resp.json()[const.RAX_AUTH_DOMAIN][const.ID]

    @unless_coverage
    def setUp(self):
        super(TestLockDownAbilityToCreateTenants, self).setUp()
        self.tenant_ids = []

    @tags('positive', 'p0', 'smoke')
    @pytest.mark.smoke
    def test_identity_admin_can_create_tenant_with_role(self):
        # Create Tenant with Role identity:rs-tenant-admin
        tenant_object = requests.Tenant(
            tenant_name=self.generate_random_string(const.TENANT_NAME_PATTERN),
            tenant_id=self.generate_random_string(const.TENANT_ID_PATTERN),
            description=self.generate_random_string(const.DESC_PATTERN),
            enabled=True,
            display_name='api_test_tenant',
            domain_id=self.domain_id)

        resp = self.identity_admin_client.add_tenant(tenant=tenant_object)
        self.assertEqual(resp.status_code, 201)
        tenant = responses.Tenant(resp.json())
        self.tenant_ids.append(tenant.id)

    @tags('positive', 'p0', 'regression')
    def test_ia_cannot_add_tenant_without_role(self):
        # Cannot create Tenant without Role identity:rs-tenant-admin

        if not self.test_config.run_service_admin_tests:
            self.skipTest('Skipping Service Admin Tests per config value')

        tenant_object = requests.Tenant(
            tenant_name=self.generate_random_string(const.TENANT_NAME_PATTERN),
            tenant_id=self.generate_random_string(const.TENANT_ID_PATTERN),
            description=self.generate_random_string(const.DESC_PATTERN),
            enabled=True,
            display_name='api_test_tenant',
            domain_id=self.domain_id)

        # Test Identity admin has no role identity:rs-tenant-admin
        test_identity_admin_client = self.generate_client(
            parent_client=self.service_admin_client,
            additional_input_data={'domain_id': self.domain_id})
        add_tenant_resp = test_identity_admin_client.add_tenant(
            tenant=tenant_object)

        # Adding tearDown step here inorder to avoid using service admin creds
        # outside this test.
        self.service_admin_client.delete_user(
            user_id=test_identity_admin_client.default_headers[
                const.X_USER_ID])

        self.assertEqual(add_tenant_resp.status_code, 403)

    @base.base.log_tearDown_error
    @unless_coverage
    def tearDown(self):
        super(TestLockDownAbilityToCreateTenants, self).tearDown()

        for tenant_id in self.tenant_ids:
            resp = self.identity_admin_client.delete_tenant(
                tenant_id=tenant_id)
            assert resp.status_code in [204, 404], (
                'Tenant with ID {0} failed to delete'.format(tenant_id))

    @classmethod
    @unless_coverage
    @base.base.log_tearDown_error
    def tearDownClass(cls):
        disable_domain_req = requests.Domain(enabled=False)
        cls.identity_admin_client.update_domain(
            domain_id=cls.domain_id, request_object=disable_domain_req)
        resp = cls.identity_admin_client.delete_domain(
            domain_id=cls.domain_id)
        assert resp.status_code == 204, (
            'Domain with ID {0} failed to delete'.format(cls.domain_id))
        super(TestLockDownAbilityToCreateTenants, cls).tearDownClass()
