# -*- coding: utf-8 -*
from tests.api.v2.federation import federation
from tests.api.v2.schema import tokens as tokens_json

from tests.api.utils.create_cert import create_self_signed_cert
from tests.api.utils import saml_helper

from tests.package.johny import constants as const
from tests.package.johny.v2.models import requests


class TestFedUserImpersonation(federation.TestBaseFederation):

    """Tests for Fed User Impersonation."""

    @classmethod
    def setUpClass(cls):
        """Class level set up for the tests

        Create users needed for the tests and generate clients for those users.
        """
        super(TestFedUserImpersonation, cls).setUpClass()
        cls.domain_id = cls.generate_random_string(pattern='[\d]{7}')
        cls.user_admin_client = cls.generate_client(
            parent_client=cls.identity_admin_client,
            additional_input_data={'domain_id': cls.domain_id})
        cls.user_admin_client.serialize_format = 'xml'
        cls.user_admin_client.default_headers[
            const.CONTENT_TYPE] = 'application/xml'

        cls.domain_ids = []

        cls.domain_ids.append(cls.domain_id)

    def setUp(self):
        super(TestFedUserImpersonation, self).setUp()

    def test_impersonate_fed_user(self):
        '''
        Test to impersonate fed user.
        '''
        (pem_encoded_cert, cert_path, _, key_path,
         f_print) = create_self_signed_cert()

        resp = self.add_idp_with_metadata_return_id(
            cert_path=cert_path, api_client=self.user_admin_client)

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

        # Impersonate federated user
        fed_user_name = resp.json()[const.ACCESS][const.USER][const.NAME]
        impersonation_request_obj = requests.ImpersonateUser(
            user_name=fed_user_name, idp=self.issuer)

        # Impersonate with racker client
        # See https://jira.rax.io/browse/CID-953
        racker_client = self.generate_racker_client()
        resp = racker_client.impersonate_user(
            request_data=impersonation_request_obj)
        self.assertEqual(resp.status_code, 200)
        self.assertSchema(response=resp,
                          json_schema=tokens_json.impersonation_item)

        # Impersonate with identity admin client
        resp = self.identity_admin_client.impersonate_user(
            request_data=impersonation_request_obj)
        self.assertEqual(resp.status_code, 200)
        self.assertSchema(response=resp,
                          json_schema=tokens_json.impersonation_item)

    def test_analyze_fed_user_tokens(self):
        (pem_encoded_cert, cert_path, _, key_path,
         f_print) = create_self_signed_cert()

        self.add_idp_with_metadata_return_id(
            cert_path=cert_path, api_client=self.user_admin_client)

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

        # Analyze federated user token
        token_id = resp.json()[const.ACCESS][const.TOKEN][const.ID]

        # Analyze Token
        # The identity_admin used should have the 'analyze-token' role inorder
        # to use the analyze token endpoint, else will result in HTTP 403.
        self.identity_admin_client.default_headers[const.X_SUBJECT_TOKEN] = \
            token_id
        analyze_token_resp = self.identity_admin_client.analyze_token()
        self.assertEqual(analyze_token_resp.status_code, 200)
        self.assertSchema(
            response=analyze_token_resp,
            json_schema=tokens_json.analyze_token)

        # Impersonate federated user
        fed_user_name = resp.json()[const.ACCESS][const.USER][const.NAME]
        impersonation_request_obj = requests.ImpersonateUser(
            user_name=fed_user_name, idp=self.issuer)

        # Impersonate with identity admin client
        resp = self.identity_admin_client.impersonate_user(
            request_data=impersonation_request_obj)
        self.assertEqual(resp.status_code, 200)

        token_id = resp.json()[const.ACCESS][const.TOKEN][const.ID]

        # Analyze Token
        self.identity_admin_client.default_headers[const.X_SUBJECT_TOKEN] = \
            token_id
        analyze_token_resp = self.identity_admin_client.analyze_token()
        self.assertEqual(analyze_token_resp.status_code, 200)
        self.assertSchema(
            response=analyze_token_resp,
            json_schema=tokens_json.analyze_token_fed_user_impersonation)

        # Impersonate with racker client
        # See https://jira.rax.io/browse/CID-953
        racker_client = self.generate_racker_client()
        resp = racker_client.impersonate_user(
            request_data=impersonation_request_obj)
        self.assertEqual(resp.status_code, 200)
        token_id = resp.json()[const.ACCESS][const.TOKEN][const.ID]

        # Analyze Token
        self.identity_admin_client.default_headers[const.X_SUBJECT_TOKEN] = \
            token_id
        analyze_token_resp = self.identity_admin_client.analyze_token()
        self.assertEqual(analyze_token_resp.status_code, 200)
        self.assertSchema(
            response=analyze_token_resp,
            json_schema=tokens_json.analyze_token_fed_user_impersonation)

    def tearDown(self):
        super(TestFedUserImpersonation, self).tearDown()
        for idp_id in self.provider_ids:
            # Fails with HTTP 403 - https://jira.rax.io/browse/CID-943
            self.user_admin_client.delete_idp(idp_id=idp_id)

    @classmethod
    def tearDownClass(cls):
        super(TestFedUserImpersonation, cls).tearDownClass()

        resp = cls.user_admin_client.list_users()
        users = resp.json()[const.USERS]
        user_ids = [user[const.ID] for user in users]

        for user_id in user_ids:
            resp = cls.identity_admin_client.delete_user(user_id=user_id)

        for domain_id in cls.domain_ids:
            disable_domain_req = requests.Domain(enabled=False)
            cls.identity_admin_client.update_domain(
                domain_id=domain_id, request_object=disable_domain_req)
            cls.identity_admin_client.delete_domain(domain_id=domain_id)
