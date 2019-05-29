# -*- coding: utf-8 -*
import pytest

from qe_coverage.opencafe_decorators import tags, unless_coverage

from tests.api.utils import func_helper
from tests.api.v2 import base

from tests.package.johny import constants as const
from tests.package.johny.v2.models import requests


class TestPhonePinOnUser(base.TestBaseV2):
    """
    Tests for verifying,

    i. that new user creation adds a phone pin on the user.
    ii. reset phone pin functionality.
    """

    @classmethod
    @unless_coverage
    def setUpClass(cls):
        """
        Class level set up for the tests
        Create users needed for the tests and generate clients for those users.
        """
        super(TestPhonePinOnUser, cls).setUpClass()

    @unless_coverage
    def setUp(self):
        super(TestPhonePinOnUser, self).setUp()
        self.user_admin_clients = []
        self.sub_user_ids = []

    @tags('positive', 'p0', 'smoke')
    @pytest.mark.smoke_alpha
    def test_user_admin_phone_pin(self):
        """
        Test to verify,
        1. [CID-2057] add user admin call generates phone pin and phone
           pin state for user
        2. [CID-2057] authentication call returns Phone Pin state
        3. phone pin can/cannot be reset based on caller
        """
        user_name = self.generate_random_string()
        password = self.generate_random_string(pattern=const.PASSWORD_PATTERN)
        input_data = {
            'domain_id': self.generate_random_string(pattern='[\d]{7}')}
        request_object = requests.UserAdd(user_name=user_name,
                                          password=password, **input_data)
        resp = self.identity_admin_client.add_user(request_object)
        self.assertEqual(resp.status_code, 201)
        user_id = resp.json()[const.USER][const.ID]
        # [CID-2057] add user returns phone pin state
        self.assertEqual(
            resp.json()['user'][const.RAX_AUTH_PHONE_PIN_STATE], const.ACTIVE)

        auth_req_obj = requests.AuthenticateWithPassword(
            user_name=user_name, password=password
        )
        resp = self.identity_admin_client.get_auth_token(
            request_object=auth_req_obj)
        phone_pin = resp.json()[
            const.ACCESS][const.USER][const.RAX_AUTH_PHONE_PIN]
        user_admin_token = resp.json()[const.ACCESS][const.TOKEN][const.ID]

        # [CID-2057] Authentication returns phone pin state
        self.assertEqual(
            resp.json()[const.ACCESS][const.USER]
            [const.RAX_AUTH_PHONE_PIN_STATE],
            const.ACTIVE)

        # [CID-2057] Validate token returns phone pin state
        resp = self.identity_admin_client.validate_token(
            token_id=user_admin_token)
        self.assertEqual(resp.status_code, 200)
        self.assertEqual(
            resp.json()[const.ACCESS][const.USER]
            [const.RAX_AUTH_PHONE_PIN_STATE],
            const.ACTIVE)

        user_admin_client = self.generate_client(token=user_admin_token)
        self.user_admin_clients.append(user_admin_client)

        # verify phone pin exists for the user-admin when called on self
        get_user_admin_resp = user_admin_client.get_user(user_id=user_id)
        self.assertIn(const.RAX_AUTH_PHONE_PIN, get_user_admin_resp.json()[
            const.USER])
        phone_pin_length = len(
            get_user_admin_resp.json()[const.USER][const.RAX_AUTH_PHONE_PIN])
        self.assertEqual(phone_pin_length, 6)

        # verify phone pin is not returned for the user-admin when called with
        # identity-admin.
        get_user_admin_resp = self.identity_admin_client.get_user(
            user_id=user_id)
        self.assertNotIn(const.RAX_AUTH_PHONE_PIN, get_user_admin_resp.json()[
            const.USER])

        # [CID-2057] verify if phone pin is not returned and phone pin state
        # is returned  for list users
        list_users_resp = self.identity_admin_client.list_users()
        self.assertNotIn(const.RAX_AUTH_PHONE_PIN, list_users_resp.json()[
            const.USERS])
        self.assertIn(const.RAX_AUTH_PHONE_PIN_STATE,
                      str(list_users_resp.json()[const.USERS]))

        # verify if phone pin is not returned
        resp = self.identity_admin_client.list_users(
            option={'domain_id': input_data['domain_id']})
        self.assertNotIn(const.RAX_AUTH_PHONE_PIN, resp.json()[
                const.USERS])

        # verify if phone pin is not returned for list users in domain
        resp = self.identity_admin_client.list_users_in_domain(
            domain_id=input_data['domain_id'])
        self.assertNotIn(const.RAX_AUTH_PHONE_PIN, resp.json()[
            const.USERS])

        # verify if phone pin is not returned with impersonation token
        impersonation_request_obj = requests.ImpersonateUser(
            user_name=user_name)

        # Get Impersonation Token
        resp = self.identity_admin_client.impersonate_user(
            request_data=impersonation_request_obj)
        self.assertEqual(resp.status_code, 200)

        token_id = resp.json()[const.ACCESS][const.TOKEN][const.ID]

        imp_client = self.generate_client(token=token_id)

        # verify if phone pin is not returned
        get_user_resp = imp_client.get_user(
            user_id=user_id)
        self.assertNotIn(const.RAX_AUTH_PHONE_PIN, get_user_resp.json()[
            const.USER])

        # Reset phone pin on user admin
        resp = self.identity_admin_client.reset_phone_pin(user_id=user_id)
        self.assertEqual(resp.status_code, 204)
        resp = self.identity_admin_client.get_auth_token(
            request_object=auth_req_obj)
        new_phone_pin = resp.json()[
            const.ACCESS][const.USER][const.RAX_AUTH_PHONE_PIN]
        self.assertNotEqual(new_phone_pin, phone_pin)

        # Cannot Reset own phone PIN
        resp = user_admin_client.reset_phone_pin(user_id=user_id)
        self.assertEqual(resp.status_code, 403)

    @tags('positive', 'p0', 'smoke')
    @pytest.mark.smoke_alpha
    def test_default_user_phone_pin(self):
        """
        Test to verify,
        1. add default user call generates phone pin for user.
        2. phone pin can/cannot be reset based on caller.
        """
        domain_id = func_helper.generate_randomized_domain_id(
            client=self.identity_admin_client)
        user_admin_client = self.generate_client(
            parent_client=self.identity_admin_client,
            additional_input_data={'domain_id': domain_id})
        self.user_admin_clients.append(user_admin_client)

        sub_user_name = self.generate_random_string(
            pattern=const.SUB_USER_PATTERN)
        sub_user_client = self.generate_client(
            parent_client=user_admin_client,
            additional_input_data={
                'domain_id': domain_id,
                'user_name': sub_user_name})
        sub_user_id = sub_user_client.default_headers[const.X_USER_ID]
        self.sub_user_ids.append(sub_user_id)
        get_user_resp = sub_user_client.get_user(user_id=sub_user_id)

        # check if phone pin exists for the sub-user when called on self
        self.assertIn(const.RAX_AUTH_PHONE_PIN, get_user_resp.json()[
            const.USER])
        phone_pin = get_user_resp.json()[const.USER][const.RAX_AUTH_PHONE_PIN]
        self.assertEqual(len(phone_pin), 6)
        get_user_resp = user_admin_client.get_user(user_id=sub_user_id)
        self.assertNotIn(const.RAX_AUTH_PHONE_PIN, get_user_resp.json()[
            const.USER])

        # Reset phone pin on user admin
        resp = user_admin_client.reset_phone_pin(user_id=sub_user_id)
        self.assertEqual(resp.status_code, 204)
        get_user_resp = sub_user_client.get_user(user_id=sub_user_id)
        new_phone_pin = get_user_resp.json()[
            const.USER][const.RAX_AUTH_PHONE_PIN]
        self.assertNotEqual(new_phone_pin, phone_pin)

        # Cannot Reset own phone PIN
        resp = sub_user_client.reset_phone_pin(user_id=sub_user_id)
        self.assertEqual(resp.status_code, 403)

    @unless_coverage
    @base.base.log_tearDown_error
    def tearDown(self):
        # Delete all clients created in the tests
        for id_ in self.sub_user_ids:
            delete_resp = self.identity_admin_client.delete_user(user_id=id_)
            self.assertEqual(
                delete_resp.status_code, 204,
                msg='User with ID {0} failed to delete'.format(
                    id_))
        for client in self.user_admin_clients:
            self.delete_client(client)
        super(TestPhonePinOnUser, self).tearDown()

    @classmethod
    @unless_coverage
    @base.base.log_tearDown_error
    def tearDownClass(cls):
        super(TestPhonePinOnUser, cls).tearDownClass()
