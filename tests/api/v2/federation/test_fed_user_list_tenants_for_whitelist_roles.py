# -*- coding: utf-8 -*
from nose.plugins.attrib import attr
from qe_coverage.opencafe_decorators import tags, unless_coverage

from tests.api.utils import func_helper, saml_helper
from tests.api.utils.create_cert import create_self_signed_cert
from tests.api.v2 import base
from tests.api.v2.federation import federation
from tests.api.v2.models import factory, responses

from tests.package.johny import constants as const
from tests.package.johny.v2.models import requests


class TestMPCWhitelistForFedUserListTenants(federation.TestBaseFederation):
    """
    Test add tenant level roles for federated users in saml auth and
    verify roles are returned in List Tenants response
    """
    @classmethod
    @unless_coverage
    def setUpClass(cls):
        """Class level set up for the tests

        Create users needed for the tests and generate clients for those users.
        """
        super(TestMPCWhitelistForFedUserListTenants, cls).setUpClass()
        cls.test_email = 'random@rackspace.com'
        cls.hierarchical_billing_observer_role_id = cls.get_role_id_by_name(
            role_name=const.HIERARCHICAL_BILLING_OBSERVER_ROLE_NAME)

    @unless_coverage
    def setUp(self):
        super(TestMPCWhitelistForFedUserListTenants, self).setUp()
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
    @attr(type='regression')
    def test_whitelist_roles_for_fed_user_list_tenants(self):

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
        list_tenants_resp = self.add_role_in_fed_auth_and_call_list_tenants(
            test_data=test_data, key_path=key_path, cert_path=cert_path,
            issuer=issuer, role=role_1.name, tenant=tenant_1,
            expected_response=200)
        list_of_tenant_ids = [
            tenant[const.ID] for tenant
            in list_tenants_resp.json()[const.TENANTS]]
        self.assertNotIn(tenant_1.id, list_of_tenant_ids)

        list_tenants_resp = self.add_role_in_fed_auth_and_call_list_tenants(
            test_data=test_data, key_path=key_path, cert_path=cert_path,
            issuer=issuer, role=const.HIERARCHICAL_BILLING_OBSERVER_ROLE_NAME,
            tenant=tenant_1, expected_response=200)
        list_of_tenant_ids = [
            tenant[const.ID] for tenant
            in list_tenants_resp.json()[const.TENANTS]]
        self.assertIn(tenant_1.id, list_of_tenant_ids)

    def add_role_in_fed_auth_and_call_list_tenants(
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
        list_tenants_resp = fed_user_client.list_tenants()
        self.assertEqual(list_tenants_resp.status_code, expected_response)
        return list_tenants_resp

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
        super(TestMPCWhitelistForFedUserListTenants, self).tearDown()

    @classmethod
    @unless_coverage
    def tearDownClass(cls):
        super(TestMPCWhitelistForFedUserListTenants, cls).tearDownClass()
