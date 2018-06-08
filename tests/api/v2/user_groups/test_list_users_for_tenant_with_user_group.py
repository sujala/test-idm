# -*- coding: utf-8 -*
from nose.plugins.attrib import attr
from qe_coverage.opencafe_decorators import tags, unless_coverage

from tests.api.v2.user_groups import usergroups

from tests.package.johny import constants as const
from tests.package.johny.v2.models import requests


class TestListUsersForTenantWithUserGroup(usergroups.TestUserGroups):

    """
    List users for tenant when a user group has a role on tenant
    """
    @classmethod
    @unless_coverage
    def setUpClass(cls):
        super(TestListUsersForTenantWithUserGroup, cls).setUpClass()

    @unless_coverage
    def setUp(self):
        super(TestListUsersForTenantWithUserGroup, self).setUp()
        self.user_admin_client = self.generate_client(
            parent_client=self.identity_admin_client,
            additional_input_data={
                'domain_id': self.domain_id,
                'password': self.password,
                'user_name': self.user_name
            })

    @tags('positive', 'p0', 'smoke')
    @attr(type='smoke_alpha')
    def test_list_users_for_tenant(self):
        """
        Test adds a user to user group & then adds a role to the user-group.
        Verifies list users for tenant shows up the user in the user-group
        both when query-param roleId is present and absent.
        """
        user_group = self.create_and_add_user_group_to_user(
            client=self.user_admin_client)
        role = self.create_role()
        tenant = self.create_tenant()

        # create tenant assignments request dicts
        tenant_assignment_req = self.generate_tenants_assignment_dict(
            role.id, tenant.id)
        tenants_role_assignment_req = requests.TenantRoleAssignments(
            tenant_assignment_req)
        assignment_resp = (
            self.user_admin_client.add_tenant_role_assignments_to_user_group(
                domain_id=self.domain_id, group_id=user_group.id,
                request_object=tenants_role_assignment_req))
        self.assertEqual(assignment_resp.status_code, 200)

        # Verify list users in tenant without query param roleId
        self.verify_users_in_tenant(tenant=tenant)

        # Verify list users in tenant with query param roleId
        self.verify_users_in_tenant(tenant=tenant, role=role)

        # Adding same role to user on tenant explicitly
        self.identity_admin_client.add_role_to_user_for_tenant(
            tenant_id=tenant.id,
            user_id=self.user_admin_client.default_headers[const.X_USER_ID],
            role_id=role.id)

        # Verify list users in tenant with query param roleId
        self.verify_users_in_tenant(tenant=tenant, role=role)

    def verify_users_in_tenant(self, tenant, role=None):

        option = None
        if role:
            option = {
                const.PARAM_ROLE_ID: role.id
            }
        list_users_resp = self.identity_admin_client.list_users_for_tenant(
            tenant_id=tenant.id, option=option)
        user_ids = [
            user[const.ID] for user in list_users_resp.json()[const.USERS]]
        self.assertIn(
            self.user_admin_client.default_headers[const.X_USER_ID],
            user_ids)

    @unless_coverage
    def tearDown(self):
        # Not explicitly adding log_tearDown_error as it is covered, by both
        # delete_client and parent class's tearDown()
        self.delete_client(client=self.user_admin_client,
                           parent_client=self.identity_admin_client)
        super(TestListUsersForTenantWithUserGroup, self).tearDown()
