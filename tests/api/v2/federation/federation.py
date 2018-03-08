# -*- coding: utf-8 -*
import copy
import os

from tests.api.utils import saml_helper
from tests.api.utils.create_cert import create_self_signed_cert

from tests.api.v2 import base
from tests.api.v2.models import factory
from tests.api.v2.schema import idp as idp_json
from tests.api.v2.schema import tokens as tokens_json

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

        cls.test_email = 'random@rackspace.com'
        cls.updated_fed_auth_schema = copy.deepcopy(tokens_json.auth)
        cls.updated_fed_auth_schema['properties'][const.ACCESS]['properties'][
            const.USER]['properties'][const.NS_FEDERATED_IDP] = (
                {'type': 'string'})
        cls.updated_fed_auth_schema['properties'][const.ACCESS]['properties'][
            const.USER]['required'] += [const.NS_FEDERATED_IDP]

    def setUp(self):
        super(TestBaseFederation, self).setUp()
        self.provider_ids = []
        self.user_ids = []
        self.domain_ids = []

    def add_idp_with_metadata(self, cert_path, api_client, issuer=None):
        # Add IDP with metadata, Validate the response code & body.
        if issuer:
            self.issuer = issuer
        else:
            self.issuer = self.generate_random_string(
                pattern='https://issuer[\d\w]{12}.com')
        auth_url = self.generate_random_string(
            pattern='auth[\-]url[\-][\d\w]{12}')

        idp_metadata = saml_helper.create_metadata(
            issuer=self.issuer, auth_url=auth_url,
            public_key_path=cert_path)

        idp_request_object = requests.IDPMetadata(metadata=idp_metadata)
        resp = api_client.create_idp(
            request_object=idp_request_object)

        return resp

    def add_idp_with_metadata_return_id(self, cert_path, api_client):
        # Add IDP with metadata, Validate the response code & body.
        resp = self.add_idp_with_metadata(
            cert_path=cert_path, api_client=api_client)

        self.assertEqual(resp.status_code, 201)

        idp_id = resp.json()[const.NS_IDENTITY_PROVIDER][const.ID]
        self.provider_ids.append(idp_id)

        updated_idp_schema = copy.deepcopy(idp_json.identity_provider)
        updated_idp_schema[const.PROPERTIES][const.NS_IDENTITY_PROVIDER][
            const.REQUIRED] += [const.PUBLIC_CERTIFICATES]
        self.assertSchema(response=resp,
                          json_schema=updated_idp_schema)
        return idp_id

    def create_idp_helper(self, dom_ids=None, fed_type=None, certs=None):
        dom_group = None
        if fed_type == const.BROKER:
            dom_group = const.GLOBAL.upper()
        request_object = factory.get_add_idp_request_object(
            approved_domain_ids=dom_ids, federation_type=fed_type,
            approved_domain_group=dom_group, public_certificates=certs)
        resp = self.identity_admin_client.create_idp(request_object)
        self.assertEquals(resp.status_code, 201)
        self.provider_ids.append(resp.json()[
            const.NS_IDENTITY_PROVIDER][const.ID])
        return request_object

    def check_bad_name(self, name):
        """ Helper method to isolate repeating bad name code. """
        request_object = factory.get_add_idp_request_object()
        request_object.idp_name = name
        resp = self.identity_admin_client.create_idp(request_object)
        self.assertEquals(resp.status_code, 400)
        self.assertEquals(resp.json()[const.BAD_REQUEST][const.MESSAGE],
                          "Error code: 'GEN-005'; Identity provider name must"
                          " consist of only alphanumeric, '.', and '-'"
                          " characters.")

    def add_and_check_broker_idp(self, certs=None):
        idp = self.create_idp_helper(fed_type=const.BROKER, certs=certs)
        idp_list = self.identity_admin_client.list_idp(
            option={"name": idp.idp_name}).json()[const.NS_IDENTITY_PROVIDERS]
        self.assertEquals(len(idp_list), 1)
        self.assertEquals(idp_list[0][const.FEDERATION_TYPE], "BROKER")
        get_resp = self.identity_admin_client.get_idp(
            idp_id=idp_list[0][const.ID])
        get_name = get_resp.json()[const.NS_IDENTITY_PROVIDER][const.NAME]
        self.assertEquals(get_name, idp.idp_name)

        self.assertEquals(get_resp.json()[
            const.NS_IDENTITY_PROVIDER][const.FEDERATION_TYPE], "BROKER")
        return idp

    def fed_user_call(self, test_data, domain_id, private_key,
                      public_key, issuer, auth_client=None):

        # Check what happens with the fed users under that domain
        subject = self.generate_random_string(
            pattern='fed[\-]user[\-][\d\w]{12}')
        fed_input_data = test_data['fed_input']
        base64_url_encode = fed_input_data['base64_url_encode']
        new_url = fed_input_data['new_url']
        content_type = fed_input_data['content_type']

        roles = None
        if 'roles' in fed_input_data:
            roles = fed_input_data['roles']

        if fed_input_data['fed_api'] == 'v2':
            cert = saml_helper.create_saml_assertion_v2(
                domain=domain_id, username=subject, issuer=issuer,
                email=self.test_email, private_key_path=private_key,
                public_key_path=public_key, response_flavor='v2DomainOrigin',
                output_format='formEncode', roles=roles)
        else:
            cert = saml_helper.create_saml_assertion_v2(
                domain=domain_id, username=subject, issuer=issuer,
                email=self.test_email, private_key_path=private_key,
                public_key_path=public_key, response_flavor='v2DomainOrigin')

        if auth_client is None:
            auth_client = self.identity_admin_client
        auth = auth_client.auth_with_saml(
            saml=cert, content_type=content_type,
            base64_url_encode=base64_url_encode, new_url=new_url)
        return auth

    def create_one_user_and_get_domain(self, auth_client=None, users=None):

        request_object = factory.get_add_user_one_call_request_object()

        if auth_client is None:
            auth_client = self.identity_admin_client

        user_resp = auth_client.add_user(request_object)

        if users is not None:
            users.append(user_resp.json()[const.USER][const.ID])
        return user_resp.json()[const.USER][const.RAX_AUTH_DOMAIN_ID]

    def create_idp_with_certs(self, domain_id, issuer, auth_client=None):

        (pem_encoded_cert, cert_path, _, key_path,
         f_print) = create_self_signed_cert()
        request_object = factory.get_add_idp_request_object(
            federation_type='DOMAIN', approved_domain_ids=[domain_id],
            issuer=issuer, public_certificates=[pem_encoded_cert])

        if auth_client is None:
            auth_client = self.identity_admin_client
        resp = auth_client.create_idp(request_object)
        self.assertEquals(resp.status_code, 201)

        updated_idp_schema = copy.deepcopy(idp_json.identity_provider)
        updated_idp_schema[const.PROPERTIES][const.NS_IDENTITY_PROVIDER][
            const.REQUIRED] += [const.PUBLIC_CERTIFICATES]
        self.assertSchema(response=resp, json_schema=updated_idp_schema)

        provider_id = resp.json()[const.NS_IDENTITY_PROVIDER][const.ID]
        self.provider_ids.append(provider_id)

        return provider_id, cert_path, key_path

    def parse_auth_response(self, response):

        fed_token = response.json()[const.ACCESS][const.TOKEN][const.ID]
        fed_user_id = response.json()[const.ACCESS][const.USER][const.ID]
        fed_username = response.json()[const.ACCESS][const.USER][const.NAME]
        return fed_token, fed_user_id, fed_username

    def update_mapping_policy(self, idp_id, client,
                              file_path='yaml/default_mapping_policy.yaml'):
        curr_dir = os.path.dirname(os.path.realpath(__file__))
        absolute_file_path = os.path.join(curr_dir, file_path)
        with open(absolute_file_path) as file_read:
            mapping = file_read.read()
        update_idp_mapping_resp = client.add_idp_mapping(
            idp_id=idp_id, request_data=mapping, content_type=const.YAML)
        assert update_idp_mapping_resp.status_code == 204

    @classmethod
    def create_user(self):
        user_name = self.generate_random_string()
        password = self.generate_random_string(const.PASSWORD_PATTERN)
        request_object = requests.UserAdd(user_name=user_name,
                                          password=password)
        self.service_admin_client.add_user(request_object)

        req_obj = requests.AuthenticateWithPassword(
            user_name=user_name,
            password=password)
        return req_obj

    @classmethod
    def create_identity_admin_with_role(self, role):
        user_obj = self.create_user()
        idp_ia_client = client.IdentityAPIClient(
            url=self.url,
            serialize_format=self.test_config.serialize_format,
            deserialize_format=self.test_config.deserialize_format)

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
            self.add_role_by_name_to_user(
                user_id=identity_admin_id, role_name=role)
        return idp_ia_client

    @classmethod
    def add_role_by_name_to_user(self, user_id, role_name):
        if self.test_config.run_service_admin_tests:
            option = {
                const.PARAM_ROLE_NAME: role_name
            }
            list_resp = self.service_admin_client.list_roles(option=option)
            mapping_rules_role_id = list_resp.json()[const.ROLES][0][const.ID]
            self.service_admin_client.add_role_to_user(
                user_id=user_id, role_id=mapping_rules_role_id)

    def tearDown(self):
        # Delete all providers created in the tests

        for id_ in self.provider_ids:
            self.identity_admin_client.delete_idp(idp_id=id_)
        # Delete all users created in the tests
        for id in self.user_ids:
            self.identity_admin_client.delete_user(user_id=id)
        for dom in self.domain_ids:
            disable_domain_req = requests.Domain(enabled=False)
            self.identity_admin_client.update_domain(
                domain_id=dom, request_object=disable_domain_req)
            self.identity_admin_client.delete_domain(domain_id=dom)
        super(TestBaseFederation, self).tearDown()

    @classmethod
    def tearDownClass(cls):
        super(TestBaseFederation, cls).tearDownClass()
