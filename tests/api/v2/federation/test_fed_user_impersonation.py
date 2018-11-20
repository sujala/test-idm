# -*- coding: utf-8 -*
from nose.plugins.attrib import attr
from qe_coverage.opencafe_decorators import tags, unless_coverage

from tests.api.v2 import base
from tests.api.v2.federation import federation
from tests.api.v2.schema import tokens as tokens_json

from tests.api.utils.create_cert import create_self_signed_cert
from tests.api.utils import func_helper
from tests.api.utils import saml_helper

from tests.package.johny import constants as const
from tests.package.johny.v2.models import requests


class TestFedUserImpersonation(federation.TestBaseFederation):

    """Tests for Fed User Impersonation."""

    @classmethod
    @unless_coverage
    def setUpClass(cls):
        """Class level set up for the tests

        Create users needed for the tests and generate clients for those users.
        """
        super(TestFedUserImpersonation, cls).setUpClass()
        cls.domain_id = func_helper.generate_randomized_domain_id(
            client=cls.identity_admin_client)
        cls.user_admin_client = cls.generate_client(
            parent_client=cls.identity_admin_client,
            additional_input_data={'domain_id': cls.domain_id},
            one_call=True)
        cls.user_admin_client.serialize_format = 'xml'
        cls.user_admin_client.default_headers[
            const.CONTENT_TYPE] = 'application/xml'

        cls.domain_ids = []

        cls.domain_ids.append(cls.domain_id)

    @unless_coverage
    def setUp(self):
        super(TestFedUserImpersonation, self).setUp()

    @tags('positive', 'p1', 'regression')
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
        impersonation_token = resp.json()[const.ACCESS][const.TOKEN][const.ID]
        self.validate_auth_by_in_auth_with_token_and_tenant(
            imp_token=impersonation_token)

        # CID-1789 check that token obtained by impersonating fed user
        # using racker can get fed user's domain
        racker_imp_client = self.generate_client(token=impersonation_token)
        self.validate_get_domain_by_imp_client(client=racker_imp_client)

        # Impersonate with identity admin client
        resp = self.identity_admin_client.impersonate_user(
            request_data=impersonation_request_obj)
        self.assertEqual(resp.status_code, 200)
        self.assertSchema(response=resp,
                          json_schema=tokens_json.impersonation_item)
        impersonation_token = resp.json()[const.ACCESS][const.TOKEN][const.ID]
        self.validate_auth_by_in_auth_with_token_and_tenant(
            imp_token=impersonation_token)

        # CID-1789 check that token obtained by impersonating fed user
        # using identity admin can get fed user's domain
        idadmin_imp_client = self.generate_client(token=impersonation_token)
        self.validate_get_domain_by_imp_client(client=idadmin_imp_client)

    def validate_get_domain_by_imp_client(self, client):
        get_dom_resp = client.get_domain(domain_id=self.domain_id)
        self.assertEqual(get_dom_resp.status_code, 200)
        self.assertEqual(get_dom_resp.json()[const.RAX_AUTH_DOMAIN][const.ID],
                         self.domain_id)

    def validate_auth_by_in_auth_with_token_and_tenant(self, imp_token):

        # Checking authBy field for auth with imp token & tenant
        req_obj = requests.AuthenticateAsTenantWithToken(
            token_id=imp_token, tenant_id=self.domain_id)
        auth_resp = self.identity_admin_client.get_auth_token(
            request_object=req_obj)
        auth_by_list = auth_resp.json()[const.ACCESS][const.TOKEN][
            const.RAX_AUTH_AUTHENTICATED_BY]
        self.assertIn(const.IMPERSONATE, auth_by_list)
        self.assertIn(const.PASSWORD.upper(), auth_by_list)

    @tags('positive', 'p1', 'regression')
    @attr('skip_at_gate')
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
        analyze_token_resp = self.identity_admin_client.analyze_token(
            idm_url=self.identity_config.idm_url)
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
        analyze_token_resp = self.identity_admin_client.analyze_token(
            idm_url=self.identity_config.idm_url)
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
        analyze_token_resp = self.identity_admin_client.analyze_token(
            idm_url=self.identity_config.idm_url)
        self.assertEqual(analyze_token_resp.status_code, 200)
        self.assertSchema(
            response=analyze_token_resp,
            json_schema=tokens_json.analyze_token_fed_user_impersonation)

    @base.base.log_tearDown_error
    @unless_coverage
    def tearDown(self):
        super(TestFedUserImpersonation, self).tearDown()

    @classmethod
    @base.base.log_tearDown_error
    @unless_coverage
    def tearDownClass(cls):
        super(TestFedUserImpersonation, cls).tearDownClass()

        resp = cls.user_admin_client.list_users()
        users = resp.json()[const.USERS]
        user_ids = [user[const.ID] for user in users]

        for user_id in user_ids:
            resp = cls.identity_admin_client.delete_user(user_id=user_id)
            assert resp.status_code == 204, \
                'User with ID {0} failed to delete'.format(user_id)

        for domain_id in cls.domain_ids:
            disable_domain_req = requests.Domain(enabled=False)
            cls.identity_admin_client.update_domain(
                domain_id=domain_id, request_object=disable_domain_req)
            resp = cls.identity_admin_client.delete_domain(domain_id=domain_id)
            assert resp.status_code == 204, \
                'Domain with ID {0} failed to delete'.format(domain_id)
