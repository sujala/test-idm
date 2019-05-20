# -*- coding: utf-8 -*
import pytest
from qe_coverage.opencafe_decorators import tags, unless_coverage

from tests.api.v2.delegation import delegation
from tests.package.johny import constants as const
from tests.package.johny.v2.models import requests


class TestListDelegates(delegation.TestBaseDelegation):

    @classmethod
    @unless_coverage
    def setUpClass(cls):
        super(TestListDelegates, cls).setUpClass()

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
            da_name=da_name)
        da_resp = cls.user_admin_client.create_delegation_agreement(
            request_object=da_req)
        cls.da_id = da_resp.json()[
            const.RAX_AUTH_DELEGATION_AGREEMENT][const.ID]

        # add user as a delegate
        cls.user_admin_client.add_user_delegate_to_delegation_agreement(
            cls.da_id, cls.sub_user_id
        )

        # Add another sub user in Domain 2
        sub_user_name2 = cls.generate_random_string(
            pattern=const.SUB_USER_PATTERN)
        additional_input_data = {
            'user_name': sub_user_name2}
        cls.sub_user_client = cls.generate_client(
            parent_client=cls.user_admin_client_2,
            additional_input_data=additional_input_data)
        cls.sub_user_id_2 = cls.sub_user_client.default_headers[
            const.X_USER_ID]
        cls.sub_user_token2 = cls.sub_user_client.default_headers[
            const.X_AUTH_TOKEN]

        # Create and add a user group as a delegate
        cls.user_group_id = cls.add_user_group(
            cls.domain_id_2, cls.user_admin_client_2)

        cls.user_admin_client_2.add_user_to_user_group(
            cls.domain_id_2, cls.user_group_id, cls.sub_user_id_2)

        ua_client = cls.user_admin_client

        ua_client.add_user_group_delegate_to_delegation_agreement(
            cls.da_id, cls.user_group_id
        )

    @unless_coverage
    def setUp(self):
        super(TestListDelegates, self).setUp()

    @classmethod
    def add_user_group(cls, domain_id, user_admin_client):

        group_name = cls.generate_random_string(
            pattern=const.USER_GROUP_NAME_PATTERN)
        group_desc = cls.generate_random_string(
            pattern=const.DESC_PATTERN)
        add_user_group_to_domain_req = requests.domainUserGroup(
            group_name=group_name, domain_id=domain_id,
            description=group_desc)
        user_group_resp = user_admin_client.add_user_group_to_domain(
            domain_id=domain_id,
            request_object=add_user_group_to_domain_req)
        assert user_group_resp.status_code == 201,\
            'user group successfully added'
        return user_group_resp.json()[const.RAX_AUTH_USER_GROUP][const.ID]

    @tags('positive', 'p0', 'smoke')
    @pytest.mark.smoke_alpha
    def test_list_delegates(self):
        # get all delegates
        resp = self.user_admin_client.list_delegates_for_delegation_agreement(
            self.da_id
        )
        self.assertEqual(resp.status_code, 200)

        assert_user_group_is_returned = False
        assert_user_is_returned = False

        for delegate in resp.json()[const.DELEGATE_REFERENCES]:
            if delegate[const.DELEGATE_TYPE] == const.USER.upper():
                self.assertEqual(delegate[const.DELEGATE_ID], self.sub_user_id)
                assert_user_is_returned = True
            elif delegate[const.DELEGATE_TYPE] == const.USER_GROUP:
                self.assertEqual(delegate[const.DELEGATE_ID],
                                 self.user_group_id)
                assert_user_group_is_returned = True

        self.assertBoolean('true', assert_user_group_is_returned)
        self.assertBoolean('true', assert_user_is_returned)

        # delete user from delegates
        self.user_admin_client.delete_user_delegate_from_delegation_agreement(
            self.da_id, self.sub_user_id
        )

        # validate only user group is returned in list delegates call
        resp = self.user_admin_client.list_delegates_for_delegation_agreement(
            self.da_id
        )
        self.assertEqual(resp.status_code, 200)
        assert_user_group_is_returned = False
        assert_user_is_returned = False

        for delegate in resp.json()[const.DELEGATE_REFERENCES]:
            if delegate[const.DELEGATE_TYPE] == const.USER.upper():
                self.assertEqual(delegate[const.DELEGATE_ID],
                                 self.sub_user_id)
                assert_user_is_returned = True
            elif delegate[const.DELEGATE_TYPE] == const.USER_GROUP:
                self.assertEqual(delegate[const.DELEGATE_ID],
                                 self.user_group_id)
                assert_user_group_is_returned = True

        self.assertBoolean('true', assert_user_group_is_returned)
        self.assertBoolean('false', assert_user_is_returned)

    @unless_coverage
    def tearDown(self):
        super(TestListDelegates, self).tearDown()

    @classmethod
    @delegation.base.base.log_tearDown_error
    @unless_coverage
    def tearDownClass(cls):
        resp = cls.user_admin_client_2.delete_user(cls.sub_user_id)
        assert resp.status_code == 204, (
            'Subuser with ID {0} failed to delete'.format(cls.sub_user_id))
        resp = cls.user_admin_client_2.delete_user(cls.sub_user_id_2)
        assert resp.status_code == 204, (
            'Subuser with ID {0} failed to delete'.format(cls.sub_user_id_2))
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
        super(TestListDelegates, cls).tearDownClass()
