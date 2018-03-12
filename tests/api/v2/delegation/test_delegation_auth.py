# -*- coding: utf-8 -*
from nose.plugins.attrib import attr

from tests.api.utils import func_helper
from tests.api.v2 import base

from tests.package.johny import constants as const
from tests.package.johny.v2.models import requests


class TestAuthUnderDelegationAgreement(base.TestBaseV2):

    @classmethod
    def setUpClass(cls):
        super(TestAuthUnderDelegationAgreement, cls).setUpClass()
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
        cls.sub_user_token = cls.sub_user_client.default_headers[
            const.X_AUTH_TOKEN]

        # Create a Delegation Agreement for Domain 1, with sub user in Domain 2
        # as the delegate
        da_name = cls.generate_random_string(
            pattern=const.DELEGATION_AGREEMENT_NAME_PATTERN)
        da_req = requests.DelegationAgreements(
            da_name=da_name, delegate_id=cls.sub_user_id)
        da_resp = cls.user_admin_client.create_delegation_agreement(
            request_object=da_req)
        cls.da_id = da_resp.json()[
            const.RAX_AUTH_DELEGATION_AGREEMENT][const.ID]

    @attr(type='smoke_alpha')
    def test_auth_validate_delegation_token(self):
        delegation_auth_req = requests.AuthenticateWithDelegationAgreement(
            token=self.sub_user_token,
            delegation_agreement_id=self.da_id)

        resp = self.identity_admin_client.get_auth_token(delegation_auth_req)
        self.assertEqual(resp.status_code, 200)
        self.assertEqual(
            resp.json()[const.ACCESS][const.TOKEN][
                const.RAX_AUTH_AUTHENTICATED_BY], const.AUTH_BY_PWD_DELEGATION)

        delegation_token = resp.json()[const.ACCESS][const.TOKEN][const.ID]
        resp = self.identity_admin_client.validate_token(
            token_id=delegation_token)
        self.assertEqual(resp.status_code, 200)

    @classmethod
    @base.base.log_tearDown_error
    def tearDownClass(cls):
        super(TestAuthUnderDelegationAgreement, cls).tearDownClass()

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
