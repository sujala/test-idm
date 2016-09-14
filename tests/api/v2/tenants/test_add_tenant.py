# -*- coding: utf-8 -*
import copy

import ddt

from tests.api.v2 import base
from tests.api.v2.models import requests
from tests.api.v2.models import responses
from tests.api.v2.schema import tenants


@ddt.ddt
class TestAddTenant(base.TestBaseV2):

    """Add Tenant Tests"""

    def setUp(self):
        super(TestAddTenant, self).setUp()
        self.tenant_ids = []

    @ddt.file_data('data_add_tenant.json')
    def test_add_tenant(self, data_schema):
        '''Tests for add tenant API

        test_data comes from a json data file that can contain various possible
        input combinations. Each of these data combination is a separate test
        case.
        NOTE: This test case illustrates providing test_data in a json file.
        @todo: The test_data file needs to be updated to include all possible
        data combinations.
        '''
        test_data = data_schema.get('test_data', {})
        tenant_name = (
            test_data.get('tenant_name',
                          self.generate_random_string('Tenant')))
        tenant_id = (test_data.get('tenant_id', tenant_name))
        description = (test_data.get('description', None))
        enabled = (test_data.get('enabled', None))
        display_name = (test_data.get('display_name', None))
        domain_id = (test_data.get('domain_id', None))

        tenant_object = requests.Tenant(
            tenant_name=tenant_name, tenant_id=tenant_id,
            description=description, enabled=enabled,
            display_name=display_name, domain_id=domain_id)

        resp = self.identity_admin_client.add_tenant(tenant=tenant_object)
        self.assertEqual(resp.status_code, 201)
        tenant = responses.Tenant(resp.json())
        self.tenant_ids.append(tenant.id)

        add_tenant_schema = copy.deepcopy(tenants.add_tenant)
        if 'additional_schema_fields' in data_schema:
            add_tenant_schema['properties']['tenant']['required'] = (
                add_tenant_schema['properties']['tenant']['required'] +
                data_schema['additional_schema_fields'])
        self.assertSchema(response=resp, json_schema=add_tenant_schema)

        # The tenant_id passed in the request is ignored. The tenant_id is set
        # to the tenant_name by default.
        self.assertEqual(tenant.id, tenant_object.tenant_name)
        self.assertEqual(tenant.name, tenant_object.tenant_name)

        test_data_enabled = test_data.get('enabled', 'true')
        self.assertBoolean(
            expected=test_data_enabled, actual=tenant.enabled)

        if tenant_object.description:
            self.assertEqual(tenant.description, tenant_object.description)
        self.assertHeaders(response=resp)

    def tearDown(self):
        # Delete all tenants created in the tests
        for id in self.tenant_ids:
            self.identity_admin_client.delete_tenant(tenant_id=id)
        super(TestAddTenant, self).tearDown()
