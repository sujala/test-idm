from tests.api.utils import func_helper
from tests.api.v2 import base
from tests.api.v2.models import factory, responses
from tests.package.johny import constants as const
from tests.package.johny.v2.models import requests


class TestBaseDelegation(base.TestBaseV2):

    @classmethod
    def setUpClass(cls):

        super(TestBaseDelegation, cls).setUpClass()
        cls.domain_ids = []
        cls.user_ids = []
        cls.rcn = cls.test_config.da_rcn

        # Add Domain 1
        cls.domain_id = func_helper.generate_randomized_domain_id(
            client=cls.identity_admin_client)
        dom_req = requests.Domain(
            domain_name=cls.domain_id, domain_id=cls.domain_id, rcn=cls.rcn)
        add_dom_resp = cls.identity_admin_client.add_domain(dom_req)
        assert add_dom_resp.status_code == 201, (
            'domain was not created successfully')
        cls.domain_ids.append(cls.domain_id)

        additional_input_data = {'domain_id': cls.domain_id}
        cls.user_admin_client = cls.generate_client(
            parent_client=cls.identity_admin_client,
            additional_input_data=additional_input_data)
        cls.user_ids.append(
            cls.user_admin_client.default_headers[const.X_USER_ID])

        # Add Domain 2
        cls.domain_id_2 = func_helper.generate_randomized_domain_id(
            client=cls.identity_admin_client)
        dom_req = requests.Domain(
            domain_name=cls.domain_id_2,
            domain_id=cls.domain_id_2,
            rcn=cls.rcn)
        add_dom_resp = cls.identity_admin_client.add_domain(dom_req)
        assert add_dom_resp.status_code == 201, (
            'domain was not created successfully')
        cls.domain_ids.append(cls.domain_id_2)

        # Create User Admin 2 in Domain 2
        additional_input_data = {'domain_id': cls.domain_id_2}
        cls.user_admin_client_2 = cls.generate_client(
            parent_client=cls.identity_admin_client,
            additional_input_data=additional_input_data)
        cls.user_admin2_id = \
            cls.user_admin_client_2.default_headers[const.X_USER_ID]
        cls.user_ids.append(cls.user_admin2_id)

        # Create RCN admin
        cls.domain_id_3 = func_helper.generate_randomized_domain_id(
            client=cls.identity_admin_client)
        dom_req = requests.Domain(
            domain_name=cls.domain_id_3,
            domain_id=cls.domain_id_3,
            rcn=cls.rcn)
        add_dom_resp = cls.identity_admin_client.add_domain(dom_req)
        assert add_dom_resp.status_code == 201, (
            'domain was not created successfully')
        cls.domain_ids.append(cls.domain_id_3)

        additional_input_data = {
            'domain_id': cls.domain_id_3,
            'roles': [{'name': const.RCN_ADMIN_ROLE_NAME}]}
        cls.rcn_admin_client = cls.generate_client(
            parent_client=cls.identity_admin_client,
            additional_input_data=additional_input_data)
        cls.user_ids.append(
            cls.rcn_admin_client.default_headers[const.X_USER_ID])

        cls.role_ids = []
        cls.tenant_ids = []

    @classmethod
    def create_domain_with_rcn(cls):

        domain_id = func_helper.generate_randomized_domain_id(
            client=cls.identity_admin_client)
        dom_req = requests.Domain(
            domain_name=domain_id, domain_id=domain_id, rcn=cls.rcn)
        add_dom_resp = cls.identity_admin_client.add_domain(dom_req)
        assert add_dom_resp.status_code == 201, (
            'domain was not created successfully')
        cls.domain_ids.append(domain_id)
        return domain_id

    @classmethod
    def get_role_id_by_name(cls, role_name):
        return super().get_role_id_by_name(cls.user_admin_client, role_name)

    def setUp(self):
        super(TestBaseDelegation, self).setUp()

    def create_role(self):

        role_obj = factory.get_add_role_request_object(
            administrator_role=const.USER_MANAGE_ROLE_NAME)
        add_role_resp = self.identity_admin_client.add_role(
            request_object=role_obj)
        self.assertEqual(add_role_resp.status_code, 201)
        role = responses.Role(add_role_resp.json())
        self.role_ids.append(role.id)
        return role

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

    def generate_tenants_assignment_dict(self, on_role, *for_tenants):

        tenant_assignment_request = {
            const.ON_ROLE: on_role,
            const.FOR_TENANTS: list(for_tenants)
        }
        return tenant_assignment_request

    def create_and_add_user_group_to_domain(self, client,
                                            domain_id=None,
                                            status_code=201):
        if domain_id is None:
            domain_id = self.domain_id
        group_req = factory.get_add_user_group_request(domain_id)
        # set the serialize format to json since that's what we support
        # for user groups
        client_default_serialize_format = client.serialize_format
        client.serialize_format = const.JSON
        resp = client.add_user_group_to_domain(
            domain_id=domain_id, request_object=group_req)
        self.assertEqual(resp.status_code, status_code)
        client.serialize_format = client_default_serialize_format

        if status_code != 201:
            return None
        else:
            return responses.UserGroup(resp.json())

    def call_create_delegation_agreement(self, client, delegate_id,
                                         da_name=None, user_delegate=True,
                                         sub_agreement_nest_level=None):
        if not da_name:
            da_name = self.generate_random_string(
                pattern=const.DELEGATION_AGREEMENT_NAME_PATTERN)
        da_req = requests.DelegationAgreements(
            da_name=da_name, sub_agreement_nest_level=sub_agreement_nest_level)
        da_resp = client.create_delegation_agreement(request_object=da_req)
        da_id = da_resp.json()[const.RAX_AUTH_DELEGATION_AGREEMENT][const.ID]

        if user_delegate:
            client.add_user_delegate_to_delegation_agreement(
                da_id, delegate_id)
        else:
            client.add_user_group_delegate_to_delegation_agreement(
                da_id, delegate_id
            )
        return da_resp, da_id

    @base.base.log_tearDown_error
    def tearDown(self):
        super(TestBaseDelegation, self).tearDown()

    @classmethod
    @base.base.log_tearDown_error
    def tearDownClass(cls):

        for user_id in cls.user_ids:
            resp = cls.identity_admin_client.delete_user(user_id=user_id)
            assert resp.status_code in [204, 404], (
                'User with ID {0} failed to delete'.format(user_id))

        disable_domain_req = requests.Domain(enabled=False)
        for domain_id in cls.domain_ids:
            cls.identity_admin_client.update_domain(
                domain_id=domain_id, request_object=disable_domain_req)

            resp = cls.identity_admin_client.delete_domain(
                domain_id=domain_id)
            assert resp.status_code == 204, (
                'Domain with ID {0} failed to delete'.format(domain_id))

        for role_id in cls.role_ids:
            resp = cls.identity_admin_client.delete_role(role_id=role_id)
            assert resp.status_code == 204, (
                'Role with ID {0} failed to delete'.format(
                    role_id))

        for tenant_id in cls.tenant_ids:
            resp = cls.identity_admin_client.delete_tenant(
                tenant_id=tenant_id)
            # For some cases, tenant is getting deleted by delete_client()
            # call, prior. Hence checking for either 204 or 404.
            assert resp.status_code in [204, 404], (
                'Tenant with ID {0} failed to delete'.format(
                    tenant_id))
