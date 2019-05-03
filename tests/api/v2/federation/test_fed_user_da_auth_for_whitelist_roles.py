# -*- coding: utf-8 -*
from munch import Munch
import pytest
from qe_coverage.opencafe_decorators import tags, unless_coverage

from tests.api.utils import func_helper
from tests.api.v2.federation import federation

from tests.api.utils.create_cert import create_self_signed_cert
from tests.api.utils import saml_helper
from tests.api.v2.models import factory, responses

from tests.package.johny import constants as const
from tests.package.johny.v2.models import requests


class TestWhitelistRolesForFedDAAuth(federation.TestBaseFederation):

    """
    Tests for Auth with Delegation agreements for federated users,
    either directly or as member of a user group.
    """
    @classmethod
    @unless_coverage
    def setUpClass(cls):
        """
        Class level set up for the tests
        Create users needed for the tests and generate clients for those users.
        """
        super(TestWhitelistRolesForFedDAAuth, cls).setUpClass()

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
        cls.hierarchical_billing_observer_role_id = cls.get_role_id_by_name(
            role_name=const.HIERARCHICAL_BILLING_OBSERVER_ROLE_NAME)
        cls.role_ids = []
        cls.tenant_ids = []

    @unless_coverage
    def setUp(self):
        super(TestWhitelistRolesForFedDAAuth, self).setUp()
        self.group_ids = []
        self.roles = []

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

        fed_auth_resp_parsed = Munch.fromDict(fed_auth_resp.json())
        fed_user_id = fed_auth_resp_parsed.access.user.id
        fed_user_token = fed_auth_resp_parsed.access.token.id

        return fed_user_id, fed_user_token

    def generate_tenants_assignment_dict(self, on_role, *for_tenants):

        tenant_assignment_request = {
            const.ON_ROLE: on_role,
            const.FOR_TENANTS: list(for_tenants)
        }
        return tenant_assignment_request

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

    def create_delegation_agreement(self, client):

        da_name = self.generate_random_string(
            pattern=const.DELEGATION_AGREEMENT_NAME_PATTERN)
        da_req = requests.DelegationAgreements(da_name=da_name)
        da_resp = client.create_delegation_agreement(
            request_object=da_req)
        da_id = da_resp.json()[
            const.RAX_AUTH_DELEGATION_AGREEMENT][const.ID]
        return da_id

    def set_up_roles_tenant_and_da(self, client):

        # create DA with sub user
        da_id = self.create_delegation_agreement(client=client)
        # create roles
        role_1 = self.create_role()
        # create tenant with type for which there is a whitelist filter
        tenant_name = ":".join([
            self.test_config.mpc_whitelist_tenant_type,
            self.generate_random_string(pattern=const.TENANT_NAME_PATTERN)])
        tenant_1 = self.create_tenant(
            name=tenant_name,
            tenant_types=[self.test_config.mpc_whitelist_tenant_type])
        return role_1, tenant_1, da_id

    @tags('positive', 'p0', 'regression')
    @pytest.mark.regression
    def test_whitelist_roles_for_d_auth_of_fed_user_as_effective_delegate(
            self):

        # create user group for domain 2
        group_two = self.create_and_add_user_group_to_domain(
            self.user_admin_client_2, self.domain_id_2)
        self.group_ids.append((group_two.id, self.domain_id_2))

        # creating idp, updating idp's policy before creating fed user so that
        # fed user can specify group membership
        fed_user_id, fed_user_token = self.create_fed_user_for_da(
            client=self.user_admin_client_2, domain=self.domain_id_2,
            update_policy=True, group=group_two)

        role_1, tenant_1, da_id = self.set_up_roles_tenant_and_da(
            client=self.user_admin_client)

        # adding user group as delegate
        ua_client = self.user_admin_client
        add_user_group_delegate_resp = (
            ua_client.add_user_group_delegate_to_delegation_agreement(
                da_id=da_id, user_group_id=group_two.id))
        self.assertEqual(add_user_group_delegate_resp.status_code, 204)

        # create tenant assignments request dicts
        tenant_assignment_req_1 = self.generate_tenants_assignment_dict(
            role_1.id, "*")
        tenants_role_assignment_req = requests.TenantRoleAssignments(
            tenant_assignment_req_1)

        assignment_resp = (
            ua_client.add_tenant_role_assignments_to_delegation_agreement(
                da_id=da_id, request_object=tenants_role_assignment_req))
        self.assertEqual(assignment_resp.status_code, 200)

        # DA auth using fed user's token
        delegation_auth_req = requests.AuthenticateWithDelegationAgreement(
            token=fed_user_token, delegation_agreement_id=da_id)
        resp = self.identity_admin_client.get_auth_token(delegation_auth_req)
        delegation_auth_roles = resp.json()[const.ACCESS][const.USER][
            const.ROLES]
        # Since it is the only tenant in the domain and DA auth always returns
        # de-normalized roles.
        self.assertEqual(delegation_auth_roles, [])

        # Now, assign a role from whitelist to DA and re-check DA auth
        tenant_assignment_req_3 = self.generate_tenants_assignment_dict(
            self.hierarchical_billing_observer_role_id, tenant_1.id)
        tenants_role_assignment_req = requests.TenantRoleAssignments(
            tenant_assignment_req_3)

        assignment_resp = (
            ua_client.add_tenant_role_assignments_to_delegation_agreement(
                da_id=da_id, request_object=tenants_role_assignment_req))
        self.assertEqual(assignment_resp.status_code, 200)

        delegation_auth_req = requests.AuthenticateWithDelegationAgreement(
            token=fed_user_token, delegation_agreement_id=da_id)
        resp = self.identity_admin_client.get_auth_token(delegation_auth_req)
        self.assertEqual(resp.status_code, 200)
        delegation_auth_roles = resp.json()[const.ACCESS][const.USER][
            const.ROLES]
        # Two role assignments above + identity:default + tenant:access
        self.assertEqual(len(delegation_auth_roles), 4)

    @federation.base.base.log_tearDown_error
    @unless_coverage
    def tearDown(self):
        for group_id, domain_id in self.group_ids:
            resp = self.identity_admin_client.delete_user_group_from_domain(
                group_id=group_id, domain_id=domain_id)
            self.assertEqual(
                resp.status_code, 204,
                msg='User group with ID {0} failed to delete'.format(
                    group_id))
        super(TestWhitelistRolesForFedDAAuth, self).tearDown()

    @classmethod
    @unless_coverage
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
        super(TestWhitelistRolesForFedDAAuth, cls).tearDownClass()

        for role_id in cls.role_ids:
            resp = cls.identity_admin_client.delete_role(role_id=role_id)
            assert resp.status_code == 204, (
                'Role with ID {0} failed to delete'.format(role_id))
        for tenant_id in cls.tenant_ids:
            resp = cls.identity_admin_client.delete_tenant(
                tenant_id=tenant_id)
            assert resp.status_code in [204, 404], (
                'Tenant with ID {0} failed to delete'.format(tenant_id))
