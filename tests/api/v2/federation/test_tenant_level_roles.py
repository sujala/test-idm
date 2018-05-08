# -*- coding: utf-8 -*
import ddt
from nose.plugins.attrib import attr

from tests.api.utils import func_helper, saml_helper
from tests.api.utils.create_cert import create_self_signed_cert
from tests.api.v2 import base
from tests.api.v2.federation import federation
from tests.api.v2.models import factory, responses

from tests.package.johny import constants as const
from tests.package.johny.v2.models import requests


@ddt.ddt
class TestTenantLevelRolesForFederation(federation.TestBaseFederation):
    """
    Test add tenant level roles for federated users in saml auth and
    verify they are returned in auth response
    """
    @classmethod
    def setUpClass(cls):
        """Class level set up for the tests

        Create users needed for the tests and generate clients for those users.
        """
        super(TestTenantLevelRolesForFederation, cls).setUpClass()
        cls.test_email = 'random@rackspace.com'

    def setUp(self):
        super(TestTenantLevelRolesForFederation, self).setUp()
        self.provider_ids = []
        self.roles = []
        self.users = []

        self.domain_id = func_helper.generate_randomized_domain_id(
            client=self.identity_admin_client)
        request_object = factory.get_add_user_one_call_request_object(
            domainid=self.domain_id)
        self.user_admin_client = self.generate_client(
            parent_client=self.identity_admin_client,
            request_object=request_object)
        self.user_admin_client.serialize_format = const.XML
        self.user_admin_client.default_headers[
            const.CONTENT_TYPE] = 'application/xml'

    def fed_user_call(self, test_data, domain_id, private_key,
                      public_key, issuer, roles=None):

        # Check what happens with the fed users under that domain
        subject = self.generate_random_string(
            pattern='fed[\-]user[\-][\d\w]{12}')
        fed_input_data = test_data['fed_input']
        base64_url_encode = fed_input_data['base64_url_encode']
        new_url = fed_input_data['new_url']
        content_type = fed_input_data['content_type']

        cert = saml_helper.create_saml_assertion_v2(
            domain=domain_id, username=subject, issuer=issuer,
            email=self.test_email, private_key_path=private_key,
            public_key_path=public_key, response_flavor='v2DomainOrigin',
            output_format='formEncode', roles=roles)

        auth = self.identity_admin_client.auth_with_saml(
            saml=cert, content_type=content_type,
            base64_url_encode=base64_url_encode, new_url=new_url)
        return auth

    def create_idp_with_certs(self, domain_id, issuer, metadata=False):

        (pem_encoded_cert, cert_path, _, key_path,
         f_print) = create_self_signed_cert()
        if metadata:
            auth_url = self.generate_random_string(
                pattern='auth[\-]url[\-][\d\w]{12}')
            meta = saml_helper.create_metadata(
                issuer=issuer, auth_url=auth_url,
                public_key_path=cert_path)
            request_object = requests.IDPMetadata(metadata=meta)
            resp = self.user_admin_client.create_idp(request_object)
        else:
            request_object = factory.get_add_idp_request_object(
                federation_type='DOMAIN', approved_domain_ids=[domain_id],
                issuer=issuer, public_certificates=[pem_encoded_cert])
            resp = self.identity_admin_client.create_idp(request_object)
        self.assertEquals(resp.status_code, 201)
        provider_id = resp.json()[const.NS_IDENTITY_PROVIDER][const.ID]
        self.provider_ids.append(provider_id)

        return provider_id, cert_path, key_path

    def create_role(self):

        role_obj = factory.get_add_role_request_object(
            administrator_role=const.USER_MANAGE_ROLE_NAME)
        add_role_resp = self.identity_admin_client.add_role(
            request_object=role_obj)
        self.assertEqual(add_role_resp.status_code, 201)
        role = responses.Role(add_role_resp.json())
        self.roles.append(role.id)
        return role

    def validate_role_present(self, auth_resp, role, positive=True):

        role_present = False
        for role_ in auth_resp.json()[const.ACCESS][const.USER][const.ROLES]:
            if role_[const.ID] == role.id:
                self.assertEqual(role_[const.TENANT_ID], self.domain_id)
                role_present = True
        self.assertEqual(role_present, positive)

    @attr(type='regression')
    @ddt.file_data('data_tenant_level_roles.json')
    def test_tenant_level_roles_in_fed_auth(self, test_data):
        ''' Verify appropriate roles are on the federated users.'''

        issuer = self.generate_random_string(pattern='issuer[\-][\d\w]{12}')
        provider_id, cert_path, key_path = self.create_idp_with_certs(
            domain_id=self.domain_id, issuer=issuer, metadata=test_data[
                'fed_input']['metadata'])
        self.update_mapping_policy(idp_id=provider_id,
                                   client=self.user_admin_client)

        role_0 = self.create_role()
        # Adding newly created role on Mosso tenant
        role_to_add = '/'.join([role_0.name, self.domain_id])

        fed_auth = self.fed_user_call(
            test_data=test_data, domain_id=self.domain_id,
            private_key=key_path, public_key=cert_path, issuer=issuer,
            roles=[role_to_add])
        self.assertEqual(fed_auth.status_code, 200)
        self.assertSchema(fed_auth, json_schema=self.updated_fed_auth_schema)

        self.validate_role_present(auth_resp=fed_auth, role=role_0)
        fed_user_id = fed_auth.json()[const.ACCESS][const.USER][const.ID]
        # IdP deletion will automatically delete the fed users. But, we are
        # still explicitly delete fed users, for safer side. This will assure
        # successful cleanup of the domain in the teardown.
        self.users.append(fed_user_id)

        # Verifying if the new fed auth request comes in with different
        # roles, application updates the auth response accordingly.
        if test_data['fed_input']['update_roles']:
            roles = []
            for _ in range(2):
                roles.append(self.create_role())
            roles_to_add = []
            for role in roles:
                # Adding newly created role on Mosso tenant
                roles_to_add.append('/'.join([role.name, self.domain_id]))

            fed_auth = self.fed_user_call(
                test_data=test_data, domain_id=self.domain_id,
                private_key=key_path, public_key=cert_path, issuer=issuer,
                roles=roles_to_add)
            self.assertEqual(fed_auth.status_code, 200)
            self.assertSchema(
                fed_auth, json_schema=self.updated_fed_auth_schema)

            fed_user_id = fed_auth.json()[const.ACCESS][const.USER][const.ID]
            self.users.append(fed_user_id)
            for role in roles:
                self.validate_role_present(auth_resp=fed_auth, role=role)
            # Validate that previous role got removed from the user's roles
            self.validate_role_present(auth_resp=fed_auth, role=role_0,
                                       positive=False)

    @base.base.log_tearDown_error
    def tearDown(self):
        for user_id in self.users:
            resp = self.identity_admin_client.delete_user(user_id=user_id)
            self.assertIn(
                resp.status_code, [204, 404],
                msg='User with ID {0} failed to delete'.format(user_id))
        for role_id in self.roles:
            resp = self.identity_admin_client.delete_role(role_id=role_id)
            self.assertEqual(
                resp.status_code, 204,
                msg='Role with ID {0} failed to delete'.format(role_id))
        self.delete_client(self.user_admin_client)
        super(TestTenantLevelRolesForFederation, self).tearDown()

    @classmethod
    def tearDownClass(cls):
        super(TestTenantLevelRolesForFederation, cls).tearDownClass()
