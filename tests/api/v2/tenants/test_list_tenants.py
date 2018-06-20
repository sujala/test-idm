# -*- coding: utf-8 -*

from qe_coverage.opencafe_decorators import tags, unless_coverage

from tests.api.v2 import base
from tests.api.v2.models import factory
from tests.api.v2.models import responses
from tests.api.v2.schema import tenants


class TestListTenants(base.TestBaseV2):

    """List Tenant Tests"""
    @unless_coverage
    def setUp(self):
        super(TestListTenants, self).setUp()

        tenant_object = factory.get_add_tenant_object()
        resp = self.identity_admin_client.add_tenant(tenant=tenant_object)
        tenant = responses.Tenant(resp.json())
        self.tenant_id = tenant.id

    @tags('positive', 'p0', 'regression')
    def test_list_tenant(self):
        '''Tests for list tenants API. '''

        resp = self.identity_admin_client.list_tenants()
        self.assertEqual(resp.status_code, 200)
        self.assertSchema(response=resp, json_schema=tenants.list_tenants)

    @unless_coverage
    def tearDown(self):
        # Delete all tenants created in the tests
        self.identity_admin_client.delete_tenant(tenant_id=self.tenant_id)
        super(TestListTenants, self).tearDown()
