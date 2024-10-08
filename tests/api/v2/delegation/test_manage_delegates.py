# -*- coding: utf-8 -*
import pytest
from qe_coverage.opencafe_decorators import tags, unless_coverage

from tests.api.v2.delegation import delegation
from tests.api.v2.models import factory, responses
from tests.package.johny import constants as const
from tests.package.johny.v2.models import requests


class TestManageDelegates(delegation.TestBaseDelegation):

    @classmethod
    @unless_coverage
    def setUpClass(cls):
        super(TestManageDelegates, cls).setUpClass()

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

    @unless_coverage
    def setUp(self):
        super(TestManageDelegates, self).setUp()

    @tags('positive', 'p0', 'regression')
    @pytest.mark.regression
    def test_add_and_remove_user_delegate(self):
        self.verify_add_and_remove_user_delegate(
            client=self.user_admin_client)

    @tags('positive', 'p0', 'regression')
    @pytest.mark.regression
    def test_add_and_remove_user_delegate_rcn_admin(self):
        self.verify_add_and_remove_user_delegate(
            client=self.rcn_admin_client)

    def verify_add_and_remove_user_delegate(self, client):

        # Create a Delegation Agreement for Domain 1, with sub user in Domain 2
        # as the delegate
        da_name = self.generate_random_string(
            pattern=const.DELEGATION_AGREEMENT_NAME_PATTERN)
        da_req = requests.DelegationAgreements(da_name=da_name)
        da_resp = client.create_delegation_agreement(
            request_object=da_req)
        self.assertEqual(da_resp.status_code, 201)
        da_id = da_resp.json()[const.RAX_AUTH_DELEGATION_AGREEMENT][const.ID]
        client.add_user_delegate_to_delegation_agreement(
            da_id, self.sub_user_id)

        # add duplicate user delegate to DA
        add_user_delegate_resp = (
            client.add_user_delegate_to_delegation_agreement(
                da_id=da_id, user_id=self.sub_user_id))
        self.assertEqual(add_user_delegate_resp.status_code, 409)

        # add user delegate to DA
        add_user_delegate_resp = (
            client.add_user_delegate_to_delegation_agreement(
                da_id=da_id, user_id=self.user_admin_client_2.default_headers[
                    const.X_USER_ID]))
        self.assertEqual(add_user_delegate_resp.status_code, 204)

        # delete user delegate from DA
        delete_resp = client.delete_user_delegate_from_delegation_agreement(
            da_id=da_id,
            user_id=self.user_admin_client_2.default_headers[const.X_USER_ID])
        self.assertEqual(delete_resp.status_code, 204)

        # repeat of the call results in 404
        delete_resp = client.delete_user_delegate_from_delegation_agreement(
            da_id=da_id,
            user_id=self.user_admin_client_2.default_headers[const.X_USER_ID])
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

    def verify_add_and_remove_user_group_delegate(self, client):

        group_one = self.create_and_add_user_group_to_domain(
            self.user_admin_client_2, domain_id=self.domain_id_2)
        self.group_ids.append((group_one.id, self.domain_id_2))

        da_name = self.generate_random_string(
            pattern=const.DELEGATION_AGREEMENT_NAME_PATTERN)
        da_req = requests.DelegationAgreements(da_name=da_name)
        da_resp = self.user_admin_client.create_delegation_agreement(
            request_object=da_req)
        self.assertEqual(da_resp.status_code, 201)
        da_id = da_resp.json()[const.RAX_AUTH_DELEGATION_AGREEMENT][const.ID]
        self.user_admin_client.add_user_delegate_to_delegation_agreement(
            da_id, self.sub_user_id)

        # add user group delegate to DA
        add_user_group_delegate_resp = (
            client.add_user_group_delegate_to_delegation_agreement(
                da_id=da_id, user_group_id=group_one.id))
        self.assertEqual(add_user_group_delegate_resp.status_code, 204)

        # repeat of the call
        add_user_group_delegate_resp = (
            client.add_user_group_delegate_to_delegation_agreement(
                da_id=da_id, user_group_id=group_one.id))
        self.assertEqual(add_user_group_delegate_resp.status_code, 409)

        # delete user group delegate from DA
        delete_resp = (
            client.delete_user_group_delegate_from_delegation_agreement(
                da_id=da_id, user_group_id=group_one.id))
        self.assertEqual(delete_resp.status_code, 204)

        # repeat of the call results in 404
        delete_resp = (
            client.delete_user_group_delegate_from_delegation_agreement(
                da_id=da_id, user_group_id=group_one.id))
        self.assertEqual(delete_resp.status_code, 404)

    @tags('positive', 'p0', 'regression')
    def test_add_and_remove_user_group_delegate(self):
        self.verify_add_and_remove_user_group_delegate(
            client=self.user_admin_client)

    @tags('positive', 'p0', 'regression')
    def test_add_and_remove_user_group_delegate_rcn_admin(self):
        self.verify_add_and_remove_user_group_delegate(
            client=self.rcn_admin_client)

    @unless_coverage
    def tearDown(self):
        super(TestManageDelegates, self).tearDown()

    @classmethod
    @delegation.base.base.log_tearDown_error
    @unless_coverage
    def tearDownClass(cls):
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
        super(TestManageDelegates, cls).tearDownClass()
