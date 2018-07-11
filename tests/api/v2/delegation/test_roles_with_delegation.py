# -*- coding: utf-8 -*
from munch import Munch

from nose.plugins.attrib import attr
from qe_coverage.opencafe_decorators import tags, unless_coverage

from tests.api.v2.delegation import delegation
from tests.package.johny import constants as const
from tests.package.johny.v2.models import requests


class TestRoleAssignmentsWithDelegation(delegation.TestBaseDelegation):

    @classmethod
    @unless_coverage
    def setUpClass(cls):
        super(TestRoleAssignmentsWithDelegation, cls).setUpClass()

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

        cls.hierarchical_observer_role_id = cls.get_role_id_by_name(
            role_name=const.HIERARCHICAL_OBSERVER_ROLE_NAME)

        cls.hierarchical_billing_observer_role_id = cls.get_role_id_by_name(
            role_name=const.HIERARCHICAL_BILLING_OBSERVER_ROLE_NAME)

        cls.hierarchical_ticket_observer_role_id = cls.get_role_id_by_name(
            role_name=const.HIERARCHICAL_TICKET_OBSERVER_ROLE_NAME)

    @unless_coverage
    def setUp(self):
        super(TestRoleAssignmentsWithDelegation, self).setUp()

    @classmethod
    def get_role_id_by_name(cls, role_name):

        option = {
            const.PARAM_ROLE_NAME: role_name
        }
        get_role_resp = cls.user_admin_client.list_roles(option=option)
        role_id = get_role_resp.json()[const.ROLES][0][const.ID]
        return role_id

    def create_delegation_agreement(self, client, user_id, principal_id):

        da_name = self.generate_random_string(
            pattern=const.DELEGATION_AGREEMENT_NAME_PATTERN)
        da_req = requests.\
            DelegationAgreements(da_name=da_name,
                                 principal_type=const.USER.upper(),
                                 principal_id=principal_id)
        da_resp = self.user_admin_client.create_delegation_agreement(
            request_object=da_req)
        da_id = da_resp.json()[
            const.RAX_AUTH_DELEGATION_AGREEMENT][const.ID]
        client.add_user_delegate_to_delegation_agreement(
            da_id, user_id)
        return da_id

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

        p_id = client.default_headers[const.X_USER_ID]
        # create DA with sub user
        da_id = self.create_delegation_agreement(
            client=client, user_id=self.sub_user_id, principal_id=p_id)
        # create roles
        role_1 = self.create_role()
        role_2 = self.create_role()
        # create tenant
        tenant_1 = self.create_tenant()
        return role_1, role_2, tenant_1, da_id

    @tags('positive', 'p0', 'regression')
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
        self.call_validate_list_da_roles_response(
            client=um_client, da_id=da_id, role_1=role_1, role_2=role_2,
            tenant=tenant_1)

        # verifying other user manager can list DA roles
        user_manager_client_2 = self.generate_client(
            parent_client=self.user_admin_client,
            additional_input_data={'is_user_manager': True})
        um_client_2 = user_manager_client_2
        self.users.append(um_client_2.default_headers[const.X_USER_ID])

        self.call_validate_list_da_roles_response(
            client=um_client_2, da_id=da_id, role_1=role_1, role_2=role_2,
            tenant=tenant_1)

        # CID-1502
        self.validate_role_deletion_is_forbidden(role=role_1)
        self.validate_role_deletion_is_forbidden(role=role_2)

        # Checking if delete role on DA removes the global role on DA, by
        # list call
        self.validate_delete_role_from_DA(
            client=um_client, da_id=da_id, role=role_2)

        self.validate_delegation_auth_after_role_deletion(
            da_id=da_id, role=role_2)

    def validate_role_deletion_is_forbidden(self, role):

        delete_resp = self.identity_admin_client.delete_role(role_id=role.id)
        self.assertEqual(delete_resp.status_code, 403)
        self.assertEqual(
            delete_resp.json()[const.FORBIDDEN][const.MESSAGE], (
                'Deleting the role associated with one or more users, user'
                ' groups or delegation agreements is not allowed'))

    def call_validate_list_da_roles_response(
            self, client, da_id, role_1, role_2, tenant):

        list_resp_parsed = self.call_list_da_roles_and_parse_the_response(
            client=client, da_id=da_id)
        tas = list_resp_parsed[const.RAX_AUTH_ROLE_ASSIGNMENTS][
            const.TENANT_ASSIGNMENTS]
        self.validate_list_da_roles_response(
            tenant_assignments=tas, role_1=role_1, role_2=role_2,
            tenant_1=tenant)

    @tags('positive', 'p0', 'regression')
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

    @tags('positive', 'p0', 'regression')
    @attr(type='regression')
    def test_roles_on_nested_DA(self):

        # Create parent DA
        da_name = self.generate_random_string(
            pattern=const.DELEGATION_AGREEMENT_NAME_PATTERN)
        da_resp, da_id = self.call_create_delegation_agreement(
            client=self.user_admin_client,
            delegate_id=self.user_admin2_id,
            da_name=da_name,
            sub_agreement_nest_level=1)

        # Create role assignment dict
        role = self.create_role()
        tenant_1 = self.create_tenant()
        tenant_assignment_req = self.generate_tenants_assignment_dict(
            role.id, tenant_1.id)
        tenants_role_assignment_req = requests.TenantRoleAssignments(
            tenant_assignment_req)

        # Create nested DA
        nested_da_name = self.generate_random_string(
            pattern=const.DELEGATION_AGREEMENT_NAME_PATTERN)
        da_req = requests.DelegationAgreements(
            da_name=nested_da_name,
            parent_da_id=da_id)
        resp = self.user_admin_client_2.create_delegation_agreement(
            request_object=da_req)
        nested_da_id = resp.json()[
            const.RAX_AUTH_DELEGATION_AGREEMENT][const.ID]

        # Grant role to nested DA
        ua_client2 = self.user_admin_client_2
        resp = ua_client2.add_tenant_role_assignments_to_delegation_agreement(
            nested_da_id, request_object=tenants_role_assignment_req)
        self.assertEqual(resp.status_code, 403)

        # Grant role to parent DA
        ua_client = self.user_admin_client
        resp = ua_client.add_tenant_role_assignments_to_delegation_agreement(
            da_id, request_object=tenants_role_assignment_req)
        self.assertEqual(resp.status_code, 200)

        # Grant role to nested DA
        ua_client2 = self.user_admin_client_2
        resp = ua_client2.add_tenant_role_assignments_to_delegation_agreement(
            nested_da_id, request_object=tenants_role_assignment_req)
        self.assertEqual(resp.status_code, 200)

        # Grant domain level role to nested DA
        tenant_assignment_req = self.generate_tenants_assignment_dict(
            role.id, '*')
        tenants_role_assignment_req = requests.TenantRoleAssignments(
            tenant_assignment_req)

        ua_client2 = self.user_admin_client_2
        resp = ua_client2.add_tenant_role_assignments_to_delegation_agreement(
            nested_da_id, request_object=tenants_role_assignment_req)
        self.assertEqual(resp.status_code, 403)

    @tags('positive', 'p0', 'regression')
    @attr(type='regression')
    def test_hierarchical_role_on_nested_DA(self):

        # Create parent DA
        da_name = self.generate_random_string(
            pattern=const.DELEGATION_AGREEMENT_NAME_PATTERN)
        da_resp, parent_da_id = self.call_create_delegation_agreement(
            client=self.user_admin_client, delegate_id=self.user_admin2_id,
            da_name=da_name, sub_agreement_nest_level=1)

        # Create role, tenants and role assignment dicts
        role = self.create_role()
        tenant_1 = self.create_tenant()
        tenant_2 = self.create_tenant()
        tenant_observer_assignment_req = self.generate_tenants_assignment_dict(
            self.hierarchical_observer_role_id, tenant_2.id, tenant_1.id)
        tenant_billing_assignment_req = self.generate_tenants_assignment_dict(
            self.hierarchical_billing_observer_role_id, tenant_2.id)
        tenant_ticketing_assignment_req = (
            self.generate_tenants_assignment_dict(
                self.hierarchical_ticket_observer_role_id, tenant_1.id))
        tenant_other_role_assignment_req = (
            self.generate_tenants_assignment_dict(
                role.id, tenant_1.id))

        # Create nested DA
        nested_da_name = self.generate_random_string(
            pattern=const.DELEGATION_AGREEMENT_NAME_PATTERN)
        da_req = requests.DelegationAgreements(
            da_name=nested_da_name, parent_da_id=parent_da_id)
        resp = self.user_admin_client_2.create_delegation_agreement(
            request_object=da_req)
        nested_da_id = resp.json()[
            const.RAX_AUTH_DELEGATION_AGREEMENT][const.ID]

        # Grant hierarchical roles and a regular role to nested DA
        ua_client2 = self.user_admin_client_2
        tenants_child_role_assignments_req = requests.TenantRoleAssignments(
            tenant_billing_assignment_req, tenant_ticketing_assignment_req,
            tenant_other_role_assignment_req)
        resp = ua_client2.add_tenant_role_assignments_to_delegation_agreement(
            nested_da_id, request_object=tenants_child_role_assignments_req)
        self.assertEqual(resp.status_code, 403)

        # Grant corresponding parent role to parent DA i.e. 'observer' and
        # the other regular role
        tenants_parent_role_assignment_req = requests.TenantRoleAssignments(
            tenant_observer_assignment_req, tenant_other_role_assignment_req)
        ua_client = self.user_admin_client
        resp = ua_client.add_tenant_role_assignments_to_delegation_agreement(
            parent_da_id, request_object=tenants_parent_role_assignment_req)
        self.assertEqual(resp.status_code, 200)

        # Grant hierarchical roles and the regular role to nested DA
        resp = ua_client2.add_tenant_role_assignments_to_delegation_agreement(
            nested_da_id, request_object=tenants_child_role_assignments_req)
        self.assertEqual(resp.status_code, 200)

        billing_observer_role_present = False
        ticketing_observer_role_present = False
        for ta in resp.json()[const.RAX_AUTH_ROLE_ASSIGNMENTS][
              const.TENANT_ASSIGNMENTS]:
            if ta[const.ON_ROLE] == self.hierarchical_billing_observer_role_id:
                self.assertEqual(ta[const.FOR_TENANTS], [tenant_2.id])
                billing_observer_role_present = True
            if ta[const.ON_ROLE] == (
                    self.hierarchical_ticket_observer_role_id):
                self.assertEqual(ta[const.FOR_TENANTS], [tenant_1.id])
                ticketing_observer_role_present = True
        self.assertTrue(billing_observer_role_present)
        self.assertTrue(ticketing_observer_role_present)

    @unless_coverage
    def tearDown(self):
        super(TestRoleAssignmentsWithDelegation, self).tearDown()

    @classmethod
    @delegation.base.base.log_tearDown_error
    @unless_coverage
    def tearDownClass(cls):
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
        super(TestRoleAssignmentsWithDelegation, cls).tearDownClass()
