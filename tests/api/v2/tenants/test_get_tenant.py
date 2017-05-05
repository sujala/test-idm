import ddt
import copy

from tests.api.v2 import base
from tests.api.v2.schema import tenants

from tests.package.johny import constants as const
from tests.package.johny.v2.models import requests


@ddt.ddt
class TestGetTenant(base.TestBaseV2):
    """Get Tenant Test"""
    def create_tenant_type(self, name):
        request_object = requests.TenantType(name, 'description')
        self.service_admin_client.add_tenant_type(tenant_type=request_object)
        self.tenant_type_ids.append(name.lower())

    def setUp(self):
        super(TestGetTenant, self).setUp()
        self.tenant_ids = []
        self.tenant_type_ids = []
        self.tenant_description = 'A tenant described'
        self.tenant_display_name = 'A name displayed'
        self.tenant_type_1 = self.generate_random_string(
            pattern=const.TENANT_TYPE_PATTERN)
        self.tenant_type_2 = self.generate_random_string(
            pattern=const.TENANT_TYPE_PATTERN)
        self.tenant_type_3 = self.generate_random_string(
            pattern=const.TENANT_TYPE_PATTERN)
        self.create_tenant_type(self.tenant_type_1)
        self.create_tenant_type(self.tenant_type_2)
        self.create_tenant_type(self.tenant_type_3)
        self.add_tenant_with_types_schema = copy.deepcopy(tenants.add_tenant)
        (self.add_tenant_with_types_schema['properties'][const.TENANT]
            ['properties'].update({const.NS_TYPES: {}}))
        (self.add_tenant_with_types_schema['properties'][const.TENANT]
            ['required'].append(const.NS_TYPES))

    def test_get_tenant_with_type(self):
        tenant_name = tenant_id = 'tname{0}'.format(
            self.generate_random_string(const.LOWER_CASE_LETTERS))
        request_object = requests.Tenant(
                tenant_name=tenant_name,
                description=self.tenant_description,
                tenant_id=tenant_id,
                enabled=True,
                tenant_types=[self.tenant_type_1,
                              self.tenant_type_2,
                              self.tenant_type_3],
                display_name=self.tenant_display_name)
        resp = self.identity_admin_client.add_tenant(tenant=request_object)
        self.assertEqual(resp.status_code, 201)
        self.tenant_ids.append(tenant_id)
        self.assertSchema(response=resp,
                          json_schema=self.add_tenant_with_types_schema)
        self.assertEqual(
            len(resp.json()[const.TENANT][const.NS_TYPES]), 3)
        resp = self.identity_admin_client.get_tenant(tenant_id=tenant_id)
        self.assertEqual(resp.status_code, 200)
        self.assertEqual(
            len(resp.json()[const.TENANT][const.NS_TYPES]), 3)
        self.assertSchema(response=resp,
                          json_schema=self.add_tenant_with_types_schema)
        for tenant_type in [self.tenant_type_1.lower(),
                            self.tenant_type_2.lower(),
                            self.tenant_type_3.lower()]:
            self.assertIn(tenant_type,
                          resp.json()[const.TENANT][const.NS_TYPES],
                          msg="Not found {0}".format(tenant_type))

    def tearDown(self):
        # Delete all users created in the tests
        for tenant_id in self.tenant_ids:
            self.identity_admin_client.delete_tenant(tenant_id=tenant_id)
        for name in self.tenant_type_ids:
            self.identity_admin_client.delete_tenant_type(name=name)
        super(TestGetTenant, self).tearDown()
