# -*- coding: utf-8 -*
import copy
import ddt

from tests.api.v2 import base
from tests.api.v2.models import responses
from tests.api.v2.schema import tenants

from tests.package.johny import constants as const
from tests.package.johny.v2.models import requests


@ddt.ddt
class TestAddTenant(base.TestBaseV2):
    """Add Tenant Tests"""
    def setUp(self):
        super(TestAddTenant, self).setUp()
        self.tenant_ids = []

    def test_create_tenant_with_types(self):
        tenant_description = 'A tenant described'
        tenant_display_name = 'A name displayed'
        tenant_type_1 = self.generate_random_string(
            pattern=const.TENANT_TYPE_PATTERN).upper()
        tenant_type_2 = self.generate_random_string(
            pattern=const.TENANT_TYPE_PATTERN).upper()
        tenant_type_3 = self.generate_random_string(
            pattern=const.TENANT_TYPE_PATTERN).upper()
        add_tenant_with_types_schema = copy.deepcopy(tenants.add_tenant)
        (add_tenant_with_types_schema['properties'][const.TENANT]
            ['properties'].update({const.NS_TYPES: {}}))
        (add_tenant_with_types_schema['properties'][const.TENANT]
            ['required'].append(const.NS_TYPES))
        tenant_name = tenant_id = 'tname{0}'.format(
            self.generate_random_string(const.LOWER_CASE_LETTERS))
        request_object = requests.Tenant(
                tenant_name=tenant_name,
                description=tenant_description,
                tenant_id=tenant_id,
                enabled=True,
                tenant_types=[tenant_type_1,
                              tenant_type_2,
                              tenant_type_3],
                display_name=tenant_display_name)
        resp = self.identity_admin_client.add_tenant(tenant=request_object)
        self.assertEqual(resp.status_code, 201)
        self.assertSchema(response=resp,
                          json_schema=add_tenant_with_types_schema)
        self.assertEqual(
            len(resp.json()[const.TENANT][const.NS_TYPES]), 3)
        for tenant_type in [tenant_type_1.lower(),
                            tenant_type_2.lower(),
                            tenant_type_3.lower()]:
            self.assertIn(tenant_type,
                          resp.json()[const.TENANT][const.NS_TYPES],
                          msg="Not found {0}".format(tenant_type))

    @ddt.file_data('data_invalid_tenant_types.json')
    def test_create_tenant_with_invalid_tenant_types(self, test_data):
        tenant_types = test_data.get('tenant_types')
        error_message = test_data.get('error_message')
        tenant_name = tenant_id = 'tname{0}'.format(
            self.generate_random_string(const.LOWER_CASE_LETTERS))
        request_object = requests.Tenant(
                tenant_name=tenant_name,
                description='A tenant described',
                tenant_id=tenant_id,
                enabled=True,
                tenant_types=tenant_types,
                display_name='A name displayed')
        resp = self.identity_admin_client.add_tenant(tenant=request_object)
        self.assertEqual(resp.status_code, 400)
        self.assertEqual(resp.json()[const.BAD_REQUEST][const.MESSAGE],
                         error_message)

    @ddt.file_data('data_add_tenant.json')
    def test_add_tenant(self, test_data):
        '''Tests for add tenant API

        test_data comes from a json data file that can contain various possible
        input combinations. Each of these data combination is a separate test
        case.
        NOTE: This test case illustrates providing test_data in a json file.
        @todo: The test_data file needs to be updated to include all possible
        data combinations.
        '''
        test_data = test_data.get('test_data', {})
        tenant_name = (
            test_data.get('tenant_name', self.generate_random_string(
                          pattern=const.TENANT_NAME_PATTERN)))
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
        if 'additional_schema_fields' in test_data:
            add_tenant_schema['properties']['tenant']['required'] = (
                add_tenant_schema['properties']['tenant']['required'] +
                test_data['additional_schema_fields'])
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
