# -*- coding: utf-8 -*
import ddt
from munch import Munch

from nose.plugins.attrib import attr

from tests.api.utils import func_helper
from tests.api.v2.schema import tokens as tokens_json
from tests.api.v2.federation import federation

from tests.api.utils.create_cert import create_self_signed_cert
from tests.api.utils import saml_helper

from tests.package.johny import constants as const
from tests.package.johny.v2.models import requests


@ddt.ddt
class TestDelegationWithFederation(federation.TestBaseFederation):

    """
    Tests for Auth with Delegation agreements for federated users,
    either directly or as member of a user group.
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
        additional_input_data = {'domain_id': cls.domain_id}
        cls.user_admin_client = cls.generate_client(
            parent_client=cls.identity_admin_client,
            additional_input_data=additional_input_data)

        cls.domain_id_2 = cls.create_domain_with_rcn()
        additional_input_data = {'domain_id': cls.domain_id_2}

        cls.user_admin_client_2 = cls.generate_client(
            parent_client=cls.identity_admin_client,
            additional_input_data=additional_input_data)
        cls.user_admin_2 = cls.user_admin_client_2.default_headers[
            const.X_USER_ID]

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

    def create_fed_user_for_da(self, client, domain, issuer=None,
                               update_policy=False, group=None):
        (pem_encoded_cert, cert_path, _, key_path,
         f_print) = create_self_signed_cert()

        client.serialize_format = 'xml'
        client.default_headers[
            const.CONTENT_TYPE] = 'application/xml'
        if issuer:
            idp_resp = self.add_idp_with_metadata(
                cert_path=cert_path, api_client=self.user_admin_client_2,
                issuer=issuer)
            idp_id = idp_resp.json()[const.NS_IDENTITY_PROVIDER][const.ID]
            self.provider_ids.append(idp_id)
        else:
            idp_id = self.add_idp_with_metadata_return_id(
                cert_path=cert_path, api_client=client)
            issuer = self.issuer

            if update_policy:
                self.update_mapping_policy(
                    idp_id=idp_id,
                    client=client,
                    file_path='yaml/mapping_policy_with_groups.yaml')

        subject = self.generate_random_string(
            pattern='fed[\-]user[\-][\d\w]{12}')
        # create v2 saml assertion
        if group:
            assertion = saml_helper.create_saml_assertion_v2(
                domain=domain, username=subject, issuer=issuer,
                email='meow@cats.com', private_key_path=key_path,
                public_key_path=cert_path, seconds_to_expiration=300,
                response_flavor='v2DomainOrigin', output_format='formEncode',
                groups=[group.name])
        else:
            assertion = saml_helper.create_saml_assertion_v2(
                domain=domain, username=subject, issuer=issuer,
                email='meow@cats.com', private_key_path=key_path,
                public_key_path=cert_path, seconds_to_expiration=300,
                response_flavor='v2DomainOrigin', output_format='formEncode')

        # saml auth
        fed_auth_resp = self.identity_admin_client.auth_with_saml(
            saml=assertion, base64_url_encode=True,
            new_url=True)
        self.assertEqual(fed_auth_resp.status_code, 200)
        return fed_auth_resp

    def validate_response(self, resp):
        # Verifying that all auth methods are returned, ignoring the order
        for auth_by in [const.AUTH_BY_DELEGATION, const.AUTH_BY_PWD,
                        const.AUTH_BY_FEDERATED]:
            self.assertIn(auth_by, resp.json()[
                const.ACCESS][const.TOKEN][const.RAX_AUTH_AUTHENTICATED_BY])
        delegation_domain = resp.json()[const.ACCESS][const.USER][
            const.RAX_AUTH_DOMAIN_ID]
        self.assertEqual(delegation_domain, self.domain_id)

    @attr(type='regression')
    def test_d_auth_with_fed_users_as_principal_and_delegate(self):

        # Creating fed user principal
        fed_auth_resp = self.create_fed_user_for_da(
            client=self.user_admin_client, domain=self.domain_id, issuer=None)
        fed_user_token = fed_auth_resp.json()[const.ACCESS][const.TOKEN][
            const.ID]
        fed_client = self.generate_client(token=fed_user_token)

        # Creating fed user delegate
        issuer_2 = self.generate_random_string(pattern=const.ISSUER_PATTERN)
        fed_auth_resp = self.create_fed_user_for_da(
            client=self.user_admin_client_2, domain=self.domain_id_2,
            issuer=issuer_2)
        fed_auth_resp_parsed = Munch.fromDict(fed_auth_resp.json())
        fed_user_id = fed_auth_resp_parsed.access.user.id
        fed_user_token = fed_auth_resp_parsed.access.token.id

        # create DA
        da_name = self.generate_random_string(
            pattern=const.DELEGATION_AGREEMENT_NAME_PATTERN)
        da_req = requests.DelegationAgreements(da_name=da_name)
        da_resp = fed_client.create_delegation_agreement(request_object=da_req)
        self.assertEqual(da_resp.status_code, 201)
        da = Munch.fromDict(da_resp.json())
        da_id = da[const.RAX_AUTH_DELEGATION_AGREEMENT].id

        res = fed_client.add_user_delegate_to_delegation_agreement(
            da_id, fed_user_id)

        # DA auth using fed user's token
        delegation_auth_req = requests.AuthenticateWithDelegationAgreement(
            token=fed_user_token, delegation_agreement_id=da_id)
        resp = self.identity_admin_client.get_auth_token(delegation_auth_req)
        self.assertEqual(resp.status_code, 200)
        self.assertSchema(response=resp, json_schema=tokens_json.auth)

        self.validate_response(resp)
        da_token = resp.json()[const.ACCESS][const.TOKEN][const.ID]

        # Validate Auth Token
        resp = self.identity_admin_client.validate_token(token_id=da_token)
        self.assertEqual(resp.status_code, 200)
        self.assertSchema(
            response=resp, json_schema=tokens_json.validate_token)
        self.validate_response(resp)

    @attr(type='regression')
    def test_d_auth_fed_user_with_user_group_as_delegate(self):

        # create user group for domain 2
        group_two = self.create_and_add_user_group_to_domain(
            self.user_admin_client_2, self.domain_id_2)
        self.group_ids.append((group_two.id, self.domain_id_2))

        # creating idp, updating idp's policy before creating fed user so that
        # fed user can specify group membership
        fed_auth_resp = self.create_fed_user_for_da(
            client=self.user_admin_client_2, domain=self.domain_id_2,
            update_policy=True, group=group_two)
        fed_auth_resp_parsed = Munch.fromDict(fed_auth_resp.json())
        fed_user_token = fed_auth_resp_parsed.access.token.id

        da_name = self.generate_random_string(
            pattern=const.DELEGATION_AGREEMENT_NAME_PATTERN)
        da_req = requests.DelegationAgreements(da_name=da_name)
        # creating the DA
        da_resp = self.user_admin_client.create_delegation_agreement(
            request_object=da_req)
        self.assertEqual(da_resp.status_code, 201)
        da = Munch.fromDict(da_resp.json())
        da_id = da[const.RAX_AUTH_DELEGATION_AGREEMENT].id

        # adding user group as delegate
        ua_client = self.user_admin_client
        add_user_group_delegate_resp = (
            ua_client.add_user_group_delegate_to_delegation_agreement(
                da_id=da_id, user_group_id=group_two.id))
        self.assertEqual(add_user_group_delegate_resp.status_code, 204)

        # DA auth using fed user's token
        delegation_auth_req = requests.AuthenticateWithDelegationAgreement(
            token=fed_user_token, delegation_agreement_id=da_id)
        resp = self.identity_admin_client.get_auth_token(delegation_auth_req)
        self.assertEqual(resp.status_code, 200)
        self.assertSchema(response=resp, json_schema=tokens_json.auth)
        self.validate_response(resp)
        da_token = resp.json()[const.ACCESS][const.TOKEN][const.ID]

        # Validate Auth Token
        resp = self.identity_admin_client.validate_token(token_id=da_token)
        self.assertEqual(resp.status_code, 200)
        self.assertSchema(
            response=resp, json_schema=tokens_json.validate_token)
        self.validate_response(resp)

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
        resp = cls.identity_admin_client.delete_user(cls.user_admin_2)
        assert resp.status_code == 204, (
            'User with ID {0} failed to delete'.format(cls.user_admin_2))
        disable_domain_req = requests.Domain(enabled=False)
        cls.identity_admin_client.update_domain(
            domain_id=cls.domain_id_2, request_object=disable_domain_req)
        resp = cls.identity_admin_client.delete_domain(cls.domain_id_2)
        assert resp.status_code == 204, (
            'Domain with ID {0} failed to delete'.format(cls.domain_id_2))
        super(TestDelegationWithFederation, cls).tearDownClass()
