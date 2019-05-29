# -*- coding: utf-8 -*
import pytest
from random import randrange

from qe_coverage.opencafe_decorators import tags, unless_coverage

from tests.api.utils import func_helper
from tests.api.v2 import base
from tests.api.v2.schema import users as users_json

from tests.package.johny import constants as const
from tests.package.johny.v2.models import requests
# TODO: update user list schema validator when defect CID-408 is fixed


class TestListUsers(base.TestBaseV2):

    """ List users test
    Adds identity_admin, user_admin, sub_user then test list user"""

    @classmethod
    @unless_coverage
    def setUpClass(cls):
        """Class level set up for the tests

        Create users needed for the tests and generate clients for those users.
        """
        super(TestListUsers, cls).setUpClass()
        contact_id = randrange(start=const.CONTACT_ID_MIN,
                               stop=const.CONTACT_ID_MAX)
        cls.DOMAIN_ID_TEST = func_helper.generate_randomized_domain_id(
            client=cls.identity_admin_client)
        cls.user_admin_client = cls.generate_client(
            parent_client=cls.identity_admin_client,
            additional_input_data={'domain_id': cls.DOMAIN_ID_TEST,
                                   'contact_id': contact_id})

        sub_user_name = cls.generate_random_string(
            pattern=const.SUB_USER_PATTERN)
        cls.user_client = cls.generate_client(
            parent_client=cls.user_admin_client,
            additional_input_data={
                'domain_id': cls.DOMAIN_ID_TEST,
                'user_name': sub_user_name})

    @unless_coverage
    def setUp(self):
        super(TestListUsers, self).setUp()
        self.user_ids = []
        self.sub_user_ids = []
        self.EMAIL_TEST = 'test-api@rackspace.com'

        for i in range(2):
            # create user admin to test
            user_name = self.generate_random_string()
            domain_id = func_helper.generate_randomized_domain_id(
                client=self.identity_admin_client)
            request_input = requests.UserAdd(user_name=user_name,
                                             domain_id=domain_id)
            resp = self.identity_admin_client.add_user(
                request_object=request_input)
            self.user_ids.append(resp.json()[const.USER][const.ID])

            # create sub user to test
            sub_username = "sub_" + self.generate_random_string()
            request_input = requests.UserAdd(user_name=sub_username,
                                             domain_id=domain_id,
                                             email=self.EMAIL_TEST)
            resp = self.user_admin_client.add_user(
                request_object=request_input)
            self.sub_user_ids.append(resp.json()[const.USER][const.ID])

    @tags('positive', 'p1', 'regression')
    def test_list_users_by_service_admin(self):
        """List by service admin user
        """
        resp = self.service_admin_client.list_users()
        self.assertEqual(resp.status_code, 200)

        self.assertSchema(response=resp,
                          json_schema=users_json.list_users)

    @tags('positive', 'p1', 'regression')
    @pytest.mark.skip_at_gate
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

    @tags('positive', 'p0', 'smoke')
    @pytest.mark.smoke_alpha
    def test_list_users_by_identity_admin(self):
        """List by identity admin user
        """
        resp = self.identity_admin_client.list_users()
        # we should only return users for admin's domain

        self.assertTrue(len(resp.json()[const.USERS]) <= 2)

        # no users returned that were added to user-admin's domain
        found_user = next((user for user in resp.json()[
            const.USERS] if user['id'] in self.sub_user_ids), None)
        self.assertEqual(resp.status_code, 200)
        self.assertEqual(found_user, None)

        self.assertSchema(response=resp,
                          json_schema=users_json.list_users)

    @tags('positive', 'p1', 'regression')
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
        # [CID-2057] Phone pin state attribute is returned for
        # list users by email
        self.assertIn(
            const.RAX_AUTH_PHONE_PIN_STATE, str(resp.json()[const.USERS]))
        for user in resp.json()[const.USERS]:
            self.assertEqual(user[const.EMAIL], self.EMAIL_TEST)

    @tags('positive', 'p1', 'regression')
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

    @tags('positive', 'p1', 'regression')
    def test_list_users_using_impersonation_token(self):
        """List users using impersonation token
        """
        resp = self.identity_admin_client.get_user(
            self.user_admin_client.default_headers[const.X_USER_ID])
        ua_username = resp.json()[const.USER][const.USERNAME]

        # Get an impersonation token. Impersonating with identity admin.
        imp_req = requests.ImpersonateUser(user_name=ua_username)
        imp_resp = self.identity_admin_client.impersonate_user(
            request_data=imp_req)
        self.assertEqual(imp_resp.status_code, 200)
        impersonation_token = imp_resp.json()[
            const.ACCESS][const.TOKEN][const.ID]
        imp_client = self.generate_client(token=impersonation_token)
        resp = imp_client.list_users()

        self.assertEqual(resp.status_code, 200)
        self.assertEqual(len(resp.json()[const.USERS]), 4)
        for user in resp.json()[const.USERS]:
            self.assertEqual(user[const.RAX_AUTH_DOMAIN_ID],
                             self.DOMAIN_ID_TEST)

    @tags('positive', 'p1', 'regression')
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

    @tags('positive', 'p1', 'regression')
    def test_list_users_by_user_default(self):
        """List by defaul user
        """
        resp = self.user_client.list_users()
        self.assertEqual(resp.status_code, 200)

        self.assertSchema(response=resp,
                          json_schema=users_json.list_users)
        self.assertEqual(len(resp.json()[const.USERS]), 1)

    @tags('negative', 'p1', 'regression')
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

    @unless_coverage
    @base.base.log_tearDown_error
    def tearDown(self):
        # Delete all users created in the tests
        for id in self.sub_user_ids:
            self.identity_admin_client.delete_user(user_id=id)
        for id in self.user_ids:
            self.identity_admin_client.delete_user(user_id=id)
        super(TestListUsers, self).tearDown()

    @classmethod
    @unless_coverage
    @base.base.log_tearDown_error
    def tearDownClass(cls):
        # Delete all users created in the setUpClass
        cls.identity_admin_client.delete_user(
            cls.user_client.default_headers[const.X_USER_ID])
        cls.delete_client(client=cls.user_admin_client,
                          parent_client=cls.identity_admin_client)
        super(TestListUsers, cls).tearDownClass()
