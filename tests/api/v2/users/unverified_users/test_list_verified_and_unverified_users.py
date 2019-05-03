# -*- coding: utf-8 -*
import pytest

from qe_coverage.opencafe_decorators import tags, unless_coverage

from tests.api.utils import func_helper
from tests.api.v2 import base

from tests.package.johny import constants as const
from tests.package.johny.v2.models import requests


class TestListVerifiedUnverifiedUsers(base.TestBaseV2):

    """ List Verified and Unverified users test
    Adds user_admin, sub_user, unverified User then
    test list users"""

    @classmethod
    @unless_coverage
    def setUpClass(cls):
        """Class level set up for the tests
        Create users needed for the tests and generate clients for those
        users.
        """
        super(TestListVerifiedUnverifiedUsers, cls).setUpClass()
        cls.unverified_user_ids = []
        cls.sub_user_ids = []
        cls.email_test = cls.generate_random_string(
            pattern=const.EMAIL_PATTERN)
        cls.rcn = cls.test_config.unverified_user_rcn
        # Add Domain w/ RCN
        cls.domain_id_1 = func_helper.generate_randomized_domain_id(
            client=cls.identity_admin_client)
        dom_req = requests.Domain(
            domain_name=cls.domain_id_1,
            domain_id=cls.domain_id_1, rcn=cls.rcn)
        add_dom_resp = cls.identity_admin_client.add_domain(dom_req)
        assert add_dom_resp.status_code == 201, (
            'domain was not created successfully')

        cls.user_admin_client_1 = cls.generate_client(
            parent_client=cls.identity_admin_client,
            additional_input_data={'domain_id': cls.domain_id_1})

        # create unverified user to test
        cls.unverified_user_email = cls.generate_random_string(
            pattern=const.UNVERIFIED_EMAIL_PATTERN)
        create_unverified_user_req = requests.UnverifiedUser(
            email=cls.unverified_user_email, domain_id=cls.domain_id_1)
        create_unverified_resp = \
            cls.user_admin_client_1.create_unverified_user(
                request_object=create_unverified_user_req)
        cls.unverified_user_ids.append(
            create_unverified_resp.json()[const.USER][const.ID])

        cls.sub_user_name_1 = cls.generate_random_string(
            pattern=const.SUB_USER_PATTERN)
        cls.user_client = cls.generate_client(
            parent_client=cls.user_admin_client_1,
            additional_input_data={
                'user_name': cls.sub_user_name_1})

        # create user manager client
        cls.user_manager_name = cls.generate_random_string(
            pattern=const.USER_MANAGER_NAME_PATTERN)
        cls.user_manager_client = cls.generate_client(
            parent_client=cls.user_admin_client_1,
            additional_input_data={
                'user_name': cls.user_manager_name,
                'is_user_manager': True})

        # Domain2
        cls.domain_id_2 = func_helper.generate_randomized_domain_id(
            client=cls.identity_admin_client)
        cls.user_admin_client_2 = cls.generate_client(
            parent_client=cls.identity_admin_client,
            additional_input_data={'domain_id': cls.domain_id_2})
        # create sub user in Domain2
        cls.sub_user_name_2 = "sub_" + cls.generate_random_string()
        request_input = requests.UserAdd(user_name=cls.sub_user_name_2,
                                         email=cls.email_test)
        resp = cls.user_admin_client_2.add_user(
                request_object=request_input)
        cls.sub_user_ids.append(resp.json()[const.USER][const.ID])

    @unless_coverage
    def setUp(self):
        super(TestListVerifiedUnverifiedUsers, self).setUp()

    @tags('positive', 'p0', 'smoke')
    @pytest.mark.smoke_alpha
    def test_list_all_users_by_identity_admin(self):
        """List all users with unverified user email by identity admin
           and verfiy unverified user is listed"
        """
        resp = self.identity_admin_client.list_users(
            option={'user_type': const.ALL,
                    'email': self.unverified_user_email})

        self.assertEqual(len(resp.json()[const.USERS]), 1)

        self.assertEqual(resp.status_code, 200)
        for user in resp.json()[const.USERS]:
            self.assertEqual(user[const.RAX_AUTH_UNVERIFIED], True)
            self.assertIn(user[const.ID], self.unverified_user_ids)
            self.assertEqual(user[const.EMAIL], self.unverified_user_email)
            self.assertEqual(user[const.ENABLED], False)

    @tags('positive', 'p0', 'smoke')
    @pytest.mark.smoke_alpha
    def test_list_unverified_users_by_identity_admin(self):
        """List unverified users with query parameter unverified
           and email by identity admin user
        """
        resp = self.identity_admin_client.list_users(
            option={'user_type': const.UNVERIFIED,
                    'email': self.unverified_user_email})

        self.assertEqual(len(resp.json()[const.USERS]), 1)

        self.assertEqual(resp.status_code, 200)
        for user in resp.json()[const.USERS]:
            self.assertEqual(user[const.RAX_AUTH_UNVERIFIED], True)
            self.assertEqual(user[const.RAX_AUTH_DOMAIN_ID], self.domain_id_1)
            self.assertIn(user[const.ID], self.unverified_user_ids)
            self.assertEqual(user[const.ENABLED], False)
            self.assertEqual(user[const.EMAIL], self.unverified_user_email)

    @tags('positive', 'p0', 'smoke')
    @pytest.mark.smoke_alpha
    def test_list_verified_users_by_identity_admin(self):
        """List verified users by identity admin
        """
        resp = self.identity_admin_client.list_users(
            option={'user_type': const.VERIFIED, 'email': self.email_test})

        self.assertEqual(len(resp.json()[const.USERS]), 1)

        self.assertEqual(resp.status_code, 200)

        # subuser returned from domain2
        for user in resp.json()[const.USERS]:
            self.assertEqual(user[const.RAX_AUTH_DOMAIN_ID],
                             self.domain_id_2)
            self.assertEqual(user[const.USERNAME], self.sub_user_name_2)
            self.assertEqual(user[const.EMAIL], self.email_test)

        resp = self.identity_admin_client.list_users(
            option={'user_type': const.VERIFIED, 'name': self.sub_user_name_2})
        self.assertEqual(resp.status_code, 400)

    @tags('positive', 'p0', 'regression')
    @pytest.mark.regression
    def test_list_verified_users_by_user_admin(self):
        """List verified users by User Admin
        """
        resp = self.user_admin_client_1.list_users(
            option={'user_type': const.VERIFIED, 'email': self.email_test})
        self.assertEqual(resp.status_code, 200)
        # no users are returned from domain2
        self.assertEqual(len(resp.json()[const.USERS]), 0)

    @tags('positive', 'p0', 'regression')
    @pytest.mark.regression
    def test_list_verified_users_by_user_manager(self):
        """List verified users by User Manager
        """
        resp = self.user_manager_client.list_users(
            option={'user_type': const.VERIFIED, 'email': self.email_test})
        self.assertEqual(resp.status_code, 200)
        # no users are returned from domain2
        self.assertEqual(len(resp.json()[const.USERS]), 0)

    @tags('positive', 'p1', 'regression')
    @pytest.mark.regression
    def test_list_verified_users_by_user_default(self):
        """List verified users by default user
        """
        resp = self.user_client.list_users(
            option={'user_type': const.VERIFIED, 'email': self.email_test})
        self.assertEqual(resp.status_code, 403)

        resp = self.user_client.list_users(
            option={'user_type': const.VERIFIED})
        self.assertEqual(resp.status_code, 200)
        for user in resp.json()[const.USERS]:
            self.assertEqual(user[const.RAX_AUTH_DOMAIN_ID],
                             self.domain_id_1)
            self.assertEqual(user[const.USERNAME], self.sub_user_name_1)

    @unless_coverage
    def tearDown(self):
        super(TestListVerifiedUnverifiedUsers, self).tearDown()

    @classmethod
    @unless_coverage
    @base.base.log_tearDown_error
    def tearDownClass(cls):
        # Delete all users created in the setUpClass
        for id in cls.unverified_user_ids:
            cls.identity_admin_client.delete_user(user_id=id)
        for id in cls.sub_user_ids:
            cls.identity_admin_client.delete_user(user_id=id)
        cls.identity_admin_client.delete_user(
            cls.user_client.default_headers[const.X_USER_ID])
        cls.identity_admin_client.delete_user(
            cls.user_manager_client.default_headers[const.X_USER_ID])
        cls.delete_client(client=cls.user_admin_client_1,
                          parent_client=cls.identity_admin_client)
        cls.delete_client(client=cls.user_admin_client_2,
                          parent_client=cls.identity_admin_client)

        super(TestListVerifiedUnverifiedUsers, cls).tearDownClass()
