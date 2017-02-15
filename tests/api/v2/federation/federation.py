# -*- coding: utf-8 -*
from tests.api.v2 import base
from tests.api.v2.models import factory

from tests.package.johny import constants as const
from tests.package.johny.v2 import client
from tests.package.johny.v2.models import requests


class TestBaseFederation(base.TestBaseV2):

    """Federation test base class. """

    @classmethod
    def setUpClass(cls):
        """Class level set up for the tests

        Create users needed for the tests and generate clients for those users.
        """
        super(TestBaseFederation, cls).setUpClass()
        user_name = cls.generate_random_string()
        password = cls.generate_random_string(const.PASSWORD_PATTERN)
        request_object = requests.UserAdd(user_name=user_name,
                                          password=password)
        resp = cls.service_admin_client.add_user(request_object)

        req_obj = requests.AuthenticateWithPassword(
            user_name=user_name,
            password=password)

        cls.idp_ia_client = client.IdentityAPIClient(
            url=cls.url,
            serialize_format=cls.test_config.serialize_format,
            deserialize_format=cls.test_config.deserialize_format)

        resp = cls.idp_ia_client.get_auth_token(request_object=req_obj)
        identity_admin_auth_token = resp.json()[const.ACCESS][const.TOKEN][
            const.ID]
        identity_admin_id = resp.json()[const.ACCESS][const.USER][const.ID]
        cls.idp_ia_client.default_headers[const.X_AUTH_TOKEN] = (
            identity_admin_auth_token)
        cls.idp_ia_client.default_headers[const.X_USER_ID] = (
            identity_admin_id
        )

        if cls.test_config.run_service_admin_tests:
            option = {
                const.PARAM_ROLE_NAME: const.PROVIDER_MANAGEMENT_ROLE_NAME
            }
            list_resp = cls.service_admin_client.list_roles(option=option)
            mapping_rules_role_id = list_resp.json()[const.ROLES][0][const.ID]
            cls.service_admin_client.add_role_to_user(
                user_id=identity_admin_id, role_id=mapping_rules_role_id)

    def setUp(self):
        super(TestBaseFederation, self).setUp()
        self.provider_ids = []
        self.user_ids = []
        self.domain_ids = []

    def create_idp_helper(self, dom_ids=None, fed_type=None, certs=None):
        dom_group = None
        if fed_type == const.BROKER:
            dom_group = const.GLOBAL.upper()
        request_object = factory.get_add_idp_request_object(
            approved_domain_ids=dom_ids, federation_type=fed_type,
            approved_domain_group=dom_group, public_certificates=certs)
        resp = self.idp_ia_client.create_idp(request_object)
        self.assertEquals(resp.status_code, 201)
        self.provider_ids.append(resp.json()[
            const.NS_IDENTITY_PROVIDER][const.ID])
        return request_object

    def check_bad_name(self, name):
        """ Helper method to isolate repeating bad name code. """
        if not self.test_config.run_service_admin_tests:
            self.skipTest('Skipping Service Admin Tests per config value')
        request_object = factory.get_add_idp_request_object()
        request_object.idp_name = name
        resp = self.idp_ia_client.create_idp(request_object)
        self.assertEquals(resp.status_code, 400)
        self.assertEquals(resp.json()[const.BAD_REQUEST][const.MESSAGE],
                          "Error code: 'GEN-005'; Identity provider name must"
                          " consist of only alphanumeric, '.', and '-'"
                          " characters.")

    def add_and_check_broker_idp(self, certs=None):
        if not self.test_config.run_service_admin_tests:
            self.skipTest('Skipping Service Admin Tests per config value')

        idp = self.create_idp_helper(fed_type=const.BROKER, certs=certs)
        idp_list = self.idp_ia_client.list_idp(
            option={"name": idp.idp_name}).json()[const.NS_IDENTITY_PROVIDERS]
        self.assertEquals(len(idp_list), 1)
        self.assertEquals(idp_list[0][const.FEDERATION_TYPE], "BROKER")
        get_resp = self.idp_ia_client.get_idp(idp_id=idp_list[0][const.ID])
        get_name = get_resp.json()[const.NS_IDENTITY_PROVIDER][const.NAME]
        self.assertEquals(get_name, idp.idp_name)

        self.assertEquals(get_resp.json()[
            const.NS_IDENTITY_PROVIDER][const.FEDERATION_TYPE], "BROKER")
        return idp

    def tearDown(self):
        # Delete all providers created in the tests

        for id_ in self.provider_ids:
            self.idp_ia_client.delete_idp(idp_id=id_)
        # Delete all users created in the tests
        for id in self.user_ids:
            self.idp_ia_client.delete_user(user_id=id)
        for dom in self.domain_ids:
            self.idp_ia_client.delete_domain(domain_id=dom)
        super(TestBaseFederation, self).tearDown()

    @classmethod
    def tearDownClass(cls):
        # Delete all users created in the setUpClass
        cls.delete_client(client=cls.idp_ia_client,
                          parent_client=cls.service_admin_client)
        super(TestBaseFederation, cls).tearDownClass()
