from tests.api.v2.federation import federation

from tests.api.utils.create_cert import create_self_signed_cert
from tests.api.utils import saml_helper

from tests.package.johny import constants as const


class TestFedUserGlobalRoles(federation.TestBaseFederation):

    """Tests for Fed User's global roles."""

    @classmethod
    def setUpClass(cls):
        """Class level set up for the tests

        Create users needed for the tests and generate clients for those users.
        """
        super(TestFedUserGlobalRoles, cls).setUpClass()
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
        super(TestFedUserGlobalRoles, self).setUp()
        self.users = []

    def test_fed_user_global_roles(self):
        """
        Test to List fed user's global roles.
        """
        (pem_encoded_cert, cert_path, _, key_path,
         f_print) = create_self_signed_cert()

        provider_id = self.add_idp_with_metadata_return_id(
            cert_path=cert_path, api_client=self.user_admin_client)
        self.update_mapping_policy(idp_id=provider_id,
                                   client=self.user_admin_client)

        subject = self.generate_random_string(
            pattern='fed[\-]user[\-][\d\w]{12}')

        cert = saml_helper.create_saml_assertion_v2(
            domain=self.domain_id, username=subject, issuer=self.issuer,
            email=self.test_email, private_key_path=key_path,
            public_key_path=cert_path, response_flavor='v2DomainOrigin',
            output_format='formEncode')

        auth = self.identity_admin_client.auth_with_saml(
            saml=cert, content_type=const.X_WWW_FORM_URLENCODED,
            base64_url_encode=True, new_url=True)
        self.assertEqual(auth.status_code, 200)
        fed_user_id = auth.json()[const.ACCESS][const.USER][const.ID]
        fed_user_client = self.generate_client(
            token=auth.json()[const.ACCESS][const.TOKEN][const.ID])
        self.users.append(fed_user_id)

        self.validate_list_fed_users_global_roles(
            user_id=fed_user_id, client=self.user_admin_client)
        self.validate_list_fed_users_global_roles(
            user_id=fed_user_id, client=fed_user_client)

    def validate_list_fed_users_global_roles(self, user_id, client):
        list_resp = client.list_roles_for_user(
            user_id=user_id)
        self.assertEqual(list_resp.status_code, 200)
        role_ids = [role[const.ID] for role in list_resp.json()[const.ROLES]]
        self.assertIn(const.USER_DEFAULT_ROLE_ID, role_ids)

    def tearDown(self):

        for user_id in self.users:
            self.identity_admin_client.delete_user(user_id=user_id)
        self.delete_client(self.user_admin_client)
        super(TestFedUserGlobalRoles, self).tearDown()
