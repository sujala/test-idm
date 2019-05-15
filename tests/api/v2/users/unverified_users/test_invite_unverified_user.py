import pytest
from qe_coverage.opencafe_decorators import tags, unless_coverage
from random import randrange

from tests.api.v2 import base
from tests.api.v2.users.unverified_users import unverified
from tests.api.v2.schema import users as users_json

from tests.package.johny import constants as const
from tests.package.johny.v2.models import requests


class InviteUnverifiedUserTest(unverified.TestBaseUnverifiedUser):

    @classmethod
    @unless_coverage
    def setUpClass(cls):
        super(InviteUnverifiedUserTest, cls).setUpClass()

    @unless_coverage
    def setUp(self):
        super(InviteUnverifiedUserTest, self).setUp()

    @tags('positive', 'p0', 'regression')
    @pytest.mark.regression
    def test_invite_unverified_user(self):

        user_id = self.create_unverified_user()
        self.users.append(user_id)

        invite_resp = self.user_admin_client.invite_unverified_user(
            user_id=user_id)
        self.assertEqual(invite_resp.status_code, 200)
        self.assertSchema(
            response=invite_resp,
            json_schema=users_json.invite_unverified_user)
        self.assertEqual(
            invite_resp.json()[const.RAX_AUTH_INVITE][const.USER_ID],
            user_id)
        registration_code = invite_resp.json()[
            const.RAX_AUTH_INVITE][const.REGISTRATION_CODE]

        get_user_resp = self.user_admin_client.get_user(user_id=user_id)
        self.assertEqual(invite_resp.status_code, 200)
        self.assertEqual(
            get_user_resp.json()[const.USER][const.RAX_AUTH_UNVERIFIED],
            True)
        self.assertEqual(
            get_user_resp.json()[const.USER][const.ENABLED],
            False)

        # Verify registration code
        resp = self.user_admin_client.verify_unverified_user_invite(
            user_id=user_id, registration_code=registration_code)
        self.assertEqual(resp.status_code, 200)

        # Accept the invite
        user_name = self.generate_random_string(
            pattern=const.USER_NAME_PATTERN)
        password = self.generate_random_string(
            pattern=const.PASSWORD_PATTERN)

        accept_invite_req = requests.AcceptInviteUnverifiedUser(
            registration_code=registration_code,
            user_name=user_name,
            password=password)
        resp = self.user_admin_client.accept_unverified_user_invite(
            user_id=user_id, request_object=accept_invite_req)
        self.assertEqual(resp.status_code, 200)
        self.assertSchema(response=resp, json_schema=users_json.update_user)

        # Auth with the 'verified' user
        auth_req_obj = requests.AuthenticateWithPassword(
            user_name=user_name, password=password)
        resp = self.user_admin_client.get_auth_token(
            request_object=auth_req_obj)
        self.assertEqual(resp.status_code, 200)

        # Verify if Phone Pin is generated for unverified user
        client = self.generate_client(
            token=resp.json()[const.ACCESS][const.TOKEN][const.ID])

        get_user_resp = client.get_user(user_id=user_id)
        self.assertIn(const.RAX_AUTH_PHONE_PIN, get_user_resp.json()[
            const.USER])

        # Verify Phone Pin is updated for verified user
        contact_id = randrange(start=const.CONTACT_ID_MIN,
                               stop=const.CONTACT_ID_MAX)
        request_object = requests.UserUpdate(
            contact_id=contact_id, phone_pin='871694')
        update_resp = client.update_user(
            user_id=user_id, request_object=request_object)
        self.assertEqual(
            update_resp.json()[const.USER][const.RAX_AUTH_PHONE_PIN], '871694')
        self.assertSchema(
           response=update_resp, json_schema=users_json.update_user)

        # get the phone pin after verified user is updated
        get_user_resp = client.get_user(user_id=user_id)
        self.assertIn(const.RAX_AUTH_PHONE_PIN, get_user_resp.json()[
            const.USER])
        phone_pin_after_update = \
            get_user_resp.json()[const.USER][const.RAX_AUTH_PHONE_PIN]

        # verify phone pin is updated
        self.assertEqual(phone_pin_after_update, '871694')

    @unless_coverage
    @base.base.log_tearDown_error
    def tearDown(self):
        super(InviteUnverifiedUserTest, self).tearDown()

    @classmethod
    @unless_coverage
    def tearDownClass(cls):
        super(InviteUnverifiedUserTest, cls).tearDownClass()
