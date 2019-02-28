# -*- coding: utf-8 -*
from nose.plugins.attrib import attr

from qe_coverage.opencafe_decorators import tags, unless_coverage

from tests.api.utils import func_helper
from tests.api.v2 import base

from tests.package.johny import constants as const
from tests.package.johny.v2.models import requests


class TestPhonePinOnAddUser(base.TestBaseV2):

    """
    Tests for verifying that new user creation adds a phone pin on the user
    """

    @classmethod
    @unless_coverage
    def setUpClass(cls):
        """
        Class level set up for the tests
        Create users needed for the tests and generate clients for those users.
        """
        super(TestPhonePinOnAddUser, cls).setUpClass()

    @unless_coverage
    def setUp(self):
        super(TestPhonePinOnAddUser, self).setUp()
        self.user_admin_clients = []
        self.sub_user_ids = []

    @tags('positive', 'p0', 'smoke')
    @attr(type='smoke_alpha')
    def test_add_user_admin_generates_phone_pin(self):
        """
        add user admin call generates phone pin for user
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

        req_obj = requests.AuthenticateWithPassword(
            user_name=user_name, password=password
        )
        resp = self.identity_admin_client.get_auth_token(
            request_object=req_obj)
        user_admin_token = resp.json()[const.ACCESS][const.TOKEN][const.ID]

        user_admin_client = self.generate_client(token=user_admin_token)
        self.user_admin_clients.append(user_admin_client)

        # check if phone pin exists for the user-admin when called on self
        get_user_admin_resp = user_admin_client.get_user(user_id=user_id)
        self.assertIn(const.RAX_AUTH_PHONE_PIN, get_user_admin_resp.json()[
            const.USER])
        self.assertEqual(len(get_user_admin_resp.json()[const.USER][
                                 const.RAX_AUTH_PHONE_PIN]), 6)
        get_user_resp = self.identity_admin_client.get_user(
            user_id=user_id)
        self.assertNotIn(const.RAX_AUTH_PHONE_PIN, get_user_resp.json()[
            const.USER])

        # verify if phone pin is not returned for list users
        list_users_resp = self.identity_admin_client.list_users()
        self.assertNotIn(const.RAX_AUTH_PHONE_PIN, list_users_resp.json()[
            const.USERS])

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

    @tags('positive', 'p0', 'smoke')
    @attr(type='smoke_alpha')
    def test_add_default_user_generates_phone_pin(self):
        """
        add default user call generates phone pin for user
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
        self.assertEqual(len(get_user_resp.json()[const.USER][
                                 const.RAX_AUTH_PHONE_PIN]), 6)
        get_user_resp = user_admin_client.get_user(user_id=sub_user_id)
        self.assertNotIn(const.RAX_AUTH_PHONE_PIN, get_user_resp.json()[
            const.USER])

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
        super(TestPhonePinOnAddUser, self).tearDown()

    @classmethod
    @unless_coverage
    @base.base.log_tearDown_error
    def tearDownClass(cls):
        super(TestPhonePinOnAddUser, cls).tearDownClass()
