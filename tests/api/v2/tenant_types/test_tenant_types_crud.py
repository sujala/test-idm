# -*- coding: utf-8 -*
from tests.api.v2 import base

from tests.package.johny import constants as const
from tests.package.johny.v2.models import requests
from tests.api.v2.schema import tenant_types


class TestTenantTypes(base.TestBaseV2):
    """Tenant Types Crud Tests"""

    def setUp(self):
        super(TestTenantTypes, self).setUp()
        self.tenant_types = []

    def create_tenant_type(self):

        name = self.generate_random_string(const.TENANT_TYPE_PATTERN)
        description = self.generate_random_string(
            'description[\-][0-9a-wA-W]{:10}')
        req_object = requests.TenantType(name=name, description=description)
        resp = self.service_admin_client.add_tenant_type(req_object)
        self.tenant_types.append(resp.json()[const.RAX_AUTH_TENANT_TYPE][
                                     const.NAME])
        return resp

    def test_add_tenant_type(self):

        create_resp = self.create_tenant_type()
        self.assertEqual(create_resp.status_code, 201)
        self.assertSchema(create_resp, tenant_types.add_tenant_type)

    def test_get_tenant_type(self):

        create_resp = self.create_tenant_type()
        self.assertEqual(create_resp.status_code, 201)
        self.assertSchema(create_resp, tenant_types.add_tenant_type)
        tenant_type = create_resp.json()[
            const.RAX_AUTH_TENANT_TYPE][const.NAME]
        get_resp = self.service_admin_client.get_tenant_type(name=tenant_type)
        self.assertEqual(get_resp.status_code, 200)
        self.assertSchema(get_resp, tenant_types.get_tenant_type)

    def test_list_tenant_types(self):

        create_resp = self.create_tenant_type()
        self.assertEqual(create_resp.status_code, 201)
        self.assertSchema(create_resp, tenant_types.add_tenant_type)
        tenant_type = create_resp.json()[
            const.RAX_AUTH_TENANT_TYPE][const.NAME]
        list_resp = self.service_admin_client.list_tenant_types()
        self.assertEqual(list_resp.status_code, 200)
        self.assertSchema(list_resp, tenant_types.list_tenant_types)
        list_of_types = [t_type[const.NAME] for t_type in list_resp.json()[
            const.RAX_AUTH_TENANT_TYPES]]
        self.assertIn(tenant_type, list_of_types)

    def test_delete_tenant_type(self):

        create_resp = self.create_tenant_type()
        self.assertEqual(create_resp.status_code, 201)
        self.assertSchema(create_resp, tenant_types.add_tenant_type)
        tenant_type = create_resp.json()[
            const.RAX_AUTH_TENANT_TYPE][const.NAME]
        delete_resp = self.service_admin_client.delete_tenant_type(
            name=tenant_type)
        self.assertEqual(delete_resp.status_code, 204)
        get_resp = self.service_admin_client.get_tenant_type(name=tenant_type)
        self.assertEqual(get_resp.status_code, 404)

    def tearDown(self):

        for tenant_type in self.tenant_types:
            self.service_admin_client.delete_tenant_type(
                tenant_type)
        super(TestTenantTypes, self).tearDown()
