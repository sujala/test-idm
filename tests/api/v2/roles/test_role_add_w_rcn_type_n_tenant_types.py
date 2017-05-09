# -*- coding: utf-8 -*
import ddt
import copy

from tests.api.v2 import base
from tests.api.v2.schema import roles as roles_json
from tests.api.v2.models import factory

from tests.package.johny import constants as const
from tests.package.johny.v2.models import requests


@ddt.ddt
class TestRoleAddWTypeAndTenantTypes(base.TestBaseV2):
    """
    1. Add ability for user to set 2 additional attributes on role creation
    1.1 'type' - an optional single valued string
    1.2 'tenantTypes' - a list of strings corresponding to the tenant types
    that an RCN role applies. Only applicable (and required) if type = 'rcn'
    2. Must support 'standard' and 'rcn' type for this story
    2.1 'standard' is the default if type is null/empty string, and is the
    value used if a role does not have a current setting for type
    2.2 'rcn' is to specify an RCN role.
    3. When 'rcn' type is specified:
    3.1 The 'assignment' MUST be 'global' (see CID-438)
    3.1.1 If the user specifies 'rcn' as the type and null/empty string for
    assignment, the application must default the assignment to 'global'
    3.2.1 If the user specifies 'rcn' as the type, but something other than
    null/empty string or 'global' for assignment, a 400 must be returned
    stating 'An RCN role must have global assignment'
    3.2 One or more tenant types MUST be supplied
    3.2.1 The provided tenant types must be automatically lower-cased prior
    to any validation
    3.2.2 The specified tenant types must meet the standard tenant type
    validation w/ the exception that "*" is reserved, to represent all
    tenant types.
    4. The response for Create Role and Get Role by ID must be updated to
    include:
    4.1 The persisted value or 'standard' if no type is stored
    (e.g. legacy roles)
    4.2 tenantTypes only if RCN was specified (attribute must not be
    returned if not an RCN role)
    """
    @classmethod
    def setUpClass(cls):
        super(TestRoleAddWTypeAndTenantTypes, cls).setUpClass()

    def create_tenant_type(self, name):
        request_object = requests.TenantType(name, 'description')
        self.service_admin_client.add_tenant_type(tenant_type=request_object)
        self.tenant_type_ids.append(name.lower())

    def setUp(self):
        super(TestRoleAddWTypeAndTenantTypes, self).setUp()
        self.role_ids = []
        self.tenant_type_ids = []
        self.add_schema_fields = [const.RAX_AUTH_ROLE_TYPE]

    def create_role(self, add_role_obj):
        resp = self.identity_admin_client.add_role(request_object=add_role_obj)
        self.assertEqual(resp.status_code, 201)
        role_id = resp.json()[const.ROLE][const.ID]
        self.role_ids.append(role_id)
        return role_id, resp

    @ddt.file_data('data_add_role_with_type.json')
    def test_add_role_w_type(self, test_data):
        # create role with type and tenant_types
        add_input = test_data['additional_input']
        add_role_obj = factory.get_add_role_request_object(**add_input)
        role_id, role_resp = self.create_role(add_role_obj=add_role_obj)
        if 'role_type' in add_input:
            self.assertEqual(role_resp.json()[const.ROLE][
                                 const.RAX_AUTH_ROLE_TYPE],
                             add_input['role_type'])
            if add_input['role_type'] == const.RCN:
                self.assertEqual(role_resp.json()[const.ROLE][
                                     const.RAX_AUTH_ASSIGNMENT],
                                 const.GLOBAL.upper())
                self.assertIn(const.NS_TYPES, role_resp.json()[const.ROLE])
                tenant_types = []
                for type in role_resp.json()[const.ROLE][const.NS_TYPES]:
                    if type != "*":
                        self.assertTrue(type.islower())
                    tenant_types.append(type)
                self.assertEqual(len(tenant_types), len(set(tenant_types)))
            else:
                self.assertNotIn(const.NS_TYPES, role_resp.json()[const.ROLE])
        else:
            self.assertEqual(role_resp.json()[const.ROLE][
                                 const.RAX_AUTH_ROLE_TYPE], const.STANDARD)
        cp_role_schema = copy.deepcopy(roles_json.add_role)
        cp_role_schema[const.PROPERTIES][const.ROLE][const.REQUIRED] = (
            roles_json.add_role[const.PROPERTIES][const.ROLE][const.REQUIRED] +
            self.add_schema_fields + test_data['additional_schema_fields']
        )
        self.assertSchema(response=role_resp, json_schema=cp_role_schema)

        # schema check for get role by id
        get_role_resp = self.identity_admin_client.get_role(role_id=role_id)
        self.assertEqual(get_role_resp.status_code, 200)
        self.assertSchema(response=get_role_resp, json_schema=cp_role_schema)

    @ddt.file_data('data_add_role_with_type_neg.json')
    def test_add_role_w_type_neg_cases(self, test_data):
        add_input = test_data['additional_input']
        add_role_obj = factory.get_add_role_request_object(**add_input)
        role_resp = self.identity_admin_client.add_role(
            request_object=add_role_obj)
        self.assertEqual(role_resp.status_code, test_data['expected_resp'][
            'code'])
        self.assertEqual(role_resp.json()[const.BAD_REQUEST][const.MESSAGE],
                         test_data['expected_resp']['message'])

    def tearDown(self):
        for id_ in self.role_ids:
            self.identity_admin_client.delete_role(role_id=id_)
        for name in self.tenant_type_ids:
            self.service_admin_client.delete_tenant_type(name=name)
