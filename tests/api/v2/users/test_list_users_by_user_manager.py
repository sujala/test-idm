# -*- coding: utf-8 -*
"""
1. When the consumer of the v2.0 list users service is a user-manager
1.1 List users must not return any user-managers except the caller
1.2 List users (w/ email queryParam) must not return any user-managers
    except the caller
1.3 List users (w/ name queryParam) must return a 403 if the user specified by
    the name is a user-manager and is not the caller
2. This functionality must be wrapped with a feature flag. If the feature flag
    is not present in the configuration file, the default should be to restrict
    user-managers from retrieving other user-managers.
Notes: before change user manager able to list other user managers with the
    same domain
"""
from qe_coverage.opencafe_decorators import tags, unless_coverage

from tests.api.v2 import base
from tests.package.johny import constants as const


class TestListUsersByUserManager(base.TestBaseV2):

    @classmethod
    @unless_coverage
    def setUpClass(cls):
        """
        Create clients with specific info need for tests
        """
        super(TestListUsersByUserManager, cls).setUpClass()
        cls.domain_ids = []
        cls.user_manager_clients = []
        cls.user_admin_clients = []
        cls.DOMAIN_ID_TEST = cls.generate_random_string(
            pattern=const.DOMAIN_PATTERN)
        cls.user_admin_email = 'test_user@test.com'
        cls.user_admin_client = cls.generate_client(
            parent_client=cls.identity_admin_client,
            additional_input_data={'domain_id': cls.DOMAIN_ID_TEST,
                                   'email': cls.user_admin_email})
        cls.domain_ids.append(cls.DOMAIN_ID_TEST)
        cls.user_admin_clients.append(cls.user_admin_client)

        domain_id = cls.generate_random_string(pattern=const.DOMAIN_PATTERN)
        cls.user_admin_client2 = cls.generate_client(
            parent_client=cls.identity_admin_client,
            additional_input_data={'domain_id': domain_id})
        cls.domain_ids.append(domain_id)
        cls.user_admin_clients.append(cls.user_admin_client2)

        # create user manage client these can move to helper func
        cls.user_manager_name = cls.generate_random_string(
            pattern=const.USER_MANAGER_NAME_PATTERN)
        cls.email = 'test@test.com'
        cls.user_manager_client = cls.generate_client(
            parent_client=cls.user_admin_client,
            additional_input_data={
                'user_name': cls.user_manager_name,
                'email': cls.email,
                'is_user_manager': True})
        cls.user_manager_clients.append(cls.user_manager_client)

        # create user manage client these can move to helper func
        cls.user_manager_name2 = cls.generate_random_string(
            pattern=const.USER_MANAGER_NAME_PATTERN)
        cls.user_manager_client2 = cls.generate_client(
            parent_client=cls.user_admin_client,
            additional_input_data={
                'user_name': cls.user_manager_name2,
                'email': cls.email,
                'is_user_manager': True})
        cls.user_manager_clients.append(cls.user_manager_client2)

        # create user manage client these can move to helper func
        cls.user_manager_name3 = cls.generate_random_string(
            pattern=const.USER_MANAGER_NAME_PATTERN)
        cls.email3 = 'test3@test.com'
        cls.user_manager_client3 = cls.generate_client(
            parent_client=cls.user_admin_client2,
            additional_input_data={
                'user_name': cls.user_manager_name3,
                'email': cls.email3,
                'is_user_manager': True})
        cls.user_manager_clients.append(cls.user_manager_client3)

    @unless_coverage
    def setUp(self):
        super(TestListUsersByUserManager, self).setUp()

    @tags('positive', 'p0', 'smoke')
    def test_list_users_by_user_manager(self):
        """List users must not return any user-managers except the caller """

        resp = self.user_manager_client.list_users()
        self.assertEqual(resp.status_code, 200)

        self.assertIn(self.user_manager_name,
                      str(resp.json()[const.USERS]))

        # include user-manage from same domain
        self.assertIn(self.user_manager_name2,
                      str(resp.json()[const.USERS]))

        # exclude user-manage from different domain
        self.assertNotIn(self.user_manager_name3,
                         str(resp.json()[const.USERS]))

    @tags('positive', 'p0', 'smoke')
    def test_list_users_with_name_query_by_user_manager(self):
        """Return only for caller other 403"""

        resp = self.user_manager_client.list_users(
            option={'name': self.user_manager_name})
        self.assertEqual(resp.status_code, 200)
        self.assertEqual(resp.json()[const.USER][const.USERNAME],
                         self.user_manager_name)

        resp2 = self.user_manager_client.list_users(
            option={'name': self.user_manager_name2})
        self.assertEqual(resp2.status_code, 200)
        self.assertEqual(resp2.json()[const.USER][const.USERNAME],
                         self.user_manager_name2)

        # list other user manager name return 403
        resp3 = self.user_manager_client.list_users(
            option={'name': self.user_manager_name3})
        self.assertEqual(resp3.status_code, 403)

    @tags('positive', 'p1', 'regression')
    def test_list_users_with_email_query_by_user_manager(self):
        """List users (w/ email queryParam) must not return any user-managers
        except the caller
        """
        # list with email option user admin email
        resp = self.user_manager_client.list_users(
            option={'email': self.user_admin_email})
        self.assertEqual(resp.status_code, 200)
        # exclude other user managers
        self.assertNotIn(self.user_manager_name,
                         str(resp.json()[const.USERS]))
        self.assertNotIn(self.user_manager_name2,
                         str(resp.json()[const.USERS]))
        self.assertNotIn(self.user_manager_name3,
                         str(resp.json()[const.USERS]))

        # list with self email
        resp2 = self.user_manager_client3.list_users(
            option={'email': self.email3})
        self.assertEqual(resp.status_code, 200)

        # list with same email
        resp3 = self.user_manager_client.list_users(
            option={'email': self.email})
        self.assertEqual(resp3.status_code, 200)

        # list with different user email
        resp4 = self.user_manager_client3.list_users(
            option={'email': self.email})
        self.assertEqual(resp4.status_code, 200)

        # list with self email
        self.assertIn(self.user_manager_name3,
                      str(resp2.json()[const.USERS]))

        # exclude other user managers
        self.assertNotIn(self.user_manager_name,
                         str(resp2.json()[const.USERS]))
        self.assertNotIn(self.user_manager_name2,
                         str(resp2.json()[const.USERS]))

        # list with same email
        self.assertIn(self.user_manager_name,
                      str(resp3.json()[const.USERS]))
        self.assertIn(self.user_manager_name2,
                      str(resp3.json()[const.USERS]))
        # exclude other user managers
        self.assertNotIn(self.user_manager_name3,
                         str(resp3.json()[const.USERS]))

    @unless_coverage
    def tearDown(self):
        super(TestListUsersByUserManager, self).tearDown()

    @classmethod
    @unless_coverage
    def tearDownClass(cls):
        for client in cls.user_manager_clients:
            cls.identity_admin_client.delete_user(
                client.default_headers[const.X_USER_ID])
        for client in cls.user_admin_clients:
            cls.delete_client(client=client,
                              parent_client=cls.identity_admin_client)
        super(TestListUsersByUserManager, cls).tearDownClass()
