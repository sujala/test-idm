# -*- coding: utf-8 -*
import ddt

from tests.api.v2 import base
from tests.api.v2.models import factory

from tests.package.johny import constants as const
from tests.package.johny.v2 import client
from tests.package.johny.v2.models import requests


@ddt.ddt
class TestAddMappingIDP(base.TestBaseV2):

    """Add IDP Mapping Tests"""

    # helpers
    @classmethod
    def add_role(cls, user_id, role_name):
        if cls.test_config.run_service_admin_tests:
            option = {
                const.PARAM_ROLE_NAME: role_name
            }
            list_resp = cls.service_admin_client.list_roles(option=option)
            mapping_rules_role_id = list_resp.json()[const.ROLES][0][const.ID]
            cls.service_admin_client.add_role_to_user(
                user_id=user_id, role_id=mapping_rules_role_id)

    @classmethod
    def create_user(cls):
        user_name = cls.generate_random_string()
        password = cls.generate_random_string(const.PASSWORD_PATTERN)
        request_object = requests.UserAdd(user_name=user_name,
                                          password=password)
        cls.service_admin_client.add_user(request_object)

        req_obj = requests.AuthenticateWithPassword(
            user_name=user_name,
            password=password)
        return req_obj

    @classmethod
    def create_identity_admin_with_role(cls, role):
        user_obj = cls.create_user()
        idp_ia_client = client.IdentityAPIClient(
            url=cls.url,
            serialize_format=cls.test_config.serialize_format,
            deserialize_format=cls.test_config.deserialize_format)

        resp = idp_ia_client.get_auth_token(request_object=user_obj)
        identity_admin_auth_token = resp.json()[const.ACCESS][const.TOKEN][
            const.ID]
        identity_admin_id = resp.json()[const.ACCESS][const.USER][const.ID]
        idp_ia_client.default_headers[const.X_AUTH_TOKEN] = (
            identity_admin_auth_token)
        idp_ia_client.default_headers[const.X_USER_ID] = (
            identity_admin_id
        )
        if role != "None":
            cls.add_role(user_id=identity_admin_id, role_name=role)
        return idp_ia_client

    @classmethod
    def setUpClass(cls):
        """Class level set up for the tests

        Create users needed for the tests and generate clients for those users.
        """

        super(TestAddMappingIDP, cls).setUpClass()
        cls.idp_ia_clients = {}
        roles = [const.PROVIDER_MANAGEMENT_ROLE_NAME,
                 const.PROVIDER_RO_ROLE_NAME, "None"]
        for role in roles:
            cls.idp_ia_clients[
                role] = cls.create_identity_admin_with_role(role)

    def setUp(self):
        super(TestAddMappingIDP, self).setUp()
        self.provider_ids = []

    def add_idp(self, idp_ia_client):
        if not self.test_config.run_service_admin_tests:
            self.skipTest('Skipping Service Admin Tests per config value')

        request_object = factory.get_add_idp_request_object()
        resp = idp_ia_client.create_idp(request_object)
        self.assertEquals(resp.status_code, 201)
        provider_id = resp.json()[const.NS_IDENTITY_PROVIDER][const.ID]
        self.provider_ids.append(provider_id)
        self.assertEquals(resp.json()[const.NS_IDENTITY_PROVIDER][const.NAME],
                          request_object.idp_name)
        return provider_id

    # verify must have role manager for put, read only for get
    def test_add_mapping_manager_role(self):
        provider_id = self.add_idp(idp_ia_client=self.idp_ia_clients[
            const.PROVIDER_MANAGEMENT_ROLE_NAME])

        resp_put_manager = self.idp_ia_clients[
            "None"].add_idp_mapping(
                idp_id=provider_id,
                request_data={})
        self.assertEquals(resp_put_manager.status_code, 403)

        resp_put_manager = self.idp_ia_clients[
            const.PROVIDER_RO_ROLE_NAME].add_idp_mapping(
                idp_id=provider_id,
                request_data={})
        self.assertEquals(resp_put_manager.status_code, 403)

        resp_put_manager = self.idp_ia_clients[
            const.PROVIDER_MANAGEMENT_ROLE_NAME].add_idp_mapping(
                idp_id=provider_id,
                request_data={})
        self.assertEquals(resp_put_manager.status_code, 204)

        resp_get_none = self.idp_ia_clients[
            "None"].get_idp_mapping(
                idp_id=provider_id)
        self.assertEquals(resp_get_none.status_code, 403)

        resp_get_ro = self.idp_ia_clients[
            const.PROVIDER_RO_ROLE_NAME].get_idp_mapping(
                idp_id=provider_id)
        self.assertEquals(resp_get_ro.status_code, 200)

        resp_get_manager = self.idp_ia_clients[
            const.PROVIDER_MANAGEMENT_ROLE_NAME].get_idp_mapping(
                idp_id=provider_id)
        self.assertEquals(resp_get_manager.status_code, 200)

    # Try none, empty {} [] for set
    @ddt.data({}, [], {"test": "test"})
    def test_add_mapping_valid_json(self, mapping):
        provider_id = self.add_idp(idp_ia_client=self.idp_ia_clients[
            const.PROVIDER_MANAGEMENT_ROLE_NAME])

        resp_put_manager = self.idp_ia_clients[
            const.PROVIDER_MANAGEMENT_ROLE_NAME].add_idp_mapping(
                idp_id=provider_id,
                request_data=mapping)
        self.assertEquals(resp_put_manager.status_code, 204)

        resp_get_ro = self.idp_ia_clients[
            const.PROVIDER_MANAGEMENT_ROLE_NAME].get_idp_mapping(
                idp_id=provider_id)
        self.assertEquals(resp_get_ro.status_code, 200)
        self.assertEquals(resp_get_ro.json(), mapping)

    # idp missing causes 404
    def test_add_mapping_missing_idp(self):
        provider_id = self.add_idp(idp_ia_client=self.idp_ia_clients[
            const.PROVIDER_MANAGEMENT_ROLE_NAME])
        idp_id = "xxx{0}xxx".format(provider_id)
        resp_put_manager = self.idp_ia_clients[
            const.PROVIDER_MANAGEMENT_ROLE_NAME].add_idp_mapping(
                idp_id=idp_id,
                request_data={})
        self.assertEquals(resp_put_manager.status_code, 404)
        self.assertEquals(resp_put_manager.json()[const.ITEM_NOT_FOUND][
            const.MESSAGE], "Identity Provider with id/name: '{0}' was"
                            " not found.".format(idp_id))

    @ddt.data("xml", "xhtml_xml")
    def test_idp_mapping_content_type_xml(self, content_type):
        provider_id = self.add_idp(idp_ia_client=self.idp_ia_clients[
            const.PROVIDER_MANAGEMENT_ROLE_NAME])
        self.idp_ia_clients[
            "bad_content_type"] = self.create_identity_admin_with_role(
                const.PROVIDER_MANAGEMENT_ROLE_NAME)
        self.idp_ia_clients["bad_content_type"].default_headers[
            const.CONTENT_TYPE] = (const.CONTENT_TYPE_VALUE.format(
                content_type))
        resp_put_manager = self.idp_ia_clients[
            "bad_content_type"].add_idp_mapping(
                idp_id=provider_id,
                request_data={})
        self.assertEquals(resp_put_manager.status_code, 400)

    @ddt.data("text", "x-www-form-urlencoded")
    def test_idp_mapping_content_type(self, content_type):
        provider_id = self.add_idp(idp_ia_client=self.idp_ia_clients[
            const.PROVIDER_MANAGEMENT_ROLE_NAME])
        self.idp_ia_clients[
            "bad_content_type"] = self.create_identity_admin_with_role(
                const.PROVIDER_MANAGEMENT_ROLE_NAME)
        self.idp_ia_clients["bad_content_type"].default_headers[
            const.CONTENT_TYPE] = (const.CONTENT_TYPE_VALUE.format(
                content_type))
        resp_put_manager = self.idp_ia_clients[
            "bad_content_type"].add_idp_mapping(
                idp_id=provider_id,
                request_data={})
        self.assertEquals(resp_put_manager.status_code, 415)

    @ddt.data("xml", "xhtml_xml", "x-www-form-urlencoded")
    def test_idp_mapping_accept_type(self, accept_type):
        provider_id = self.add_idp(idp_ia_client=self.idp_ia_clients[
            const.PROVIDER_MANAGEMENT_ROLE_NAME])
        self.idp_ia_clients[
            "bad_accept_type"] = self.create_identity_admin_with_role(
                const.PROVIDER_MANAGEMENT_ROLE_NAME)
        self.idp_ia_clients["bad_accept_type"].default_headers[
            const.ACCEPT] = (const.ACCEPT_ENCODING_VALUE.format(
                accept_type))
        mapping = {"testkey": "testvalue"}
        resp_put_manager = self.idp_ia_clients[
            "bad_accept_type"].add_idp_mapping(
                idp_id=provider_id,
                request_data=mapping)
        # According to Jorge, despite the AC, the accept type should just be
        # ignored OR return a 406 (from tomcat).
        self.assertTrue(resp_put_manager.status_code == 204 or
                        resp_put_manager.status_code == 406)
        # if the accept type was ignored, we still need to validate
        # the mapping was stored correctly.
        if resp_put_manager.status_code == 204:
            resp_get_ro = self.idp_ia_clients[
                const.PROVIDER_MANAGEMENT_ROLE_NAME].get_idp_mapping(
                    idp_id=provider_id)
            self.assertEquals(resp_get_ro.status_code, 200)
            self.assertEquals(resp_get_ro.json(), mapping)

    def test_idp_mapping_max_size(self):
        max_size_in_kilo = const.MAX_SIZE_IN_KILOBYTES
        provider_id = self.add_idp(idp_ia_client=self.idp_ia_clients[
            const.PROVIDER_MANAGEMENT_ROLE_NAME])
        resp_put_manager = self.idp_ia_clients[
            const.PROVIDER_MANAGEMENT_ROLE_NAME].add_idp_mapping(
                idp_id=provider_id,
                request_data={"test": self.generate_random_string(
                    const.IDP_MAPPING_PATTERN.format(
                        mapping_size=5000))})
        self.assertEquals(resp_put_manager.status_code, 400)
        self.assertEquals(resp_put_manager.json()[const.BAD_REQUEST][
            const.MESSAGE], u"Max size exceed. Policy file must be less tha"
                            "n {max_size}"
                            " Kilobytes.".format(max_size=max_size_in_kilo))

    def tearDown(self):
        # Delete all providers created in the tests
        for id_ in self.provider_ids:
            self.idp_ia_clients[
                const.PROVIDER_MANAGEMENT_ROLE_NAME].delete_idp(idp_id=id_)

        super(TestAddMappingIDP, self).tearDown()

    @classmethod
    def tearDownClass(cls):
        # Delete all users created in the setUpClass
        for idp_ia_client in cls.idp_ia_clients:
            cls.delete_client(client=cls.idp_ia_clients[idp_ia_client],
                              parent_client=cls.service_admin_client)
        super(TestAddMappingIDP, cls).tearDownClass()
