# -*- coding: utf-8 -*
import copy

from tests.api.v2.federation import federation
from tests.api.v2.schema import idp as idp_json

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
        cls.idp_ids = []

        cls.domain_ids.append(cls.domain_id)

    def setUp(self):
        super(TestFedUserImpersonation, self).setUp()

    def add_idp_w_metadata(self, cert_path):
        # Add IDP with metadata, Validate the response code & body.
        self.issuer = self.generate_random_string(
            pattern='https://issuer[\d\w]{12}.com')
        auth_url = self.generate_random_string(
            pattern='auth[\-]url[\-][\d\w]{12}')

        idp_metadata = saml_helper.create_metadata(
            issuer=self.issuer, auth_url=auth_url,
            public_key_path=cert_path)

        idp_request_object = requests.IDPMetadata(metadata=idp_metadata)
        resp = self.user_admin_client.create_idp(
            request_object=idp_request_object)
        self.assertEqual(resp.status_code, 201)
        idp_id = resp.json()[const.NS_IDENTITY_PROVIDER][const.ID]
        self.idp_ids.append(idp_id)

        updated_idp_schema = copy.deepcopy(idp_json.identity_provider)
        updated_idp_schema[const.PROPERTIES][const.NS_IDENTITY_PROVIDER][
            const.REQUIRED] += [const.PUBLIC_CERTIFICATES]
        self.assertSchema(response=resp,
                          json_schema=updated_idp_schema)
        return idp_id

    def test_impersonate_fed_user(self):
        '''
        Test to impersonate fed user.
        '''
        (pem_encoded_cert, cert_path, _, key_path,
         f_print) = create_self_signed_cert()

        resp = self.add_idp_w_metadata(cert_path=cert_path)

        # V1 Federation - Auth as fed user in the registered domain
        subject = self.generate_random_string(
            pattern='fed[\-]user[\-][\d\w]{12}')
        assertion = saml_helper.create_saml_assertion(
            domain=self.domain_id, subject=subject, issuer=self.issuer,
            email='meow@cats.com', base64_url_encode=False,
            private_key_path=key_path,
            public_key_path=cert_path,
            seconds_to_expiration=300)
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

        # Impersonate with identity admin client
        resp = self.identity_admin_client.impersonate_user(
            request_data=impersonation_request_obj)
        self.assertEqual(resp.status_code, 200)

    def tearDown(self):
        super(TestFedUserImpersonation, self).tearDown()
        for idp_id in self.idp_ids:
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
            cls.identity_admin_client.delete_domain(domain_id=domain_id)
