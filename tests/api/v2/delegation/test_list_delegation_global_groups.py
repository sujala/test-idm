# -*- coding: utf-8 -*
import pytest
from qe_coverage.opencafe_decorators import tags, unless_coverage

from tests.api.v2.delegation import delegation
from tests.api.v2.schema import tokens as tokens_json
from tests.package.johny import constants as const
from tests.package.johny.v2.models import requests


class TestListDelegationGlobalGroups(delegation.TestBaseDelegation):

    @classmethod
    @unless_coverage
    def setUpClass(cls):
        super(TestListDelegationGlobalGroups, cls).setUpClass()
        cls.admin_auth_token = cls.identity_admin_client.default_headers[
            const.X_AUTH_TOKEN]

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
        cls.users = [cls.sub_user_id]

    @unless_coverage
    def setUp(self):
        super(TestListDelegationGlobalGroups, self).setUp()

    def create_delegation_agreement(self, user_id):

        # Create a Delegation Agreement for Domain 1, with sub user in Domain 2
        # as the delegate
        da_name = self.generate_random_string(
            pattern=const.DELEGATION_AGREEMENT_NAME_PATTERN)
        da_req = requests.DelegationAgreements(da_name=da_name)
        da_resp = self.user_admin_client.create_delegation_agreement(
            request_object=da_req)
        da_id = da_resp.json()[
            const.RAX_AUTH_DELEGATION_AGREEMENT][const.ID]
        self.user_admin_client.add_user_delegate_to_delegation_agreement(
            da_id, user_id)
        return da_id

    @tags('positive', 'p1', 'regression')
    @pytest.mark.regression
    def test_auth_list_global_groups(self):

        # create DA with sub user
        da_id = self.create_delegation_agreement(user_id=self.sub_user_id)

        delegation_auth_req = requests.AuthenticateWithDelegationAgreement(
            token=self.sub_user_token,
            delegation_agreement_id=da_id)

        resp = self.identity_admin_client.get_auth_token(delegation_auth_req)
        self.assertEqual(resp.status_code, 200)
        self.assertSchema(response=resp, json_schema=tokens_json.auth)

        # Verifying that all auth methods are returned, ignoring the order
        for auth_by in [const.AUTH_BY_DELEGATION, const.AUTH_BY_PWD]:
            self.assertIn(auth_by, resp.json()[
                const.ACCESS][const.TOKEN][const.RAX_AUTH_AUTHENTICATED_BY])

        delegation_token = resp.json()[const.ACCESS][const.TOKEN][const.ID]
        resp = self.identity_admin_client.validate_token(
            token_id=delegation_token)
        self.assertEqual(resp.status_code, 200)
        user_id = resp.json()[const.ACCESS][const.USER][const.ID]

        delegation_client = self.generate_client(token=delegation_token)

        resp = delegation_client.list_groups(user_id)

        # validate that the default global groups are returned (only 1)
        self.assertEqual(len(resp.json()[const.NS_GROUPS]), 1)

        # validate that the default global group is returned
        self.assertEqual(
            resp.json()[const.NS_GROUPS][0][const.NAME], "Default")
        self.assertEqual(
            resp.json()[const.NS_GROUPS][0][const.ID], "0")
        self.assertEqual(
            resp.json()[const.NS_GROUPS][0][const.DESCRIPTION],
            "Default Limits")

    @unless_coverage
    def tearDown(self):
        super(TestListDelegationGlobalGroups, self).tearDown()
        self.identity_admin_client.default_headers[const.X_AUTH_TOKEN] = (
            self.admin_auth_token)

    @classmethod
    @delegation.base.base.log_tearDown_error
    @unless_coverage
    def tearDownClass(cls):
        for user in cls.users:
            resp = cls.user_admin_client_2.delete_user(user)
            assert resp.status_code == 204, (
                'Subuser with ID {0} failed to delete'.format(user))
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
        super(TestListDelegationGlobalGroups, cls).tearDownClass()
