# -*- coding: utf-8 -*
from nose.plugins.attrib import attr

from qe_coverage.opencafe_decorators import tags, unless_coverage

from tests.api.utils import func_helper
from tests.api.v2 import base

from tests.package.johny import constants as const
from tests.package.johny.v2.models import requests


class TestListVerifiedUnverifedUsersInDomain(base.TestBaseV2):

    @classmethod
    @unless_coverage
    def setUpClass(cls):
        """Class level set up for the tests

        Create users needed for the tests and generate clients for those users.
        """
        super(TestListVerifiedUnverifedUsersInDomain, cls).setUpClass()
        cls.unverified_user_ids = []
        cls.rcn = cls.test_config.unverified_user_rcn
        # Add Domain w/ RCN
        cls.DOMAIN_ID_TEST = func_helper.generate_randomized_domain_id(
            client=cls.identity_admin_client)
        dom_req = requests.Domain(
            domain_name=cls.DOMAIN_ID_TEST,
            domain_id=cls.DOMAIN_ID_TEST, rcn=cls.rcn)
        add_dom_resp = cls.identity_admin_client.add_domain(dom_req)
        assert add_dom_resp.status_code == 201, (
            'domain was not created successfully')

        cls.user_admin_client = cls.generate_client(
            parent_client=cls.identity_admin_client,
            additional_input_data={'domain_id': cls.DOMAIN_ID_TEST})

        # create unverified user to test
        test_email = cls.generate_random_string(
            pattern=const.UNVERIFIED_EMAIL_PATTERN)
        create_unverified_user_req = requests.UnverifiedUser(
            email=test_email, domain_id=cls.DOMAIN_ID_TEST)
        create_unverified_resp = \
            cls.user_admin_client.create_unverified_user(
                request_object=create_unverified_user_req)
        cls.unverified_user_ids.append(
            create_unverified_resp.json()[const.USER][const.ID])

    @unless_coverage
    def setUp(self):
        super(TestListVerifiedUnverifedUsersInDomain, self).setUp()
        self.new_user_ids = []

    @tags('positive', 'p0', 'smoke')
    @attr(type='smoke_alpha')
    def test_list_all_enabled_users_in_domain_by_identity_admin(self):
        """List by identity admin user and verfiy unverified users are
           not listed"
        """
        resp = self.identity_admin_client.list_users_in_domain(
            self.DOMAIN_ID_TEST,
            option={'user_type': const.ALL,  const.ENABLED: True})

        self.assertEqual(len(resp.json()[const.USERS]), 1)

        self.assertEqual(resp.status_code, 200)
        for user in resp.json()[const.USERS]:
            self.assertEqual(user[const.RAX_AUTH_DOMAIN_ID],
                             self.DOMAIN_ID_TEST)
            self.assertNotIn(user[const.ID], self.unverified_user_ids)

    @tags('positive', 'p0', 'smoke')
    @attr(type='smoke_alpha')
    def test_list_unverified_users_in_domain_by_identity_admin(self):
        """List by identity admin user
        """
        resp = self.identity_admin_client.list_users_in_domain(
            self.DOMAIN_ID_TEST, option={'user_type': const.UNVERIFIED})

        self.assertEqual(len(resp.json()[const.USERS]), 1)

        self.assertEqual(resp.status_code, 200)
        for user in resp.json()[const.USERS]:
            self.assertEqual(user[const.RAX_AUTH_DOMAIN_ID],
                             self.DOMAIN_ID_TEST)
            self.assertIn(user[const.ID], self.unverified_user_ids)
            self.assertEqual(user[const.ENABLED], False)

    @tags('positive', 'p0', 'smoke')
    @attr(type='smoke_alpha')
    def test_list_verified_users_in_domain_by_identity_admin(self):
        """List by identity admin user and verify users on different
        domain are not listed
        """
        # create user admin in a different domain to test
        user_name = self.generate_random_string()
        domain_id = func_helper.generate_randomized_domain_id(
            client=self.identity_admin_client)
        request_input = requests.UserAdd(
            user_name=user_name, domain_id=domain_id)
        resp = self.identity_admin_client.add_user(
            request_object=request_input)
        new_user_id = resp.json()[const.USER][const.ID]
        self.new_user_ids.append(new_user_id)

        resp = self.identity_admin_client.list_users_in_domain(
            self.DOMAIN_ID_TEST, option={'user_type': const.VERIFIED})

        self.assertEqual(len(resp.json()[const.USERS]), 1)

        self.assertEqual(resp.status_code, 200)
        user_ids = []
        for user in resp.json()[const.USERS]:
            user_ids.append(user[const.ID])
            self.assertEqual(user[const.RAX_AUTH_DOMAIN_ID],
                             self.DOMAIN_ID_TEST)

        self.assertNotIn(new_user_id, user_ids)

    @tags('positive', 'p0', 'regression')
    @attr(type='regression')
    def test_list_all_users_in_domain_by_user_admin(self):
        """List by user-admin
        """
        resp = self.user_admin_client.list_users_in_domain(
                    self.DOMAIN_ID_TEST, option={'user_type': const.ALL})

        self.assertEqual(resp.status_code, 403)

    @unless_coverage
    @base.base.log_tearDown_error
    def tearDown(self):
        # Delete all users created in the tests
        for id in self.new_user_ids:
            self.identity_admin_client.delete_user(user_id=id)
        super(TestListVerifiedUnverifedUsersInDomain, self).tearDown()

    @classmethod
    @unless_coverage
    @base.base.log_tearDown_error
    def tearDownClass(cls):
        # Delete all users created in the setUpClass
        for id in cls.unverified_user_ids:
            cls.identity_admin_client.delete_user(user_id=id)
        cls.delete_client(client=cls.user_admin_client,
                          parent_client=cls.identity_admin_client)
        super(TestListVerifiedUnverifedUsersInDomain, cls).tearDownClass()
