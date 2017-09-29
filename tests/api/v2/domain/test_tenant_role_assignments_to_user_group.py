# -*- coding: utf-8 -*
import ddt

from tests.api.v2 import base
from tests.api.v2.schema import user_groups
from tests.api.v2.models import factory, responses
from tests.package.johny import constants as const
from tests.package.johny.v2.models import requests


@ddt.ddt
class AddTenantRoleAssignmentsToUserGroup(base.TestBaseV2):
    """
    Tests for Add tenant-role assignments for user group for a domain service
    """
    @classmethod
    def setUpClass(cls):
        super(AddTenantRoleAssignmentsToUserGroup, cls).setUpClass()

    def setUp(self):
        self.domain_id = self.generate_random_string(pattern='[\d]{7}')
        self.user_admin_client = self.generate_client(
            parent_client=self.identity_admin_client,
            additional_input_data={'domain_id': self.domain_id})
        self.clients = {
            'user_admin': self.user_admin_client,
        }
        self.role_ids = []
        self.tenant_ids = []

    def generate_tenants_assignment_dict(self, on_role, *for_tenants):

        tenant_assignment_request = {
            const.ON_ROLE: on_role,
            const.FOR_TENANTS: list(for_tenants)
        }
        return tenant_assignment_request

    def create_role(self):

        role_req = factory.get_add_role_request_object(
            administrator_role=const.USER_MANAGE_ROLE_NAME)
        add_role_resp = self.identity_admin_client.add_role(
            request_object=role_req)
        self.assertEqual(add_role_resp.status_code, 201)
        role = responses.Role(add_role_resp.json())
        self.role_ids.append(role.id)
        return role

    def create_tenant(self):

        tenant_req = factory.get_add_tenant_object(domain_id=self.domain_id)
        add_tenant_resp = self.identity_admin_client.add_tenant(
            tenant=tenant_req)
        self.assertEqual(add_tenant_resp.status_code, 201)
        tenant = responses.Tenant(add_tenant_resp.json())
        self.tenant_ids.append(tenant.id)
        return tenant

    def test_add_tenant_role_assignments_to_user_group(self):
        group_req = factory.get_add_user_group_request(self.domain_id)
        create_group_resp = self.user_admin_client.add_user_group_to_domain(
            domain_id=self.domain_id, request_object=group_req)
        self.assertEqual(create_group_resp.status_code, 201)
        group = responses.UserGroup(create_group_resp.json())

        # create roles
        role_1 = self.create_role()
        role_2 = self.create_role()

        # create tenant
        tenant_1 = self.create_tenant()

        # create tenant assignments request dicts
        tenant_assignment_req_1 = self.generate_tenants_assignment_dict(
            role_1.id, tenant_1.id)
        tenant_assignment_req_2 = self.generate_tenants_assignment_dict(
            role_2.id, "*")
        tenants_role_assignment_req = requests.TenantRoleAssignments(
            tenant_assignment_req_1, tenant_assignment_req_2)

        assignment_resp = (
            self.user_admin_client.add_tenant_role_assignments_to_user_group(
                domain_id=self.domain_id, group_id=group.id,
                request_object=tenants_role_assignment_req))
        self.assertEqual(assignment_resp.status_code, 200)
        self.assertSchema(
            assignment_resp,
            json_schema=user_groups.tenants_role_assignments_for_user_group)

        for assignment in assignment_resp.json()[
              const.RAX_AUTH_ROLE_ASSIGNMENTS][const.TENANT_ASSIGNMENTS]:
            if assignment[const.ON_ROLE] == role_1.id:
                self.assertEqual(assignment[const.FOR_TENANTS], [tenant_1.id])
            elif assignment[const.ON_ROLE] == role_2.id:
                self.assertEqual(assignment[const.FOR_TENANTS], ["*"])

    def tearDown(self):
        super(AddTenantRoleAssignmentsToUserGroup, self).tearDown()
        # This deletes the domain which automatically deletes any user groups
        # in that domain. Hence, not explicitly deleting the user groups
        self.delete_client(self.user_admin_client,
                           parent_client=self.identity_admin_client)
        for tenant_id in self.tenant_ids:
            self.identity_admin_client.delete_tenant(tenant_id=tenant_id)
        for role_id in self.role_ids:
            self.identity_admin_client.delete_role(role_id=role_id)
