# -*- coding: utf-8 -*
import pytest
from qe_coverage.opencafe_decorators import tags, unless_coverage

from tests.api.utils import func_helper
from tests.api.v2.delegation import delegation
from tests.api.v2.schema import tokens as tokens_json
from tests.package.johny import constants as const
from tests.package.johny.v2.models import requests


class TestAuthUnderDelegationAgreement(delegation.TestBaseDelegation):

    @classmethod
    @unless_coverage
    def setUpClass(cls):
        super(TestAuthUnderDelegationAgreement, cls).setUpClass()
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
        cls.users = []

    @unless_coverage
    def setUp(self):
        super(TestAuthUnderDelegationAgreement, self).setUp()

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
    def test_auth_validate_delegation_token(self):

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
        self.assertSchema(
            response=resp, json_schema=tokens_json.validate_token)
        user_id = resp.json()[const.ACCESS][const.USER][const.ID]

        resp = self.identity_admin_client.get_user(user_id)

        # validate that delegation agreement does not exist in response
        self.assertNotIn(const.RAX_AUTH_DELEGATION_AGREEMENT_ID,
                         resp.json()[const.USER])

        self.identity_admin_client.default_headers[const.X_AUTH_TOKEN] = (
            delegation_token)

        resp = self.identity_admin_client.get_user(user_id)

        # validate that delegation agreement exists in response
        self.assertEqual(
            resp.json()[const.USER][const.RAX_AUTH_DELEGATION_AGREEMENT_ID],
            da_id)

    @tags('positive', 'p0', 'regression')
    @pytest.mark.regression
    def test_reconcile_DA_delegate_user_delete(self):

        # create DA, with sub user as the delegate
        da_id = self.create_delegation_agreement(user_id=self.sub_user_id)

        delegation_auth_req = requests.AuthenticateWithDelegationAgreement(
            token=self.sub_user_token,
            delegation_agreement_id=da_id)

        resp = self.identity_admin_client.get_auth_token(delegation_auth_req)
        self.assertEqual(resp.status_code, 200)

        # Delete Delegate User
        self.user_admin_client_2.delete_user(user_id=self.sub_user_id)
        resp = self.identity_admin_client.get_auth_token(delegation_auth_req)
        self.assertEqual(resp.status_code, 401)

    @tags('positive', 'p0', 'regression')
    @pytest.mark.regression
    def test_mfa_auth_followed_by_delegation(self):
        sub_user_name_2 = self.generate_random_string(
            pattern=const.SUB_USER_PATTERN)
        sub_user_pass_2 = self.generate_random_string(
            pattern=const.PASSWORD_PATTERN)
        request_object = requests.UserAdd(
            user_name=sub_user_name_2, password=sub_user_pass_2)
        add_sub_user_resp = self.user_admin_client_2.add_user(request_object)
        self.assertEqual(add_sub_user_resp.status_code, 201)
        sub_user_id_2 = add_sub_user_resp.json()[const.USER][const.ID]
        self.users.append(sub_user_id_2)

        # setup mfa for sub-user
        secret = func_helper.setup_mfa_for_user(
            user_id=sub_user_id_2, client=self.identity_admin_client)
        auth_obj = requests.AuthenticateWithPassword(
            user_name=sub_user_name_2, password=sub_user_pass_2)
        # first step of MFA
        auth_resp = self.identity_admin_client.get_auth_token(
            request_object=auth_obj)
        # second step of MFA
        mfa_auth_resp = self.call_second_step_of_mfa(
            first_auth_resp=auth_resp, secret=secret)
        self.assertEqual(mfa_auth_resp.status_code, 200)
        mfa_token = mfa_auth_resp.json()[const.ACCESS][const.TOKEN][const.ID]

        # create DA with sub user
        da_id = self.create_delegation_agreement(user_id=sub_user_id_2)

        # Trying delegation using mfa-auth token
        delegation_auth_req = requests.AuthenticateWithDelegationAgreement(
            token=mfa_token, delegation_agreement_id=da_id)

        resp = self.identity_admin_client.get_auth_token(delegation_auth_req)
        self.assertEqual(resp.status_code, 200)
        self.assertSchema(response=resp, json_schema=tokens_json.auth)

        # Verifying that all auth methods are returned, ignoring the order
        for auth_by in [const.AUTH_BY_DELEGATION, const.AUTH_BY_PWD,
                        const.AUTH_BY_OTPPASSCODE]:
            self.assertIn(auth_by, resp.json()[
                const.ACCESS][const.TOKEN][const.RAX_AUTH_AUTHENTICATED_BY])

        delegation_token = resp.json()[const.ACCESS][const.TOKEN][const.ID]
        resp = self.identity_admin_client.validate_token(
            token_id=delegation_token)
        self.assertEqual(resp.status_code, 200)
        self.assertSchema(
            response=resp, json_schema=tokens_json.validate_token)

    def call_second_step_of_mfa(self, first_auth_resp, secret):

        self.assertEqual(first_auth_resp.status_code, 401)
        auth_header = first_auth_resp.headers[const.WWW_AUTHENTICATE]
        session_id = auth_header.split('sessionId=\'')[1].split('\'')[0]

        # authenticate with passcode & session ID (2nd mfa auth step)
        code = func_helper.get_oath_from_secret(secret=secret)

        kwargs = {'session_id': session_id, 'pass_code': code}
        return self.identity_admin_client.auth_with_mfa_cred(**kwargs)

    @unless_coverage
    def tearDown(self):
        super(TestAuthUnderDelegationAgreement, self).tearDown()
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
        super(TestAuthUnderDelegationAgreement, cls).tearDownClass()
