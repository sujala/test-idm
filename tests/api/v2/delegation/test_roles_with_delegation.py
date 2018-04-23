# -*- coding: utf-8 -*
from munch import Munch
from nose.plugins.attrib import attr

from tests.api.utils import func_helper
from tests.api.v2 import base
from tests.api.v2.models import factory, responses

from tests.package.johny import constants as const
from tests.package.johny.v2.models import requests


class TestRoleAssignmentsWithDelegation(base.TestBaseV2):

    @classmethod
    def setUpClass(cls):
        super(TestRoleAssignmentsWithDelegation, cls).setUpClass()
        cls.rcn = cls.test_config.da_rcn

        # Add Domain 1
        cls.domain_id = func_helper.generate_randomized_domain_id(
            client=cls.identity_admin_client)
        dom_req = requests.Domain(
            domain_name=cls.domain_id, domain_id=cls.domain_id, rcn=cls.rcn)
        add_dom_resp = cls.identity_admin_client.add_domain(dom_req)
        assert add_dom_resp.status_code == 201, (
            'domain was not created successfully')

        additional_input_data = {'domain_id': cls.domain_id}
        cls.user_admin_client = cls.generate_client(
            parent_client=cls.identity_admin_client,
            additional_input_data=additional_input_data)

        # Add Domain 2
        cls.domain_id_2 = func_helper.generate_randomized_domain_id(
            client=cls.identity_admin_client)
        dom_req = requests.Domain(
            domain_name=cls.domain_id_2,
            domain_id=cls.domain_id_2,
            rcn=cls.rcn)
        add_dom_resp = cls.identity_admin_client.add_domain(dom_req)
        assert add_dom_resp.status_code == 201, (
            'domain was not created successfully')

        # Create User Admin 2 in Domain 2
        additional_input_data = {'domain_id': cls.domain_id_2}
        cls.user_admin_client_2 = cls.generate_client(
            parent_client=cls.identity_admin_client,
            additional_input_data=additional_input_data)

        # Add a sub user in Domain 2
        sub_user_name = cls.generate_random_string(
            pattern=const.SUB_USER_PATTERN)
        additional_input_data = {
            'user_name': sub_user_name}
        cls.sub_user_client = cls.generate_client(
            parent_client=cls.user_admin_client_2,
            additional_input_data=additional_input_data)
        cls.sub_user_id = cls.sub_user_client.default_headers[const.X_USER_ID]
        cls.users = []
        cls.role_ids = []
        cls.tenant_ids = []

    def setUp(self):

        super(TestRoleAssignmentsWithDelegation, self).setUp()

    def create_delegation_agreement(self, client, user_id):

        da_name = self.generate_random_string(
            pattern=const.DELEGATION_AGREEMENT_NAME_PATTERN)
        da_req = requests.DelegationAgreements(
            da_name=da_name, delegate_id=user_id)
        da_resp = client.create_delegation_agreement(
            request_object=da_req)
        return da_resp.json()[
            const.RAX_AUTH_DELEGATION_AGREEMENT][const.ID]

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

    def create_tenant(self, domain=None):
        if not domain:
            domain = self.domain_id
        tenant_req = factory.get_add_tenant_object(domain_id=domain)
        add_tenant_resp = self.identity_admin_client.add_tenant(
            tenant=tenant_req)
        self.assertEqual(add_tenant_resp.status_code, 201)
        tenant = responses.Tenant(add_tenant_resp.json())
        self.tenant_ids.append(tenant.id)
        return tenant

    def call_list_da_roles_and_parse_the_response(self, client, da_id):
        list_resp = (
            client.list_tenant_role_assignments_for_delegation_agreement(
                da_id=da_id))
        self.assertEqual(list_resp.status_code, 200)
        list_resp_parsed = Munch.fromDict(list_resp.json())
        return list_resp_parsed

    def validate_list_da_roles_response(
            self, tenant_assignments, role_1, role_2, tenant_1,
            for_default_user=False):
        role_1_present = False
        role_2_present = False
        for ta in tenant_assignments:
            if ta[const.ON_ROLE] == role_1.id:
                role_1_present = True
                self.assertEqual(ta[const.FOR_TENANTS], [tenant_1.id])
            if ta[const.ON_ROLE] == role_2.id:
                role_2_present = True
                if for_default_user:
                    self.assertEqual(ta[const.FOR_TENANTS], [tenant_1.id])
                else:
                    self.assertEqual(ta[const.FOR_TENANTS], ["*"])
        self.assertTrue(role_1_present)
        self.assertTrue(role_2_present)

    def set_up_roles_tenant_and_da(self, client):

        # create DA with sub user
        da_id = self.create_delegation_agreement(
            client=client, user_id=self.sub_user_id)
        # create roles
        role_1 = self.create_role()
        role_2 = self.create_role()
        # create tenant
        tenant_1 = self.create_tenant()
        return role_1, role_2, tenant_1, da_id

    @attr(type='regression')
    def test_grant_and_delete_roles_to_da_when_user_manager_principal(self):
        """
        Tests for when user manager is a principal for a DA.
        """

        user_manager_client = self.generate_client(
            parent_client=self.user_admin_client,
            additional_input_data={'is_user_manager': True})
        um_client = user_manager_client
        self.users.append(um_client.default_headers[const.X_USER_ID])

        role_1, role_2, tenant_1, da_id = self.set_up_roles_tenant_and_da(
            client=um_client)

        # create tenant assignments request dicts
        tenant_assignment_req_1 = self.generate_tenants_assignment_dict(
            role_1.id, tenant_1.id)
        tenant_assignment_req_2 = self.generate_tenants_assignment_dict(
            role_2.id, "*")
        tenants_role_assignment_req = requests.TenantRoleAssignments(
            tenant_assignment_req_1, tenant_assignment_req_2)

        assignment_resp = (
            um_client.add_tenant_role_assignments_to_delegation_agreement(
                da_id=da_id, request_object=tenants_role_assignment_req))
        self.assertEqual(assignment_resp.status_code, 200)

        # list tenant assignments for da
        list_resp_parsed = self.call_list_da_roles_and_parse_the_response(
            client=um_client, da_id=da_id)
        tas = list_resp_parsed[const.RAX_AUTH_ROLE_ASSIGNMENTS][
            const.TENANT_ASSIGNMENTS]
        self.validate_list_da_roles_response(
            tenant_assignments=tas, role_1=role_1, role_2=role_2,
            tenant_1=tenant_1)

        # Checking if delete role on DA removes the global role on DA, by
        # list call
        self.validate_delete_role_from_DA(
            client=um_client, da_id=da_id, role=role_2)

        self.validate_delegation_auth_after_role_deletion(
            da_id=da_id, role=role_2)

    @attr(type='regression')
    def test_grant_roles_to_da_when_default_user_principal(self):
        """
        Tests for when default user is a principal for a DA. Various
        cases are tested such as when the role is not added to the default
        user but attempted to be added to DA, role added to user on tenant
        but attempted to be added to DA globally.
        """
        default_user_name = self.generate_random_string(
            pattern=const.SUB_USER_PATTERN)
        default_user_client = self.generate_client(
            parent_client=self.user_admin_client,
            additional_input_data={'username': default_user_name})
        du_client = default_user_client
        self.users.append(du_client.default_headers[const.X_USER_ID])

        role_1, role_2, tenant_1, da_id = self.set_up_roles_tenant_and_da(
            client=du_client)

        # create tenant assignments request dicts
        tenant_assignment_req_1 = self.generate_tenants_assignment_dict(
            role_1.id, tenant_1.id)
        tenants_role_assignment_req = requests.TenantRoleAssignments(
            tenant_assignment_req_1)

        # Verify that role must be added to default user before adding it to DA
        assignment_resp = (
            du_client.add_tenant_role_assignments_to_delegation_agreement(
                da_id=da_id, request_object=tenants_role_assignment_req))
        self.assertEqual(assignment_resp.status_code, 403)

        # Adding the role to the default user principal
        self.identity_admin_client.add_role_to_user(
            role_id=role_1.id, user_id=du_client.default_headers[
                const.X_USER_ID])
        assignment_resp = (
            du_client.add_tenant_role_assignments_to_delegation_agreement(
                da_id=da_id, request_object=tenants_role_assignment_req))
        self.assertEqual(assignment_resp.status_code, 200)
        self.call_list_da_roles_and_parse_the_response(
            client=du_client, da_id=da_id)

        # Verify 403 when one of the role is not on default user
        tenant_assignment_req_2 = self.generate_tenants_assignment_dict(
            role_2.id, "*")
        tenants_role_assignment_req = requests.TenantRoleAssignments(
            tenant_assignment_req_1, tenant_assignment_req_2)
        assignment_resp = (
            du_client.add_tenant_role_assignments_to_delegation_agreement(
                da_id=da_id, request_object=tenants_role_assignment_req))
        self.assertEqual(assignment_resp.status_code, 403)

        # Add role 2 to user on tenant 1
        self.identity_admin_client.add_role_to_user_for_tenant(
            tenant_id=tenant_1.id, role_id=role_2.id,
            user_id=du_client.default_headers[const.X_USER_ID])

        # Verify role 2 cannot be added globally to DA
        assignment_resp = (
            du_client.add_tenant_role_assignments_to_delegation_agreement(
                da_id=da_id, request_object=tenants_role_assignment_req))
        self.assertEqual(assignment_resp.status_code, 403)

        # Now, change tenant assignment for role 2
        tenant_assignment_req_2 = self.generate_tenants_assignment_dict(
            role_2.id, tenant_1.id)
        tenants_role_assignment_req = requests.TenantRoleAssignments(
            tenant_assignment_req_2)
        assignment_resp = (
            du_client.add_tenant_role_assignments_to_delegation_agreement(
                da_id=da_id, request_object=tenants_role_assignment_req))
        self.assertEqual(assignment_resp.status_code, 200)

        list_resp_parsed = self.call_list_da_roles_and_parse_the_response(
            client=du_client, da_id=da_id)
        tas = list_resp_parsed[const.RAX_AUTH_ROLE_ASSIGNMENTS][
            const.TENANT_ASSIGNMENTS]
        self.validate_list_da_roles_response(
            tenant_assignments=tas, role_1=role_1, role_2=role_2,
            tenant_1=tenant_1, for_default_user=True)

        # Checking if delete role on DA removes the tenant role on DA, by
        # list call
        self.validate_delete_role_from_DA(
            client=du_client, da_id=da_id, role=role_1)

        self.validate_delegation_auth_after_role_deletion(
            da_id=da_id, role=role_1)

    def validate_delegation_auth_after_role_deletion(self, da_id, role):

        delegation_auth_req = requests.AuthenticateWithDelegationAgreement(
            token=self.sub_user_client.default_headers[const.X_AUTH_TOKEN],
            delegation_agreement_id=da_id)
        resp = self.identity_admin_client.get_auth_token(delegation_auth_req)
        self.assertEqual(resp.status_code, 200)
        resp_parsed = Munch.fromDict(resp.json())
        role_ids = map(lambda role_: role_.id, resp_parsed.access.user.roles)
        self.assertNotIn(role.id, role_ids)

    def validate_delete_role_from_DA(self, client, da_id, role):

        delete_resp = client.delete_role_on_delegation_agreement(
            da_id=da_id, role_id=role.id)
        self.assertEqual(delete_resp.status_code, 204)
        list_resp_parsed = self.call_list_da_roles_and_parse_the_response(
            client=client, da_id=da_id)
        tas = list_resp_parsed[const.RAX_AUTH_ROLE_ASSIGNMENTS][
            const.TENANT_ASSIGNMENTS]
        role_ids = [role_['onRole'] for role_ in tas]
        self.assertNotIn(role.id, role_ids)
        delete_resp = client.delete_role_on_delegation_agreement(
            da_id=da_id, role_id=role.id
        )
        self.assertEqual(delete_resp.status_code, 404)

    def tearDown(self):
        super(TestRoleAssignmentsWithDelegation, self).tearDown()

    @classmethod
    @base.base.log_tearDown_error
    def tearDownClass(cls):
        super(TestRoleAssignmentsWithDelegation, cls).tearDownClass()

        resp = cls.user_admin_client_2.delete_user(cls.sub_user_id)
        assert resp.status_code == 204, (
            'Subuser with ID {0} failed to delete'.format(cls.sub_user_id))
        for user in cls.users:
            resp = cls.user_admin_client.delete_user(user)
            assert resp.status_code == 204, (
                'Subuser with ID {0} failed to delete'.format(user))
        resp = cls.identity_admin_client.delete_user(
            user_id=cls.user_admin_client_2.default_headers[const.X_USER_ID])
        assert resp.status_code == 204, (
            'User with ID {0} failed to delete'.format(
                cls.user_admin_client_2.default_headers[const.X_USER_ID]))

        resp = cls.identity_admin_client.delete_user(
            user_id=cls.user_admin_client.default_headers[const.X_USER_ID])
        assert resp.status_code == 204, (
            'User with ID {0} failed to delete'.format(
                cls.user_admin_client.default_headers[const.X_USER_ID]))

        for role_id in cls.role_ids:
            resp = cls.identity_admin_client.delete_role(role_id=role_id)
            assert resp.status_code == 204, (
                'Role with ID {0} failed to delete'.format(role_id))
        for tenant_id in cls.tenant_ids:
            resp = cls.identity_admin_client.delete_tenant(
                tenant_id=tenant_id)
            assert resp.status_code == 204, (
                'Tenant with ID {0} failed to delete'.format(tenant_id))

        # Delete Domain 1
        disable_domain_req = requests.Domain(enabled=False)
        resp = cls.identity_admin_client.update_domain(
            domain_id=cls.domain_id, request_object=disable_domain_req)
        resp = cls.identity_admin_client.delete_domain(
            domain_id=cls.domain_id)
        assert resp.status_code == 204, (
            'Domain with ID {0} failed to delete'.format(cls.domain_id))

        # Delete Domain 2
        resp = cls.identity_admin_client.update_domain(
            domain_id=cls.domain_id_2, request_object=disable_domain_req)
        resp = cls.identity_admin_client.delete_domain(
            domain_id=cls.domain_id_2)
        assert resp.status_code == 204, (
            'Domain with ID {0} failed to delete'.format(cls.domain_id_2))
