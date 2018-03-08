# -*- coding: utf-8 -*
import ddt

from tests.api.utils import saml_helper
from tests.api.utils.create_cert import create_self_signed_cert
from tests.api.v2.federation import federation
from tests.api.v2.models import factory
from tests.api.v2.models import responses

from tests.package.johny import constants as const


@ddt.ddt
class TestDeleteFederatedUser(federation.TestBaseFederation):

    """Delete Federated User Tests."""

    @classmethod
    def setUpClass(cls):
        """Class level set up for the tests."""
        super(TestDeleteFederatedUser, cls).setUpClass()

        # Add User
        cls.domain_id = cls.generate_random_string(const.DOMAIN_PATTERN)
        cls.user_admin_client = cls.generate_client(
            parent_client=cls.identity_admin_client,
            additional_input_data={'domain_id': cls.domain_id})

        # create a cert
        (cls.pem_encoded_cert, cls.cert_path, _, cls.key_path,
         cls.f_print) = create_self_signed_cert()

        # Add IDP with domain belonging to the user
        cls.idp_request_object = factory.get_add_idp_request_object(
            public_certificates=[cls.pem_encoded_cert],
            approved_domain_ids=[cls.domain_id])
        cls.identity_admin_client.create_idp(cls.idp_request_object)

    def setUp(self):
        super(TestDeleteFederatedUser, self).setUp()

        subject = self.generate_random_string(
            pattern=const.FED_USER_PATTERN)
        self.assertion = saml_helper.create_saml_assertion_v2(
            domain=self.domain_id,
            username=subject,
            issuer=self.idp_request_object.issuer,
            email=const.EMAIL_RANDOM,
            private_key_path=self.key_path,
            public_key_path=self.cert_path, response_flavor='v2DomainOrigin')

        saml_resp = self.identity_admin_client.auth_with_saml(
            saml=self.assertion, content_type=const.XML,
            base64_url_encode=False,
            new_url=False)
        self.assertEquals(saml_resp.status_code, 200)
        self.auth_resp = responses.Access(saml_resp.json())

    def test_delete_federated_user(self):
        resp = self.user_admin_client.delete_user(
            user_id=self.auth_resp.access.user.id)
        self.assertEquals(resp.status_code, 204)

        saml_resp = self.identity_admin_client.auth_with_saml(
            saml=self.assertion, content_type=const.XML,
            base64_url_encode=False,
            new_url=False)
        self.assertEquals(saml_resp.status_code, 200)

    def test_validate_token_for_deleted_federated_user(self):
        resp = self.user_admin_client.delete_user(
            user_id=self.auth_resp.access.user.id)
        self.assertEquals(resp.status_code, 204)

        validate_token_resp = self.user_admin_client.validate_token(
            token_id=self.auth_resp.access.token.id)
        # TODO: Need to confirm if the expected response is 403 or 404
        self.assertEquals(validate_token_resp.status_code, 403)

        validate_token_resp = self.identity_admin_client.validate_token(
            token_id=self.auth_resp.access.token.id)
        self.assertEquals(validate_token_resp.status_code, 404)

    def tearDown(self):
        super(TestDeleteFederatedUser, self).tearDown()

    @classmethod
    def tearDownClass(cls):
        super(TestDeleteFederatedUser, cls).tearDownClass()
