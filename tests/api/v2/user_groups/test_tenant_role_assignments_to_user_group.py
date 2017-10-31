# -*- coding: utf-8 -*
from nose.plugins.attrib import attr

from tests.api.v2.models import factory, responses
from tests.api.v2.schema import user_groups
from tests.api.v2.user_groups import usergroups
from tests.package.johny import constants as const
from tests.package.johny.v2.models import requests


class CrudTenantRoleAssignmentsToUserGroup(usergroups.TestUserGroups):
    """
    Tests for crud tenant-role assignments for user group for a domain service
    """
    @classmethod
    def setUpClass(cls):
        super(CrudTenantRoleAssignmentsToUserGroup, cls).setUpClass()

    def setUp(self):
        super(CrudTenantRoleAssignmentsToUserGroup, self).setUp()
        self.user_admin_client = self.generate_client(
            parent_client=self.identity_admin_client,
            additional_input_data={'domain_id': self.domain_id})
        self.user_manager_client = self.generate_client(
            parent_client=self.user_admin_client,
            additional_input_data={'is_user_manager': True})

    def setup_user_group(self):
        group_req = factory.get_add_user_group_request(self.domain_id)
        create_group_resp = self.user_admin_client.add_user_group_to_domain(
            domain_id=self.domain_id, request_object=group_req)
        self.assertEqual(create_group_resp.status_code, 201)
        return responses.UserGroup(create_group_resp.json())

    @attr(type='smoke_alpha')
    def test_crud_tenant_role_assignments_to_user_group(self):
        group = self.setup_user_group()
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

        # Testing the list tenant assignments
        self.verify_list_tenant_assignments_service(
            group=group, role_1=role_1, role_2=role_2, tenant=tenant_1)

        # Testing delete tenant assignment
        self.verify_delete_tenant_role_assignments_service(
            group=group, role_1=role_1, role_2=role_2)

        # Testing group tenant assignments are handled properly when
        # tenant is deleted
        self.verify_tenant_assignments_for_group_after_tenant_deletion(
            tenant=tenant_1.id, group=group, role_2=role_2)

    def verify_tenant_assignments_for_group_after_tenant_deletion(
            self, tenant, group, role_2):

        del_resp = self.identity_admin_client.delete_tenant(tenant_id=tenant)
        self.assertEqual(del_resp.status_code, 204)

        list_resp = (self.user_admin_client.
                     list_tenant_role_assignments_to_user_group(
                      domain_id=self.domain_id, group_id=group.id))
        self.assertEqual(list_resp.status_code, 200)

        # Verify when tenant is deleted, the corresponding tenant role
        # assignment for the user group is removed
        assignments = [assignment for assignment in list_resp.json()[
              const.RAX_AUTH_ROLE_ASSIGNMENTS][const.TENANT_ASSIGNMENTS]]
        self.assertEqual(len(assignments), 1)
        self.assertEqual(assignments[0][const.ON_ROLE], role_2.id)
        self.assertEqual(assignments[0][const.FOR_TENANTS], ["*"])

    def verify_list_tenant_assignments_service(
            self, group, role_1, role_2, tenant):

        manager_client = self.user_manager_client
        list_resp = manager_client.list_tenant_role_assignments_to_user_group(
            domain_id=self.domain_id, group_id=group.id)
        self.assertEqual(list_resp.status_code, 200)
        self.assertSchema(
            list_resp,
            json_schema=user_groups.tenants_role_assignments_for_user_group)

        for assignment in list_resp.json()[
              const.RAX_AUTH_ROLE_ASSIGNMENTS][const.TENANT_ASSIGNMENTS]:
            if assignment[const.ON_ROLE] == role_1.id:
                self.assertEqual(assignment[const.FOR_TENANTS], [tenant.id])
            elif assignment[const.ON_ROLE] == role_2.id:
                self.assertEqual(assignment[const.FOR_TENANTS], ["*"])

    def verify_delete_tenant_role_assignments_service(
            self, group, role_1, role_2):

        manager_client = self.user_manager_client
        resp = manager_client.delete_tenant_role_assignments_from_user_group(
            domain_id=self.domain_id, group_id=group.id, role_id=role_1.id)
        self.assertEqual(resp.status_code, 204)

        # Testing the list tenant assignments again to verify delete
        list_resp = manager_client.list_tenant_role_assignments_to_user_group(
            domain_id=self.domain_id, group_id=group.id)
        self.assertEqual(list_resp.status_code, 200)

        assignments = list_resp.json()[
              const.RAX_AUTH_ROLE_ASSIGNMENTS][const.TENANT_ASSIGNMENTS]
        self.assertEqual(len(assignments), 1)
        self.assertEqual(assignments[0][const.ON_ROLE], role_2.id)
        self.assertEqual(assignments[0][const.FOR_TENANTS], ["*"])

    @attr(type='smoke_alpha')
    def test_add_role_to_user_group_for_tenant(self):
        group = self.setup_user_group()
        role = self.create_role()
        tenant = self.create_tenant()

        resp = self.user_admin_client.add_role_to_user_group_for_tenant(
            domain_id=self.domain_id, group_id=group.id, role_id=role.id,
            tenant_id=tenant.id)
        self.assertEqual(resp.status_code, 204)

        list_resp = (
            self.user_admin_client.list_tenant_role_assignments_to_user_group(
              domain_id=self.domain_id, group_id=group.id))
        self.assertEqual(list_resp.status_code, 200)
        self.assertSchema(
            list_resp,
            json_schema=user_groups.tenants_role_assignments_for_user_group)
        assignments = list_resp.json()[
              const.RAX_AUTH_ROLE_ASSIGNMENTS][const.TENANT_ASSIGNMENTS]
        self.assertEqual(len(assignments), 1)
        self.assertEqual(assignments[0][const.ON_ROLE], role.id)
        self.assertEqual(assignments[0][const.FOR_TENANTS], [tenant.id])

    def tearDown(self):

        # Not calling 'log_tearDown_error' as delete_client() method is
        # already wrapped with it. So, any cleanup failures will be caught.
        # Deleting the user manager first, so that domain can be
        # safely deleted in the subsequent user-admin client cleanup
        self.identity_admin_client.delete_user(
            self.user_manager_client.default_headers[const.X_USER_ID])

        # This deletes the domain which automatically deletes any user groups
        # in that domain. Hence, not explicitly deleting the user groups
        self.delete_client(self.user_admin_client,
                           parent_client=self.identity_admin_client)
        super(CrudTenantRoleAssignmentsToUserGroup, self).tearDown()
