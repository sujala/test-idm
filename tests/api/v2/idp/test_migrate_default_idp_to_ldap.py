"""
JIRA CID-612
Migrate default mapping policy to LDAP Based Reloadable Property
"""
import json
import ddt
from tests.api.v2 import base
from tests.api.v2.models import factory
from tests.package.johny import constants as const
from tests.package.johny.v2.models import requests


@ddt.ddt
class TestMigrateDefaultIDPtoLDAP(base.TestBaseV2):
    """
    TestMigrateDefaultIDPtoLDAP
    """
    @classmethod
    def setUpClass(cls):
        super(TestMigrateDefaultIDPtoLDAP, cls).setUpClass()
        cls.devops_client.default_headers.update(
            cls.identity_admin_client.default_headers)

    def setUp(self):
        super(TestMigrateDefaultIDPtoLDAP, self).setUp()
        self.idp_ids = []
        self.devops_props_ids = []
        self.tenant_ids = []
        self.user_ids = []
        self.domain_ids = []

    def generate_prop(self, test_data):
        prop = {
            "prop_name": "my_prop_name",
            "prop_value": 'prop_value',
            "prop_value_type": 'STRING',
            "prop_description": "myDesc",
            "prop_version": "3.10.0",
            "prop_reloadable": True,
            "prop_searchable": True}
        prop['prop_name'] = "{0}_prop_{1}".format(
            test_data['prop_value_type'],
            self.generate_random_string(pattern=const.UPPER_CASE_LETTERS))
        for key in test_data:
            prop[key] = test_data[key]
        prop['prop_description'] = self.generate_random_string(
            pattern=prop['prop_description'])
        return prop

    def create_idp(self, domain_id):
        random_string = self.generate_random_string(
            pattern=const.UPPER_CASE_LETTERS)
        idp_data = {
            'issuer': "http://issuer{0}.com".format(random_string),
            'description': "desc{0}".format(random_string),
            'idp_name': "name{0}".format(random_string),
            'authentication_url': "http://auth{0}.com".format(random_string),
            'federation_type': "DOMAIN",
            'approved_domain_ids': [domain_id]}
        req_obj = requests.IDP(**idp_data)
        resp = self.identity_admin_client.create_idp(req_obj)
        self.assertEqual(resp.status_code, 201)
        self.idp_ids.append(
            resp.json()[const.NS_IDENTITY_PROVIDER][const.ID])
        return resp.json()[const.NS_IDENTITY_PROVIDER][const.ID]

    def get_default_policy(self):
        resp = self.devops_client.get_devops_properties(
            const.FEDERATION_IDENTITY_PROVIDER_DEFAULT_POLICY)
        self.assertEqual(resp.status_code, 200)
        return resp.json()

    def verify_policy(self, idp_id, policy):
        resp = self.identity_admin_client.get_idp_mapping(
            idp_id=idp_id)
        self.assertEqual(resp.status_code, 200)
        self.assertEqual(
            resp.json(),
            json.loads(policy[const.PROPERTIES][0][const.VALUE]))

    def update_default_policy(self):
        # Get current default policy
        default_policy_1 = self.get_default_policy()
        property_id = default_policy_1[const.PROPERTIES][0][const.ID]
        # Update the default policy
        updated_property = '{{"updatedfizz{0}": "updatedbuzz{0}"}}'.format(
            self.generate_random_string(pattern=const.UPPER_CASE_LETTERS))
        req_obj = requests.DevopsProp(prop_value=updated_property)
        resp = self.devops_client.update_devops_prop(
            devops_props_id=property_id, request_object=req_obj)
        self.assertEqual(resp.status_code, 200)
        # Get the newly updated default policy
        default_policy_2 = self.get_default_policy()
        # Verify policy was updated
        self.assertEqual(
            json.loads(default_policy_2[const.PROPERTIES][0][const.VALUE]),
            json.loads(updated_property))

    def reset_default_policy(self, policy):

        default_policy_1 = self.get_default_policy()
        property_id = default_policy_1[const.PROPERTIES][0][const.ID]
        req_obj = requests.DevopsProp(prop_value=policy)
        resp = self.devops_client.update_devops_prop(
            devops_props_id=property_id, request_object=req_obj)
        self.assertEqual(resp.status_code, 200)

    def get_domain_id_from_one_call_user_create(self):
        req_obj = factory.get_add_user_one_call_request_object()
        resp = self.identity_admin_client.add_user(request_object=req_obj)
        self.assertEqual(resp.status_code, 201)
        tenant_id = [role for role in resp.json()[const.USER][const.ROLES]
                     if role['name'] == const.COMPUTE_ROLE_NAME][0][
                     const.TENANT_ID]
        self.tenant_ids.append(tenant_id)
        self.domain_ids.append(tenant_id)
        self.user_ids.append(resp.json()[const.USER][const.ID])
        return tenant_id

    @ddt.file_data('data_create_prop_types.json')
    def test_create_devops_prop_type(self, test_data):
        prop = self.generate_prop(test_data)
        req_obj = requests.DevopsProp(**prop)
        resp = self.devops_client.create_devops_prop(
            request_object=req_obj)
        self.assertEqual(resp.status_code, 201)
        self.devops_props_ids.append(
            resp.json()[const.IDENTITY_PROPERTY][const.ID])

    @ddt.file_data('data_create_invalid_prop_types.json')
    def test_create_invalid_devops_prop_type(self, test_data):
        prop = self.generate_prop(test_data)
        req_obj = requests.DevopsProp(**prop)
        resp = self.devops_client.create_devops_prop(
            request_object=req_obj)
        self.assertEqual(resp.status_code, 400)

    def test_update_idp_default_policy_affects_only_idp_created_after_update(
      self):
        domain_id = self.get_domain_id_from_one_call_user_create()

        # Create IDP
        idp_id = self.create_idp(domain_id)

        # Get current default policy
        default_policy_1 = self.get_default_policy()
        original_mapping = default_policy_1[const.PROPERTIES][0][
            const.PROP_VALUE]

        try:
            # Update default policy
            self.update_default_policy()

            # Create another IDP
            idp_id_2 = self.create_idp(domain_id)

            # Get updated default policy
            default_policy_2 = self.get_default_policy()

            # Verify mapping on this IDP matches the newly updated default
            # policy
            self.verify_policy(idp_id_2, default_policy_2)

            # Verify the IDP previously created still has previous mapping
            self.verify_policy(idp_id, default_policy_1)

        finally:
            # Reset the default policy
            self.reset_default_policy(original_mapping)

    def tearDown(self):
        for user_id in self.user_ids:
            self.identity_admin_client.delete_user(user_id=user_id)
        for tenant_id in self.tenant_ids:
            self.identity_admin_client.delete_tenant(tenant_id=tenant_id)
        for domain_id in self.domain_ids:
            disable_domain_req = requests.Domain(enabled=False)
            self.identity_admin_client.update_domain(
                domain_id=domain_id, request_object=disable_domain_req)
            self.identity_admin_client.delete_domain(domain_id=domain_id)
        for idp_id in self.idp_ids:
            self.identity_admin_client.delete_idp(idp_id=idp_id)
        for devops_props_id in self.devops_props_ids:
            self.devops_client.delete_devops_prop(
                devops_props_id=devops_props_id)
