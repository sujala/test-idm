# -*- coding: utf-8 -*
import ddt
from munch import Munch

from tests.api.utils import func_helper
from tests.api.v2.federation import federation
from tests.api.v2.models import factory, responses

from tests.api.utils.create_cert import create_self_signed_cert
from tests.api.utils import saml_helper

from tests.package.johny import constants as const
from tests.package.johny.v2.models import requests


@ddt.ddt
class TestDelegationWithFederation(federation.TestBaseFederation):

    """
    Tests for Fed Users creating Delegation agreements, either directly or
    as being member of a user group
    """

    @classmethod
    def setUpClass(cls):
        """
        Class level set up for the tests
        Create users needed for the tests and generate clients for those users.
        """
        super(TestDelegationWithFederation, cls).setUpClass()

        cls.rcn = cls.test_config.da_rcn
        cls.domain_id = cls.create_domain_with_rcn()
        additional_input_data = {
            'domain_id': cls.domain_id
        }
        cls.user_admin_client = cls.generate_client(
            parent_client=cls.identity_admin_client,
            additional_input_data=additional_input_data)

        cls.domain_id_2 = cls.create_domain_with_rcn()
        input_data = {
            'domain_id': cls.domain_id_2
        }
        req_object = factory.get_add_user_request_object(
            input_data=input_data)
        resp = cls.identity_admin_client.add_user(req_object)
        cls.user_admin_2 = responses.User(resp.json())

    def setUp(self):
        super(TestDelegationWithFederation, self).setUp()
        self.group_ids = []

    @classmethod
    def create_domain_with_rcn(cls):

        domain_id = func_helper.generate_randomized_domain_id(
            client=cls.identity_admin_client)
        dom_req = requests.Domain(
            domain_name=domain_id, domain_id=domain_id, rcn=cls.rcn)
        add_dom_resp = cls.identity_admin_client.add_domain(dom_req)
        assert add_dom_resp.status_code == 201, (
            'domain was not created successfully')
        return domain_id

    @ddt.data(['xml', False, False], ['formEncode', True, False],
              ['xml', False, True], ['formEncode', True, True])
    @ddt.unpack
    def test_delegation_agreement_crd_by_fed_user(
            self, output_format, base64_url_encode, new_url):

        (pem_encoded_cert, cert_path, _, key_path,
         f_print) = create_self_signed_cert()

        self.user_admin_client.serialize_format = 'xml'
        self.user_admin_client.default_headers[
            const.CONTENT_TYPE] = 'application/xml'
        self.add_idp_with_metadata_return_id(
            cert_path=cert_path, api_client=self.user_admin_client)

        subject = self.generate_random_string(
            pattern='fed[\-]user[\-][\d\w]{12}')
        # create v2 saml assertion
        assertion = saml_helper.create_saml_assertion_v2(
            domain=self.domain_id, username=subject, issuer=self.issuer,
            email='meow@cats.com', private_key_path=key_path,
            public_key_path=cert_path, seconds_to_expiration=300,
            response_flavor='v2DomainOrigin', output_format=output_format)
        # saml auth
        resp = self.identity_admin_client.auth_with_saml(
            saml=assertion, base64_url_encode=base64_url_encode,
            new_url=new_url)
        self.assertEqual(resp.status_code, 200)
        fed_user_token = resp.json()[const.ACCESS][const.TOKEN][const.ID]
        fed_client = self.generate_client(token=fed_user_token)

        da_name = self.generate_random_string(
            pattern=const.DELEGATION_AGREEMENT_NAME_PATTERN)
        da_req = requests.DelegationAgreements(da_name=da_name)

        self.validate_da_crd(client=fed_client, da_req=da_req)

    @ddt.data(['xml', False, False], ['formEncode', True, False],
              ['xml', False, True], ['formEncode', True, True])
    @ddt.unpack
    def test_delegation_agreement_crd_by_user_group(
            self, output_format, base64_url_encode, new_url):

        # create user groups for domain
        group_one = self.create_and_add_user_group_to_domain(
            self.user_admin_client, self.domain_id)
        self.group_ids.append((group_one.id, self.domain_id))

        (pem_encoded_cert, cert_path, _, key_path,
         f_print) = create_self_signed_cert()

        idp_id = self.add_idp_with_metadata_return_id(
            cert_path=cert_path, api_client=self.user_admin_client)
        self.update_mapping_policy(
            idp_id=idp_id,
            client=self.user_admin_client,
            file_path='yaml/mapping_policy_with_groups.yaml')

        subject = self.generate_random_string(
            pattern='fed[\-]user[\-][\d\w]{12}')
        # create v2 saml assertion.....adding fed user to user-group as well
        assertion = saml_helper.create_saml_assertion_v2(
            domain=self.domain_id, username=subject, issuer=self.issuer,
            email='meow@cats.com', private_key_path=key_path,
            public_key_path=cert_path, seconds_to_expiration=300,
            response_flavor='v2DomainOrigin', groups=[group_one.name],
            output_format=output_format)
        # saml auth
        resp = self.identity_admin_client.auth_with_saml(
            saml=assertion, base64_url_encode=base64_url_encode,
            new_url=new_url)
        self.assertEqual(resp.status_code, 200)
        fed_auth_resp = Munch.fromDict(resp.json())
        fed_user_token = fed_auth_resp.access.token.id
        fed_client = self.generate_client(token=fed_user_token)

        da_name = self.generate_random_string(
            pattern=const.DELEGATION_AGREEMENT_NAME_PATTERN)
        da_req = requests.DelegationAgreements(
            da_name=da_name,
            principal_id=group_one.id,
            principal_type=const.USER_GROUP)
        self.validate_da_crd(client=fed_client, da_req=da_req)

    def validate_da_crd(self, client, da_req):

        # fed user creating the DA
        da_resp = client.create_delegation_agreement(request_object=da_req)
        self.assertEqual(da_resp.status_code, 201)

        da = Munch.fromDict(da_resp.json())
        da_id = da[const.RAX_AUTH_DELEGATION_AGREEMENT].id

        # fed user getting the DA
        get_resp = client.get_delegation_agreement(da_id=da_id)
        self.assertEqual(get_resp.status_code, 200)

        # fed user deleting the DA
        get_resp = client.delete_delegation_agreement(da_id=da_id)
        self.assertEqual(get_resp.status_code, 204)
        get_resp = client.get_delegation_agreement(da_id=da_id)
        self.assertEqual(get_resp.status_code, 404)

    @federation.base.base.log_tearDown_error
    def tearDown(self):
        for group_id, domain_id in self.group_ids:
            resp = self.identity_admin_client.delete_user_group_from_domain(
                group_id=group_id, domain_id=domain_id)
            self.assertEqual(
                resp.status_code, 204,
                msg='User group with ID {0} failed to delete'.format(
                    group_id))
        super(TestDelegationWithFederation, self).tearDown()

    @classmethod
    @federation.base.base.log_tearDown_error
    def tearDownClass(cls):
        # domain 1
        cls.delete_client(cls.user_admin_client)

        # domain 2
        resp = cls.identity_admin_client.delete_user(cls.user_admin_2.id)
        assert resp.status_code == 204, (
            'User with ID {0} failed to delete'.format(cls.user_admin_2.id))
        disable_domain_req = requests.Domain(enabled=False)
        cls.identity_admin_client.update_domain(
            domain_id=cls.domain_id_2, request_object=disable_domain_req)
        resp = cls.identity_admin_client.delete_domain(cls.domain_id_2)
        assert resp.status_code == 204, (
            'Domain with ID {0} failed to delete'.format(cls.domain_id_2))
        super(TestDelegationWithFederation, cls).tearDownClass()
