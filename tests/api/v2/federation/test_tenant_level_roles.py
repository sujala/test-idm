# -*- coding: utf-8 -*
import ddt
import pytest
from qe_coverage.opencafe_decorators import tags, unless_coverage

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
    @unless_coverage
    def setUpClass(cls):
        """Class level set up for the tests

        Create users needed for the tests and generate clients for those users.
        """
        super(TestTenantLevelRolesForFederation, cls).setUpClass()
        cls.test_email = 'random@rackspace.com'
        cls.hierarchical_billing_observer_role_id = cls.get_role_id_by_name(
            role_name=const.HIERARCHICAL_BILLING_OBSERVER_ROLE_NAME)

    @unless_coverage
    def setUp(self):
        super(TestTenantLevelRolesForFederation, self).setUp()
        self.provider_ids = []
        self.roles = []
        self.users = []
        self.tenant_ids = []

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
                      public_key, issuer, roles=None, apply_rcn_roles=False):

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

        options = {
            const.QUERY_PARAM_APPLY_RCN_ROLES: apply_rcn_roles
        }
        auth = self.identity_admin_client.auth_with_saml(
            saml=cert, content_type=content_type,
            base64_url_encode=base64_url_encode, new_url=new_url,
            options=options)
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
        self.assertEqual(resp.status_code, 201)
        provider_id = resp.json()[const.NS_IDENTITY_PROVIDER][const.ID]
        self.provider_ids.append(provider_id)

        return provider_id, cert_path, key_path

    def validate_role_present(self, auth_resp, role, positive=True):

        role_present = False
        for role_ in auth_resp.json()[const.ACCESS][const.USER][const.ROLES]:
            if role_[const.ID] == role.id:
                self.assertEqual(role_[const.TENANT_ID], self.domain_id)
                role_present = True
        self.assertEqual(role_present, positive)

    @unless_coverage
    @pytest.mark.regression
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

        # Testing list effective roles for fed user, CID-1522
        eff_roles = self.user_admin_client.list_effective_roles_for_user(
            user_id=fed_user_id)
        roles_assignments = eff_roles.json()[const.RAX_AUTH_ROLE_ASSIGNMENTS][
            const.TENANT_ASSIGNMENTS]
        for assignment in roles_assignments:
            if assignment[const.ON_ROLE_NAME] == role_0.name:
                self.assertEqual(
                    assignment[const.SOURCES][0][const.ASSIGNMENT_TYPE],
                    const.TENANT_ASSIGNMENT_TYPE)
                self.assertEqual(
                    assignment[const.SOURCES][0][const.SOURCE_TYPE],
                    const.USER_SOURCE_TYPE)

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

    def create_tenant(self, domain=None, name=None, tenant_types=None):

        if not domain:
            domain = self.domain_id
        if name:
            tenant_req = factory.get_add_tenant_object(
                domain_id=domain, tenant_name=name, tenant_types=tenant_types)
        else:
            tenant_req = factory.get_add_tenant_object(
                domain_id=domain, tenant_types=tenant_types)
        add_tenant_resp = self.identity_admin_client.add_tenant(
            tenant=tenant_req)
        self.assertEqual(add_tenant_resp.status_code, 201)
        tenant = responses.Tenant(add_tenant_resp.json())
        self.tenant_ids.append(tenant.id)
        return tenant

    def set_up_role_and_tenant(self):

        # create role
        role_1 = self.create_role()
        # create tenant with type for which there is a whitelist filter
        tenant_name = ":".join([
            self.test_config.mpc_whitelist_tenant_type,
            self.generate_random_string(pattern=const.ID_PATTERN)])
        tenant_1 = self.create_tenant(
            name=tenant_name,
            tenant_types=[self.test_config.mpc_whitelist_tenant_type])
        return role_1, tenant_1

    @tags('positive', 'p0', 'regression')
    @pytest.mark.regression
    def test_whitelist_roles_for_fed_user_on_tenant(self):

        test_data = {"fed_input": {
            "base64_url_encode": True,
            "new_url": True,
            "content_type": "x-www-form-urlencoded",
            "fed_api": "v2",
            "metadata": True,
        }}
        issuer = self.generate_random_string(pattern='issuer[\-][\d\w]{12}')
        provider_id, cert_path, key_path = self.create_idp_with_certs(
            domain_id=self.domain_id, issuer=issuer, metadata=test_data[
                'fed_input']['metadata'])
        self.update_mapping_policy(idp_id=provider_id,
                                   client=self.user_admin_client)

        role_1, tenant_1 = self.set_up_role_and_tenant()
        self.validate_auth_with_fed_token(
            test_data=test_data, key_path=key_path, cert_path=cert_path,
            issuer=issuer, role=role_1.name, tenant=tenant_1,
            expected_response=401, apply_rcn_roles=True)

        self.validate_auth_with_fed_token(
            test_data=test_data, key_path=key_path, cert_path=cert_path,
            issuer=issuer, role=const.HIERARCHICAL_BILLING_OBSERVER_ROLE_NAME,
            tenant=tenant_1, expected_response=200, apply_rcn_roles=True)

    def validate_auth_with_fed_token(self, test_data, key_path, cert_path,
                                     issuer, role, tenant, expected_response,
                                     apply_rcn_roles=False):

        role_to_add = '/'.join([role, tenant.id])
        fed_auth = self.fed_user_call(
            test_data=test_data, domain_id=self.domain_id,
            private_key=key_path, public_key=cert_path, issuer=issuer,
            roles=[role_to_add], apply_rcn_roles=apply_rcn_roles)
        self.assertEqual(fed_auth.status_code, 200)
        # This method is for CID-1601
        self.validate_wl_role_in_fed_auth_response(
            auth=fed_auth, role=role, tenant=tenant,
            expected_response=expected_response)

        fed_user_id = fed_auth.json()[const.ACCESS][const.USER][const.ID]
        self.users.append(fed_user_id)
        fed_user_token = fed_auth.json()[const.ACCESS][const.TOKEN][const.ID]
        auth_with_token_req = requests.AuthenticateAsTenantWithToken(
            token_id=fed_user_token, tenant_id=tenant.id
        )
        auth_with_token_resp = self.identity_admin_client.get_auth_token(
            request_object=auth_with_token_req)
        self.assertEqual(auth_with_token_resp.status_code, expected_response)

    def validate_wl_role_in_fed_auth_response(
            self, auth, role, tenant, expected_response):

        roles = auth.json()[const.ACCESS][const.USER][const.ROLES]
        role_count = 0
        for role_ in roles:
            if role_[const.NAME] == role:
                self.assertEqual(role_[const.TENANT_ID], tenant.id)
                role_count += 1
        if expected_response == 401:
            self.assertEqual(role_count, 0)
        elif expected_response == 200:
            self.assertEqual(role_count, 1)

    @tags('positive', 'p0', 'regression')
    @pytest.mark.regression
    def test_list_wl_role_for_fed_user_on_tenant(self):

        test_data = {"fed_input": {
            "base64_url_encode": True,
            "new_url": True,
            "content_type": "x-www-form-urlencoded",
            "fed_api": "v2",
            "metadata": True,
        }}
        issuer = self.generate_random_string(pattern='issuer[\-][\d\w]{12}')
        provider_id, cert_path, key_path = self.create_idp_with_certs(
            domain_id=self.domain_id, issuer=issuer, metadata=test_data[
                'fed_input']['metadata'])
        self.update_mapping_policy(idp_id=provider_id,
                                   client=self.user_admin_client)

        role_1, tenant_1 = self.set_up_role_and_tenant()
        list_roles = self.fed_auth_and_call_get_roles_for_user_on_tenant(
            test_data=test_data, key_path=key_path, cert_path=cert_path,
            issuer=issuer, role=role_1.name, tenant=tenant_1,
            expected_response=200)
        self.assertEqual(list_roles.json()[
                    const.RAX_AUTH_ROLE_ASSIGNMENTS][
                    const.TENANT_ASSIGNMENTS], {})

        list_roles = self.fed_auth_and_call_get_roles_for_user_on_tenant(
            test_data=test_data, key_path=key_path, cert_path=cert_path,
            issuer=issuer, role=const.HIERARCHICAL_BILLING_OBSERVER_ROLE_NAME,
            tenant=tenant_1, expected_response=200)
        roles_assignments = list_roles.json()[const.RAX_AUTH_ROLE_ASSIGNMENTS][
            const.TENANT_ASSIGNMENTS]
        for assignment in roles_assignments:
            if assignment[const.ON_ROLE_NAME] == \
                                const.HIERARCHICAL_BILLING_OBSERVER_ROLE_NAME:
                self.assertEqual(
                    assignment[const.SOURCES][0][const.ASSIGNMENT_TYPE],
                    const.TENANT_ASSIGNMENT_TYPE)
                self.assertEqual(
                    assignment[const.SOURCES][0][const.SOURCE_TYPE],
                    const.USER_SOURCE_TYPE)
                self.assertEqual(
                    assignment[const.FOR_TENANTS][0],
                    tenant_1.id)

    def fed_auth_and_call_get_roles_for_user_on_tenant(
            self, test_data, key_path,
            cert_path, issuer, role, tenant, expected_response):

        role_to_add = '/'.join([role, tenant.id])
        fed_auth = self.fed_user_call(
            test_data=test_data, domain_id=self.domain_id,
            private_key=key_path, public_key=cert_path, issuer=issuer,
            roles=[role_to_add])
        self.assertEqual(fed_auth.status_code, 200)
        fed_user_id = fed_auth.json()[const.ACCESS][const.USER][const.ID]
        self.users.append(fed_user_id)
        fed_user_token = fed_auth.json()[const.ACCESS][const.TOKEN][const.ID]
        fed_user_client = self.generate_client(
            token=fed_user_token)
        list_roles_resp = fed_user_client.list_effective_roles_for_user(
                    user_id=fed_user_id,
                    params={const.ON_TENANT_ID: tenant.id})
        self.assertEqual(list_roles_resp.status_code, expected_response)
        return list_roles_resp

    @base.base.log_tearDown_error
    @unless_coverage
    def tearDown(self):
        for user_id in self.users:
            resp = self.identity_admin_client.delete_user(user_id=user_id)
            self.assertIn(
                resp.status_code, [204, 404],
                msg='User with ID {0} failed to delete'.format(user_id))
        for tenant_id in self.tenant_ids:
            resp = self.identity_admin_client.delete_tenant(
                tenant_id=tenant_id)
            self.assertIn(
                resp.status_code, [204, 404],
                msg='Tenant with ID {0} failed to delete'.format(tenant_id))
        for role_id in self.roles:
            resp = self.identity_admin_client.delete_role(role_id=role_id)
            self.assertEqual(
                resp.status_code, 204,
                msg='Role with ID {0} failed to delete'.format(role_id))
        self.delete_client(self.user_admin_client)
        super(TestTenantLevelRolesForFederation, self).tearDown()

    @classmethod
    @unless_coverage
    def tearDownClass(cls):
        super(TestTenantLevelRolesForFederation, cls).tearDownClass()
