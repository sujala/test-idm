# -*- coding: utf-8 -*
import pytest
import copy
import ddt
from qe_coverage.opencafe_decorators import tags, unless_coverage

from tests.api.v2 import base
from tests.api.v2.models import responses
from tests.api.v2.schema import tenants

from tests.package.johny import constants as const
from tests.package.johny.v2.models import requests


@ddt.ddt
class TestAddTenant(base.TestBaseV2):
    """Add Tenant Tests"""
    @classmethod
    @unless_coverage
    def setUpClass(cls):
        super(TestAddTenant, cls).setUpClass()

    def create_tenant_type(self, name):
        request_object = requests.TenantType(name, 'description')
        self.service_admin_client.add_tenant_type(tenant_type=request_object)
        self.tenant_type_ids.append(name.lower())

    @unless_coverage
    def setUp(self):
        super(TestAddTenant, self).setUp()
        self.tenant_ids = []
        self.tenant_type_ids = []
        self.tenant_type_1 = self.generate_random_string(
            pattern=const.TENANT_TYPE_PATTERN)
        self.tenant_type_2 = self.generate_random_string(
            pattern=const.TENANT_TYPE_PATTERN)
        self.tenant_type_3 = self.generate_random_string(
            pattern=const.TENANT_TYPE_PATTERN)
        self.create_tenant_type(self.tenant_type_1)
        self.create_tenant_type(self.tenant_type_2)
        self.create_tenant_type(self.tenant_type_3)

    @tags('positive', 'p1', 'regression')
    def test_create_tenant_with_types(self):
        tenant_description = 'A tenant described'
        tenant_display_name = 'A name displayed'
        add_tenant_with_types_schema = copy.deepcopy(tenants.add_tenant)
        tenant_name = tenant_id = 'tname{0}'.format(
            self.generate_random_string(const.LOWER_CASE_LETTERS))
        request_object = requests.Tenant(
                tenant_name=tenant_name,
                description=tenant_description,
                tenant_id=tenant_id,
                enabled=True,
                tenant_types=[self.tenant_type_1,
                              self.tenant_type_2,
                              self.tenant_type_3],
                display_name=tenant_display_name)
        resp = self.identity_admin_client.add_tenant(tenant=request_object)
        self.assertEqual(resp.status_code, 201)
        self.assertSchema(response=resp,
                          json_schema=add_tenant_with_types_schema)
        tenant = responses.Tenant(resp.json())
        self.tenant_ids.append(tenant.id)
        self.assertEqual(
            len(resp.json()[const.TENANT][const.NS_TYPES]), 3)
        for tenant_type in [self.tenant_type_1.lower(),
                            self.tenant_type_2.lower(),
                            self.tenant_type_3.lower()]:
            self.assertIn(tenant_type,
                          resp.json()[const.TENANT][const.NS_TYPES],
                          msg="Not found {0}".format(tenant_type))

    @tags('positive', 'p1', 'regression')
    def test_create_tenant_infers_tenant_type(self):
        tenant_description = 'A tenant described'
        add_tenant_with_types_schema = copy.deepcopy(tenants.add_tenant)
        tenant_name = tenant_id = '{0}:{1}'.format(
            self.tenant_type_1,
            self.generate_random_string(const.LOWER_CASE_LETTERS))
        request_object = requests.Tenant(
            tenant_name=tenant_name,
            description=tenant_description,
            tenant_id=tenant_id,
            enabled=True)
        resp = self.identity_admin_client.add_tenant(tenant=request_object)
        self.assertEqual(resp.status_code, 201)
        self.assertSchema(response=resp,
                          json_schema=add_tenant_with_types_schema)
        tenant = responses.Tenant(resp.json())
        self.tenant_ids.append(tenant.id)
        self.assertEqual(
            len(resp.json()[const.TENANT][const.NS_TYPES]), 1)
        self.assertIn(self.tenant_type_1,
                      resp.json()[const.TENANT][const.NS_TYPES],
                      msg="Not found {0}".format(self.tenant_type_1))

    @unless_coverage
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

    @unless_coverage
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

    @tags('negative', 'p1', 'regression')
    @pytest.mark.skip_at_gate
    def test_add_tenant_with_non_existing_tenant_type(self):

        # Randomness is making sure it does not exist before. This avoids
        # using service-admin client to check if tenant type already exists
        tenant_type_name = self.generate_random_string(
            pattern='newtype[\-][0-9a-z]{8}')

        tenant_name = tenant_id = self.generate_random_string(
            const.TENANT_NAME_PATTERN)
        request_object = requests.Tenant(
                tenant_name=tenant_name, tenant_id=tenant_id,
                tenant_types=[tenant_type_name])
        resp = self.identity_admin_client.add_tenant(tenant=request_object)
        self.assertEqual(resp.status_code, 400)
        self.assertEqual(resp.json()['badRequest']['message'],
                         "TenantType with name: '{0}' was not found.".format(
                             tenant_type_name))

    @unless_coverage
    def tearDown(self):
        # Delete all tenants created in the tests
        for id in self.tenant_ids:
            self.identity_admin_client.delete_tenant(tenant_id=id)
        for name in self.tenant_type_ids:
            self.service_admin_client.delete_tenant_type(name=name)
        super(TestAddTenant, self).tearDown()

    @classmethod
    @unless_coverage
    def tearDownClass(cls):
        super(TestAddTenant, cls).tearDownClass()
