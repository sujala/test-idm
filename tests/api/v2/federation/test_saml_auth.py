# -*- coding: utf-8 -*
import pytest
from qe_coverage.opencafe_decorators import tags, unless_coverage
import ddt

from tests.api.utils import saml_helper
from tests.api.utils.create_cert import create_self_signed_cert
from tests.api.v2.federation import federation

from tests.package.johny import constants as const


@ddt.ddt
class TestSAMLAuth(federation.TestBaseFederation):

    """Add IDP Tests
    Currently only tests which involve setting the name."""

    @classmethod
    @unless_coverage
    def setUpClass(cls):
        """Class level set up for the tests

        Create users needed for the tests and generate clients for those users.
        """
        super(TestSAMLAuth, cls).setUpClass()

    @unless_coverage
    def setUp(self):
        super(TestSAMLAuth, self).setUp()
        # create a cert
        (self.pem_encoded_cert, self.cert_path, _, self.key_path,
         self.f_print) = create_self_signed_cert()

        self.idp = self.add_and_check_broker_idp(certs=[self.pem_encoded_cert])

    @tags('negative', 'p1', 'regression')
    @pytest.mark.skip_at_gate
    def test_cant_auth_with_broker_idp(self):
        """ Note: will fail once broker auth is enabled. """
        test_data = {"fed_input": {
                     "base64_url_encode": False,
                     "new_url": False,
                     "content_type": "xml"}}
        subject = self.generate_random_string(
            pattern='fed[\-]user[\-][\d\w]{12}')
        fed_input_data = test_data['fed_input']
        base64_url_encode = fed_input_data['base64_url_encode']
        new_url = fed_input_data['new_url']
        content_type = fed_input_data['content_type']

        dom_id = self.generate_random_string(const.NUMERIC_DOMAIN_ID_PATTERN)
        test_email = "random@rackspace.com"
        issuer = self.idp.issuer
        assertion = saml_helper.create_saml_assertion_v2(
            domain=dom_id, username=subject, issuer=issuer,
            email=test_email, private_key_path=self.key_path,
            public_key_path=self.cert_path, response_flavor='v2DomainOrigin')
        resp = self.identity_admin_client.auth_with_saml(
            saml=assertion, content_type=content_type,
            base64_url_encode=base64_url_encode, new_url=new_url)
        self.assertEqual(
            resp.json()[const.FORBIDDEN][const.MESSAGE],
            "Error code: 'FED2-004'; The Origin IDP is not valid")
        self.assertEqual(403, resp.status_code)

    @unless_coverage
    def tearDown(self):
        super(TestSAMLAuth, self).tearDown()

    @classmethod
    @unless_coverage
    def tearDownClass(cls):
        super(TestSAMLAuth, cls).tearDownClass()
