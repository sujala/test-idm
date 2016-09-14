# -*- coding: utf-8 -*
import copy

import ddt

from tests.api.v2 import base
from tests.api.v2.models import requests
from tests.api.v2.models import responses
from tests.api.v2.schema import tenants


@ddt.ddt
class TestUpdateTenant(base.TestBaseV2):

    """Update Tenant Tests"""

    def setUp(self):
        super(TestUpdateTenant, self).setUp()

        self.tenant_name = self.generate_random_string('Tenant')
        self.description = ('Orginal description')
        tenant_object = requests.Tenant(
            tenant_name=self.tenant_name, tenant_id=self.tenant_name,
            description=self.description)
        resp = self.identity_admin_client.add_tenant(tenant=tenant_object)

        tenant = responses.Tenant(resp.json())
        self.tenant_id = tenant.id

    @ddt.file_data('data_update_tenant.json')
    def test_update_tenant(self, data_schema):
        '''Tests for update tenant API

        @todo: The test_data file needs to be updated to include all possible
        data combinations.
        '''
        # Get the values before the update tenant API call
        before = self.identity_admin_client.get_tenant(
            tenant_id=self.tenant_id)
        tenant = responses.Tenant(before.json())

        test_data = data_schema.get('test_data', {})
        tenant_name = test_data.get('tenant_name', self.tenant_name)
        tenant_id = test_data.get('tenant_id', None)
        description = test_data.get('description', None)
        enabled = test_data.get('enabled', None)
        display_name = test_data.get('display_name', None)

        tenant_object = requests.Tenant(
            tenant_name=tenant_name, tenant_id=tenant_id,
            description=description, enabled=enabled,
            display_name=display_name)

        update_resp = self.identity_admin_client.update_tenant(
            tenant_id=self.tenant_id, request_object=tenant_object)
        self.assertEqual(update_resp.status_code, 200)

        update_tenant_schema = copy.deepcopy(tenants.update_tenant)
        if 'additional_schema' in 'data_schema':
            update_tenant_schema['properties']['tenant']['required'] = (
                update_tenant_schema['properties']['tenant']['required'] +
                data_schema['additonal_schema'])
        self.assertSchema(
            response=update_resp, json_schema=update_tenant_schema)

        after = self.identity_admin_client.get_tenant(
            tenant_id=self.tenant_id)
        updated_tenant = responses.Tenant(after.json())

        if 'enabled' in test_data:
            self.assertBoolean(
                expected=test_data['enabled'], actual=updated_tenant.enabled)
        else:
            self.assertEqual(updated_tenant.enabled, tenant.enabled)

        expected_description = test_data.get('description', tenant.description)
        self.assertEqual(updated_tenant.description, expected_description)

        expected_tenant_name = test_data.get('tenant_name', tenant.name)
        self.assertEqual(updated_tenant.name, expected_tenant_name)

        # Verify that the tenant_id is not updated
        self.assertEqual(updated_tenant.id, tenant.id)
        self.assertEqual(updated_tenant.domain_id, tenant.domain_id)

    def tearDown(self):
        # Delete all tenants created in the tests
        self.identity_admin_client.delete_tenant(tenant_id=self.tenant_id)
        super(TestUpdateTenant, self).tearDown()
