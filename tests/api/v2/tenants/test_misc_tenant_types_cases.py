from nose.plugins.attrib import attr
import copy
import ddt

from qe_coverage.opencafe_decorators import tags, unless_coverage

from tests.api.v2 import base
from tests.api.v2.schema import tenants
from tests.api.v2.models import factory

from tests.package.johny import constants as const
from tests.package.johny.v2.models import requests


@ddt.ddt
class TestTenantTypes(base.TestBaseV2):
    """Test Tenant Types"""
    def create_tenant_type(self, name):
        request_object = requests.TenantType(name, 'description')
        self.service_admin_client.add_tenant_type(tenant_type=request_object)
        self.tenant_type_ids.append(name.lower())

    @unless_coverage
    def setUp(self):
        super(TestTenantTypes, self).setUp()
        self.tenant_ids = []
        self.role_ids = []
        self.user_ids = []
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
        for tenant_type in ['a', 'b', 'c']:
            self.create_tenant_type(tenant_type)

        self.add_tenant_schema = tenants.add_tenant
        self.add_tenant_with_types_schema = copy.deepcopy(tenants.add_tenant)
        (self.add_tenant_with_types_schema['properties'][const.TENANT]
            ['properties'].update(
                {const.NS_TYPES: {'type': 'array'}}))
        (self.add_tenant_with_types_schema['properties'][const.TENANT]
            ['required'].append(const.NS_TYPES))

    def create_tenant_with_types(self):
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
        self.assertEqual(len(
            resp.json()[const.TENANT][const.NS_TYPES]), 3)
        return tenant_id

    @tags('positive', 'p1', 'regression')
    def test_list_tenant_with_types(self):
        tenant_name = self.create_tenant_with_types()
        resp = self.identity_admin_client.get_tenant(tenant_id=tenant_name)
        self.assertEqual(resp.status_code, 200)

        uadm_username = "uadm_" + self.generate_random_string(
            pattern=const.UPPER_CASE_LETTERS)
        req_obj = factory.get_add_user_request_object_pull(
            user_name=uadm_username)
        resp = self.identity_admin_client.add_user(request_object=req_obj)
        self.assertEqual(resp.status_code, 201)
        new_user_id = resp.json()[const.USER][const.ID]
        uadm_password = resp.json()[const.USER][const.NS_PASSWORD]
        self.user_ids.append(new_user_id)

        new_role_name = "NewRole"+self.generate_random_string(
            pattern=const.UPPER_CASE_LETTERS)
        req_obj = factory.get_add_role_request_object(
            role_name=new_role_name,
            administrator_role="identity:user-manage")
        resp = self.identity_admin_client.add_role(request_object=req_obj)
        self.assertEqual(resp.status_code, 201)
        new_role_id = resp.json()[const.ROLE][const.ID]
        self.role_ids.append(new_role_id)

        resp = self.service_admin_client.add_role_to_user_for_tenant(
                   tenant_id=tenant_name, role_id=new_role_id,
                   user_id=new_user_id)
        self.assertEqual(resp.status_code, 200)
        auth_obj = requests.AuthenticateWithPassword(
            user_name=uadm_username, password=uadm_password
        )
        resp = self.identity_admin_client.get_auth_token(
                   request_object=auth_obj)
        self.assertEqual(resp.status_code, 200)
        uadm_token = resp.json()[const.ACCESS][const.TOKEN][const.ID]

        resp = self.identity_admin_client.list_tenants(requestslib_kwargs={
                   'headers': {'X-Auth-Token': uadm_token}})
        self.assertEqual(resp.status_code, 200)
        self.assertTrue(len(resp.json()[const.TENANTS]) > 0,
                        msg="Empty tenant list!")
        target_tenant = None
        for tenant in resp.json()[const.TENANTS]:
            if tenant[const.NAME] == tenant_name:
                target_tenant = tenant
                break
        self.assertIsNotNone(target_tenant, msg="Couldn't find tenant!")
        self.assertIn(const.NS_TYPES, tenant)
        self.assertEqual(len(target_tenant[const.NS_TYPES]), 3)

    @tags('positive', 'p1', 'regression')
    def test_list_tenants_with_types_by_domain(self):
        tenant_id = tenant_name = self.create_tenant_with_types()
        resp = self.identity_admin_client.get_tenant(tenant_id=tenant_id)
        self.assertEqual(resp.status_code, 200)
        domain_id = resp.json()[const.TENANT][const.RAX_AUTH_DOMAIN_ID]
        resp = self.identity_admin_client.list_tenants_in_domain(
                   domain_id=domain_id)
        self.assertEqual(resp.status_code, 200)
        self.assertTrue(len(resp.json()[const.TENANTS]) > 0,
                        msg="Empty tenant list!")
        target_tenant = None
        for tenant in resp.json()[const.TENANTS]:
            if tenant[const.NAME] == tenant_name:
                target_tenant = tenant
                break
        self.assertIsNotNone(target_tenant, msg="Couldn't find tenant!")
        self.assertIn(const.NS_TYPES, tenant)
        self.assertEqual(len(target_tenant[const.NS_TYPES]), 3)

    @tags('positive', 'p1', 'regression')
    def test_delete_tenant_types_from_tenant(self):
        tenant_id = tenant_name = self.create_tenant_with_types()
        request_object = requests.Tenant(
                tenant_name=tenant_name,
                description=self.tenant_description,
                tenant_id=tenant_id,
                enabled=True,
                tenant_types=[],
                display_name=self.tenant_display_name)
        resp = self.identity_admin_client.update_tenant(
            tenant_id=tenant_id, request_object=request_object)
        self.assertEqual(resp.status_code, 200)
        self.assertSchema(response=resp,
                          json_schema=self.add_tenant_schema)

    # Negative duplicates ignored
    @tags('positive', 'p1', 'regression')
    def test_duplicate_tenant_types_create(self):
        tenant_name = tenant_id = 'tname{0}'.format(
            self.generate_random_string(const.LOWER_CASE_LETTERS))
        request_object = requests.Tenant(
                tenant_name=tenant_name,
                description=self.tenant_description,
                tenant_id=tenant_id,
                enabled=True,
                tenant_types=["B", "B", "A", "A", "C"],
                display_name=self.tenant_display_name)
        resp = self.identity_admin_client.add_tenant(tenant=request_object)
        self.assertEqual(resp.status_code, 201)
        self.assertEqual(
            ["a", "b", "c"],
            sorted(resp.json()[const.TENANT][const.NS_TYPES]))

    @tags('positive', 'p1', 'regression')
    @attr('skip_at_gate')
    def test_duplicate_tenant_types_update(self):
        tenant_id = tenant_name = self.create_tenant_with_types()
        request_object = requests.Tenant(
                tenant_name=tenant_name,
                description=self.tenant_description,
                tenant_id=tenant_id,
                enabled=True,
                tenant_types=["B", "B", "A", "A", "C"],
                display_name=self.tenant_display_name)
        resp = self.identity_admin_client.update_tenant(
            tenant_id=tenant_id, request_object=request_object)
        self.assertEqual(resp.status_code, 200)
        self.assertEqual(
            ["a", "b", "c"],
            sorted(resp.json()[const.TENANT][const.NS_TYPES]))

    @unless_coverage
    def tearDown(self):
        for tenant_id in self.tenant_ids:
            self.identity_admin_client.delete_tenant(tenant_id=tenant_id)
        for user_id in self.user_ids:
            self.identity_admin_client.delete_user(user_id=user_id)
        for role_id in self.role_ids:
            self.identity_admin_client.delete_role(role_id=role_id)
        for name in self.tenant_type_ids:
            self.service_admin_client.delete_tenant_type(name=name)

        super(TestTenantTypes, self).tearDown()
