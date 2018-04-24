# -*- coding: utf-8 -*
from nose.plugins.attrib import attr

from tests.api.utils import func_helper
from tests.api.v2 import base
from tests.api.v2.models import responses, factory
from tests.package.johny import constants as const
from tests.package.johny.v2.models import requests
from tests.api.v2.schema import delegation


class TestListDelegationRoles(base.TestBaseV2):

    @classmethod
    def setUpClass(cls):
        super(TestListDelegationRoles, cls).setUpClass()
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
            additional_input_data=additional_input_data,
            one_call=True)

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

        res = cls.user_admin_client.add_user_delegate_to_delegation_agreement(
            cls.da_id, cls.sub_user_id)

        cls.tenant_ids = []
        cls.role_ids = []

    def create_role(self):

        role_obj = factory.get_add_role_request_object(
            administrator_role=const.USER_MANAGE_ROLE_NAME)
        add_role_resp = self.identity_admin_client.add_role(
            request_object=role_obj)
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
            json_schema=delegation.tenants_role_assignments)
        tenant_roles = [tenant_role for tenant_role in resp.json()[
            const.RAX_AUTH_ROLE_ASSIGNMENTS][const.TENANT_ASSIGNMENTS]]
        self.assertEqual(len(tenant_roles), 1)
        self.assertEquals(role.name, tenant_roles[0][const.ON_ROLE_NAME])
        self.assertEquals(role.id, tenant_roles[0][const.ON_ROLE])
        self.assertIn(tenant.name, tenant_roles[0][const.FOR_TENANTS])

    @classmethod
    @base.base.log_tearDown_error
    def tearDownClass(cls):
        super(TestListDelegationRoles, cls).tearDownClass()

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

        disable_domain_req = requests.Domain(enabled=False)

        # Delete Domain 1
        cls.identity_admin_client.update_domain(
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

        for role_id in cls.role_ids:
            resp = cls.identity_admin_client.delete_role(role_id=role_id)
            assert resp.status_code == 204, (
                'Role with ID {0} failed to delete'.format(
                    role_id))
        for tenant_id in cls.tenant_ids:
            resp = cls.identity_admin_client.delete_tenant(
                tenant_id=tenant_id)
            # For some cases, tenant is getting deleted by delete_client()
            # call, prior. Hence checking for either 204 or 404.
            assert resp.status_code in [204, 404], (
                'Tenant with ID {0} failed to delete'.format(
                    tenant_id))
