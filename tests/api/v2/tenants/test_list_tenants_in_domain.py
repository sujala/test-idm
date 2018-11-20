# -*- coding: utf-8 -*
from nose.plugins.attrib import attr
from qe_coverage.opencafe_decorators import tags, unless_coverage

from tests.api.v2 import base
from tests.api.v2.models import factory
from tests.api.v2.models import responses
from tests.api.utils import func_helper
from tests.package.johny import constants as const
from tests.package.johny.v2.models import requests


class TestListTenantsInDomain(base.TestBaseV2):

    """List Tenants in Domain Tests"""
    @classmethod
    @unless_coverage
    def setUpClass(cls):
        super(TestListTenantsInDomain, cls).setUpClass()
        cls.domain_id = func_helper.generate_randomized_domain_id(
            client=cls.identity_admin_client)
        dom_req = requests.Domain(
            domain_name=cls.domain_id,
            domain_id=cls.domain_id)
        add_dom_resp = cls.identity_admin_client.add_domain(dom_req)
        assert add_dom_resp.status_code == 201, (
            'domain was not created successfully')
        cls.tenant_ids = []
        # create an enabled tenant in domain.
        tenant_object = factory.get_add_tenant_object(
            domain_id=cls.domain_id)
        resp = cls.identity_admin_client.add_tenant(tenant=tenant_object)
        tenant = responses.Tenant(resp.json())
        cls.enabled_tenant_id = tenant.id
        cls.tenant_ids.append(cls.enabled_tenant_id)
        # create a disabled tenant in domain.
        tenant_name = tenant_id = 'tname{0}'.format(
            cls.generate_random_string(const.LOWER_CASE_LETTERS))
        tenant_object = factory.get_add_tenant_object(
            enabled=False, tenant_name=tenant_name,
            tenant_id=tenant_id, domain_id=cls.domain_id)
        resp = cls.identity_admin_client.add_tenant(tenant=tenant_object)
        tenant = responses.Tenant(resp.json())
        cls.disabled_tenant_id = tenant.id
        cls.tenant_ids.append(cls.disabled_tenant_id)

    @unless_coverage
    def setUp(self):
        super(TestListTenantsInDomain, self).setUp()

    @tags('positive', 'p0', 'regression')
    @attr(type='regression')
    def test_list_enabled_tenants_in_domain(self):
        '''Test for list enabled tenants in domain API. '''
        self.validate_list_tenants_in_domain_response(
            True, self.enabled_tenant_id)

    @tags('positive', 'p0', 'regression')
    @attr(type='regression')
    def test_list_disabled_tenants_in_domain(self):
        '''Test for list disabled tenants API. '''
        self.validate_list_tenants_in_domain_response(
            False, self.disabled_tenant_id)

    def validate_list_tenants_in_domain_response(
            self, test_data, tenant_id):
        resp = self.identity_admin_client.list_tenants_in_domain(
            self.domain_id, option={const.ENABLED: test_data})
        self.assertEqual(resp.status_code, 200)
        self.assertEqual(len(resp.json()[const.TENANTS]), 1)
        for tenant in resp.json()[const.TENANTS]:
            self.assertEqual(tenant[const.ID], tenant_id)
            self.assertEqual(tenant[const.RAX_AUTH_DOMAIN_ID],
                             self.domain_id)

    @tags('positive', 'p0', 'regression')
    @attr(type='regression')
    def test_list_tenants_in_domain_for_param_with_invalid_value(self):
        '''Tests for list tenants API with invalid
            value for enabled query param. '''
        resp = self.identity_admin_client.list_tenants_in_domain(
            self.domain_id, option={const.ENABLED: '!@#'})
        self.assertEqual(resp.status_code, 200)
        self.assertEqual(len(resp.json()[const.TENANTS]), 2)
        tenant_ids = []
        for tenant in resp.json()[const.TENANTS]:
            tenant_ids.append(tenant[const.ID])
            self.assertEqual(tenant[const.RAX_AUTH_DOMAIN_ID],
                             self.domain_id)
        self.assertIn(self.enabled_tenant_id, tenant_ids)
        self.assertIn(self.disabled_tenant_id, tenant_ids)

    @unless_coverage
    def tearDown(self):
        super(TestListTenantsInDomain, self).tearDown()

    @classmethod
    @unless_coverage
    def tearDownClass(cls):
                # Delete all tenants created in the tests.
        for tenant_id in cls.tenant_ids:
            cls.identity_admin_client.delete_tenant(tenant_id=tenant_id)
        super(TestListTenantsInDomain, cls).tearDownClass()
