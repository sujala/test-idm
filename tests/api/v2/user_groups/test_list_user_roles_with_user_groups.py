# -*- coding: utf-8 -*
import copy

from nose.plugins.attrib import attr

from tests.api import base
from tests.api.v2.user_groups import usergroups
from tests.api.v2.schema import user_groups

from tests.package.johny import constants as const
from tests.package.johny.v2.models import requests


class TestListUsersRolesWithUserGroups(usergroups.TestUserGroups):

    """
    List user roles with user groups
    """
    @classmethod
    def setUpClass(cls):
        super(TestListUsersRolesWithUserGroups, cls).setUpClass()

    def setUp(self):
        super(TestListUsersRolesWithUserGroups, self).setUp()
        self.user_admin_wl_domain_client = None
        self.user_ids = []

    def test_list_user_roles_no_groups(self):
        self.user_admin_wl_domain_client = self.generate_client(
            parent_client=self.identity_admin_client,
            additional_input_data={
                'domain_id': self.domain_id,
                'password': self.password,
                'user_name': self.user_name
            })
        resp = self.user_admin_wl_domain_client.list_roles_for_user(
            user_id=self.user_admin_wl_domain_client.default_headers[
                const.X_USER_ID])

        self.assertEqual(resp.status_code, 200)
        list_role_names = [role[const.NAME] for role in resp.json()[
            const.ROLES]]
        self.assertEqual(len(list_role_names), 1)
        self.assertIn(const.USER_ADMIN_ROLE_NAME, list_role_names)

    def test_list_user_roles_with_user_group_without_roles(self):
        self.user_admin_wl_domain_client = self.generate_client(
            parent_client=self.identity_admin_client,
            additional_input_data={
                'domain_id': self.domain_id,
                'password': self.password,
                'user_name': self.user_name
            })

        self.create_and_add_user_group_to_user(
            self.user_admin_wl_domain_client)
        resp = self.user_admin_wl_domain_client.list_roles_for_user(
            user_id=self.user_admin_wl_domain_client.default_headers[
                const.X_USER_ID])

        self.assertEqual(resp.status_code, 200)
        list_role_names = [role[const.NAME] for role in resp.json()[
            const.ROLES]]
        self.assertEqual(len(list_role_names), 1)
        self.assertIn(const.USER_ADMIN_ROLE_NAME, list_role_names)

    @attr(type='smoke_alpha')
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

        resp = self.user_admin_wl_domain_client.list_roles_for_user(
            user_id=self.user_admin_wl_domain_client.default_headers[
                const.X_USER_ID])

        self.assertEqual(resp.status_code, 200)
        role_name_list = [role[const.NAME] for role in resp.json()[
            const.ROLES]]

        self.assertEqual(len(role_name_list), 2)
        # role is not explicitly assigned to user
        self.assertNotIn(role_1.name, role_name_list)
        # role is globally assigned to a domain
        self.assertIn(role_2.name, role_name_list)
        # role explicitly assigned to user
        self.assertIn(const.USER_ADMIN_ROLE_NAME, role_name_list)

    def test_domain_admin_change_with_user_groups(self):
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
        tenant_assignment_req_2 = self.generate_tenants_assignment_dict(
            role_1.id, "*")
        tenants_role_assignment_req = requests.TenantRoleAssignments(
            tenant_assignment_req_2)
        assignment_resp = (self.user_admin_wl_domain_client
                           .add_tenant_role_assignments_to_user_group(
                            domain_id=self.domain_id, group_id=group.id,
                            request_object=tenants_role_assignment_req))
        self.assertEqual(assignment_resp.status_code, 200)

        # create a sub user to make domain admin change call
        input_data = {'domain_id': self.domain_id}
        add_user_req = usergroups.factory.get_add_user_request_object(
            input_data=input_data)
        add_sub_user_resp = self.user_admin_wl_domain_client.add_user(
            add_user_req)
        self.assertEqual(add_sub_user_resp.status_code, 201)
        old_sub_user_id = add_sub_user_resp.json()[const.USER][const.ID]

        request_object = requests.DomainAdministratorChange(
            old_sub_user_id,
            self.user_admin_wl_domain_client.default_headers[const.X_USER_ID])
        # promote user manage to user admin
        resp = self.identity_admin_client.change_administrators(
            domain_id=self.domain_id, request_object=request_object)
        self.assertEqual(resp.status_code, 204)

        # Recording user ids for cleaner teardown
        old_user_admin_id = copy.deepcopy(
            self.user_admin_wl_domain_client.default_headers[const.X_USER_ID])
        self.user_ids.append(old_user_admin_id)
        self.user_admin_wl_domain_client.default_headers[const.X_USER_ID] = (
            old_sub_user_id)

        # Verify list global roles for old user admin
        self.verify_list_user_roles_response(
            user_id=old_user_admin_id, global_role=role_1)

        # Verify role added to user group is not on the old sub user after
        # domain admin change call, since this user is not added to the
        # user group
        self.verify_list_user_roles_response(
            user_id=old_sub_user_id, global_role=role_1, is_present=False)

    def verify_list_user_roles_response(self, user_id, global_role,
                                        is_present=True):

        resp = self.identity_admin_client.list_roles_for_user(
            user_id=user_id)

        self.assertEqual(resp.status_code, 200)
        role_name_list = [role[const.NAME] for role in resp.json()[
            const.ROLES]]
        if is_present:
            self.assertIn(global_role.name, role_name_list)
        else:
            self.assertNotIn(global_role.name, role_name_list)

    @base.log_tearDown_error
    def tearDown(self):
        for user_id in self.user_ids:
            resp = self.identity_admin_client.delete_user(user_id=user_id)
            self.assertEqual(
                resp.status_code, 204,
                msg='User with ID {0} failed to delete 0'.format(
                    user_id))
        # This deletes the domain which automatically deletes any user groups
        # in that domain. Hence, not explicitly deleting the user groups
        self.delete_client(self.user_admin_wl_domain_client,
                           parent_client=self.identity_admin_client)
        super(TestListUsersRolesWithUserGroups, self).tearDown()

    @classmethod
    def tearDownClass(cls):
        super(TestListUsersRolesWithUserGroups, cls).tearDownClass()
