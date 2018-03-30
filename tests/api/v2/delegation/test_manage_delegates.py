# -*- coding: utf-8 -*
from nose.plugins.attrib import attr

from tests.api.utils import func_helper
from tests.api.v2 import base
from tests.api.v2.models import factory, responses

from tests.package.johny import constants as const
from tests.package.johny.v2.models import requests


class TestManageDelegates(base.TestBaseV2):

    @classmethod
    def setUpClass(cls):
        super(TestManageDelegates, cls).setUpClass()
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
        cls.group_ids = []

    @attr(type='regression')
    def test_add_and_remove_user_delegate(self):

        # Create a Delegation Agreement for Domain 1, with sub user in Domain 2
        # as the delegate
        da_name = self.generate_random_string(
            pattern=const.DELEGATION_AGREEMENT_NAME_PATTERN)
        da_req = requests.DelegationAgreements(
            da_name=da_name, delegate_id=self.sub_user_id)
        da_resp = self.user_admin_client.create_delegation_agreement(
            request_object=da_req)
        self.assertEqual(da_resp.status_code, 201)
        da_id = da_resp.json()[const.RAX_AUTH_DELEGATION_AGREEMENT][const.ID]

        # add user delegate to DA
        add_user_delegate_resp = (
            self.user_admin_client.add_user_delegate_to_delegation_agreement(
              da_id=da_id, user_id=self.sub_user_id))
        self.assertEqual(add_user_delegate_resp.status_code, 409)

        # add user delegate to DA
        add_user_delegate_resp = (
            self.user_admin_client.add_user_delegate_to_delegation_agreement(
              da_id=da_id, user_id=self.user_admin_client_2.default_headers[
                    const.X_USER_ID]))
        self.assertEqual(add_user_delegate_resp.status_code, 204)

        ua_client = self.user_admin_client
        # delete user delegate from DA
        delete_resp = ua_client.delete_user_delegate_from_delegation_agreement(
            da_id=da_id, user_id=self.user_admin_client_2.default_headers[
                const.X_USER_ID])
        self.assertEqual(delete_resp.status_code, 204)

        # repeat of the call results in 404
        delete_resp = ua_client.delete_user_delegate_from_delegation_agreement(
            da_id=da_id, user_id=self.user_admin_client_2.default_headers[
                const.X_USER_ID])
        self.assertEqual(delete_resp.status_code, 404)

    def create_and_add_user_group_to_domain(self, client,
                                            domain_id=None,
                                            status_code=201):
        if domain_id is None:
            domain_id = self.domain_id_2
        group_req = factory.get_add_user_group_request(domain_id)
        resp = client.add_user_group_to_domain(
            domain_id=domain_id, request_object=group_req)
        self.assertEqual(resp.status_code, status_code)

        if status_code != 201:
            return None
        else:
            return responses.UserGroup(resp.json())

    @attr(type='regression')
    def test_add_and_remove_user_group_delegate(self):

        group_one = self.create_and_add_user_group_to_domain(
            self.user_admin_client_2, domain_id=self.domain_id_2)
        self.group_ids.append((group_one.id, self.domain_id_2))

        da_name = self.generate_random_string(
            pattern=const.DELEGATION_AGREEMENT_NAME_PATTERN)
        da_req = requests.DelegationAgreements(
            da_name=da_name, delegate_id=self.sub_user_id)
        da_resp = self.user_admin_client.create_delegation_agreement(
            request_object=da_req)
        self.assertEqual(da_resp.status_code, 201)
        da_id = da_resp.json()[const.RAX_AUTH_DELEGATION_AGREEMENT][const.ID]

        # add user group delegate to DA
        ua_client = self.user_admin_client
        add_user_group_delegate_resp = (
            ua_client.add_user_group_delegate_to_delegation_agreement(
              da_id=da_id, user_group_id=group_one.id))
        self.assertEqual(add_user_group_delegate_resp.status_code, 204)

        # repeat of the call
        add_user_group_delegate_resp = (
            ua_client.add_user_group_delegate_to_delegation_agreement(
              da_id=da_id, user_group_id=group_one.id))
        self.assertEqual(add_user_group_delegate_resp.status_code, 409)

        # delete user group delegate from DA
        delete_resp = (
            ua_client.delete_user_group_delegate_from_delegation_agreement(
              da_id=da_id, user_group_id=group_one.id))
        self.assertEqual(delete_resp.status_code, 204)

        # repeat of the call results in 404
        delete_resp = (
            ua_client.delete_user_group_delegate_from_delegation_agreement(
              da_id=da_id, user_group_id=group_one.id))
        self.assertEqual(delete_resp.status_code, 404)

    @classmethod
    @base.base.log_tearDown_error
    def tearDownClass(cls):
        super(TestManageDelegates, cls).tearDownClass()

        for group_id, domain_id in cls.group_ids:
            resp = cls.user_admin_client_2.delete_user_group_from_domain(
                group_id=group_id, domain_id=domain_id)
            assert resp.status_code == 204, (
                'User group with ID {0} failed to delete'.format(
                    group_id))
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
        cls.identity_admin_client.update_domain(
            domain_id=cls.domain_id_2, request_object=disable_domain_req)
        resp = cls.identity_admin_client.delete_domain(
            domain_id=cls.domain_id_2)
        assert resp.status_code == 204, (
            'Domain with ID {0} failed to delete'.format(cls.domain_id_2))
