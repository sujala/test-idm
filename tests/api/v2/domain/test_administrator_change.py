# -*- coding: utf-8 -*

from tests.api.utils import func_helper
from tests.api.v2 import base

from tests.package.johny import constants as const
from tests.package.johny.v2.models import requests


class TestAdministratorChange(base.TestBaseV2):
    """
    Tests AdministratorChange call
    """

    @classmethod
    def setUpClass(cls):
        """Class level set up for the tests

        Create users needed for the tests and generate clients for those users.
        """
        super(TestAdministratorChange, cls).setUpClass()

        # cls.test_domain_id = cls.generate_random_string(
        #     pattern='[\d]{7}')
        cls.test_domain_id = func_helper.generate_randomized_domain_id(
            client=cls.identity_admin_client)
        cls.test_user_admin_name = cls.generate_random_string(
            pattern=const.USER_NAME_PATTERN
        )
        cls.test_user_admin_client = cls.generate_client(
            parent_client=cls.identity_admin_client,
            additional_input_data={
                'domain_id': cls.test_domain_id,
                'user_name': cls.test_user_admin_name
            })
        cls.test_user_adm_token = cls.test_user_admin_client.default_headers[
            const.X_AUTH_TOKEN]

    def setUp(self):
        super(TestAdministratorChange, self).setUp()
        self.sub_user_ids = []

        # create sub user to test
        self.test_user_manage_name = "sub_" + self.generate_random_string()
        request_object = requests.UserAdd(self.test_user_manage_name,
                                          domain_id=self.test_domain_id)
        resp = self.test_user_admin_client.add_user(
            request_object=request_object)
        self.test_user_manager_id = resp.json()[const.USER][const.ID]
        self.test_user_manager_pass = resp.json()[
            const.USER][const.OS_KSADM_PASSWORD]
        self.sub_user_ids.append(resp.json()[const.USER][const.ID])

        self.test_user_admin_client.add_role_to_user(
            const.USER_MANAGER_ROLE_ID, self.test_user_manager_id)

    def test_admin_swap(self):
        request_object = requests.DomainAdministratorChange(
            self.test_user_manager_id,
            self.test_user_admin_client.default_headers[const.X_USER_ID]
        )

        # promote user manage to user admin
        resp = self.identity_admin_client.change_administrators(
            domain_id=self.test_domain_id, request_object=request_object)
        self.assertEqual(resp.status_code, 204)

        # validate promoted user now has admin role and does not have
        # user-manage
        auth_obj = requests.AuthenticateWithPassword(
            user_name=self.test_user_manage_name,
            password=self.test_user_manager_pass
        )
        auth = self.identity_admin_client.get_auth_token(
            request_object=auth_obj)
        self.assertEqual(auth.status_code, 200)
        self.assertIn(const.USER_ADMIN_ROLE_NAME,
                      str(auth.json()[const.ACCESS][const.USER][
                              const.ROLES]))
        self.assertNotIn(const.USER_MANAGE_ROLE_NAME,
                         str(auth.json()[const.ACCESS][const.USER][
                                 const.ROLES]))
        self.assertNotIn(const.USER_DEFAULT_ROLE_NAME,
                         str(auth.json()[const.ACCESS][const.USER][
                                 const.ROLES]))

        # validate demoted user now does not have admin role
        auth = self.identity_admin_client.validate_token(
            token_id=self.test_user_adm_token
        )
        self.assertEqual(auth.status_code, 200)
        self.assertIn(const.USER_DEFAULT_ROLE_NAME,
                      str(auth.json()[const.ACCESS][const.USER][
                              const.ROLES]))
        self.assertNotIn(const.USER_ADMIN_ROLE_NAME,
                         str(auth.json()[const.ACCESS][const.USER][
                                 const.ROLES]))
        self.assertNotIn(const.USER_MANAGE_ROLE_NAME,
                         str(auth.json()[const.ACCESS][const.USER][
                                 const.ROLES]))

    @base.base.log_tearDown_error
    def tearDown(self):
        # Delete all users created in the tests
        resp = self.identity_admin_client.delete_user(
            self.test_user_admin_client.default_headers[const.X_USER_ID])
        self.assertEqual(
            resp.status_code, 204,
            msg='User with ID {0} failed to delete'.format(
                self.test_user_admin_client.default_headers[const.X_USER_ID]))

        for id_ in self.sub_user_ids:
            resp = self.identity_admin_client.delete_user(user_id=id_)
            self.assertEqual(
                resp.status_code, 204,
                msg='User with ID {0} failed to delete'.format(id_))

        disable_domain_req = requests.Domain(enabled=False)
        resp = self.identity_admin_client.update_domain(
            domain_id=self.test_domain_id, request_object=disable_domain_req)

        resp = self.identity_admin_client.delete_domain(
            domain_id=self.test_domain_id)
        self.assertEqual(
            resp.status_code, 204,
            msg='Domain with ID {0} failed to delete'.format(
                self.test_domain_id))
        super(TestAdministratorChange, self).tearDown()

    @classmethod
    def tearDownClass(cls):
        super(TestAdministratorChange, cls).tearDownClass()
