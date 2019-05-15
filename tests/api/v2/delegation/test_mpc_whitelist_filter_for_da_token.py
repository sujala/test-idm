# -*- coding: utf-8 -*
import pytest
from qe_coverage.opencafe_decorators import tags, unless_coverage

from tests.api.v2.delegation import delegation
from tests.package.johny import constants as const
from tests.package.johny.v2.models import requests


class TestMPCWhitelistFilterForDAToken(delegation.TestBaseDelegation):

    @classmethod
    @unless_coverage
    def setUpClass(cls):
        super(TestMPCWhitelistFilterForDAToken, cls).setUpClass()

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

        cls.hierarchical_billing_observer_role_id = cls.get_role_id_by_name(
            role_name=const.HIERARCHICAL_BILLING_OBSERVER_ROLE_NAME)

    @unless_coverage
    def setUp(self):
        super(TestMPCWhitelistFilterForDAToken, self).setUp()

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

    def set_up_roles_tenant_and_da(self, client):

        p_id = client.default_headers[const.X_USER_ID]
        # create DA with sub user
        da_id = self.create_delegation_agreement(
            client=client, user_id=self.sub_user_id, principal_id=p_id)
        # create roles
        role_1 = self.create_role()
        role_2 = self.create_role()

        # create tenant with type for which there is a whitelist filter
        tenant_name = ":".join([
            self.test_config.mpc_whitelist_tenant_type,
            self.generate_random_string(pattern=const.TENANT_NAME_PATTERN)])
        tenant_1 = self.create_tenant(
            name=tenant_name,
            tenant_types=[self.test_config.mpc_whitelist_tenant_type])
        return role_1, role_2, tenant_1, da_id

    @tags('positive', 'p0', 'regression')
    @pytest.mark.regression
    def test_whitelist_filter_for_mpc_tenant(self):
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
            role_1.id, "*")
        tenant_assignment_req_2 = self.generate_tenants_assignment_dict(
            role_2.id, tenant_1.id)
        tenants_role_assignment_req = requests.TenantRoleAssignments(
            tenant_assignment_req_1, tenant_assignment_req_2)

        assignment_resp = (
            um_client.add_tenant_role_assignments_to_delegation_agreement(
                da_id=da_id, request_object=tenants_role_assignment_req))
        self.assertEqual(assignment_resp.status_code, 200)

        delegation_auth_req = requests.AuthenticateWithDelegationAgreement(
            token=self.sub_user_client.default_headers[const.X_AUTH_TOKEN],
            delegation_agreement_id=da_id)
        resp = self.identity_admin_client.get_auth_token(delegation_auth_req)
        self.assertEqual(resp.status_code, 200)

        delegation_auth_roles = resp.json()[const.ACCESS][const.USER][
            const.ROLES]
        # Since it is the only tenant in the domain and DA auth always returns
        # de-normalized roles.
        self.assertEqual(delegation_auth_roles, [])

        # Now, assign a role from whitelist to DA and re-check DA auth
        tenant_assignment_req_3 = self.generate_tenants_assignment_dict(
            self.hierarchical_billing_observer_role_id, tenant_1.id)
        tenants_role_assignment_req = requests.TenantRoleAssignments(
            tenant_assignment_req_3)

        assignment_resp = (
            um_client.add_tenant_role_assignments_to_delegation_agreement(
                da_id=da_id, request_object=tenants_role_assignment_req))
        self.assertEqual(assignment_resp.status_code, 200)

        delegation_auth_req = requests.AuthenticateWithDelegationAgreement(
            token=self.sub_user_client.default_headers[const.X_AUTH_TOKEN],
            delegation_agreement_id=da_id)
        resp = self.identity_admin_client.get_auth_token(delegation_auth_req)
        self.assertEqual(resp.status_code, 200)
        delegation_auth_roles = resp.json()[const.ACCESS][const.USER][
            const.ROLES]
        # Three role assignments above + identity:default + tenant:access
        self.assertEqual(len(delegation_auth_roles), 5)

    @unless_coverage
    def tearDown(self):
        super(TestMPCWhitelistFilterForDAToken, self).tearDown()

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
        super(TestMPCWhitelistFilterForDAToken, cls).tearDownClass()
