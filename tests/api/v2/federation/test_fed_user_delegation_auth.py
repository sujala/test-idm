# -*- coding: utf-8 -*
from collections import defaultdict
import ddt
from munch import Munch
import pytest
from qe_coverage.opencafe_decorators import tags, unless_coverage

from tests.api.utils import func_helper
from tests.api.v2.schema import tokens as tokens_json
from tests.api.v2.federation import federation

from tests.api.utils.create_cert import create_self_signed_cert
from tests.api.utils import saml_helper
from tests.api.v2.models import factory, responses

from tests.package.johny import constants as const
from tests.package.johny.v2.models import requests


@ddt.ddt
class TestDelegationWithFederation(federation.TestBaseFederation):

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
        cls.role_ids = []
        cls.tenant_ids = []
        cls.hierarchical_billing_observer_role_id = cls.get_role_id_by_name(
            role_name=const.HIERARCHICAL_BILLING_OBSERVER_ROLE_NAME)

    @unless_coverage
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

        fed_auth_resp_parsed = Munch.fromDict(fed_auth_resp.json())
        fed_user_id = fed_auth_resp_parsed.access.user.id
        fed_user_token = fed_auth_resp_parsed.access.token.id

        return fed_user_id, fed_user_token

    def validate_response(self, resp):
        # Verifying that all auth methods are returned, ignoring the order
        for auth_by in [const.AUTH_BY_DELEGATION, const.AUTH_BY_PWD,
                        const.AUTH_BY_FEDERATED]:
            self.assertIn(auth_by, resp.json()[
                const.ACCESS][const.TOKEN][const.RAX_AUTH_AUTHENTICATED_BY])
        delegation_domain = resp.json()[const.ACCESS][const.USER][
            const.RAX_AUTH_DOMAIN_ID]
        self.assertEqual(delegation_domain, self.domain_id)

    def generate_tenants_assignment_dict(self, on_role, *for_tenants):

        tenant_assignment_request = {
            const.ON_ROLE: on_role,
            const.FOR_TENANTS: list(for_tenants)
        }
        return tenant_assignment_request

    def create_role(self):

        role_req = factory.get_add_role_request_object(
            administrator_role=const.USER_MANAGE_ROLE_NAME)
        add_role_resp = self.identity_admin_client.add_role(
            request_object=role_req)
        self.assertEqual(add_role_resp.status_code, 201)
        role = responses.Role(add_role_resp.json())
        self.role_ids.append(role.id)
        return role

    def create_tenant(self, domain=None, faws=False):
        if not domain:
            domain = self.domain_id
        if faws:
            tenant_name = self.generate_random_string(
                pattern=('faws:' + const.TENANT_NAME_PATTERN))
            tenant_req = factory.get_add_tenant_object(
                tenant_name=tenant_name, domain_id=domain,
                tenant_types=['faws'])
        else:
            tenant_req = factory.get_add_tenant_object(domain_id=domain)
        add_tenant_resp = self.identity_admin_client.add_tenant(
            tenant=tenant_req)
        self.assertEqual(add_tenant_resp.status_code, 201)
        tenant = responses.Tenant(add_tenant_resp.json())
        self.tenant_ids.append(tenant.id)
        return tenant

    @tags('positive', 'p1', 'regression')
    @pytest.mark.regression
    def test_d_auth_with_fed_users_as_principal_and_delegate(self):
        '''
        Tests with federated User as Principal & Delegate.

        This test covers the followng scenarios,
        1. Auth as federated user delegate.
        2. Delete federated user delegate.
        3. Delete federated user principal.

        '''

        # Creating fed user principal
        principal_id, principal_token = self.create_fed_user_for_da(
            client=self.user_admin_client, domain=self.domain_id, issuer=None)
        fed_prinicipal_client = self.generate_client(token=principal_token)

        # Creating fed user delegate
        delegate_id, delegate_token = self.create_fed_user_for_da(
            client=self.user_admin_client_2, domain=self.domain_id_2,
            issuer=self.generate_random_string(pattern=const.ISSUER_PATTERN))

        # create DA
        da_req = requests.DelegationAgreements(
            principal_type=const.USER.upper(),
            principal_id=principal_id,
            da_name=self.generate_random_string(
                pattern=const.DELEGATION_AGREEMENT_NAME_PATTERN))
        self.user_admin_client.serialize_format = 'json'
        self.user_admin_client.default_headers[const.CONTENT_TYPE] = \
            'application/json'

        da_resp = self.user_admin_client.create_delegation_agreement(
            request_object=da_req)
        self.assertEqual(da_resp.status_code, 201)
        da = Munch.fromDict(da_resp.json())
        da_id = da[const.RAX_AUTH_DELEGATION_AGREEMENT].id

        fed_prinicipal_client.add_user_delegate_to_delegation_agreement(
            da_id=da_id, user_id=delegate_id)

        # DA auth using delegate federated user's token
        delegation_auth_req = requests.AuthenticateWithDelegationAgreement(
            token=delegate_token, delegation_agreement_id=da_id)
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

        # Delete Federated User Delegate
        resp = self.identity_admin_client.delete_user(user_id=delegate_id)
        self.assertEqual(resp.status_code, 204)

        # Validate the previously issued delegation token is invalid, after
        # the federated user is deleted.
        resp = self.identity_admin_client.validate_token(token_id=da_token)
        self.assertEqual(resp.status_code, 404)

        # Add a federated user as delegate
        delegate2_id, delegate2_token = self.create_fed_user_for_da(
            client=self.user_admin_client_2, domain=self.domain_id_2,
            issuer=self.generate_random_string(pattern=const.ISSUER_PATTERN))
        fed_prinicipal_client.add_user_delegate_to_delegation_agreement(
            da_id=da_id, user_id=delegate2_id)

        # Verify Delegate 2 is listed as a delegate in the DA
        delegate2_client = self.generate_client(token=delegate2_token)
        resp = delegate2_client.list_delegates_for_delegation_agreement(
            da_id=da_id)
        self.assertEqual(resp.status_code, 200)
        self.assertEqual(
            resp.json()[const.DELEGATE_REFERENCES][0][const.DELEGATE_ID],
            delegate2_id)

        # Delete Federated User Principal - this will trigger the DA delete.
        resp = self.identity_admin_client.delete_user(user_id=principal_id)
        self.assertEqual(resp.status_code, 204)

        # Verify Delegae 2 can no longer retrieve the DA
        resp = delegate2_client.list_delegates_for_delegation_agreement(
            da_id=da_id)
        self.assertEqual(resp.status_code, 404)

    @tags('positive', 'p1', 'regression')
    @pytest.mark.regression
    def test_d_auth_fed_user_with_user_group_as_delegate(self):

        # create user group for domain 2
        group_two = self.create_and_add_user_group_to_domain(
            self.user_admin_client_2, self.domain_id_2)
        self.group_ids.append((group_two.id, self.domain_id_2))

        # creating idp, updating idp's policy before creating fed user so that
        # fed user can specify group membership
        fed_user_id, fed_user_token = self.create_fed_user_for_da(
            client=self.user_admin_client_2, domain=self.domain_id_2,
            update_policy=True, group=group_two)

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

        # Validate DA roles after tenant update
        self.validate_da_roles_on_tenant_update(
            da_id=da_id, fed_user_token=fed_user_token)

    def validate_da_roles_on_tenant_update(self, da_id, fed_user_token):

        role_1 = self.create_role()
        # create tenants, 1 of them is faws
        tenant_1 = self.create_tenant(faws=True)
        tenant_2 = self.create_tenant()

        tenant_assignment_req_1 = self.generate_tenants_assignment_dict(
            self.hierarchical_billing_observer_role_id,
            tenant_1.id, tenant_2.id)
        tenant_assignment_req_2 = self.generate_tenants_assignment_dict(
            role_1.id, "*")
        tenants_role_assignment_req = requests.TenantRoleAssignments(
            tenant_assignment_req_1, tenant_assignment_req_2)

        ua_client = self.user_admin_client
        assignment_resp = (
            ua_client.add_tenant_role_assignments_to_delegation_agreement(
                da_id=da_id, request_object=tenants_role_assignment_req))
        self.assertEqual(assignment_resp.status_code, 200)

        # check delegation auth after granting roles to DA
        delegation_auth_req = requests.AuthenticateWithDelegationAgreement(
            token=fed_user_token, delegation_agreement_id=da_id)
        resp = self.identity_admin_client.get_auth_token(delegation_auth_req)
        self.assertEqual(resp.status_code, 200)
        resp_parsed = Munch.fromDict(resp.json())

        role_to_tenant_map = defaultdict(list)
        for role_ in resp_parsed.access.user.roles:
            role_to_tenant_map[role_.id].append(role_[const.TENANT_ID])
        self.assertIn(
            tenant_1.id,
            role_to_tenant_map[self.hierarchical_billing_observer_role_id])
        self.assertIn(
            tenant_2.id,
            role_to_tenant_map[self.hierarchical_billing_observer_role_id])
        self.assertIn(tenant_1.id, role_to_tenant_map[role_1.id])
        self.assertIn(tenant_2.id, role_to_tenant_map[role_1.id])

        # remove tenant 1 from domain
        self.identity_admin_client.delete_tenant_from_domain(
            domain_id=self.domain_id, tenant_id=tenant_1.id)
        # check roles on DA after tenant removal from domain
        list_resp = (
            ua_client.list_tenant_role_assignments_for_delegation_agreement(
                da_id=da_id))
        self.assertEqual(list_resp.status_code, 200)
        list_resp_parsed = Munch.fromDict(list_resp.json())
        for role_assignment_ in list_resp_parsed[
                const.RAX_AUTH_ROLE_ASSIGNMENTS][const.TENANT_ASSIGNMENTS]:
            if role_assignment_.onRole == \
                            self.hierarchical_billing_observer_role_id:
                self.assertEqual(role_assignment_.forTenants, [tenant_2.id])
            if role_assignment_.onRole == role_1.id:
                self.assertEqual(role_assignment_.forTenants, ['*'])

        # check delegation auth after a tenant removal from domain
        delegation_auth_req = requests.AuthenticateWithDelegationAgreement(
            token=fed_user_token, delegation_agreement_id=da_id)
        resp = self.identity_admin_client.get_auth_token(delegation_auth_req)
        self.assertEqual(resp.status_code, 200)
        resp_parsed = Munch.fromDict(resp.json())
        role_to_tenant_map = defaultdict(list)
        for role_ in resp_parsed.access.user.roles:
            role_to_tenant_map[role_.id].append(role_[const.TENANT_ID])
        self.assertNotIn(
            tenant_1.id,
            role_to_tenant_map[self.hierarchical_billing_observer_role_id])
        self.assertIn(
            tenant_2.id,
            role_to_tenant_map[self.hierarchical_billing_observer_role_id])
        self.assertNotIn(tenant_1.id, role_to_tenant_map[role_1.id])
        self.assertIn(tenant_2.id, role_to_tenant_map[role_1.id])

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
        super(TestDelegationWithFederation, self).tearDown()

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
        super(TestDelegationWithFederation, cls).tearDownClass()

        for role_id in cls.role_ids:
            resp = cls.identity_admin_client.delete_role(role_id=role_id)
            assert resp.status_code == 204, (
                'Role with ID {0} failed to delete'.format(role_id))
        for tenant_id in cls.tenant_ids:
            resp = cls.identity_admin_client.delete_tenant(
                tenant_id=tenant_id)
            assert resp.status_code in [204, 404], (
                'Tenant with ID {0} failed to delete'.format(tenant_id))
