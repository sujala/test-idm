# -*- coding: utf-8 -*
from qe_coverage.opencafe_decorators import tags, unless_coverage
import urlparse

from tests.api.v2.delegation import delegation
from tests.api.v2.models import factory
from tests.package.johny import constants as const
from tests.package.johny.v2.models import requests


class TestMPCWhiteListFilterListEndpointsForDelegationToken(
        delegation.TestBaseDelegation):

    @classmethod
    @unless_coverage
    def setUpClass(cls):
        super(
            TestMPCWhiteListFilterListEndpointsForDelegationToken,
            cls).setUpClass()

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
        cls.hierarchical_observer_role_id = cls.get_role_id_by_name(
            role_name=const.HIERARCHICAL_OBSERVER_ROLE_NAME)

    @unless_coverage
    def setUp(self):
        super(
            TestMPCWhiteListFilterListEndpointsForDelegationToken,
            self).setUp()
        self.service_ids = []
        self.template_ids = []

    def create_service(self):
        request_object = factory.get_add_service_object()
        resp = self.service_admin_client.add_service(
            request_object=request_object)
        self.assertEqual(resp.status_code, 201)
        service_id = resp.json()[const.NS_SERVICE][const.ID]
        self.service_name = resp.json()[const.NS_SERVICE][const.NAME]
        self.service_ids.append(service_id)
        return service_id

    def create_endpoint_template(self):
        service_id = self.create_service()
        request_object = factory.get_add_endpoint_template_object(
            service_id=service_id
        )
        resp = self.identity_admin_client.add_endpoint_template(
            request_object=request_object
        )
        self.assertEqual(resp.status_code, 201)
        template_id = resp.json()[const.OS_KSCATALOG_ENDPOINT_TEMPLATE][
            const.ID]
        self.template_ids.append(template_id)
        return resp

    def set_up_tenant(self):

        # create tenant with type for which there is a whitelist filter
        tenant_name = ":".join([
            self.test_config.mpc_whitelist_tenant_type,
            self.generate_random_string(pattern=const.TENANT_NAME_PATTERN)])
        tenant_1 = self.create_tenant(
            name=tenant_name,
            tenant_types=[self.test_config.mpc_whitelist_tenant_type],
            domain=self.domain_id)
        return tenant_1

    def create_delegation_agreement(self):
        # Create a Delegation Agreement for Domain 1, with sub user in Domain 2
        # as the delegate
        da_name = self.generate_random_string(
            pattern=const.DELEGATION_AGREEMENT_NAME_PATTERN)
        da_req = requests.\
            DelegationAgreements(da_name=da_name)
        da_resp = self.user_admin_client.create_delegation_agreement(
            request_object=da_req)
        da_id = da_resp.json()[
            const.RAX_AUTH_DELEGATION_AGREEMENT][const.ID]
        self.user_admin_client.add_user_delegate_to_delegation_agreement(
            da_id, self.sub_user_id)
        return da_id

    @tags('positive', 'p0', 'regression')
    def test_mpc_whitelist_filter_list_endpoints_for_delegation_token(self):

        # Create new endpoint template
        create_resp = self.create_endpoint_template()
        self.template_id = create_resp.json()[
            const.OS_KSCATALOG_ENDPOINT_TEMPLATE][const.ID]
        self.public_url = create_resp.json()[
            const.OS_KSCATALOG_ENDPOINT_TEMPLATE][const.PUBLIC_URL]

        self.tenant_1 = self.set_up_tenant()

        self.identity_admin_client.add_endpoint_to_tenant(
            tenant_id=self.tenant_1.id, endpoint_template_id=self.template_id)

        # create role
        role_1 = self.create_role()
        da_id = self.create_delegation_agreement()

        # # Now, assign a role to DA and check DA auth
        tenant_assignment_req_1 = self.generate_tenants_assignment_dict(
            role_1.id, self.tenant_1.id)
        tenants_role_assignment_req = requests.TenantRoleAssignments(
            tenant_assignment_req_1)

        assignment_resp = (
            self.user_admin_client.
            add_tenant_role_assignments_to_delegation_agreement(
                da_id=da_id, request_object=tenants_role_assignment_req))
        self.assertEqual(assignment_resp.status_code, 200)

        delegation_auth_req = requests.AuthenticateWithDelegationAgreement(
            token=self.sub_user_token,
            delegation_agreement_id=da_id)

        resp = self.identity_admin_client.get_auth_token(delegation_auth_req)
        self.assertEqual(resp.status_code, 200)
        for auth_by in [const.AUTH_BY_DELEGATION, const.AUTH_BY_PWD]:
            self.assertIn(auth_by, resp.json()[
                const.ACCESS][const.TOKEN][const.RAX_AUTH_AUTHENTICATED_BY])

        delegation_token = resp.json()[const.ACCESS][const.TOKEN][const.ID]
        delegate_client = self.generate_client(token=delegation_token)

        # call list endpoints for DA token using DA token
        self.validate_list_endpoints_response_no_whitelist(
            token=delegation_token, client=delegate_client)
        # call list endpoints for DA token using identity admin's token
        self.validate_list_endpoints_response_no_whitelist(
            token=delegation_token, client=self.identity_admin_client)

        # Now, assign a role from whitelist to DA and check DA auth
        tenant_assignment_req_2 = self.generate_tenants_assignment_dict(
            self.hierarchical_observer_role_id, self.tenant_1.id)
        tenants_role_assignment_req = requests.TenantRoleAssignments(
            tenant_assignment_req_2)

        assignment_resp = (
            self.user_admin_client.
            add_tenant_role_assignments_to_delegation_agreement(
                da_id=da_id, request_object=tenants_role_assignment_req))
        self.assertEqual(assignment_resp.status_code, 200)

        delegation_auth_req = requests.AuthenticateWithDelegationAgreement(
            token=self.sub_user_token,
            delegation_agreement_id=da_id)

        resp = self.identity_admin_client.get_auth_token(delegation_auth_req)
        self.assertEqual(resp.status_code, 200)
        for auth_by in [const.AUTH_BY_DELEGATION, const.AUTH_BY_PWD]:
            self.assertIn(auth_by, resp.json()[
                const.ACCESS][const.TOKEN][const.RAX_AUTH_AUTHENTICATED_BY])

        delegation_token = resp.json()[const.ACCESS][const.TOKEN][const.ID]
        delegate_client = self.generate_client(token=delegation_token)

        # call list endpoints for DA token using DA token
        self.validate_list_endpoints_response_whitelist(
            token=delegation_token, client=delegate_client)
        # call list endpoints for DA token using identity admin's token
        self.validate_list_endpoints_response_whitelist(
            token=delegation_token, client=self.identity_admin_client)

    def validate_list_endpoints_response_whitelist(self, client, token):
        list_endpoints_resp = client.list_endpoints_for_token(token=token)
        self.assertEqual(list_endpoints_resp.status_code, 200)
        self.assertNotEqual(list_endpoints_resp.json()[const.ENDPOINTS], [])
        expected_url = urlparse.urljoin(self.public_url, self.tenant_1.id)
        endpoint = list_endpoints_resp.json()[const.ENDPOINTS]
        self.assertEqual(endpoint[0][const.ID], self.template_id)
        self.assertEqual(endpoint[0][const.TENANT_ID], self.tenant_1.id)
        self.assertEqual(endpoint[0][const.PUBLIC_URL], expected_url)

    def validate_list_endpoints_response_no_whitelist(self, client, token):
        list_endpoints_resp = client.list_endpoints_for_token(token=token)
        self.assertEqual(list_endpoints_resp.status_code, 200)
        self.assertEqual(list_endpoints_resp.json()[const.ENDPOINTS], [])

    @unless_coverage
    def tearDown(self):
        super(
            TestMPCWhiteListFilterListEndpointsForDelegationToken,
            self).tearDown()
        for id_ in self.template_ids:
            self.service_admin_client.delete_endpoint_from_tenant(
                tenant_id=self.tenant_1.id, endpoint_template_id=id_)
            # Disable the endpoint template, so that it can be deleted
            endpoint_template = requests.EndpointTemplateUpdate(
                template_id=id_, enabled='false')
            resp = self.service_admin_client.update_endpoint_template(
                template_id=id_, request_object=endpoint_template)
            self.assertEqual(resp.status_code, 200)
            resp = self.identity_admin_client.delete_endpoint_template(
                template_id=id_)
            self.assertEqual(resp.status_code, 204)
        for id_ in self.service_ids:
            resp = self.service_admin_client.delete_service(service_id=id_)
            self.assertEqual(resp.status_code, 204)

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
        super(
            TestMPCWhiteListFilterListEndpointsForDelegationToken,
            cls).tearDownClass()
