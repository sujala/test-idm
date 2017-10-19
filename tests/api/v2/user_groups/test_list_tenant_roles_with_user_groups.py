# -*- coding: utf-8 -*
from tests.api.v2.user_groups import usergroups
from tests.api.v2.schema import user_groups

from tests.package.johny import constants as const
from tests.package.johny.v2.models import requests


class TestListTenantRolesWithUserGroups(usergroups.TestUserGroups):

    """
    List user roles with user groups
    """
    @classmethod
    def setUpClass(cls):
        super(TestListTenantRolesWithUserGroups, cls).setUpClass()

    def setUp(self):
        super(TestListTenantRolesWithUserGroups, self).setUp()
        self.user_admin_wl_domain_client = None
        # get tenant access role id
        get_role_resp = self.identity_admin_client.get_role_by_name(
            role_name=const.TENANT_ACCESS_ROLE_NAME)
        self.tenant_access_role_id = get_role_resp.json()[const.ROLES][0][
            const.ID]

    def test_list_tenant_roles_no_groups(self):
        self.user_admin_wl_domain_client = self.generate_client(
            parent_client=self.identity_admin_client,
            additional_input_data={
                'domain_id': self.domain_id,
                'password': self.password,
                'user_name': self.user_name
            })
        # create tenant
        tenant_1 = self.create_tenant()

        resp = self.identity_admin_client.list_roles_for_user_on_tenant(
            tenant_id=tenant_1.id,
            user_id=self.user_admin_wl_domain_client.default_headers[
                const.X_USER_ID])

        self.assertEqual(resp.status_code, 200)
        list_role_ids = [role[const.ID] for role in resp.json()[const.ROLES]]
        self.assertEqual(len(list_role_ids), 1)
        # only retrieve tenant access role
        self.assertIn(self.tenant_access_role_id, list_role_ids)

    def test_list_user_roles_with_user_group_without_roles(self):
        self.user_admin_wl_domain_client = self.generate_client(
            parent_client=self.identity_admin_client,
            additional_input_data={
                'domain_id': self.domain_id,
                'password': self.password,
                'user_name': self.user_name
            })

        # create tenant
        tenant_1 = self.create_tenant()

        self.create_and_add_user_group_to_user(
            self.user_admin_wl_domain_client)
        resp = self.identity_admin_client.list_roles_for_user_on_tenant(
            tenant_id=tenant_1.id,
            user_id=self.user_admin_wl_domain_client.default_headers[
                const.X_USER_ID])

        self.assertEqual(resp.status_code, 200)
        list_role_ids = [k[const.ID] for k in resp.json()[const.ROLES]]
        self.assertEqual(len(list_role_ids), 1)
        # only retrieve tenant access role
        self.assertIn(self.tenant_access_role_id, list_role_ids)

    def test_list_user_roles_with_user_group_with_roles(self):
        self.user_admin_wl_domain_client = self.generate_client(
            parent_client=self.identity_admin_client,
            additional_input_data={
                'domain_id': self.domain_id,
                'password': self.password,
                'user_name': self.user_name
            })

        group = self.create_and_add_user_group_to_user(
            self.user_admin_wl_domain_client)

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

        assignment_resp = (self.user_admin_wl_domain_client
                           .add_tenant_role_assignments_to_user_group(
                            domain_id=self.domain_id, group_id=group.id,
                            request_object=tenants_role_assignment_req))
        self.assertEqual(assignment_resp.status_code, 200)
        self.assertSchema(
            assignment_resp,
            json_schema=user_groups.tenants_role_assignments_for_user_group)

        resp = self.identity_admin_client.list_roles_for_user_on_tenant(
            tenant_id=tenant_1.id,
            user_id=self.user_admin_wl_domain_client.default_headers[
                const.X_USER_ID])

        self.assertEqual(resp.status_code, 200)
        list_role_ids = [k[const.ID] for k in resp.json()[const.ROLES]]

        self.assertEqual(len(list_role_ids), 2)
        # role is not explicitly assigned to user
        self.assertIn(role_1.id, list_role_ids)
        # role is globally assigned to a user
        self.assertNotIn(role_2.id, list_role_ids)
        # role implicitly to user for all tenants within domain
        self.assertIn(self.tenant_access_role_id, list_role_ids)

    def tearDown(self):
        super(TestListTenantRolesWithUserGroups, self).tearDown()
        # This deletes the domain which automatically deletes any user groups
        # in that domain. Hence, not explicitly deleting the user groups
        self.delete_client(self.user_admin_wl_domain_client,
                           parent_client=self.identity_admin_client)
        for role_id in self.role_ids:
            self.identity_admin_client.delete_role(role_id=role_id)

    @classmethod
    def tearDownClass(cls):
        super(TestListTenantRolesWithUserGroups, cls).tearDownClass()
