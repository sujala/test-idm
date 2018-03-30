# -*- coding: utf-8 -*
from tests.api.v2 import base
from tests.api.utils import func_helper
from tests.api.v2.models import factory, responses

from tests.package.johny import constants as const
from tests.package.johny.v2.models import requests


class TestListEffectiveRolesWithDA(base.TestBaseV2):

    """ List effective role for user."""

    @classmethod
    def setUpClass(cls):
        super(TestListEffectiveRolesWithDA, cls).setUpClass()
        cls.rcn = cls.test_config.da_rcn

        cls.role_ids = []
        cls.tenant_ids = []
        cls.user_ids = []
        cls.domain_ids = []

        # Add Domain 1
        cls.domain_id = func_helper.generate_randomized_domain_id(
            client=cls.identity_admin_client)
        dom_req = requests.Domain(
            domain_name=cls.domain_id, domain_id=cls.domain_id, rcn=cls.rcn)
        add_dom_resp = cls.identity_admin_client.add_domain(dom_req)
        assert add_dom_resp.status_code == 201, (
            'domain was not created successfully')
        cls.domain_ids.append(cls.domain_id)

        additional_input_data = {'domain_id': cls.domain_id}
        cls.user_admin_client = cls.generate_client(
            parent_client=cls.identity_admin_client,
            additional_input_data=additional_input_data)
        cls.user_ids.append(
            cls.user_admin_client.default_headers[const.X_USER_ID])

        # Add Domain 2
        cls.domain2_id = func_helper.generate_randomized_domain_id(
            client=cls.identity_admin_client)
        dom_req = requests.Domain(
            domain_name=cls.domain2_id,
            domain_id=cls.domain2_id,
            rcn=cls.rcn)
        add_dom_resp = cls.identity_admin_client.add_domain(dom_req)
        assert add_dom_resp.status_code == 201, (
            'domain was not created successfully')
        cls.domain_ids.append(cls.domain2_id)

        # Create User Admin 2 in Domain 2
        additional_input_data = {'domain_id': cls.domain2_id}
        cls.user_admin2_client = cls.generate_client(
            parent_client=cls.identity_admin_client,
            additional_input_data=additional_input_data)
        cls.user_admin2_id = cls.user_admin2_client.default_headers[
            const.X_USER_ID]
        user_admin2_token = cls.user_admin2_client.default_headers[
            const.X_AUTH_TOKEN]
        cls.user_ids.append(cls.user_admin2_id)

        # Create a Delegation Agreement for Domain 1, with user-admin in
        # Domain 2 as the delegate
        da_name = cls.generate_random_string(
            pattern=const.DELEGATION_AGREEMENT_NAME_PATTERN)
        da_req = requests.DelegationAgreements(
            da_name=da_name, delegate_id=cls.user_admin2_id)
        da_resp = cls.user_admin_client.create_delegation_agreement(
            request_object=da_req)
        da_id = da_resp.json()[const.RAX_AUTH_DELEGATION_AGREEMENT][const.ID]

        delegation_auth_req = requests.AuthenticateWithDelegationAgreement(
            token=user_admin2_token, delegation_agreement_id=da_id)
        resp = cls.identity_admin_client.get_auth_token(delegation_auth_req)
        delegation_token = resp.json()[const.ACCESS][const.TOKEN][const.ID]
        cls.delegation_client = cls.user_admin2_client
        cls.delegation_client.default_headers[const.X_AUTH_TOKEN] = (
            delegation_token)

    def test_list_effective_roles_with_da_token(self):
        # create and add role to user
        self.create_role_and_add_to_user(
            self.user_admin_client.default_headers[const.X_USER_ID]
        )

        # create tenant and add role on tenant for user
        self.create_tenant_and_add_role_on_tenant_for_user(
            self.user_admin_client.default_headers[const.X_USER_ID])

        resp = self.delegation_client.list_effective_roles_for_user(
            user_id=self.delegation_client.default_headers[const.X_USER_ID])

        # validate return 200
        self.assertEqual(resp.status_code, 200)

        # @todo - Add validations for roles returned, after CID-1396 is done.
        role_assignments = resp.json()[const.RAX_AUTH_ROLE_ASSIGNMENTS]
        self.assertNotEqual(role_assignments[const.TENANT_ASSIGNMENTS], [])

    def create_role_and_add_to_user(self, user_id):
        # create global role
        role_id, role_name = self.create_role()
        # add global role to manage
        resp = self.identity_admin_client.add_role_to_user(
            role_id=role_id,
            user_id=user_id)
        self.assertEqual(resp.status_code, 200)

        return role_name

    def create_tenant_and_add_role_on_tenant_for_user(self, user_id):
        # create tenant
        tenant = self.create_tenant()
        # create tenant role
        tenant_role_id, tenant_role_name = self.create_role()
        # add tenant role to sub user for tenant
        resp = self.identity_admin_client.add_role_to_user_for_tenant(
            tenant_id=tenant.id,
            user_id=user_id,
            role_id=tenant_role_id)
        self.assertEqual(resp.status_code, 200)

        return tenant, tenant_role_name

    def create_role(self, admin_role=None, role_name=None):
        if role_name is None:
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
        self.role_ids.append((role_id, role_name))
        return role_id, role_name

    def create_tenant(self):
        tenant_req = factory.get_add_tenant_object(domain_id=self.domain_id)
        add_tenant_resp = self.identity_admin_client.add_tenant(
            tenant=tenant_req)
        self.assertEqual(add_tenant_resp.status_code, 201)
        tenant = responses.Tenant(add_tenant_resp.json())
        self.tenant_ids.append(tenant.id)
        return tenant

    @base.base.log_tearDown_error
    def tearDown(self):
        # Delete sub users created in the setUpClass
        for id_ in self.user_ids:
            resp = self.identity_admin_client.delete_user(id_)
            assert resp.status_code == 204, (
                'User with ID {0} failed to delete'.format(id_))

        for id_ in self.role_ids:
            resp = self.identity_admin_client.delete_role(id_)
            assert resp.status_code == 204, (
                'Role with ID {0} failed to delete'.format(id_))

        for id_ in self.tenant_ids:
            resp = self.identity_admin_client.delete_tenant(id_)
            assert resp.status_code == 204, (
                'Tenant with ID {0} failed to delete'.format(id_))

        disable_domain_req = requests.Domain(enabled=False)
        for id_ in self.domain_ids:
            self.identity_admin_client.update_domain(
                domain_id=id_, request_object=disable_domain_req)
            resp = self.identity_admin_client.delete_domain(id_)
            assert resp.status_code == 204, (
                'Domain with ID {0} failed to delete'.format(id_))
        super(TestListEffectiveRolesWithDA, self).tearDown()
