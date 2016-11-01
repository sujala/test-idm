from tests.api import base
from tests.api import devops_client
from tests.api.v2 import client
from tests.api.v2.models import requests
from tests.api import constants as const
from tests.api.utils import header_validation


class TestBaseV2(base.TestBase):

    """Child class of fixtures.BaseTestFixture for testing CDN.

    Inherit from this and write your test methods. If the child class defines
    a prepare(self) method, this method will be called before executing each
    test method.
    """

    @classmethod
    def setUpClass(cls):

        super(TestBaseV2, cls).setUpClass()

        cls.identity_admin_client = client.IdentityAPIClient(
            url=cls.url,
            serialize_format=cls.test_config.serialize_format,
            deserialize_format=cls.test_config.deserialize_format)
        resp = cls.identity_admin_client.get_auth_token(
            user=cls.identity_config.identity_admin_user_name,
            password=cls.identity_config.identity_admin_password)
        identity_admin_auth_token = resp.json()[const.ACCESS][const.TOKEN][
            const.ID]
        identity_admin_id = resp.json()[const.ACCESS][const.USER][const.ID]
        cls.identity_admin_client.default_headers[const.X_AUTH_TOKEN] = (
            identity_admin_auth_token)
        cls.identity_admin_client.default_headers[const.X_USER_ID] = (
            identity_admin_id
        )

        if cls.test_config.run_service_admin_tests:
            cls.service_admin_client = client.IdentityAPIClient(
                url=cls.url,
                serialize_format=cls.test_config.serialize_format,
                deserialize_format=cls.test_config.deserialize_format)
            cls.service_admin_client.default_headers[const.X_AUTH_TOKEN] = (
                cls.identity_config.service_admin_auth_token)
            cls.devops_client = devops_client.IdentityDevopsClient(
                cls.devops_url,
                serialize_format=cls.test_config.serialize_format,
                deserialize_format=cls.test_config.deserialize_format)
            cls.devops_client.default_headers[const.X_AUTH_TOKEN] = (
                cls.identity_config.service_admin_auth_token)

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

    @classmethod
    def generate_client(cls, parent_client=None, additional_input_data=None):
        """Return a client object

        :param parent_client: client object of the parent user. The parent user
        type can be identity_admin or user_admin. If no parent_client is given,
        uses the service_admin (or) identity_admin level user's client
        depending on wherther run_service_admin_tests in the config file is
        True or False respectively.

        :param additional_input_data: A dictionary with any additional fields
        that are required to add a user before generating the
        corresponding client.
        eg. {'domain_id': 'meow', 'user_name': 'cat'}
        """

        user_name = (additional_input_data.get(
            'user_name',
            cls.generate_random_string(pattern=const.USER_NAME_PATTERN)))
        domain_id = (additional_input_data.get('domain_id', None))
        password = (additional_input_data.get('password', None))
        contact_id = (additional_input_data.get('contact_id', None))

        if not parent_client:
            if cls.test_config.run_service_admin_tests:
                parent_client = cls.service_admin_client
            else:
                parent_client = cls.identity_admin_client

        request_object = requests.UserAdd(
            user_name=user_name, domain_id=domain_id, password=password,
            contact_id=contact_id
        )
        resp = parent_client.add_user(request_object)
        user_id = resp.json()[const.USER][const.ID]
        if ('is_user_manager' in additional_input_data and
                additional_input_data['is_user_manager'] is True):
            parent_client.add_role_to_user(
                user_id=user_id, role_id=const.USER_MANAGER_ROLE_ID)

        id_client = client.IdentityAPIClient(
            url=cls.url,
            serialize_format=cls.test_config.serialize_format,
            deserialize_format=cls.test_config.deserialize_format)
        if const.PASSWORD not in additional_input_data:
            password = resp.json()[const.USER][const.NS_PASSWORD]
        resp = id_client.get_auth_token(
            user=user_name, password=password)
        auth_token = resp.json()[const.ACCESS][const.TOKEN][const.ID]
        user_id = resp.json()[const.ACCESS][const.USER][const.ID]
        id_client.default_headers[const.X_USER_ID] = user_id
        id_client.default_headers[const.X_AUTH_TOKEN] = auth_token
        return id_client

    def generate_client_with_x_auth_token(cls, x_auth_token=None):
        """
        Return a client object using x-auth-token. So the call being made
        with the client generated by this method will be a call made by the
        user to whom the token belongs
        """
        id_client = client.IdentityAPIClient(
            url=cls.url,
            serialize_format=cls.test_config.serialize_format,
            deserialize_format=cls.test_config.deserialize_format)

        id_client.default_headers['X-Auth-Token'] = x_auth_token
        return id_client

    @classmethod
    def delete_client(cls, client, parent_client=None):
        """Return delete client object

        :param parent_client: client object of the parent user. The parent user
        type can be identity_admin or user_admin. If no parent_client is given,
        uses the service_admin (or) identity_admin level user's client
        depending on wherther run_service_admin_tests in the config file is
        True or False respectively.
        """
        if not parent_client:
            if cls.test_config.run_service_admin_tests:
                parent_client = cls.service_admin_client
            else:
                parent_client = cls.identity_admin_client
        user_id = client.default_headers[const.X_USER_ID]
        resp = parent_client.delete_user(user_id=user_id)

        return resp

    @classmethod
    def tearDownClass(cls):
        """Deletes the added resources."""
        super(TestBaseV2, cls).tearDownClass()
