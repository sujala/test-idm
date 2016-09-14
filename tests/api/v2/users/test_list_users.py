# -*- coding: utf-8 -*
import ddt

from tests.api.utils import header_validation
from tests.api.v2 import base
from tests.api.v2.schema import users as users_json
from tests.api import constants as const
from tests.api.v2.models import requests

# TODO: update user list schema validator when defect CID-408 is fixed


@ddt.ddt
class TestListUsers(base.TestBaseV2):

    """ List users test
    Adds identity_admin, user_admin, sub_user then test list user"""

    @classmethod
    def setUpClass(cls):
        """Class level set up for the tests

        Create users needed for the tests and generate clients for those users.
        """
        super(TestListUsers, cls).setUpClass()
        cls.DOMAIN_ID_TEST = "test-spec-user-list"
        cls.user_admin_client = cls.generate_client(
            parent_client=cls.identity_admin_client,
            additional_input_data={'domain_id': cls.DOMAIN_ID_TEST})

        sub_user_name = cls.generate_random_string(
            pattern=const.SUB_USER_PATTERN)
        cls.user_client = cls.generate_client(
            parent_client=cls.user_admin_client,
            additional_input_data={
                'domain_id': cls.DOMAIN_ID_TEST,
                'user_name': sub_user_name})

        cls.unexpected_headers_HTTP_201 = [
            header_validation.validate_transfer_encoding_header_not_present]
        cls.unexpected_headers_HTTP_400 = [
            header_validation.validate_location_header_not_present,
            header_validation.validate_content_length_header_not_present]
        cls.header_validation_functions_HTTP_201 = (
            cls.default_header_validations +
            cls.unexpected_headers_HTTP_201 + [
                header_validation.validate_header_location,
                header_validation.validate_header_content_length])
        cls.header_validation_functions_HTTP_400 = (
            cls.default_header_validations +
            cls.unexpected_headers_HTTP_400 + [
                header_validation.validate_header_transfer_encoding])

    def setUp(self):
        super(TestListUsers, self).setUp()
        self.user_ids = []
        self.sub_user_ids = []
        self.EMAIL_TEST = 'test-api@rackspace.com'

        for i in xrange(2):
            # create identity admin users to test
            ida_username = "iadm_" + self.generate_random_string()
            request_input = requests.UserAdd(user_name=ida_username,
                                             domain_id=const.DOMAIN_TEST)
            resp = self.service_admin_client.add_user(
                request_object=request_input)
            self.user_ids.append(resp.json()[const.USER][const.ID])

            # create user admin to test
            user_name = self.generate_random_string()
            request_input = requests.UserAdd(user_name=user_name,
                                             domain_id=const.DOMAIN_TEST)
            resp = self.identity_admin_client.add_user(
                request_object=request_input)
            self.user_ids.append(resp.json()[const.USER][const.ID])

            # create sub user to test
            sub_username = "sub_" + self.generate_random_string()
            request_input = requests.UserAdd(user_name=sub_username,
                                             domain_id=self.DOMAIN_ID_TEST,
                                             email=self.EMAIL_TEST)
            resp = self.user_admin_client.add_user(
                request_object=request_input)
            self.sub_user_ids.append(resp.json()[const.USER][const.ID])

    def test_list_users_by_service_admin(self):
        """List by service admin user
        """
        resp = self.service_admin_client.list_users()
        self.assertEqual(resp.status_code, 200)

        self.assertSchema(response=resp,
                          json_schema=users_json.list_users)

    def test_list_user_email_by_service_admin(self):
        """List by admin user filter by email
        """
        resp = self.service_admin_client.list_users(
            option={'email': self.EMAIL_TEST}
        )
        self.assertEqual(resp.status_code, 200)
        # TODO: update user list schema validator when defect CID-408 is fixed
        self.assertSchema(response=resp,
                          json_schema=users_json.list_users)
        self.assertEqual(len(resp.json()[const.USERS]), 2)
        for user in resp.json()[const.USERS]:
            self.assertEqual(user[const.EMAIL], self.EMAIL_TEST)

    def test_list_users_by_identity_admin(self):
        """List by identity admin user
        """
        resp = self.identity_admin_client.list_users()
        self.assertEqual(resp.status_code, 200)

        self.assertSchema(response=resp,
                          json_schema=users_json.list_users)

    def test_list_user_email_by_identity_admin(self):
        """List by admin user filter by email
        """
        resp = self.identity_admin_client.list_users(
            option={'email': self.EMAIL_TEST}
        )
        self.assertEqual(resp.status_code, 200)
        self.assertSchema(response=resp,
                          json_schema=users_json.list_users)
        self.assertEqual(len(resp.json()[const.USERS]), 2)
        for user in resp.json()[const.USERS]:
            self.assertEqual(user[const.EMAIL], self.EMAIL_TEST)

    def test_list_users_by_user_admin(self):
        """List by admin user
        """
        resp = self.user_admin_client.list_users()
        self.assertEqual(resp.status_code, 200)

        self.assertSchema(response=resp,
                          json_schema=users_json.list_users)
        self.assertEqual(len(resp.json()[const.USERS]), 4)
        for user in resp.json()[const.USERS]:
            self.assertEqual(user[const.RAX_AUTH_DOMAIN_ID],
                             self.DOMAIN_ID_TEST)

    def test_list_user_email_by_user_admin(self):
        """List by admin user filter by email
        """
        resp = self.user_admin_client.list_users(
            option={'email': self.EMAIL_TEST}
        )
        self.assertEqual(resp.status_code, 200)
        self.assertSchema(response=resp,
                          json_schema=users_json.list_users)
        self.assertEqual(len(resp.json()[const.USERS]), 2)
        for user in resp.json()[const.USERS]:
            self.assertEqual(user[const.EMAIL], self.EMAIL_TEST)

    def test_list_users_by_user_default(self):
        """List by defaul user
        """
        resp = self.user_client.list_users()
        self.assertEqual(resp.status_code, 200)

        self.assertSchema(response=resp,
                          json_schema=users_json.list_users)
        self.assertEqual(len(resp.json()[const.USERS]), 1)

    def test_list_user_email_by_user_default(self):
        """List by default user filter by email
        """
        resp = self.user_client.list_users(
            option={'email': self.EMAIL_TEST}
        )
        self.assertEqual(resp.status_code, 403)  # TODO: check why?
        # self.assertSchema(response=resp,
        #                  json_schema=users_json.list_users)
        # self.assertEqual(len(resp.json()[const.USERS]), 2)
        # for user in resp.json()[const.USERS]:
        #     self.assertEqual(user[const.EMAIL], self.EMAIL_TEST)

    def tearDown(self):
        # Delete all users created in the tests
        for id in self.sub_user_ids:
            self.identity_admin_client.delete_user(user_id=id)
        for id in self.user_ids:
            self.service_admin_client.delete_user(user_id=id)
        super(TestListUsers, self).tearDown()

    @classmethod
    def tearDownClass(cls):
        # Delete all users created in the setUpClass
        cls.delete_client(client=cls.user_client,
                          parent_client=cls.user_admin_client)
        cls.delete_client(client=cls.user_admin_client,
                          parent_client=cls.identity_admin_client)
        super(TestListUsers, cls).tearDownClass()
