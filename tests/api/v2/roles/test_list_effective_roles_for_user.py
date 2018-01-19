# -*- coding: utf-8 -*
from nose.plugins.attrib import attr
from random import randrange

from tests.api.v2 import base
from tests.api.v2.models import factory, responses

from tests.package.johny import constants as const


class TestListEffectiveRolesForUser(base.TestBaseV2):

    """ List effective role for user
    """

    @classmethod
    def setUpClass(cls):
        """Class level set up for the tests

        Create users needed for the tests and generate clients for those users.
        """
        super(TestListEffectiveRolesForUser, cls).setUpClass()

        contact_id = randrange(start=const.CONTACT_ID_MIN,
                               stop=const.CONTACT_ID_MAX)
        cls.domain_id = cls.generate_random_string(
            pattern='test[\-]spec[\-]user[\-]list[\-][\d]{5}')
        cls.user_admin_client = cls.generate_client(
            parent_client=cls.identity_admin_client,
            additional_input_data={'domain_id': cls.domain_id,
                                   'contact_id': contact_id},
            one_call=True)

        cls.user_manager_client = cls.generate_client(
            parent_client=cls.user_admin_client,
            additional_input_data={'is_user_manager': True})

        sub_user_name = cls.generate_random_string(
            pattern=const.SUB_USER_PATTERN)
        cls.user_client = cls.generate_client(
            parent_client=cls.user_manager_client,
            additional_input_data={
                'domain_id': cls.domain_id,
                'user_name': sub_user_name})

        cls.role_ids = []
        cls.tenant_ids = []

    @attr(type='smoke_alpha')
    def test_effective_roles_for_default_user(self):
        # create global role
        global_role_id, global_role_name = self.create_role()
        # add global role to sub-user
        resp = self.identity_admin_client.add_role_to_user(
            role_id=global_role_id,
            user_id=self.user_client.default_headers[const.X_USER_ID])
        self.assertEqual(resp.status_code, 200)

        # create tenant
        tenant = self.create_tenant()
        # create tenant role
        tenant_role_id, tenant_role_name = self.create_role()
        # add tenant role to sub user for tenant
        resp = self.identity_admin_client.add_role_to_user_for_tenant(
            tenant_id=tenant.id,
            user_id=self.user_client.default_headers[const.X_USER_ID],
            role_id=tenant_role_id)
        self.assertEqual(resp.status_code, 200)

        # create user group role
        user_group_role_id, user_group_role_name = self.create_role(
            admin_role=const.USER_MANAGE_ROLE_NAME)
        # Create user group; add to user-default
        user_group = self.add_sub_user_to_user_group()
        # add role to user group
        self.user_manager_client.add_role_to_user_group_for_tenant(
            domain_id=self.domain_id,
            group_id=user_group.id,
            role_id=user_group_role_id,
            tenant_id=tenant.id)
        self.assertEqual(resp.status_code, 200)

        # get roles as user admin
        resp = self.user_admin_client.list_effective_roles_for_user(
            user_id=self.user_client.default_headers[const.X_USER_ID])

        # validate return 403
        self.assertEqual(resp.status_code, 403)

        # get roles as identity admin
        resp = self.identity_admin_client.list_effective_roles_for_user(
            user_id=self.user_client.default_headers[const.X_USER_ID])

        # validate return 200
        self.assertEqual(resp.status_code, 200)

        # tenant role, identity:default, global role, user group role
        # tenant access
        self.assertEqual(
            len(resp.json()[const.RAX_AUTH_ROLE_ASSIGNMENTS][
                    const.TENANT_ASSIGNMENTS]),
            5)

        tenant_role_assignment_checked = False
        user_group_role_assignment_checked = False
        identity_default_role_assignment_checked = False
        global_role_assignment_checked = False
        tenant_access_role_assignment_checked = False

        for tenant_assignment in resp.json()[
                const.RAX_AUTH_ROLE_ASSIGNMENTS][const.TENANT_ASSIGNMENTS]:
            if tenant_assignment["onRoleName"] == tenant_role_name:
                tenant_role_assignment_checked = True
                self.assertEqual(
                    tenant_assignment["sources"][0]["sourceType"],
                    const.USER_SOURCE_TYPE
                )
                self.assertEqual(
                    tenant_assignment["sources"][0]["assignmentType"],
                    const.TENANT_ASSIGNMENT_TYPE
                )
            elif tenant_assignment[
                    "onRoleName"] == const.USER_DEFAULT_ROLE_NAME:
                identity_default_role_assignment_checked = True
                self.assertEqual(
                    tenant_assignment["sources"][0]["sourceType"],
                    const.USER_SOURCE_TYPE
                )
                self.assertEqual(
                    tenant_assignment["sources"][0]["assignmentType"],
                    const.DOMAIN_ASSIGNMENT_TYPE
                )
            elif tenant_assignment["onRoleName"] == global_role_name:
                global_role_assignment_checked = True
                self.assertEqual(
                    tenant_assignment["sources"][0]["sourceType"],
                    const.USER_SOURCE_TYPE
                )
                self.assertEqual(
                    tenant_assignment["sources"][0]["assignmentType"],
                    const.DOMAIN_ASSIGNMENT_TYPE
                )
            elif tenant_assignment[
                    "onRoleName"] == const.TENANT_ACCESS_ROLE_NAME:
                tenant_access_role_assignment_checked = True
                self.assertEqual(
                    tenant_assignment["sources"][0]["sourceType"],
                    const.SYSTEM_SOURCE_TYPE
                )
                self.assertEqual(
                    tenant_assignment["sources"][0]["assignmentType"],
                    const.TENANT_ASSIGNMENT_TYPE
                )
            elif tenant_assignment["onRoleName"] == user_group_role_name:
                user_group_role_assignment_checked = True
                self.assertEqual(
                    tenant_assignment["sources"][0]["sourceType"],
                    const.USERGROUP_SOURCE_TYPE
                )
                self.assertEqual(
                    tenant_assignment["sources"][0]["assignmentType"],
                    const.TENANT_ASSIGNMENT_TYPE
                )

        self.assertEqual(tenant_role_assignment_checked, True)
        self.assertEqual(identity_default_role_assignment_checked, True)
        self.assertEqual(global_role_assignment_checked, True)
        self.assertEqual(tenant_access_role_assignment_checked, True)
        self.assertEqual(user_group_role_assignment_checked, True)

    def create_role(self, admin_role=None):
        role_name = self.generate_random_string(
            pattern=const.ROLE_NAME_PATTERN)
        if admin_role:
            role_object = factory.get_add_role_request_object(
                role_name=role_name,
                administrator_role=admin_role)
        else:
            role_object = factory.get_add_role_request_object(
                role_name=role_name)
        resp = self.identity_admin_client.add_role(request_object=role_object)
        self.assertEqual(resp.status_code, 201)
        role_id = resp.json()[const.ROLE][const.ID]
        self.role_ids.append(role_id)
        return role_id, role_name

    def create_tenant(self):
        tenant_req = factory.get_add_tenant_object(domain_id=self.domain_id)
        add_tenant_resp = self.identity_admin_client.add_tenant(
            tenant=tenant_req)
        self.assertEqual(add_tenant_resp.status_code, 201)
        tenant = responses.Tenant(add_tenant_resp.json())
        self.tenant_ids.append(tenant.id)
        return tenant

    def add_sub_user_to_user_group(self):
        group_req = factory.get_add_user_group_request(self.domain_id)
        resp = self.user_admin_client.add_user_group_to_domain(
            domain_id=self.domain_id, request_object=group_req)
        self.assertEqual(resp.status_code, 201)
        group = responses.UserGroup(resp.json())

        # adding user to the user group
        add_resp = self.user_admin_client.add_user_to_user_group(
            user_id=self.user_client.default_headers[
                const.X_USER_ID],
            group_id=group.id, domain_id=self.domain_id
        )
        self.assertEqual(add_resp.status_code, 204)
        return group

    @classmethod
    @base.base.log_tearDown_error
    def tearDownClass(cls):
        # Delete sub users created in the setUpClass
        resp = cls.identity_admin_client.delete_user(
            cls.user_client.default_headers[const.X_USER_ID])
        assert resp.status_code == 204, (
            'User with ID {0} failed to delete'.format(
                cls.user_client.default_headers[const.X_USER_ID]))
        resp = cls.identity_admin_client.delete_user(
            cls.user_manager_client.default_headers[const.X_USER_ID])
        assert resp.status_code == 204, (
            'User with ID {0} failed to delete'.format(
                cls.user_manager_client.default_headers[const.X_USER_ID]))
        # Delete client will delete user-admin, tenant & the domain
        cls.delete_client(client=cls.user_admin_client,
                          parent_client=cls.identity_admin_client)
        for role_id in cls.role_ids:
            resp = cls.identity_admin_client.delete_role(role_id=role_id)
            assert resp.status_code == 204, (
                'Role with ID {0} failed to delete'.format(role_id))
        super(TestListEffectiveRolesForUser, cls).tearDownClass()
