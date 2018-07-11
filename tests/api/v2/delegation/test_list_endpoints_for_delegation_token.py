# -*- coding: utf-8 -*
from nose.plugins.attrib import attr
from qe_coverage.opencafe_decorators import tags, unless_coverage

from tests.api.utils import func_helper
from tests.api.v2.delegation import delegation
from tests.api.v2.models import responses, factory
from tests.package.johny import constants as const
from tests.package.johny.v2.models import requests


class TestListEndpointsForDelegationToken(delegation.TestBaseDelegation):

    @classmethod
    @unless_coverage
    def setUpClass(cls):
        super(TestListEndpointsForDelegationToken, cls).setUpClass()

        # Add Domain 3
        cls.domain_id_one_call = func_helper.generate_randomized_domain_id(
            client=cls.identity_admin_client)
        dom_req = requests.Domain(
            domain_name=cls.domain_id_one_call,
            domain_id=cls.domain_id_one_call, rcn=cls.rcn)
        add_dom_resp = cls.identity_admin_client.add_domain(dom_req)
        assert add_dom_resp.status_code == 201, (
            'domain was not created successfully')

        additional_input_data = {'domain_id': cls.domain_id_one_call}
        cls.user_admin_client_one_call = cls.generate_client(
            parent_client=cls.identity_admin_client,
            additional_input_data=additional_input_data,
            one_call=True)

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
        da_resp = cls.user_admin_client_one_call.create_delegation_agreement(
            request_object=da_req)
        cls.da_id = da_resp.json()[
            const.RAX_AUTH_DELEGATION_AGREEMENT][const.ID]
        ua_client_one_call = cls.user_admin_client_one_call
        ua_client_one_call.add_user_delegate_to_delegation_agreement(
            cls.da_id, cls.sub_user_id)

    @unless_coverage
    def setUp(self):
        super(TestListEndpointsForDelegationToken, self).setUp()

    def create_tenant_with_faws_prefix(self, domain_id=None):
        if domain_id is None:
            domain_id = self.domain_id
        tenant_name = '{0}:{1}'.format(
            const.TENANT_TYPE_FAWS, self.generate_random_string(
                pattern=const.TENANT_NAME_PATTERN))
        tenant_req = factory.get_add_tenant_object(tenant_name=tenant_name,
                                                   domain_id=domain_id)
        add_tenant_resp = self.identity_admin_client.add_tenant(
            tenant=tenant_req)
        self.assertEqual(add_tenant_resp.status_code, 201)
        return responses.Tenant(add_tenant_resp.json())

    @tags('positive', 'p0', 'regression')
    @attr(type='regression')
    def test_list_endpoints_for_delegation_token(self):

        # Commented till CID-1439 is fixed...will need to add more checks once
        # uncommented.
        # self.create_tenant_with_faws_prefix(self.domain_id)

        delegation_auth_req = requests.AuthenticateWithDelegationAgreement(
            token=self.sub_user_token,
            delegation_agreement_id=self.da_id)

        resp = self.identity_admin_client.get_auth_token(delegation_auth_req)
        self.assertEqual(resp.status_code, 200)
        for auth_by in [const.AUTH_BY_DELEGATION, const.AUTH_BY_PWD]:
            self.assertIn(auth_by, resp.json()[
                const.ACCESS][const.TOKEN][const.RAX_AUTH_AUTHENTICATED_BY])

        delegation_token = resp.json()[const.ACCESS][const.TOKEN][const.ID]
        delegate_client = self.generate_client(token=delegation_token)

        # call list endpoints for DA token using DA token
        self.validate_list_endpoints_response(
            token=delegation_token, client=delegate_client)
        # call list endpoints for DA token using identity admin's token
        self.validate_list_endpoints_response(
            token=delegation_token, client=self.identity_admin_client)

    def validate_list_endpoints_response(self, client, token):
        list_endpoints_resp = client.list_endpoints_for_token(token=token)
        self.assertEqual(list_endpoints_resp.status_code, 200)
        # Since principal is created via one user call
        self.assertNotEqual(list_endpoints_resp.json()[const.ENDPOINTS], [])

    @unless_coverage
    def tearDown(self):
        super(TestListEndpointsForDelegationToken, self).tearDown()

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
        cls.delete_client(cls.user_admin_client_one_call)
        super(TestListEndpointsForDelegationToken, cls).tearDownClass()
