from tests.api import base
from tests.api.v2 import client


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
        identity_admin_auth_token = resp.json()['access']['token']['id']
        cls.identity_admin_client.default_headers['X-Auth-Token'] = (
            identity_admin_auth_token)

        if cls.test_config.run_service_admin_tests:
            cls.service_admin_client = client.IdentityAPIClient(
                url=cls.url,
                serialize_format=cls.test_config.serialize_format,
                deserialize_format=cls.test_config.deserialize_format)
            cls.service_admin_client.default_headers['X-Auth-Token'] = (
                cls.identity_config.service_admin_auth_token)

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
            cls.generate_random_string(pattern='api[\-]test[\-][\d\w]{12}')))
        domain_id = (additional_input_data.get('domain_id', None))
        password = (additional_input_data.get('password', None))

        if not parent_client:
            if cls.test_config.run_service_admin_tests:
                parent_client = cls.service_admin_client
            else:
                parent_client = cls.identity_admin_client

        resp = parent_client.add_user(
            user_name=user_name, domain_id=domain_id, password=password)
        id_client = client.IdentityAPIClient(
            url=cls.url,
            serialize_format=cls.test_config.serialize_format,
            deserialize_format=cls.test_config.deserialize_format)
        if 'password' not in additional_input_data:
            password = resp.json()['user']['OS-KSADM:password']
        resp = id_client.get_auth_token(
            user=user_name, password=password)
        auth_token = resp.json()['access']['token']['id']
        id_client.default_headers['X-Auth-Token'] = auth_token
        return id_client

    @classmethod
    def tearDownClass(cls):
        """Deletes the added resources."""
        super(TestBaseV2, cls).tearDownClass()
