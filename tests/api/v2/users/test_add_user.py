# -*- coding: utf-8 -*
import copy

import ddt
from hypothesis import given, strategies

from tests.api.utils import header_validation
from tests.api.v2 import base
from tests.api.v2.models import requests
from tests.api.v2.schema import users as users_json
from tests.api import constants as const


@ddt.ddt
class TestAddUser(base.TestBaseV2):

    """Add User Tests
    Adds user_admin, sub_user, user_manage level users."""

    @classmethod
    def setUpClass(cls):
        """Class level set up for the tests

        Create users needed for the tests and generate clients for those users.
        """
        super(TestAddUser, cls).setUpClass()

        cls.user_admin_client = cls.generate_client(
            parent_client=cls.identity_admin_client,
            additional_input_data={'domain_id': const.DOMAIN_API_TEST})

        sub_user_name = cls.generate_random_string(
            pattern=const.SUB_USER_PATTERN)
        cls.user_client = cls.generate_client(
            parent_client=cls.user_admin_client,
            additional_input_data={
                'domain_id': const.DOMAIN_API_TEST,
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
        super(TestAddUser, self).setUp()
        self.user_ids = []
        self.add_input = {'domain_id': const.DOMAIN_TEST}
        self.add_schema_fields = [const.NS_PASSWORD]

    @ddt.file_data('data_add_user_admin_user.json')
    def test_add_user_admin_user(self, test_data):
        '''Add user_admin type users

        test_data comes from a json data file that can contain various possible
        input combinations. Each of these data combination is a separate test
        case.
        NOTE: This test case illustrates providing test_data in a json file.
        @todo: The test_data file needs to be updated to include all possible
        data combinations.
        '''
        user_name = self.generate_random_string()
        input_data = test_data['additional_input']
        request_object = requests.UserAdd(user_name=user_name, **input_data)
        resp = self.identity_admin_client.add_user(request_object)
        self.assertEqual(resp.status_code, 201)
        self.user_ids.append(resp.json()[const.USER][const.ID])

        updated_json_schema = copy.deepcopy(users_json.add_user)
        updated_json_schema[const.PROPERTIES][const.USER][const.REQUIRED] = (
            users_json.add_user[const.PROPERTIES][const.USER][const.REQUIRED] +
            test_data['additional_schema_fields'])

        self.assertSchema(response=resp, json_schema=updated_json_schema)

        self.assertHeaders(
            resp, *self.header_validation_functions_HTTP_201)

    @ddt.file_data('data_add_user_w_mfa_attrs.json')
    def test_add_user_admin_user_w_mfa_attrs(self, test_data):
        '''Add user_admin type users

        test_data comes from a json data file that can contain various possible
        input combinations. Each of these data combination is a separate test
        case.
        NOTE: This test case illustrates providing test_data in a json file.
        '''
        mfa_input = test_data['mfa_input']
        data_list = self.generate_data_combinations(data_dict=mfa_input)
        # run multiple combination of mfa input attributes
        for ea_mfa_input in data_list:
            user_name = self.generate_random_string()
            add_input = self.add_input
            input_data = dict(add_input, **ea_mfa_input)
            request_object = requests.UserAdd(user_name=user_name,
                                              **input_data)
            resp = self.identity_admin_client.add_user(request_object)
            self.assertEqual(resp.status_code, 201,
                             "Fail to create user with {0}".format(input_data))
            self.user_ids.append(resp.json()[const.USER]['id'])

            updated_json_schema = copy.deepcopy(users_json.add_user)
            updated_json_schema[const.PROPERTIES][const.USER][
                const.REQUIRED] = (
                users_json.add_user[const.PROPERTIES][const.USER][
                    const.REQUIRED] + self.add_schema_fields)

            self.assertEqual(resp.json()[const.USER][
                                 const.RAX_AUTH_MULTI_FACTOR_ENABLED], False)
            self.assertSchema(response=resp, json_schema=updated_json_schema)
            self.assertHeaders(response=resp)

    @ddt.file_data('data_add_user_w_mfa_attrs.json')
    def test_add_user_default_user_w_mfa_attrs(self, test_data):
        '''Add user_defaut type users

        test_data comes from a json data file that can contain various possible
        input combinations. Each of these data combination is a separate test
        case.
        NOTE: This test case illustrates providing test_data in a json file.
        '''
        mfa_input = test_data['mfa_input']
        data_list = self.generate_data_combinations(data_dict=mfa_input)
        # run multiple combination of mfa input attributes
        for ea_mfa_input in data_list:
            add_input = self.add_input
            input_data = dict(add_input, **ea_mfa_input)
            user_name = self.generate_random_string(
                pattern=const.SUB_USER_PATTERN)
            request_object = requests.UserAdd(
                user_name=user_name,
                **input_data
            )
            resp = self.user_admin_client.add_user(request_object)
            self.assertEqual(resp.status_code, 201,
                             "Fail to create user with {0}".format(input_data))
            self.user_ids.append(resp.json()[const.USER][const.ID])

            updated_json_schema = copy.deepcopy(users_json.add_user)
            updated_json_schema[const.PROPERTIES][const.USER][
                const.REQUIRED] = (
                users_json.add_user[const.PROPERTIES][const.USER][
                    const.REQUIRED] + self.add_schema_fields)

            self.assertEqual(resp.json()[const.USER][
                                 const.RAX_AUTH_MULTI_FACTOR_ENABLED], False)
            self.assertSchema(response=resp, json_schema=updated_json_schema)
            self.assertHeaders(response=resp)

    @ddt.data(True, False, '')
    def test_add_user(self, enabled):
        """Add user_admin type users

        various possible test_data for the enabled field is defined in the line
        above the test method definition.Each of these data is run as a
        separate test case. This test case currently exists for
        illustrating a different method of providing test data.

        @todo: This can be merged into the previous test_case
        'test_add_user_admin_user'.
        """
        user_name = self.generate_random_string()
        password = self.generate_random_string(pattern=const.PASSWORD_PATTERN)
        request_object = requests.UserAdd(
            user_name=user_name,
            password=password,
            enabled=enabled,
            domain_id=const.DOMAIN_TEST
        )
        resp = self.identity_admin_client.add_user(request_object)
        self.assertEqual(resp.status_code, 201)
        self.user_ids.append(resp.json()[const.USER][const.ID])

        self.assertSchema(response=resp, json_schema=users_json.add_user)
        self.assertHeaders(
            resp, *self.header_validation_functions_HTTP_201)

    @ddt.data(['  ', 'me@mail.com', True], ['വളം', 'me@mail.com', True],
              ['first last', 'valid@email.com', ''])
    @ddt.unpack
    def test_add_user_ddt(self, user_name, email_id, enabled):
        """Add user_admin type users

        various possible test_data for the enabled field is defined in the line
        above the test method definition.Each of these data is run as a
        separate test case. This test case currently exists for
        illustrating a different method of providing test data.

        @todo: This can be merged into the previous test_case
        'test_add_user_admin_user'."""
        request_object = requests.UserAdd(
            user_name=user_name,
            email=email_id,
            enabled=enabled,
            domain_id=const.DOMAIN_TEST
        )
        resp = self.identity_admin_client.add_user(request_object)
        self.assertEqual(resp.status_code, 400)

        # This fails on 'Connection' header being absent when run
        # in docker environment but passes in Staging
        self.assertHeaders(
            resp, *self.header_validation_functions_HTTP_400)

    @given(strategies.text(), strategies.booleans())
    def test_add_user_hypothesis(self, user_name, enabled):
        """Property Based Testing

        Generate possible test inputs based on the definition of the input
        fields.
        """
        if not self.test_config.run_hypothesis_tests:
            self.skipTest('Skipping Hypothesis tests per config value')
        request_object = requests.UserAdd(
            user_name=user_name,
            email=const.EMAIL_RANDOM,
            enabled=enabled
        )
        resp = self.user_admin_client.add_user(request_object)
        self.assertEqual(resp.status_code, 400)
        self.assertHeaders(
            resp, *self.header_validation_functions_HTTP_400)

    def tearDown(self):
        # Delete all users created in the tests
        for id in self.user_ids:
            resp = self.identity_admin_client.delete_user(user_id=id)
            self.assertEqual(resp.status_code, 204)
        super(TestAddUser, self).tearDown()

    @classmethod
    def tearDownClass(cls):
        # Delete all users created in the setUpClass
        cls.delete_client(client=cls.user_client,
                          parent_client=cls.user_admin_client)
        cls.delete_client(client=cls.user_admin_client,
                          parent_client=cls.identity_admin_client)
        super(TestAddUser, cls).tearDownClass()


@ddt.ddt
class TestServiceAdminLevelAddUser(base.TestBaseV2):

    """Add User Tests

    Adds identity_admin users using a service admin level user.
    All tests in this class require a service admin level user credentials.
    """

    def setUp(self):
        super(TestServiceAdminLevelAddUser, self).setUp()
        if not self.test_config.run_service_admin_tests:
            self.skipTest('Skipping Service Admin Tests per config value')
        self.user_ids = []
        self.unexpected_headers_HTTP_201 = [
            header_validation.validate_transfer_encoding_header_not_present]
        self.header_validation_functions_HTTP_201 = (
            self.default_header_validations +
            self.unexpected_headers_HTTP_201 + [
                header_validation.validate_header_location,
                header_validation.validate_header_content_length])
        self.add_input = {'domain_id': const.DOMAIN_TEST}
        self.add_schema_fields = [const.NS_PASSWORD]

    @ddt.file_data('data_add_identity_admin_user.json')
    def test_add_identity_admin_user(self, test_data):
        """Add identity admin level users

        Add identity admin level users with various test input combinations
        defined in the test data json file.
        """
        user_name = self.generate_random_string()
        input_data = test_data['additional_input']
        request_object = requests.UserAdd(
            user_name=user_name,
            **input_data
        )
        resp = self.service_admin_client.add_user(request_object)
        self.assertEqual(resp.status_code, 201)
        self.user_ids.append(resp.json()[const.USER][const.ID])

        updated_json_schema = copy.deepcopy(users_json.add_user)
        updated_json_schema[const.PROPERTIES][const.USER][const.REQUIRED] = (
            users_json.add_user[const.PROPERTIES][const.USER][const.REQUIRED] +
            test_data['additional_schema_fields'])
        self.assertSchema(response=resp, json_schema=updated_json_schema)

        self.assertHeaders(
            resp, *self.header_validation_functions_HTTP_201)

    @ddt.file_data('data_add_user_w_mfa_attrs.json')
    def test_add_user_identity_admin_w_mfa_attrs(self, test_data):
        '''Add identity_admin type users

        test_data comes from a json data file that can contain various possible
        input combinations. Each of these data combination is a separate test
        case.
        NOTE: This test case illustrates providing test_data in a json file.
        '''
        mfa_input = test_data['mfa_input']
        data_list = self.generate_data_combinations(data_dict=mfa_input)
        # run multiple combination of mfa input attributes
        for ea_mfa_input in data_list:
            user_name = self.generate_random_string()
            add_input = self.add_input
            input_data = dict(add_input, **ea_mfa_input)
            request_object = requests.UserAdd(
                user_name=user_name,
                **input_data
            )
            resp = self.service_admin_client.add_user(request_object)
            self.assertEqual(resp.status_code, 201,
                             "Fail to create user with {0}".format(input_data))
            self.user_ids.append(resp.json()[const.USER][const.ID])

            updated_json_schema = copy.deepcopy(users_json.add_user)
            updated_json_schema[const.PROPERTIES][const.USER][
                const.REQUIRED] = (
                users_json.add_user[const.PROPERTIES][const.USER][
                    const.REQUIRED] + self.add_schema_fields)

            self.assertEqual(resp.json()[const.USER][
                                 const.RAX_AUTH_MULTI_FACTOR_ENABLED], False)
            self.assertSchema(response=resp, json_schema=updated_json_schema)
            self.assertHeaders(response=resp)

    def tearDown(self):
        # Delete all users created in the tests
        for id in self.user_ids:
            resp = self.service_admin_client.delete_user(user_id=id)
            self.assertEqual(resp.status_code, 204)
        super(TestServiceAdminLevelAddUser, self).tearDown()
