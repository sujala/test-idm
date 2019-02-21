# -*- coding: utf-8 -*
from nose.plugins.attrib import attr
from qe_coverage.opencafe_decorators import tags, unless_coverage

from tests.api.utils import func_helper
from tests.api.v2 import base
from tests.api.v2.models import responses
from tests.package.johny import constants as const
from tests.package.johny.v2.models import requests
import time


class TestOtpMfaUser(base.TestBaseV2):

    @classmethod
    @unless_coverage
    def setUpClass(cls):
        super(TestOtpMfaUser, cls).setUpClass()

    @unless_coverage
    def setUp(self):
        super(TestOtpMfaUser, self).setUp()
        self.user_ids = []
        self.domain_ids = []

    def create_user(self):
        """regular"""
        username = self.generate_random_string(
            pattern=const.USER_NAME_PATTERN)
        domain_id = func_helper.generate_randomized_domain_id(
            client=self.identity_admin_client, pattern=const.NUMBERS_PATTERN)
        request_object = requests.UserAdd(user_name=username,
                                          domain_id=domain_id)
        resp = self.identity_admin_client.add_user(
            request_object=request_object)
        self.assertEqual(resp.status_code, 201)
        user_id = resp.json()[const.USER][const.ID]
        self.user_ids.append(user_id)
        resp_domain_id = resp.json()[const.USER][const.RAX_AUTH_DOMAIN_ID]
        self.domain_ids.append(resp_domain_id)
        user_resp = responses.User(resp_json=resp.json())
        return {'username': user_resp.user_name,
                'password': user_resp.password,
                'user_id': user_resp.id,
                'domain_id': resp_domain_id}

    def call_second_step_of_mfa(self, first_auth_resp, secret):

        self.assertEqual(first_auth_resp.status_code, 401)
        auth_header = first_auth_resp.headers[const.WWW_AUTHENTICATE]
        session_id = auth_header.split('sessionId=\'')[1].split('\'')[0]

        # authenticate with passcode & session ID (2nd mfa auth step)
        code = func_helper.get_oath_from_secret(secret=secret)

        kwargs = {'session_id': session_id, 'pass_code': code}
        return self.identity_admin_client.auth_with_mfa_cred(**kwargs)

    @tags('positive', 'p0', 'regression')
    @attr(type='regression')
    def test_when_otp_mfa_user_disabled_mfa_is_still_enabled(self):
        # when an OTP MFA user is disabled, MFA is still enabled
        # if user account is enabled
        create_user_info = self.create_user()
        username = create_user_info['username']
        password = create_user_info['password']
        user_id = create_user_info['user_id']

        secret = func_helper.setup_mfa_for_user(
            user_id=user_id, client=self.identity_admin_client)
        # Adding the sleep
        # because minor skew among the individual nodes can cause
        # revocation of MFA session ID
        time.sleep(2)

        # first step of mfa
        auth_obj = requests.AuthenticateWithPassword(user_name=username,
                                                     password=password)
        auth_resp = self.identity_admin_client.get_auth_token(
            request_object=auth_obj)

        # second step of MFA
        mfa_auth_resp = self.call_second_step_of_mfa(
            first_auth_resp=auth_resp, secret=secret)
        self.assertEqual(mfa_auth_resp.status_code, 200)
        mfa_token = mfa_auth_resp.json()[const.ACCESS][const.TOKEN][const.ID]
        self.assertIsNotNone(mfa_token)

        # Disable the user
        update_user_object = requests.UserUpdate(enabled=False)
        update_user_resp = self.identity_admin_client.update_user(
                        user_id=user_id, request_object=update_user_object)
        self.assertEqual(update_user_resp.status_code, 200)

        # auth
        auth_obj = requests.AuthenticateWithPassword(user_name=username,
                                                     password=password)
        auth_resp = self.identity_admin_client.get_auth_token(
            request_object=auth_obj)
        self.assertEqual(auth_resp.status_code, 403)

        # Re enable the user
        update_user_object = requests.UserUpdate(enabled=True)
        update_user_resp = self.identity_admin_client.update_user(
            user_id=user_id, request_object=update_user_object)
        self.assertEqual(update_user_resp.status_code, 200)

        # MFA should be enabled for this user still
        resp = self.identity_admin_client.get_user(user_id)
        self.assertEqual(resp.json()[const.USER][
            const.RAX_AUTH_FACTOR_TYPE], 'OTP')
        self.assertEqual(resp.json()[const.USER][
            const.RAX_AUTH_MULTI_FACTOR_ENABLED], True)
        self.assertEqual(resp.json()[const.USER][
            const.ENABLED], True)
        self.assertEqual(resp.json()[const.USER][
            const.RAX_AUTH_MULTI_FACTOR_STATE], 'ACTIVE')

    @unless_coverage
    def tearDown(self):
        super(TestOtpMfaUser, self).tearDown()
        for id_ in self.user_ids:
            resp = self.identity_admin_client.delete_user(user_id=id_)
            self.assertEqual(
                resp.status_code, 204,
                msg='User with ID {0} failed to delete'.format(id_))
        for id_ in self.domain_ids:
            domain_object = requests.Domain(
                domain_name=id_, enabled=False)
            self.identity_admin_client.update_domain(
                domain_id=id_, request_object=domain_object)
            resp = self.identity_admin_client.delete_domain(domain_id=id_)
            self.assertEqual(
                resp.status_code, 204,
                msg='Domain with ID {0} failed to delete'.format(id_))

    @classmethod
    @unless_coverage
    def tearDownClass(cls):
        super(TestOtpMfaUser, cls).tearDownClass()
