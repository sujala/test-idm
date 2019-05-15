# -*- coding: utf-8 -*
from munch import Munch
import pytest
from qe_coverage.opencafe_decorators import tags, unless_coverage

from tests.api.utils import func_helper
from tests.api.v2.user_groups import usergroups
from tests.api.v2.models import factory, responses

from tests.package.johny import constants as const
from tests.package.johny.v2.models import requests


class TestDelegationRolesWithUserGroups(usergroups.TestUserGroups):

    """
    Tests for Delegation agreements roles for user group as principal
    """
    @classmethod
    @unless_coverage
    def setUpClass(cls):
        super(TestDelegationRolesWithUserGroups, cls).setUpClass()

    @unless_coverage
    def setUp(self):
        """
        Test level set up for the tests
        Create users needed for the tests and generate clients for those users.
        """
        super(TestDelegationRolesWithUserGroups, self).setUp()
        self.rcn = self.test_config.da_rcn
        self.domain_id_1 = self.create_domain_with_rcn()
        additional_input_data = {
            'domain_id': self.domain_id_1
        }
        self.user_admin_client = self.generate_client(
            parent_client=self.identity_admin_client,
            additional_input_data=additional_input_data)

        self.domain_id_2 = self.create_domain_with_rcn()
        input_data = {
            'domain_id': self.domain_id_2
        }
        req_object = factory.get_add_user_request_object(
            input_data=input_data)
        resp = self.identity_admin_client.add_user(req_object)
        self.user_admin_2 = responses.User(resp.json())
        self.group_ids = []

    def create_domain_with_rcn(self):

        domain_id = func_helper.generate_randomized_domain_id(
            client=self.identity_admin_client)
        dom_req = requests.Domain(
            domain_name=domain_id, domain_id=domain_id, rcn=self.rcn)
        add_dom_resp = self.identity_admin_client.add_domain(dom_req)
        self.assertEqual(add_dom_resp.status_code, 201, (
            'domain was not created successfully'))
        return domain_id

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

    @tags('positive', 'p0', 'smoke')
    @pytest.mark.smoke_alpha
    def test_da_roles_for_user_group_principal(self):

        # create user group for domain & add user admin to the user group
        group_one = self.create_and_add_user_group_to_domain(
            client=self.user_admin_client, domain_id=self.domain_id_1)
        self.group_ids.append((group_one.id, self.domain_id_1))
        resp = self.user_admin_client.add_user_to_user_group(
            domain_id=self.domain_id_1, group_id=group_one.id,
            user_id=self.user_admin_client.default_headers[const.X_USER_ID])
        self.assertEqual(resp.status_code, 204)

        # create DA with user group as principal
        da_name = self.generate_random_string(
            pattern=const.DELEGATION_AGREEMENT_NAME_PATTERN)
        da_req = requests.DelegationAgreements(
            da_name=da_name,
            principal_id=group_one.id,
            principal_type=const.USER_GROUP)
        da_resp = self.user_admin_client.create_delegation_agreement(
            request_object=da_req)
        self.assertEqual(da_resp.status_code, 201)
        da = Munch.fromDict(da_resp.json())
        da_id = da[const.RAX_AUTH_DELEGATION_AGREEMENT].id

        # create roles
        role_1 = self.create_role()
        role_2 = self.create_role()
        # create tenant
        tenant_1 = self.create_tenant(domain=self.domain_id_1)

        tenant_assignment_req_1 = self.generate_tenants_assignment_dict(
            role_1.id, tenant_1.id)
        tenant_assignment_req_2 = self.generate_tenants_assignment_dict(
            role_2.id, "*")
        tenants_role_assignment_req = requests.TenantRoleAssignments(
            tenant_assignment_req_1, tenant_assignment_req_2)

        # Verify that role must be added to user group member before adding it
        # to DA
        ua_client = self.user_admin_client
        assignment_resp = (
            ua_client.add_tenant_role_assignments_to_delegation_agreement(
                da_id=da_id, request_object=tenants_role_assignment_req))
        self.assertEqual(assignment_resp.status_code, 403)

        # Add role 2 to user group on tenant 1
        self.identity_admin_client.add_role_to_user_group_for_tenant(
            domain_id=self.domain_id_1, group_id=group_one.id,
            role_id=role_2.id, tenant_id=tenant_1.id)

        # Verify role 2 cannot be added globally to DA
        tenants_role_assignment_req = requests.TenantRoleAssignments(
            tenant_assignment_req_2)
        assignment_resp = (
            ua_client.add_tenant_role_assignments_to_delegation_agreement(
                da_id=da_id, request_object=tenants_role_assignment_req))
        self.assertEqual(assignment_resp.status_code, 403)
        self.assertEqual(
            assignment_resp.json()[const.FORBIDDEN][const.MESSAGE], (
                "Error code: 'GEN-005'; Invalid assignment for role '{}'."
                " Not authorized to assign this role with provided "
                "tenants.".format(role_2.id)))

        # Now, change tenant assignment for role 2
        tenant_assignment_req_2 = self.generate_tenants_assignment_dict(
            role_2.id, tenant_1.id)
        tenants_role_assignment_req = requests.TenantRoleAssignments(
            tenant_assignment_req_2)
        assignment_resp = (
            ua_client.add_tenant_role_assignments_to_delegation_agreement(
                da_id=da_id, request_object=tenants_role_assignment_req))
        self.assertEqual(assignment_resp.status_code, 200)

        # Adding the role 1 to the user & check if its still 403 so that it
        # can be assigned to DA
        self.identity_admin_client.add_role_to_user(
            role_id=role_1.id, user_id=ua_client.default_headers[
                const.X_USER_ID])
        tenants_role_assignment_req = requests.TenantRoleAssignments(
            tenant_assignment_req_1)
        assignment_resp = (
            ua_client.add_tenant_role_assignments_to_delegation_agreement(
                da_id=da_id, request_object=tenants_role_assignment_req))
        self.assertEqual(assignment_resp.status_code, 403)

        # Adding the role 1 to the user group so that it can be assigned to DA
        ua_client.add_tenant_role_assignments_to_user_group(
            domain_id=self.domain_id_1, group_id=group_one.id,
            request_object=tenants_role_assignment_req)
        assignment_resp = (
            ua_client.add_tenant_role_assignments_to_delegation_agreement(
                da_id=da_id, request_object=tenants_role_assignment_req))
        self.assertEqual(assignment_resp.status_code, 200)

        list_resp_parsed = self.call_list_da_roles_and_parse_the_response(
            client=ua_client, da_id=da_id)
        tas = list_resp_parsed[const.RAX_AUTH_ROLE_ASSIGNMENTS][
            const.TENANT_ASSIGNMENTS]
        self.validate_list_da_roles_response(
            tenant_assignments=tas, role_1=role_1, role_2=role_2,
            tenant_1=tenant_1, for_default_user=True)

    @unless_coverage
    @usergroups.base.base.log_tearDown_error
    def tearDown(self):
        for group_id, domain_id in self.group_ids:
            resp = self.identity_admin_client.delete_user_group_from_domain(
                group_id=group_id, domain_id=domain_id)
            self.assertEqual(
                resp.status_code, 204,
                msg='User group with ID {0} failed to delete'.format(
                    group_id))
        self.delete_client(self.user_admin_client)
        # domain 2
        resp = self.identity_admin_client.delete_user(self.user_admin_2.id)
        self.assertEqual(resp.status_code, 204,
                         msg='User with ID {0} failed to delete'.format(
                             self.user_admin_2.id))
        disable_domain_req = requests.Domain(enabled=False)
        self.identity_admin_client.update_domain(
            domain_id=self.domain_id_2, request_object=disable_domain_req)
        resp = self.identity_admin_client.delete_domain(self.domain_id_2)
        self.assertEqual(resp.status_code, 204,
                         msg='Domain with ID {0} failed to delete'.format(
                             self.domain_id_2))
        super(TestDelegationRolesWithUserGroups, self).tearDown()

    @classmethod
    @unless_coverage
    def tearDownClass(cls):
        super(TestDelegationRolesWithUserGroups, cls).tearDownClass()
