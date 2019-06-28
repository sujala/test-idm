import urllib.parse
import base64
from tests.api import base
from tests.api.utils import header_validation
from tests.api.v1_1 import client
from tests.package.johny.v2 import client as client_v2
from tests.package.johny.v2.models import requests as requests_v2
from tests.package.johny import constants as const


class TestBaseV1(base.TestBase):
    @classmethod
    def setUpClass(cls):

        super(TestBaseV1, cls).setUpClass()
        VERSION = 'v1.1'
        cls.url = urllib.parse.urljoin(
            cls.identity_config.base_url, cls.identity_config.cloud_url)
        cls.url = urllib.parse.urljoin(cls.url, VERSION)
        cls.identity_admin_client = client.IdentityAPIClient(
            url=cls.url,
            serialize_format=cls.test_config.serialize_format,
            deserialize_format=cls.test_config.deserialize_format)
        username = cls.identity_config.identity_admin_user_name
        password = cls.identity_config.identity_admin_password
        encrypted_password = base64.encodebytes(
            '{0}:{1}'.format(username, password).encode('utf-8')
        )[:-1]
        cls.identity_admin_client.default_headers['Authorization'] = \
            'Basic {0}'.format(encrypted_password.decode())

        cls.url_v2 = urllib.parse.urljoin(
            cls.identity_config.base_url, cls.identity_config.cloud_url)
        cls.url_v2 = urllib.parse.urljoin(
            cls.url_v2, cls.identity_config.api_version)
        cls.identity_admin_client_v2 = client_v2.IdentityAPIClient(
            url=cls.url_v2,
            serialize_format=cls.test_config.serialize_format,
            deserialize_format=cls.test_config.deserialize_format)

        req_obj = requests_v2.AuthenticateWithPassword(
            user_name=cls.identity_config.identity_admin_user_name,
            password=cls.identity_config.identity_admin_password)
        resp = cls.identity_admin_client_v2.get_auth_token(
            request_object=req_obj)
        identity_admin_auth_token = resp.json()[const.ACCESS][const.TOKEN][
            const.ID]
        identity_admin_id = resp.json()[const.ACCESS][const.USER][const.ID]
        cls.identity_admin_client_v2.default_headers[const.X_AUTH_TOKEN] = (
            identity_admin_auth_token)
        cls.identity_admin_client_v2.default_headers[const.X_USER_ID] = (
            identity_admin_id
        )

        cls.unexpected_headers_HTTP_201 = [
            header_validation.validate_transfer_encoding_header_not_present]
        cls.unexpected_headers_HTTP_400 = [
            header_validation.validate_location_header_not_present,
            header_validation.validate_content_length_header_not_present]
        cls.unexpected_headers_HTTP_200 = [
            header_validation.validate_location_header_not_present]
        cls.header_validation_functions_HTTP_200 = (
            cls.default_header_validations +
            cls.unexpected_headers_HTTP_200)
        cls.header_validation_functions_HTTP_201 = (
            cls.default_header_validations +
            cls.unexpected_headers_HTTP_201 + [
                header_validation.validate_header_location,
                header_validation.validate_header_content_length])
        cls.header_validation_functions_HTTP_400 = (
            cls.default_header_validations +
            cls.unexpected_headers_HTTP_400)

    @classmethod
    def tearDownClass(cls):
        """Deletes the added resources."""
        super(TestBaseV1, cls).tearDownClass()
