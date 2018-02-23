import copy
from random import randrange

import ddt

from tests.api.v2 import base
from tests.api.v2.schema import users as users_json

from tests.package.johny import constants as const
from tests.package.johny.v2.models import requests


@ddt.ddt
class TestUpdateUser(base.TestBaseV2):

    """Update User Tests
    Update user_admin, sub_user, user_manage level users."""

    @classmethod
    def setUpClass(cls):
        """Class level set up for the tests

        Create users needed for the tests and generate clients for those users.
        """
        super(TestUpdateUser, cls).setUpClass()

        domain_id = cls.generate_random_string(pattern='[\d]{7}')
        cls.user_admin_client = cls.generate_client(
            parent_client=cls.identity_admin_client,
            additional_input_data={'domain_id': domain_id})

        sub_user_name = cls.generate_random_string(
            pattern=const.SUB_USER_PATTERN)
        cls.user_client = cls.generate_client(
            parent_client=cls.user_admin_client,
            additional_input_data={
                'domain_id': domain_id,
                'user_name': sub_user_name})

        contact_id = randrange(start=const.CONTACT_ID_MIN,
                               stop=const.CONTACT_ID_MAX)

        # these client will be updated by the test
        test_domain_id = cls.generate_random_string(
            pattern='[\d]{7}')
        cls.test_identity_admin_client = cls.generate_client(
            parent_client=cls.service_admin_client,
            additional_input_data={'domain_id': test_domain_id})

        cls.test_user_admin_client = cls.generate_client(
            parent_client=cls.identity_admin_client,
            additional_input_data={'domain_id': test_domain_id,
                                   'contact_id': contact_id})

        sub_user_name = cls.generate_random_string(
            pattern=const.SUB_USER_PATTERN)
        cls.test_user_client = cls.generate_client(
            parent_client=cls.user_admin_client,
            additional_input_data={
                'domain_id': test_domain_id,
                'user_name': sub_user_name})

    def setUp(self):
        super(TestUpdateUser, self).setUp()
        self.user_ids = []
        self.sub_user_ids = []

    def create_identity_admin(self):

        # create identity admin user to test
        ida_username = "iadm_" + self.generate_random_string()
        ida_domain_id = self.generate_random_string(pattern='[\d]{7}')
        request_object = requests.UserAdd(ida_username,
                                          domain_id=ida_domain_id)
        resp = self.service_admin_client.add_user(
            request_object=request_object)
        test_identity_adm_id = resp.json()[const.USER][const.ID]
        self.user_ids.append(test_identity_adm_id)
        return test_identity_adm_id

    def create_user_admin(self):

        # create user admin to test
        user_name = self.generate_random_string()
        domain_id = self.generate_random_string(pattern='[\d]{7}')
        request_object = requests.UserAdd(user_name,
                                          domain_id=domain_id)
        resp = self.identity_admin_client.add_user(
            request_object=request_object)
        test_user_admin_id = resp.json()[const.USER][const.ID]
        self.user_ids.append(test_user_admin_id)
        return test_user_admin_id

    def create_default_user(self):

        # create sub user to test
        sub_username = "sub_" + self.generate_random_string()
        request_object = requests.UserAdd(sub_username)
        resp = self.user_admin_client.add_user(request_object=request_object)
        test_sub_user_id = resp.json()[const.USER][const.ID]
        self.sub_user_ids.append(test_sub_user_id)
        return test_sub_user_id

    @ddt.file_data('data_update_user_info.json')
    def test_update_identity_admin_user_info(self, test_data):
        """Update identity admin user

        test_data comes from a json data file that can contain various possible
        input combinations. Each of these data combination is a separate test
        case.
        """
        user_id = self.create_identity_admin()
        update_data = test_data['update_input']
        request_object = requests.UserUpdate(**update_data)
        resp = self.service_admin_client.update_user(
            user_id=user_id, request_object=request_object
        )
        self.assertEqual(resp.status_code, 200)
        updated_json_schema = copy.deepcopy(users_json.update_user)
        updated_json_schema[const.PROPERTIES][const.USER][const.REQUIRED] = (
            users_json.update_user[const.PROPERTIES][const.USER][
                const.REQUIRED] + test_data['update_schema_fields'])
        self.assertSchema(response=resp, json_schema=updated_json_schema)

    @ddt.file_data('data_update_user_info.json')
    def test_update_user_admin_user_set_mfa_attrs(self, test_data):
        """Update admin user

        test_data comes from a json data file that can contain various possible
        input combinations. Each of these data combination is a separate test
        case.
        """
        user_id = self.create_user_admin()
        update_data = test_data['update_input']
        request_object = requests.UserUpdate(**update_data)
        resp = self.identity_admin_client.update_user(
            user_id=user_id, request_object=request_object
        )
        self.assertEqual(resp.status_code, 200)
        updated_json_schema = copy.deepcopy(users_json.update_user)
        updated_json_schema[const.PROPERTIES][const.USER][const.REQUIRED] = (
            users_json.update_user[const.PROPERTIES][const.USER][
                const.REQUIRED] + test_data['update_schema_fields'])
        self.assertSchema(response=resp, json_schema=updated_json_schema)

    @ddt.file_data('data_update_user_info.json')
    def test_update_user_default_user_set_mfa_attrs(self, test_data):
        """Update sub user

        test_data comes from a json data file that can contain various possible
        input combinations. Each of these data combination is a separate test
        case.
        """
        user_id = self.create_default_user()
        update_data = test_data['update_input']
        request_object = requests.UserUpdate(**update_data)
        resp = self.user_admin_client.update_user(
            user_id=user_id, request_object=request_object
        )
        self.assertEqual(resp.status_code, 200)
        updated_json_schema = copy.deepcopy(users_json.update_user)
        updated_json_schema[const.PROPERTIES][const.USER][const.REQUIRED] = (
            users_json.update_user[const.PROPERTIES][const.USER][
                const.REQUIRED] + test_data['update_schema_fields'])
        self.assertSchema(response=resp, json_schema=updated_json_schema)

    @ddt.file_data('data_update_user_info.json')
    def test_update_identity_admin_with_its_token(self, test_data):
        """
        Test update user admin with its own token
         expect 200 in response
        :return:
        """
        user_id = self.test_identity_admin_client.default_headers['X-User-Id']
        update_data = test_data['update_input']
        # avoid update password
        if 'password' not in update_data:
            request_object = requests.UserUpdate(**update_data)
            resp = self.test_identity_admin_client.update_user(
                user_id=user_id, request_object=request_object)
            if 'enabled' in update_data:
                self.assertEqual(resp.status_code, 400)
                self.assertEqual(
                    resp.json()['badRequest']['message'],
                    'User cannot enable/disable his/her own account.')
            else:
                self.assertEqual(resp.status_code, 200)

    @ddt.file_data('data_update_user_info.json')
    def test_update_user_admin_with_its_token(self, test_data):
        """
        Test update user admin with its own token
         expect 200 in response
        :return:
        """
        user_id = self.test_user_admin_client.default_headers['X-User-Id']
        update_data = test_data['update_input']
        # avoid update password
        if 'password' not in update_data:
            request_object = requests.UserUpdate(**update_data)
            resp = self.test_user_admin_client.update_user(
                user_id=user_id, request_object=request_object)
            if 'enabled' in update_data:
                self.assertEqual(resp.status_code, 400)
                self.assertEqual(
                    resp.json()['badRequest']['message'],
                    'User cannot enable/disable his/her own account.')
            else:
                self.assertEqual(resp.status_code, 200)

    @ddt.file_data('data_update_user_info.json')
    def test_update_sub_user_with_its_token(self, test_data):
        """
        Test update sub user with its own token
         expect 200 in response
        :return:
        """
        user_id = self.test_user_client.default_headers['X-User-Id']
        update_data = test_data['update_input']
        # avoid update password
        if 'password' not in update_data:
            request_object = requests.UserUpdate(**update_data)
            resp = self.test_user_client.update_user(
                user_id=user_id, request_object=request_object)
            if 'enabled' in update_data:
                self.assertEqual(resp.status_code, 400)
                self.assertEqual(
                    resp.json()['badRequest']['message'],
                    'User cannot enable/disable his/her own account.')
            else:
                self.assertEqual(resp.status_code, 200)

    @ddt.file_data('data_update_user_multi_attrs.json')
    def test_update_identity_admin_user_multi_info_mfa_attrs(self, test_data):
        """Update identity admin user

        test_data comes from a json data file that can contain various possible
        input combinations. Each of these data combination is a separate test
        case.
        """
        user_id = self.create_identity_admin()
        update_data = test_data['update_input']
        data_list = self.generate_data_combinations(start=2,
                                                    data_dict=update_data)
        for ea_data_dict in data_list:
            request_object = requests.UserUpdate(**ea_data_dict)
            resp = self.service_admin_client.update_user(
                user_id=user_id, request_object=request_object
            )
            self.assertEqual(resp.status_code, 200,
                             "failed to update with {}".format(ea_data_dict))
            updated_json_schema = copy.deepcopy(users_json.update_user)
            updated_json_schema[const.PROPERTIES][const.USER][
                const.REQUIRED] = (
                users_json.update_user[const.PROPERTIES][const.USER][
                    const.REQUIRED] + test_data['update_schema_fields'])
            if 'email' in ea_data_dict:
                updated_json_schema[const.PROPERTIES][const.USER][
                    const.REQUIRED] = (
                    users_json.update_user[const.PROPERTIES][const.USER][
                        const.REQUIRED] + ['email'])
            if 'contact_id' in ea_data_dict:
                updated_json_schema[const.PROPERTIES][const.USER][
                    const.REQUIRED] = (
                    users_json.update_user[const.PROPERTIES][const.USER][
                        const.REQUIRED] + ['RAX-AUTH:contactId'])
                iac = self.identity_admin_client
                t_id = iac.default_headers[const.X_AUTH_TOKEN]
                validate_resp = self.identity_admin_client.validate_token(
                    token_id=t_id)
                print(validate_resp)
            self.assertSchema(response=resp, json_schema=updated_json_schema)

    @ddt.file_data('data_update_user_multi_attrs.json')
    def test_update_admin_user_multi_info_n_mfa_attrs(self, test_data):
        """Update identity admin user

        test_data comes from a json data file that can contain various possible
        input combinations. Each of these data combination is a separate test
        case.
        """
        user_id = self.create_user_admin()
        update_data = test_data['update_input']
        data_list = self.generate_data_combinations(start=2,
                                                    data_dict=update_data)
        for ea_data_dict in data_list:
            request_object = requests.UserUpdate(**ea_data_dict)
            resp = self.identity_admin_client.update_user(
                user_id=user_id, request_object=request_object
            )
            self.assertEqual(resp.status_code, 200,
                             "failed to update with {}".format(ea_data_dict))
            updated_json_schema = copy.deepcopy(users_json.update_user)
            updated_json_schema[const.PROPERTIES][const.USER][
                const.REQUIRED] = (
                users_json.update_user[const.PROPERTIES][const.USER][
                    const.REQUIRED] + test_data['update_schema_fields'])
            if 'email' in ea_data_dict:
                updated_json_schema[const.PROPERTIES][const.USER][
                    const.REQUIRED] = (
                    users_json.update_user[const.PROPERTIES][const.USER][
                        const.REQUIRED] + ['email'])
            if 'contact_id' in ea_data_dict:
                updated_json_schema[const.PROPERTIES][const.USER][
                    const.REQUIRED] = (
                    users_json.update_user[const.PROPERTIES][const.USER][
                        const.REQUIRED] + ['RAX-AUTH:contactId'])
            self.assertSchema(response=resp, json_schema=updated_json_schema)

    @ddt.file_data('data_update_user_multi_attrs.json')
    def test_update_default_user_multi_info_n_mfa_attrs(self, test_data):
        """Update identity admin user

        test_data comes from a json data file that can contain various possible
        input combinations. Each of these data combination is a separate test
        case.
        """
        user_id = self.create_default_user()
        update_data = test_data['update_input']
        data_list = self.generate_data_combinations(start=2,
                                                    data_dict=update_data)
        for ea_data_dict in data_list:
            request_object = requests.UserUpdate(**ea_data_dict)
            resp = self.user_admin_client.update_user(
                user_id=user_id, request_object=request_object
            )
            self.assertEqual(resp.status_code, 200,
                             "failed to update with {}".format(ea_data_dict))
            updated_json_schema = copy.deepcopy(users_json.update_user)
            updated_json_schema[const.PROPERTIES][const.USER][
                const.REQUIRED] = (
                users_json.update_user[const.PROPERTIES][const.USER][
                    const.REQUIRED] + test_data['update_schema_fields'])
            if 'email' in ea_data_dict:
                updated_json_schema[const.PROPERTIES][const.USER][
                    const.REQUIRED] = (
                    users_json.update_user[const.PROPERTIES][const.USER][
                        const.REQUIRED] + ['email'])
            self.assertSchema(response=resp, json_schema=updated_json_schema)

    @ddt.file_data('data_update_user_info_neg.json')
    def test_update_identity_admin_user_neg(self, test_data):
        """
        Test with invalid data form json data file
        :param test_data:
        :return:
        """
        user_id = self.create_identity_admin()
        update_data = test_data['update_input']
        request_object = requests.UserUpdate(**update_data)
        resp = self.service_admin_client.update_user(
            user_id=user_id, request_object=request_object
        )
        self.assertEqual(resp.status_code, test_data['response_status'])

    @ddt.file_data('data_update_user_info_neg.json')
    def test_update_user_admin_user_neg(self, test_data):
        """
        Test with invalid data form json data file
        :param test_data:
        :return:
        """
        user_id = self.create_user_admin()
        update_data = test_data['update_input']
        request_object = requests.UserUpdate(**update_data)
        resp = self.identity_admin_client.update_user(
            user_id=user_id, request_object=request_object
        )
        self.assertEqual(resp.status_code, test_data['response_status'])

    @ddt.file_data('data_update_user_info_neg.json')
    def test_update_default_user_neg(self, test_data):
        """
        Test with invalid data form json data file
        :param test_data:
        :return:
        """
        user_id = self.create_default_user()
        update_data = test_data['update_input']
        request_object = requests.UserUpdate(**update_data)
        resp = self.user_admin_client.update_user(
            user_id=user_id, request_object=request_object
        )
        self.assertEqual(resp.status_code, test_data['response_status'])

    @ddt.data('identity_admin_client', 'user_admin_client', 'user_client')
    def test_identity_admin_update_permission(self, access_level):
        """
        Test update identity admin without permission
         identity admin can't be update using itself token or lower level token
        :return:
        """
        user_id = self.create_identity_admin()
        request_object = requests.UserUpdate(
            email='test_update_email@rackspace.com')
        if access_level == 'identity_admin_client':
            resp = self.identity_admin_client.update_user(
                user_id=user_id, request_object=request_object
            )
        elif access_level == 'user_admin_client':
            resp = self.user_admin_client.update_user(
                user_id=user_id, request_object=request_object
            )
        elif access_level == 'user_client':
            resp = self.user_client.update_user(
                user_id=user_id, request_object=request_object
            )
        self.assertEqual(resp.status_code, 403)

    @ddt.data('user_admin_client', 'user_client')
    def test_user_admin_update_permission(self, access_level):
        """
        Test update user admin without permission
         user admin can't be update using other user admin token or
         defaul user token
        :return:
        """
        user_id = self.create_user_admin()
        request_object = requests.UserUpdate(
            email='test_update_email@rackspace.com')
        if access_level == 'user_admin_client':
            resp = self.user_admin_client.update_user(
                user_id=user_id, request_object=request_object
            )
        elif access_level == 'user_client':
            resp = self.user_client.update_user(
                user_id=user_id, request_object=request_object
            )
        self.assertEqual(resp.status_code, 403)

    def test_user_default_update_permission(self):
        """
        Test update user default without permission
         user default can't be updated using another default user's token
        :return:
        """
        user_id = self.create_default_user()
        request_object = requests.UserUpdate(
            email='test_update_email@rackspace.com')
        resp = self.user_client.update_user(
            user_id=user_id, request_object=request_object
        )
        self.assertEqual(resp.status_code, 403)

    def tearDown(self):
        # Delete all users created in the tests
        for id in self.sub_user_ids:
            self.identity_admin_client.delete_user(user_id=id)
        for id in self.user_ids:
            self.service_admin_client.delete_user(user_id=id)
        super(TestUpdateUser, self).tearDown()

    @classmethod
    def tearDownClass(cls):
        # Delete all users created in the setUpClass
        cls.identity_admin_client.delete_user(
            cls.user_client.default_headers[const.X_USER_ID])
        cls.identity_admin_client.delete_user(
            cls.test_user_client.default_headers[const.X_USER_ID])
        cls.service_admin_client.delete_user(
            cls.test_identity_admin_client.default_headers[const.X_USER_ID])
        cls.delete_client(client=cls.test_user_admin_client,
                          parent_client=cls.identity_admin_client)
        cls.delete_client(client=cls.user_admin_client,
                          parent_client=cls.identity_admin_client)
        super(TestUpdateUser, cls).tearDownClass()
