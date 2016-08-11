# -*- coding: utf-8 -*
import copy

import ddt
from hypothesis import given, strategies

from tests.api.utils import header_validation
from tests.api.v2 import base
from tests.api.v2.schema import users as users_json


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
            additional_input_data={'domain_id': 'api-test'})

        sub_user_name = cls.generate_random_string(
            pattern='sub[\-]user[\d\w]{12}')
        cls.user_client = cls.generate_client(
            parent_client=cls.user_admin_client,
            additional_input_data={
                'domain_id': 'api-test',
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
        self.user_id = []

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
        resp = self.identity_admin_client.add_user(
            user_name=user_name, **input_data)
        self.assertEqual(resp.status_code, 201)

        updated_json_schema = copy.deepcopy(users_json.add_user)
        updated_json_schema['properties']['user']['required'] = (
            users_json.add_user['properties']['user']['required'] +
            test_data['addional_schema_fields'])

        self.assertSchema(response=resp, json_schema=updated_json_schema)

        self.assertHeaders(
            resp, *self.header_validation_functions_HTTP_201)
        self.user_id.append(resp.json()['user']['id'])

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
        password = self.generate_random_string(pattern='Password1[\d\w]{10}')
        resp = self.identity_admin_client.add_user(
            user_name=user_name,
            password=password,
            enabled=enabled,
            domain_id='random')
        self.assertEqual(resp.status_code, 201)
        self.assertSchema(response=resp, json_schema=users_json.add_user)
        self.assertHeaders(
            resp, *self.header_validation_functions_HTTP_201)

        self.user_id.append(resp.json()['user']['id'])

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
        resp = self.identity_admin_client.add_user(
            user_name=user_name, email=email_id, enabled=enabled,
            domain_id='random')
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
        resp = self.user_admin_client.add_user(
            user_name=user_name,
            email='random@nowhere.com',
            enabled=enabled)
        self.assertEqual(resp.status_code, 400)
        self.assertHeaders(
            resp, *self.header_validation_functions_HTTP_400)

    def tearDown(self):
        # Delete all users created in the tests
        for id in self.user_id:
            self.identity_admin_client.delete_user(user_id=id)
        super(TestAddUser, self).tearDown()

    @classmethod
    def tearDownClass(cls):
        # @todo: Delete all users created in the setUpClass
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
        self.user_id = []
        self.unexpected_headers_HTTP_201 = [
            header_validation.validate_transfer_encoding_header_not_present]
        self.header_validation_functions_HTTP_201 = (
            self.default_header_validations +
            self.unexpected_headers_HTTP_201 + [
                header_validation.validate_header_location,
                header_validation.validate_header_content_length])

    @ddt.file_data('data_add_identity_admin_user.json')
    def test_add_identity_admin_user(self, test_data):
        """Add identity admin level users

        Add identity admin level users with various test input combinations
        defined in the test data json file.
        """
        user_name = self.generate_random_string()
        input_data = test_data['additional_input']
        resp = self.service_admin_client.add_user(
            user_name=user_name, **input_data)
        self.assertEqual(resp.status_code, 201)

        updated_json_schema = ''

        updated_json_schema = copy.deepcopy(users_json.add_user)
        updated_json_schema['properties']['user']['required'] = (
            users_json.add_user['properties']['user']['required'] +
            test_data['addional_schema_fields'])
        self.assertSchema(response=resp, json_schema=updated_json_schema)

        self.assertHeaders(
            resp, *self.header_validation_functions_HTTP_201)
        self.user_id.append(resp.json()['user']['id'])

    def tearDown(self):
        # Delete all users created in the tests
        for id in self.user_id:
            self.service_admin_client.delete_user(user_id=id)
        super(TestServiceAdminLevelAddUser, self).tearDown()
