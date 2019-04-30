# -*- coding: utf-8 -*
from nose.plugins.attrib import attr
from qe_coverage.opencafe_decorators import tags, unless_coverage

from tests.api.v2.delegation import delegation
from tests.package.johny import constants as const
from tests.package.johny.v2.models import requests
from tests.api.v2.schema import delegation as da_schema


class TestListDelegationRoles(delegation.TestBaseDelegation):

    @classmethod
    @unless_coverage
    def setUpClass(cls):
        super(TestListDelegationRoles, cls).setUpClass()

        # Add a sub user in Domain 2
        sub_user_name = cls.generate_random_string(
            pattern=const.SUB_USER_PATTERN)
        additional_input_data = {
            'user_name': sub_user_name}
        cls.sub_user_client = cls.generate_client(
            parent_client=cls.user_admin_client_2,
            additional_input_data=additional_input_data)
        cls.sub_user_id = cls.sub_user_client.default_headers[const.X_USER_ID]
        cls.sub_user_token = cls.sub_user_client.default_headers[
            const.X_AUTH_TOKEN]

        # Create a Delegation Agreement for Domain 1, with sub user in Domain 2
        # as the delegate
        da_name = cls.generate_random_string(
            pattern=const.DELEGATION_AGREEMENT_NAME_PATTERN)
        da_req = requests.DelegationAgreements(da_name=da_name)
        da_resp = cls.user_admin_client.create_delegation_agreement(
            request_object=da_req)
        cls.da_id = da_resp.json()[
            const.RAX_AUTH_DELEGATION_AGREEMENT][const.ID]

        cls.user_admin_client.add_user_delegate_to_delegation_agreement(
            cls.da_id, cls.sub_user_id)

    @unless_coverage
    def setUp(self):
        super(TestListDelegationRoles, self).setUp()

    @tags('positive', 'p0', 'smoke')
    @attr(type='smoke_alpha')
    def test_list_delegation_roles(self):
        # create role
        role = self.create_role()

        # create tenant
        tenant_1 = self.create_tenant()

        # create tenant assignments request dicts
        tenant_assignment_req = self.generate_tenants_assignment_dict(
            role.id, tenant_1.id)
        tenants_role_assignment_req = requests.TenantRoleAssignments(
            tenant_assignment_req)

        # grant role to da (as principal)
        ua_client = self.user_admin_client
        ua_client.add_tenant_role_assignments_to_delegation_agreement(
            self.da_id, request_object=tenants_role_assignment_req)

        # get roles as a principal
        resp = ua_client.list_tenant_role_assignments_for_delegation_agreement(
            self.da_id)
        self.assert_response(resp, role, tenant_1)

        # get roles as delegate
        su_client = self.sub_user_client
        resp = su_client.list_tenant_role_assignments_for_delegation_agreement(
            self.da_id)
        self.assert_response(resp, role, tenant_1)

    def assert_response(self, resp, role, tenant):
        self.assertEqual(resp.status_code, 200)
        self.assertSchema(
            resp,
            json_schema=da_schema.tenants_role_assignments)
        tenant_roles = [tenant_role for tenant_role in resp.json()[
            const.RAX_AUTH_ROLE_ASSIGNMENTS][const.TENANT_ASSIGNMENTS]]
        self.assertEqual(len(tenant_roles), 1)
        self.assertEqual(role.name, tenant_roles[0][const.ON_ROLE_NAME])
        self.assertEqual(role.id, tenant_roles[0][const.ON_ROLE])
        self.assertIn(tenant.name, tenant_roles[0][const.FOR_TENANTS])

    @unless_coverage
    def tearDown(self):
        super(TestListDelegationRoles, self).tearDown()

    @classmethod
    @delegation.base.base.log_tearDown_error
    @unless_coverage
    def tearDownClass(cls):
        resp = cls.user_admin_client_2.delete_user(cls.sub_user_id)
        assert resp.status_code == 204, (
            'Subuser with ID {0} failed to delete'.format(cls.sub_user_id))
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
        super(TestListDelegationRoles, cls).tearDownClass()
