# -*- coding: utf-8 -*

from tests.api.utils import func_helper
from tests.api.v2 import base
from tests.api.v2.models import factory, responses
from tests.api.v2.schema import user_groups
from tests.package.johny import constants as const
from tests.package.johny.v2.models import requests


class TestMultipleRoleAssignmentsToUser(base.TestBaseV2):
    """
    Tests to verify if multiple roles can be assigned to a user in
    a single api call
    """
    @classmethod
    def setUpClass(cls):
        super(TestMultipleRoleAssignmentsToUser, cls).setUpClass()

    def setUp(self):
        super(TestMultipleRoleAssignmentsToUser, self).setUp()
        self.domain_id = func_helper.generate_randomized_domain_id(
            client=self.identity_admin_client)
        self.user_admin_client = self.generate_client(
            parent_client=self.identity_admin_client,
            additional_input_data={'domain_id': self.domain_id})
        self.role_ids = []
        self.tenant_ids = []

    def create_role(self, assignment_type='BOTH'):

        role_req = factory.get_add_role_request_object(
            administrator_role=const.USER_MANAGE_ROLE_NAME,
            assignment=assignment_type)
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

    def generate_tenants_assignment_dict(self, on_role, *for_tenants):
        tenant_assignment_request = {
            const.ON_ROLE: on_role,
            const.FOR_TENANTS: list(for_tenants)
        }
        return tenant_assignment_request

    def test_grant_multiple_roles_to_user(self):

        # create roles
        role_1 = self.create_role(assignment_type='TENANT')
        role_2 = self.create_role(assignment_type='BOTH')
        # create tenant
        tenant_1 = self.create_tenant()

        # create tenant assignments request dicts
        tenant_assignment_req_1 = self.generate_tenants_assignment_dict(
            role_1.id, tenant_1.id)
        tenant_assignment_req_2 = self.generate_tenants_assignment_dict(
            role_2.id, "*")
        tenants_role_assignment_req = requests.TenantRoleAssignments(
            tenant_assignment_req_1, tenant_assignment_req_2)

        assignment_resp = self.call_add_multiple_roles_service_and_validate(
            tenants_role_assignment_req=tenants_role_assignment_req)
        # Checking if response returns info of both the roles 1 & 2 and see
        # if it is correct
        self.verify_role_assignments_for_user(
            assignment_resp, role_1=role_1, role_2=role_2, tenant=tenant_1)

        # Updating role assignment for role_2
        tenant_assignment_req_2 = self.generate_tenants_assignment_dict(
            role_2.id, tenant_1.id)

        tenants_role_assignment_req = requests.TenantRoleAssignments(
            tenant_assignment_req_2)
        assignment_resp = self.call_add_multiple_roles_service_and_validate(
            tenants_role_assignment_req=tenants_role_assignment_req)

        self.verify_role_assignments_for_user(
            assignment_resp, role_1=role_1, role_2=role_2, tenant=tenant_1,
            role_2_global=False)

        # Trying to add a role globally for which assignment type='TENANT'
        tenant_assignment_req_1 = self.generate_tenants_assignment_dict(
            role_1.id, "*")
        tenants_role_assignment_req = requests.TenantRoleAssignments(
            tenant_assignment_req_1)
        self.call_add_multiple_roles_service_and_validate(
            tenants_role_assignment_req=tenants_role_assignment_req,
            status_code=403)

    def call_add_multiple_roles_service_and_validate(
            self, tenants_role_assignment_req, status_code=200):

        assignment_resp = (
            self.identity_admin_client.add_role_assignments_to_user(
                user_id=self.user_admin_client.default_headers[
                    const.X_USER_ID],
                request_object=tenants_role_assignment_req))
        self.assertEqual(assignment_resp.status_code, status_code)
        if status_code == 200:
            self.assertSchema(
                assignment_resp,
                json_schema=(
                    user_groups.tenants_role_assignments_for_user_group))
        return assignment_resp

    def verify_role_assignments_for_user(
            self, roles_resp, role_1, role_2, tenant, role_2_global=True):

        for assignment in roles_resp.json()[
              const.RAX_AUTH_ROLE_ASSIGNMENTS][const.TENANT_ASSIGNMENTS]:
            if assignment[const.ON_ROLE] == role_1.id:
                self.assertEqual(assignment[const.FOR_TENANTS], [tenant.id])
            elif assignment[const.ON_ROLE] == role_2.id:
                if role_2_global:
                    self.assertEqual(assignment[const.FOR_TENANTS], ["*"])
                else:
                    self.assertEqual(
                        assignment[const.FOR_TENANTS], [tenant.id])

    @base.base.log_tearDown_error
    def tearDown(self):
        super(TestMultipleRoleAssignmentsToUser, self).tearDown()
        self.delete_client(self.user_admin_client,
                           parent_client=self.identity_admin_client)
        for role_id in self.role_ids:
            resp = self.identity_admin_client.delete_role(role_id=role_id)
            self.assertEqual(
                resp.status_code, 204,
                msg='Role with ID {0} failed to delete'.format(
                    role_id))
        for tenant_id in self.tenant_ids:
            resp = self.identity_admin_client.delete_tenant(
                tenant_id=tenant_id)
            # For some cases, tenant is getting deleted by delete_client()
            # call, prior. Hence checking for either 204 or 404.
            self.assertIn(
                resp.status_code, [204, 404],
                msg='Tenant with ID {0} failed to delete'.format(
                    tenant_id))
