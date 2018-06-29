# -*- coding: utf-8 -*
import copy
import json
import os
import sys

import ddt
from nose.plugins.attrib import attr
from qe_coverage.opencafe_decorators import tags, unless_coverage

from tests.api.utils import data_file_iterator
from tests.api.utils import func_helper
from tests.api.utils.create_cert import create_self_signed_cert
from tests.api.v2 import base
from tests.api.v2.federation import federation
from tests.api.v2.models import factory

from tests.package.johny import constants as const


@ddt.ddt
class TestAddMappingIDP(federation.TestBaseFederation):

    """Add IDP Mapping Tests"""

    @classmethod
    @unless_coverage
    def setUpClass(cls):
        """Class level set up for the tests

        Create users needed for the tests and generate clients for those users.
        """

        super(TestAddMappingIDP, cls).setUpClass()

        # user admin client
        cls.domain_id = func_helper.generate_randomized_domain_id(
            client=cls.identity_admin_client)
        cls.idp_user_admin_client = cls.generate_client(
            parent_client=cls.identity_admin_client,
            additional_input_data={'domain_id': cls.domain_id})
        cls.idp_user_admin_client.serialize_format = 'xml'
        cls.idp_user_admin_client.default_headers[
            const.CONTENT_TYPE] = 'application/xml'

        # user manage client
        cls.idp_user_manage_client = cls.generate_client(
            parent_client=cls.idp_user_admin_client,
            additional_input_data={'domain_id': cls.domain_id,
                                   'is_user_manager': True})
        cls.idp_user_manage_client.serialize_format = 'xml'
        cls.idp_user_manage_client.default_headers[
            const.CONTENT_TYPE] = 'application/xml'

        cls.default_headers = copy.deepcopy(
            cls.identity_admin_client.default_headers)

    @unless_coverage
    def setUp(self):
        super(TestAddMappingIDP, self).setUp()
        self.idp_id = None
        self.provider_ids = []

    def create_and_validate_idp_mapping(
            self, mapping, provider_id, api_client):
        resp_put_manager = api_client.add_idp_mapping(
            idp_id=provider_id,
            request_data=mapping,
            content_type=const.YAML)
        self.assertEquals(resp_put_manager.status_code, 204)
        resp_get_ro = api_client.get_idp_mapping(
            idp_id=provider_id, headers={
                const.ACCEPT: const.YAML_ACCEPT_ENCODING_VALUE
            })

        return resp_get_ro

    def add_idp(self, idp_ia_client):
        request_object = factory.get_add_idp_request_object()
        resp = self.identity_admin_client.create_idp(request_object)
        self.assertEquals(resp.status_code, 201)
        provider_id = resp.json()[const.NS_IDENTITY_PROVIDER][const.ID]
        self.provider_ids.append(provider_id)
        self.assertEquals(resp.json()[const.NS_IDENTITY_PROVIDER][const.NAME],
                          request_object.idp_name)
        return provider_id

    def get_valid_mapping_policy(self):
        mapping = {}
        full_path = os.path.realpath(__file__)
        with open("{}/data_update_idp_mapping_policy.json".format(
                os.path.dirname(full_path)), "r") as policy:
            mapping = json.load(policy)['valid']
        return mapping

    def validate_fed_user_auth_bad_request(
            self, cert_path, domain_id, issuer, key_path, api_client):
        test_data = {
            "fed_input": {
                "base64_url_encode": True,
                "new_url": True,
                "content_type": "x-www-form-urlencoded",
                "fed_api": "v2",
                "roles": [
                    "lbaas:admin"
                ]
            }
        }
        fed_auth = self.fed_user_call(
            test_data=test_data, domain_id=domain_id, private_key=key_path,
            public_key=cert_path, issuer=issuer,
            auth_client=api_client)
        self.assertEqual(fed_auth.status_code, 400)
        self.assertEqual(fed_auth.json()['badRequest']['message'],
                         "Error code: 'FED2-016'; Invalid role 'lbaas:admin'")

    def validate_fed_auth_success(
            self, cert_path, domain_id, issuer, key_path, api_client):
        test_data = {
            "fed_input": {
                "base64_url_encode": True,
                "new_url": True,
                "content_type": "x-www-form-urlencoded",
                "fed_api": "v2"
            }
        }
        fed_auth = self.fed_user_call(
            test_data=test_data, domain_id=domain_id, private_key=key_path,
            public_key=cert_path, issuer=issuer,
            auth_client=api_client)
        self.assertEqual(fed_auth.status_code, 200)
        self.assertSchema(fed_auth, self.updated_fed_auth_schema)
        fed_token, _, _ = self.parse_auth_response(fed_auth)

    @tags('positive', 'p0', 'regression')
    @attr(type='regression')
    @data_file_iterator.data_file_provider((
        "yaml/blacklist_mapping_policy.yaml",
    ))
    def test_add_mapping_blacklisted_yaml_with_auth(self, mapping):
        domain_id = self.create_user_and_get_domain(
            auth_client=self.identity_admin_client)

        issuer = self.generate_random_string(pattern='issuer[\-][\d\w]{12}')
        provider_id, cert_path, key_path = self.create_idp_with_certs(
            domain_id=domain_id, issuer=issuer,
            auth_client=self.identity_admin_client)

        resp_get_ro = self.create_and_validate_idp_mapping(
            mapping, provider_id, api_client=self.identity_admin_client)
        self.assertEquals(resp_get_ro.status_code, 200)
        self.assertEquals(resp_get_ro.headers[const.CONTENT_TYPE],
                          const.YAML_CONTENT_TYPE_VALUE)
        self.assertEquals(resp_get_ro.text, mapping)

        self.validate_fed_user_auth_bad_request(
            cert_path, domain_id, issuer, key_path, self.identity_admin_client)

    @tags('positive', 'p0', 'regression')
    @attr(type='regression')
    @data_file_iterator.data_file_provider((
        "yaml/blacklist_mapping_policy.yaml",
    ))
    def test_add_mapping_blacklisted_yaml_with_auth_user_admin(self, mapping):
        (pem_encoded_cert, cert_path, _, key_path,
         f_print) = create_self_signed_cert()

        self.idp_id = self.add_idp_with_metadata_return_id(
            cert_path=cert_path, api_client=self.idp_user_admin_client)

        resp_put_manager = self.idp_user_admin_client.add_idp_mapping(
            idp_id=self.idp_id,
            request_data=mapping,
            content_type=const.YAML)
        self.assertEquals(resp_put_manager.status_code, 204)

        resp_get_ro = self.create_and_validate_idp_mapping(
            mapping, self.idp_id, api_client=self.idp_user_admin_client
        )

        self.assertEquals(resp_get_ro.status_code, 200)
        self.assertEquals(resp_get_ro.headers[const.CONTENT_TYPE],
                          const.YAML_CONTENT_TYPE_VALUE)
        self.assertEquals(resp_get_ro.text, mapping)

        self.validate_fed_user_auth_bad_request(
            cert_path, self.domain_id, self.issuer, key_path,
            self.idp_user_admin_client)

    @tags('positive', 'p0', 'regression')
    @attr(type='regression')
    @data_file_iterator.data_file_provider((
        "yaml/blacklist_mapping_policy.yaml",
    ))
    def test_add_mapping_blacklisted_yaml_with_auth_user_manage(self, mapping):
        (pem_encoded_cert, cert_path, _, key_path,
         f_print) = create_self_signed_cert()

        self.idp_id = self.add_idp_with_metadata_return_id(
            cert_path=cert_path, api_client=self.idp_user_manage_client)

        resp_get_ro = self.create_and_validate_idp_mapping(
            mapping, self.idp_id, api_client=self.idp_user_manage_client)

        self.assertEquals(resp_get_ro.status_code, 200)
        self.assertEquals(resp_get_ro.headers[const.CONTENT_TYPE],
                          const.YAML_CONTENT_TYPE_VALUE)
        self.assertEquals(resp_get_ro.text, mapping)

        self.validate_fed_user_auth_bad_request(
            cert_path, self.domain_id, self.issuer, key_path,
            self.idp_user_manage_client)

    @tags('positive', 'p0', 'regression')
    @attr(type='regression')
    @data_file_iterator.data_file_provider((
        "yaml/default_mapping_policy.yaml",
    ))
    def test_add_mapping_valid_yaml_with_auth(self, mapping):
        domain_id = self.create_user_and_get_domain(
            auth_client=self.identity_admin_client)

        issuer = self.generate_random_string(pattern='issuer[\-][\d\w]{12}')
        provider_id, cert_path, key_path = self.create_idp_with_certs(
            domain_id=domain_id, issuer=issuer,
            auth_client=self.identity_admin_client)

        self.create_and_validate_idp_mapping(
            mapping, provider_id, api_client=self.identity_admin_client)

        self.validate_fed_auth_success(
            cert_path, domain_id, issuer, key_path, self.identity_admin_client)

    @tags('positive', 'p0', 'regression')
    @attr(type='regression')
    @data_file_iterator.data_file_provider((
        "yaml/default_mapping_policy.yaml",
    ))
    def test_add_mapping_valid_yaml_with_auth_user_admin(self, mapping):
        (pem_encoded_cert, cert_path, _, key_path,
         f_print) = create_self_signed_cert()

        self.idp_id = self.add_idp_with_metadata_return_id(
            cert_path=cert_path, api_client=self.idp_user_admin_client)

        resp_put_manager = self.idp_user_admin_client.add_idp_mapping(
            idp_id=self.idp_id,
            request_data=mapping,
            content_type=const.YAML)
        self.assertEquals(resp_put_manager.status_code, 204)

        resp_get_ro = self.idp_user_admin_client.get_idp_mapping(
            idp_id=self.idp_id, headers={
                const.ACCEPT: const.YAML_ACCEPT_ENCODING_VALUE
            })
        self.assertEquals(resp_get_ro.status_code, 200)
        self.assertEquals(resp_get_ro.headers[const.CONTENT_TYPE],
                          const.YAML_CONTENT_TYPE_VALUE)
        self.assertEquals(resp_get_ro.text, mapping)

        self.validate_fed_auth_success(
            cert_path, self.domain_id, self.issuer, key_path,
            self.idp_user_admin_client)

    @tags('positive', 'p0', 'regression')
    @attr(type='regression')
    @data_file_iterator.data_file_provider((
        "yaml/default_mapping_policy.yaml",
    ))
    def test_add_mapping_valid_yaml_with_auth_user_manage(self, mapping):
        (pem_encoded_cert, cert_path, _, key_path,
         f_print) = create_self_signed_cert()

        self.idp_id = self.add_idp_with_metadata_return_id(
            cert_path=cert_path, api_client=self.idp_user_manage_client)

        resp_put_manager = self.idp_user_manage_client.add_idp_mapping(
            idp_id=self.idp_id,
            request_data=mapping,
            content_type=const.YAML)
        self.assertEquals(resp_put_manager.status_code, 204)

        resp_get_ro = self.idp_user_manage_client.get_idp_mapping(
            idp_id=self.idp_id, headers={
                const.ACCEPT: const.YAML_ACCEPT_ENCODING_VALUE
            })
        self.assertEquals(resp_get_ro.status_code, 200)
        self.assertEquals(resp_get_ro.headers[const.CONTENT_TYPE],
                          const.YAML_CONTENT_TYPE_VALUE)
        self.assertEquals(resp_get_ro.text, mapping)

        self.validate_fed_auth_success(
            cert_path, self.domain_id, self.issuer, key_path,
            self.idp_user_manage_client)

    # verify must have role manager for put, read only for get
    @unless_coverage
    @attr(type='regression')
    @ddt.file_data('data_update_idp_mapping_policy.json')
    def test_add_mapping_manager_role(self, mapping):
        provider_id = self.add_idp(idp_ia_client=self.identity_admin_client)
        resp_put_manager = self.identity_admin_client.add_idp_mapping(
            idp_id=provider_id, request_data=mapping)
        self.assertEquals(resp_put_manager.status_code, 204)

        resp_get_manager = self.identity_admin_client.get_idp_mapping(
            idp_id=provider_id)
        self.assertEquals(resp_get_manager.status_code, 200)

    @unless_coverage
    @attr(type='regression')
    @ddt.file_data('data_update_idp_valid_mapping_policy.json')
    def test_add_mapping_valid_json(self, mapping):
        provider_id = self.add_idp(idp_ia_client=self.identity_admin_client)

        resp_put_manager = self.identity_admin_client.add_idp_mapping(
            idp_id=provider_id,
            request_data=mapping)
        self.assertEquals(resp_put_manager.status_code, 204)

        resp_get_ro = self.identity_admin_client.get_idp_mapping(
            idp_id=provider_id)
        self.assertEquals(resp_get_ro.status_code, 200)
        self.assertEquals(resp_get_ro.headers[const.CONTENT_TYPE],
                          const.CONTENT_TYPE_VALUE.format(
                              const.JSON))
        self.assertEquals(resp_get_ro.json(), mapping)

    @tags('negative', 'p0', 'regression')
    @attr(type='regression')
    @data_file_iterator.data_file_provider((
        "yaml/default_mapping_invalid_policy.yaml",
    ))
    def test_add_mapping_invalid_yaml(self, mapping):
        provider_id = self.add_idp(idp_ia_client=self.identity_admin_client)

        current_resp_policy = self.identity_admin_client.get_idp_mapping(
            idp_id=provider_id)

        resp_put_manager = self.identity_admin_client.add_idp_mapping(
            idp_id=provider_id,
            request_data=mapping,
            content_type=const.YAML)
        self.assertEquals(resp_put_manager.status_code, 400)

        resp_get_ro = self.identity_admin_client.get_idp_mapping(
            idp_id=provider_id, headers={
                const.ACCEPT: const.YAML_ACCEPT_ENCODING_VALUE
            })
        self.assertEquals(resp_get_ro.status_code, 404)
        self.assertEquals(resp_get_ro.json()["itemNotFound"]["message"],
                          "No [YAML] mapping policy found for IDP with "
                          "ID {}.".format(provider_id))

        resp_get_ro = self.identity_admin_client.get_idp_mapping(
            idp_id=provider_id)
        self.assertEquals(resp_get_ro.status_code, 200)
        self.assertNotEquals(resp_get_ro.text, mapping)
        self.assertEquals(resp_get_ro.json(), current_resp_policy.json())

    @tags('positive', 'p0', 'regression')
    @attr(type='regression')
    @data_file_iterator.data_file_provider((
        "yaml/default_mapping_policy.yaml",
        "yaml/blacklist_mapping_policy.yaml",
        "yaml/name_and_roles_regex_mapping_policy.yaml",
        "yaml/quoted_attrs_mapping_policy.yaml",
        "yaml/roles_with_spaces_mapping_policy.yaml",
    ))
    def test_add_mapping_valid_yaml(self, mapping):
        provider_id = self.add_idp(idp_ia_client=self.identity_admin_client)

        self.create_and_validate_idp_mapping(
            mapping, provider_id, api_client=self.identity_admin_client)

        resp_get_ro = self.identity_admin_client.get_idp_mapping(
            idp_id=provider_id)
        self.assertEquals(resp_get_ro.status_code, 404)
        self.assertEquals(resp_get_ro.json()["itemNotFound"]["message"],
                          "No [JSON] mapping policy found for IDP with "
                          "ID {}.".format(provider_id))

    @unless_coverage
    # Try none, empty {} [] for set
    @ddt.data({}, [], {"test": "test"})
    def test_add_mapping_invalid_json(self, mapping):
        """We now fail invalid policies - CID-1000
        """
        provider_id = self.add_idp(idp_ia_client=self.identity_admin_client)
        current_resp_policy = self.identity_admin_client.get_idp_mapping(
            idp_id=provider_id)

        resp_put_manager = self.identity_admin_client.add_idp_mapping(
            idp_id=provider_id,
            request_data=mapping)
        self.assertEquals(resp_put_manager.status_code, 400)

        resp_get_ro = self.identity_admin_client.get_idp_mapping(
            idp_id=provider_id)
        self.assertEquals(resp_get_ro.status_code, 200)
        self.assertNotEquals(resp_get_ro.json(), mapping)
        self.assertEquals(resp_get_ro.json(), current_resp_policy.json())

    # idp missing causes 404
    @unless_coverage
    @ddt.file_data('data_update_idp_mapping_policy.json')
    @attr('skip_at_gate')
    def test_add_mapping_missing_idp(self, mapping):
        provider_id = self.add_idp(idp_ia_client=self.identity_admin_client)
        idp_id = "xxx{0}xxx".format(provider_id)
        resp_put_manager = self.identity_admin_client.add_idp_mapping(
            idp_id=idp_id, request_data=mapping)
        self.assertEquals(resp_put_manager.status_code, 404)
        self.assertEquals(resp_put_manager.json()[const.ITEM_NOT_FOUND][
            const.MESSAGE], "Identity Provider with id/name: '{0}' was"
                            " not found.".format(idp_id))

    @unless_coverage
    @ddt.data("xml", "xhtml_xml")
    @attr('skip_at_gate')
    def test_idp_mapping_content_type_xml(self, content_type):
        provider_id = self.add_idp(idp_ia_client=self.identity_admin_client)

        self.identity_admin_client.default_headers[const.CONTENT_TYPE] = (
            const.CONTENT_TYPE_VALUE.format(content_type))
        resp_put_manager = self.identity_admin_client.add_idp_mapping(
            idp_id=provider_id,
            request_data={},
            content_type=None)
        self.identity_admin_client.default_headers[const.CONTENT_TYPE] = (
            self.default_headers[const.CONTENT_TYPE])
        self.assertEquals(resp_put_manager.status_code, 400)

    @unless_coverage
    @ddt.data("text", "x-www-form-urlencoded")
    def test_idp_mapping_content_type(self, content_type):
        provider_id = self.add_idp(idp_ia_client=self.identity_admin_client)
        self.identity_admin_client.default_headers[const.CONTENT_TYPE] = (
            const.CONTENT_TYPE_VALUE.format(content_type))
        resp_put_manager = self.identity_admin_client.add_idp_mapping(
            idp_id=provider_id,
            request_data={},
            content_type=None)
        self.identity_admin_client.default_headers[const.CONTENT_TYPE] = (
            self.default_headers[const.CONTENT_TYPE])
        self.assertEquals(resp_put_manager.status_code, 415)

    @unless_coverage
    @ddt.data("xml", "xhtml_xml", "x-www-form-urlencoded")
    def test_idp_mapping_accept_type(self, accept_type):
        provider_id = self.add_idp(idp_ia_client=self.identity_admin_client)
        self.identity_admin_client.default_headers[const.ACCEPT] = (
            const.ACCEPT_ENCODING_VALUE.format(accept_type))
        mapping = self.get_valid_mapping_policy()
        resp_put_manager = self.identity_admin_client.add_idp_mapping(
            idp_id=provider_id, request_data=mapping)

        self.identity_admin_client.default_headers[const.ACCEPT] = (
            self.default_headers[const.ACCEPT])

        # According to Jorge, despite the AC, the accept type should just be
        # ignored OR return a 406 (from tomcat).
        self.assertTrue(resp_put_manager.status_code == 204 or
                        resp_put_manager.status_code == 406)
        # if the accept type was ignored, we still need to validate
        # the mapping was stored correctly.
        if resp_put_manager.status_code == 204:
            resp_get_ro = self.identity_admin_client.get_idp_mapping(
                idp_id=provider_id)
            self.assertEquals(resp_get_ro.status_code, 200)
            self.assertEquals(resp_get_ro.json(), mapping)

    @tags('negative', 'p0', 'regression')
    @attr(type='regression')
    def test_idp_mapping_max_size(self):
        max_size_in_kilo = self.test_config.max_mapping_policy_size_in_kb
        provider_id = self.add_idp(idp_ia_client=self.identity_admin_client)
        mapping = self.get_valid_mapping_policy()
        # get current size of the mapping policy, so that we can add more
        # characters to cross max size
        current_size = sys.getsizeof(mapping)

        # this size will make it larger than the max limit
        mapping['mapping']['rules'][0]['remote'][0]['path'] = (
            self.generate_random_string(
                const.IDP_MAPPING_PATTERN.format(
                    mapping_size=(int(
                        max_size_in_kilo) * 1025 - current_size + 2))))
        resp_put_manager = self.identity_admin_client.add_idp_mapping(
            idp_id=provider_id,
            request_data=mapping)
        self.assertEquals(resp_put_manager.status_code, 400)
        self.assertEquals(resp_put_manager.json()[const.BAD_REQUEST][
            const.MESSAGE], u"Max size exceed. Policy file must be less tha"
                            "n {max_size}"
                            " Kilobytes.".format(max_size=max_size_in_kilo))

    @tags('positive', 'p0', 'regression')
    @attr(type='regression')
    def test_idp_mapping_upto_max_size(self):
        max_size_in_kilo = self.test_config.max_mapping_policy_size_in_kb
        provider_id = self.add_idp(idp_ia_client=self.identity_admin_client)
        mapping = self.get_valid_mapping_policy()
        # get current size of the mapping policy, so that we can add more
        # characters to reach max size
        current_size = sys.getsizeof(mapping)

        # this size will pad it up to the max limit
        mapping['mapping']['rules'][0]['remote'][0]['path'] = (
            self.generate_random_string(const.IDP_MAPPING_PATTERN.format(
                mapping_size=(int(max_size_in_kilo) * 1025 - current_size))))
        resp_put_manager = self.identity_admin_client.add_idp_mapping(
            idp_id=provider_id,
            request_data=mapping)
        self.assertEquals(resp_put_manager.status_code, 204)

    @base.base.log_tearDown_error
    @unless_coverage
    def tearDown(self):
        super(TestAddMappingIDP, self).tearDown()

    @classmethod
    @unless_coverage
    def tearDownClass(cls):
        super(TestAddMappingIDP, cls).tearDownClass()
