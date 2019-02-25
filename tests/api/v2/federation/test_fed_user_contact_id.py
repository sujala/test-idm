from nose.plugins.attrib import attr
from qe_coverage.opencafe_decorators import tags, unless_coverage

from tests.api.utils.create_cert import create_self_signed_cert
from tests.api.utils import func_helper
from tests.api.utils import saml_helper
from tests.api.v2 import base
from tests.api.v2.federation import federation
from tests.api.v2.schema import tokens as tokens_json

from tests.package.johny import constants as const
from tests.package.johny.v2.models import requests


class TestFedUserContactId(federation.TestBaseFederation):

    """Tests for Conatct Id on Fed User."""

    @classmethod
    @unless_coverage
    def setUpClass(cls):
        """Class level set up for the tests

        Create users needed for the tests and generate clients for those users.
        """
        super(TestFedUserContactId, cls).setUpClass()
        cls.domain_id = func_helper.generate_randomized_domain_id(
            client=cls.identity_admin_client)
        cls.user_admin_client = cls.generate_client(
            parent_client=cls.identity_admin_client,
            additional_input_data={'domain_id': cls.domain_id})
        cls.user_admin_client.serialize_format = 'xml'
        cls.user_admin_client.default_headers[
            const.CONTENT_TYPE] = 'application/xml'

        cls.domain_ids = []

        cls.domain_ids.append(cls.domain_id)

    @unless_coverage
    def setUp(self):
        super(TestFedUserContactId, self).setUp()
        self.users = []

    @tags('positive', 'p0', 'smoke')
    @attr(type='smoke_alpha')
    def test_fed_user_contact_id(self):
        """
        Test to Add Contact Id on a fed user.
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
        self.assertSchema(auth, json_schema=self.updated_fed_auth_schema)
        fed_token_id = auth.json()[const.ACCESS][const.TOKEN][const.ID]

        fed_user_id = auth.json()[const.ACCESS][const.USER][const.ID]
        self.users.append(fed_user_id)

        # Validate fed token returns the same fields as validate token
        # for provisioned users.
        resp = self.identity_admin_client.validate_token(
            token_id=fed_token_id)
        self.assertEqual(resp.status_code, 200)
        self.assertSchema(
            response=resp, json_schema=tokens_json.validate_token)

        # Add Contact ID to the fed user
        contact_id = self.generate_random_string(
            pattern='fed[\-]user[\-]contact[\-][\d]{12}')
        update_user_object = requests.UserUpdate(contact_id=contact_id)
        add_contact_resp = self.identity_admin_client.update_user(
            user_id=fed_user_id, request_object=update_user_object)
        self.assertEqual(add_contact_resp.status_code, 200)
        self.assertEqual(
            add_contact_resp.json()[const.USER][const.RAX_AUTH_CONTACTID],
            contact_id)

        # Get User By Id returns the ContactId
        resp = self.identity_admin_client.get_user(user_id=fed_user_id)
        self.assertEqual(resp.status_code, 200)
        self.assertEqual(
            resp.json()[const.USER][const.RAX_AUTH_CONTACTID],
            contact_id)

        # List Users In Domain returns the ContactId
        resp = self.identity_admin_client.list_users_in_domain(
            domain_id=self.domain_id)
        self.assertEqual(resp.status_code, 200)
        fed_user = [item for item in resp.json()[const.USERS] if
                    item[const.USERNAME] == subject][0]
        self.assertEqual(fed_user[const.RAX_AUTH_CONTACTID], contact_id)

        # List Users returns the ContactId
        resp = self.user_admin_client.list_users()
        self.assertEqual(resp.status_code, 200)
        fed_user = [item for item in resp.json()[const.USERS] if
                    item[const.USERNAME] == subject][0]
        self.assertEqual(fed_user[const.RAX_AUTH_CONTACTID], contact_id)

        # Validate Token returns the ContactId
        resp = self.identity_admin_client.validate_token(
            token_id=fed_token_id)
        self.assertEqual(resp.status_code, 200)
        self.assertEqual(
            resp.json()[const.ACCESS][const.USER][const.RAX_AUTH_CONTACTID],
            contact_id)
        # self.assertEqual(1, 2)

    @base.base.log_tearDown_error
    @unless_coverage
    def tearDown(self):

        for user_id in self.users:
            resp = self.identity_admin_client.delete_user(user_id=user_id)
            self.assertEqual(
                resp.status_code, 204,
                msg='User with ID {0} failed to delete'.format(user_id))

        super(TestFedUserContactId, self).tearDown()

    @classmethod
    @unless_coverage
    def tearDownClass(cls):
        cls.delete_client(cls.user_admin_client)
        super(TestFedUserContactId, cls).tearDownClass()
