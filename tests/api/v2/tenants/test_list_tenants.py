# -*- coding: utf-8 -*
from tests.api.v2 import base
from tests.api.v2.models import requests
from tests.api.v2.models import responses
from tests.api.v2.schema import tenants


class TestListTenants(base.TestBaseV2):

    """List Tenant Tests"""

    def setUp(self):
        super(TestListTenants, self).setUp()

        self.tenant_name = self.generate_random_string('Tenant')
        self.description = ('Orginal description')
        tenant_object = requests.Tenant(
            tenant_name=self.tenant_name, tenant_id=self.tenant_name,
            description=self.description)
        resp = self.identity_admin_client.add_tenant(tenant=tenant_object)
        tenant = responses.Tenant(resp.json())
        self.tenant_id = tenant.id

    def test_list_tenant(self):
        '''Tests for list tenants API. '''

        resp = self.identity_admin_client.list_tenants()
        self.assertEqual(resp.status_code, 200)
        self.assertSchema(response=resp, json_schema=tenants.list_tenants)

    def tearDown(self):
        # Delete all tenants created in the tests
        self.identity_admin_client.delete_tenant(tenant_id=self.tenant_id)
        super(TestListTenants, self).tearDown()
