import urllib.parse
from tests.api import base
from tests.api.v1_0 import client


class TestBaseV10(base.TestBase):

    test_path = ''

    @classmethod
    def setUpClass(cls):
        super(TestBaseV10, cls).setUpClass()
        VERSION = 'v1.0'
        cls.url = urllib.parse.urljoin(
            cls.identity_config.base_url, cls.identity_config.cloud_url)

        if cls.test_path == 'auth':
            cls.url = urllib.parse.urljoin(cls.url, cls.test_path)
        else:
            cls.url = urllib.parse.urljoin(cls.url, VERSION)

        cls.identity_client = client.IdentityAPIClient(
            url=cls.url,
            serialize_format=cls.test_config.serialize_format,
            deserialize_format=cls.test_config.deserialize_format)

    @classmethod
    def tearDownClass(cls):
        """Deletes the added resources."""
        super(TestBaseV10, cls).tearDownClass()
