import urllib.parse
import base64
from tests.api import base
from tests.api.utils import header_validation
from tests.api.v1_1 import client


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
