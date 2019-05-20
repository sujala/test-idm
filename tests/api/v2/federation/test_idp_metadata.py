# -*- coding: utf-8 -*
import copy

import pytest
from qe_coverage.opencafe_decorators import tags, unless_coverage

from tests.api.utils import func_helper
from tests.api.v2 import base
from tests.api.v2.federation import federation
from tests.api.v2.schema import idp as idp_json
from tests.api.v2.schema import users as user_json

from tests.api.utils.create_cert import create_self_signed_cert
from tests.api.utils import saml_helper

from tests.package.johny import constants as const
from tests.package.johny.v2.models import requests


class TestIDPMetadata(federation.TestBaseFederation):

    """IDP Metadata Tests."""

    @classmethod
    @unless_coverage
    def setUpClass(cls):
        """Class level set up for the tests

        Create users needed for the tests and generate clients for those users.
        """
        super(TestIDPMetadata, cls).setUpClass()
        cls.domain_id = func_helper.generate_randomized_domain_id(
            client=cls.identity_admin_client)
        cls.user_admin_client = cls.generate_client(
            parent_client=cls.identity_admin_client,
            additional_input_data={'domain_id': cls.domain_id})
        cls.user_admin_client.serialize_format = 'xml'
        cls.user_admin_client.default_headers[
            const.CONTENT_TYPE] = 'application/xml'

        # user default client
        cls.idp_user_default_client = cls.generate_client(
            parent_client=cls.user_admin_client,
            additional_input_data={'domain_id': cls.domain_id})
        cls.idp_user_default_client.serialize_format = 'xml'
        cls.idp_user_default_client.default_headers[
            const.CONTENT_TYPE] = 'application/xml'

        cls.domain_ids = []
        cls.domain_ids.append(cls.domain_id)

    @unless_coverage
    def setUp(self):
        super(TestIDPMetadata, self).setUp()

    @tags('positive', 'p0', 'regression')
    @pytest.mark.regression
    def test_add_idp_auth_fed_user(self):
        '''
        Test to Add IDP with metadata & auth as a fed user for the domain.
        It also verifies the phone pin for the fed user.
        '''
        (pem_encoded_cert, cert_path, _, key_path,
         f_print) = create_self_signed_cert()

        idp_id = self.add_idp_with_metadata_return_id(
            cert_path=cert_path, api_client=self.user_admin_client)

        # Get IDP Metadata - supports only XML response
        self.user_admin_client.default_headers[
            const.ACCEPT] = 'application/xml'
        self.user_admin_client.deserialize_format = 'xml'

        resp = self.user_admin_client.get_idp_metadata(idp_id=idp_id)
        self.assertEqual(resp.status_code, 200)

        # Switch back to Original Headers.
        self.user_admin_client.default_headers[const.ACCEPT] = \
            'application/' + self.test_config.deserialize_format
        self.user_admin_client.deserialize_format = \
            self.test_config.deserialize_format

        # Get IDP
        resp = self.user_admin_client.get_idp(idp_id=idp_id)
        self.assertEqual(resp.status_code, 200)

        updated_idp_schema = copy.deepcopy(idp_json.identity_provider)
        updated_idp_schema[const.PROPERTIES][const.NS_IDENTITY_PROVIDER][
            const.REQUIRED] += [const.PUBLIC_CERTIFICATES]
        self.assertSchema(response=resp, json_schema=updated_idp_schema)

        # V1 Federation - Auth as fed user in the registered domain
        subject = self.generate_random_string(
            pattern='fed[\-]user[\-][\d\w]{12}')
        assertion = saml_helper.create_saml_assertion_v2(
            domain=self.domain_id, username=subject, issuer=self.issuer,
            email='meow@cats.com', private_key_path=key_path,
            public_key_path=cert_path, seconds_to_expiration=300,
            response_flavor='v2DomainOrigin')
        resp = self.identity_admin_client.auth_with_saml(
            saml=assertion, content_type='xml',
            base64_url_encode=False, new_url=False)
        self.assertEqual(resp.status_code, 200)
        if self.test_config.deserialize_format == const.JSON:
            self.assertSchema(resp, json_schema=self.updated_fed_auth_schema)

        fed_user_id = resp.json()[const.ACCESS][const.USER][const.ID]
        get_resp = self.identity_admin_client.get_user(fed_user_id)

        updated_get_user_schema = copy.deepcopy(user_json.get_user)
        updated_get_user_schema[const.PROPERTIES][
            const.USER][const.REQUIRED] += [const.FEDERATED_IDP]

        # Currently, MFA-enabled attribute is not returned for fed users
        updated_get_user_schema[const.PROPERTIES][
            const.USER][const.REQUIRED].remove(
            const.RAX_AUTH_MULTI_FACTOR_ENABLED)
        self.assertSchema(response=get_resp,
                          json_schema=updated_get_user_schema)

        # verify a correct phone pin
        fed_user_phone_pin = resp.json()[const.ACCESS][const.USER][
            const.RAX_AUTH_PHONE_PIN]
        phone_pin_verify_req = requests.PhonePin(fed_user_phone_pin)
        verify_resp = self.identity_admin_client.verify_phone_pin_for_user(
            user_id=fed_user_id, request_object=phone_pin_verify_req)
        self.assertEqual(verify_resp.status_code, 200)

        # verify an incorrect phone pins
        wrong_pin = fed_user_phone_pin + '1'
        phone_pin_verify_req = requests.PhonePin(wrong_pin)
        verify_resp = self.identity_admin_client.verify_phone_pin_for_user(
            user_id=fed_user_id, request_object=phone_pin_verify_req)
        self.assertEqual(verify_resp.status_code, 200)

        wrong_pin = fed_user_phone_pin + ' '
        phone_pin_verify_req = requests.PhonePin(wrong_pin)
        verify_resp = self.identity_admin_client.verify_phone_pin_for_user(
            user_id=fed_user_id, request_object=phone_pin_verify_req)
        self.assertEqual(verify_resp.status_code, 200)

    @tags('positive', 'p0', 'regression')
    @pytest.mark.regression
    def test_update_idp_cert_w_metadata(self):
        '''
        Test to Add IDP with metadata & auth as a fed user for the domain.
        '''
        (pem_encoded_cert, cert_path, _, key_path,
         f_print) = create_self_signed_cert()

        idp_id = self.add_idp_with_metadata_return_id(
            cert_path=cert_path, api_client=self.user_admin_client)

        # Update IDP certs by replacing metadata
        (pem_encoded_cert, cert_path, _, key_path,
         f_print) = create_self_signed_cert()

        auth_url = self.generate_random_string(
            pattern='auth[\-]url[\-][\d\w]{12}')

        idp_metadata_updated = saml_helper.create_metadata(
            issuer=self.issuer, auth_url=auth_url,
            public_key_path=cert_path)

        idp_request_object = requests.IDPMetadata(
            metadata=idp_metadata_updated)
        resp = self.user_admin_client.update_idp_metadata(
            idp_id=idp_id, request_object=idp_request_object)

        resp = self.user_admin_client.get_idp(idp_id=idp_id)
        self.assertEqual(resp.status_code, 200)
        updated_idp_schema = copy.deepcopy(idp_json.identity_provider)
        updated_idp_schema[const.PROPERTIES][const.NS_IDENTITY_PROVIDER][
            const.REQUIRED] += [const.PUBLIC_CERTIFICATES]
        self.assertSchema(response=resp, json_schema=updated_idp_schema)

        # V1 Federation - Auth as fed user in the registered domain
        subject = self.generate_random_string(
            pattern='fed[\-]user[\-][\d\w]{12}')
        assertion = saml_helper.create_saml_assertion_v2(
            domain=self.domain_id, username=subject, issuer=self.issuer,
            email='meow@cats.com', private_key_path=key_path,
            public_key_path=cert_path, seconds_to_expiration=300,
            response_flavor='v2DomainOrigin')
        resp = self.identity_admin_client.auth_with_saml(
            saml=assertion, content_type='xml',
            base64_url_encode=False, new_url=False)
        self.assertEqual(resp.status_code, 200)
        if self.test_config.deserialize_format == const.JSON:
            self.assertSchema(resp, json_schema=self.updated_fed_auth_schema)

        # Add Certificate endpoint is not supported when IDP has metadata
        resp = self.user_admin_client.add_certificate(
            idp_id=idp_id, request_object=idp_request_object)
        self.assertEqual(resp.status_code, 403)

    @tags('positive', 'p0', 'regression')
    @pytest.mark.regression
    def test_list_idp(self):
        '''Test to List IDP.'''
        (pem_encoded_cert, cert_path, _, key_path,
         f_print) = create_self_signed_cert()

        self.add_idp_with_metadata_return_id(
            cert_path=cert_path, api_client=self.user_admin_client)

        resp = self.user_admin_client.list_idp()
        self.assertEqual(resp.status_code, 200)
        self.assertSchema(response=resp, json_schema=idp_json.list_idps)

    @tags('positive', 'p0', 'regression')
    @pytest.mark.regression
    def test_add_mapping_user_default(self):
        (pem_encoded_cert, cert_path, _, key_path,
         f_print) = create_self_signed_cert()

        resp = self.add_idp_with_metadata(
            cert_path=cert_path, api_client=self.idp_user_default_client)

        self.assertEqual(resp.status_code, 403)

    @tags('negative', 'p1', 'regression')
    def test_add_idp_with_dup_entityid(self):
        """
        CID-1156 was found during SIT by other team. Ideally would like to
        run this in Staging, but currently this module is setup in such a way
        that it would attempt to use service admin token. Hence, not tagging it
        as of now.
        TODO: Tag this test for staging asap once we get rid of service admin
        usage.
        """
        (pem_encoded_cert, cert_path, _, key_path,
         f_print) = create_self_signed_cert()

        resp = self.add_idp_with_metadata(
            cert_path=cert_path, api_client=self.user_admin_client)
        self.assertEqual(resp.status_code, 201)
        self.provider_ids.append(
            resp.json()[const.NS_IDENTITY_PROVIDER][const.ID])
        issuer = resp.json()[const.NS_IDENTITY_PROVIDER][const.ISSUER]

        resp = self.add_idp_with_metadata(
            cert_path=cert_path, api_client=self.user_admin_client,
            issuer=issuer)

        self.assertEqual(resp.status_code, 409)
        self.assertEqual(
            resp.json()[const.BAD_REQUEST][const.MESSAGE],
            "Error code: 'FED_IDP-004'; Provider already exists with this "
            "issuer or entityID")

    @unless_coverage
    def tearDown(self):
        super(TestIDPMetadata, self).tearDown()

    @classmethod
    @unless_coverage
    @base.base.log_tearDown_error
    def tearDownClass(cls):
        super(TestIDPMetadata, cls).tearDownClass()
        resp = cls.user_admin_client.list_users()
        users = resp.json()[const.USERS]
        # Listing all users except user-admin
        user_ids = [user[const.ID] for user in users if user[
            const.ID] != cls.user_admin_client.default_headers[
            const.X_USER_ID]]

        for user_id in user_ids:
            resp = cls.identity_admin_client.delete_user(user_id=user_id)
            assert resp.status_code == 204, \
                'User with ID {0} failed to delete'.format(user_id)
        resp = cls.identity_admin_client.delete_user(
            user_id=cls.user_admin_client.default_headers[const.X_USER_ID])
        assert resp.status_code == 204, \
            'User with ID {0} failed to delete'.format(user_id)

        for domain_id in cls.domain_ids:
            disable_domain_req = requests.Domain(enabled=False)
            cls.identity_admin_client.update_domain(
                domain_id=domain_id, request_object=disable_domain_req)
            resp = cls.identity_admin_client.delete_domain(domain_id=domain_id)
            assert resp.status_code == 204, \
                'Domain with ID {0} failed to delete'.format(domain_id)
