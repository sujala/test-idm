# -*- coding: utf-8 -*
import copy

import ddt

# from tests.api import base as parent_base
from tests.api import constants as const
from tests.api.v2 import base
from tests.api.v2 import client
from tests.api.v2.models import requests
from tests.api.v2.models import factory
from tests.api.v2.models import responses
from tests.api.v2.schema import tenants


@ddt.ddt
class TestUpdateTenant(base.TestBaseV2):

    """Update Tenant Tests."""

    @classmethod
    def setUpClass(cls):
        super(TestUpdateTenant, cls).setUpClass()

        '''Feature Flag:
        feature.allow.tenant.name.to.be.changed.via.update.tenant=true will
        allow tenant name to be updated via the update tenant call.
        feature.allow.tenant.name.to.be.changed.via.update.tenant=false will
        cause the tenant name to be ignored in the update tenant call.
        '''
        if cls.test_config.run_service_admin_tests:
            update_tenant_name_flag_properties = (
                cls.devops_client.get_devops_properties(
                    const.FEATURE_FLAG_ALLOW_TENANT_NAME_UPDATE))
            update_tenant_name_resp_dict = (
                update_tenant_name_flag_properties.json())
            cls.update_tenant_name_feature_flag = (
                update_tenant_name_resp_dict[
                    const.RELOADABLE_PROP_FILE][0][const.VALUE])

    def setUp(self):
        super(TestUpdateTenant, self).setUp()

        self.tenant_name = self.generate_random_string(
            const.TENANT_NAME_PATTERN)
        tenant_object = factory.get_add_tenant_object(
            tenant_name=self.tenant_name, tenant_id=self.tenant_name)
        resp = self.identity_admin_client.add_tenant(tenant=tenant_object)

        tenant = responses.Tenant(resp.json())
        self.tenant_id = tenant.id
        self.tenant_ids = []
        self.tenant_ids.append(self.tenant_id)
        self.user_ids = []
        self.role_ids = []
        self.domain_ids = []

    @ddt.file_data('data_update_tenant.json')
    def test_update_tenant(self, data_schema):
        '''Tests for update tenant API

        @todo: The test_data file needs to be updated to include all possible
        data combinations.
        '''
        # Get the values before the update tenant API call
        before = self.identity_admin_client.get_tenant(
            tenant_id=self.tenant_id)
        tenant = responses.Tenant(before.json())

        test_data = data_schema.get('test_data', {})
        tenant_name = test_data.get('tenant_name', self.tenant_name)
        tenant_id = test_data.get('tenant_id', None)
        description = test_data.get('description', None)
        enabled = test_data.get('enabled', None)
        display_name = test_data.get('display_name', None)

        tenant_object = requests.Tenant(
            tenant_name=tenant_name, tenant_id=tenant_id,
            description=description, enabled=enabled,
            display_name=display_name)

        update_resp = self.identity_admin_client.update_tenant(
            tenant_id=self.tenant_id, request_object=tenant_object)
        self.assertEqual(update_resp.status_code, 200)

        update_tenant_schema = copy.deepcopy(tenants.update_tenant)
        if 'additional_schema' in 'data_schema':
            update_tenant_schema['properties']['tenant']['required'] = (
                update_tenant_schema['properties']['tenant']['required'] +
                data_schema['additonal_schema'])
        self.assertSchema(
            response=update_resp, json_schema=update_tenant_schema)

        after = self.identity_admin_client.get_tenant(
            tenant_id=self.tenant_id)
        updated_tenant = responses.Tenant(after.json())

        if 'enabled' in test_data:
            self.assertBoolean(
                expected=test_data['enabled'], actual=updated_tenant.enabled)
        else:
            self.assertEqual(updated_tenant.enabled, tenant.enabled)

        expected_description = test_data.get('description', tenant.description)
        self.assertEqual(updated_tenant.description, expected_description)

        expected_tenant_name = test_data.get('tenant_name', tenant.name)
        self.assertEqual(updated_tenant.name, expected_tenant_name)

        # Verify that the tenant_id is not updated
        self.assertEqual(updated_tenant.id, tenant.id)
        self.assertEqual(updated_tenant.domain_id, tenant.domain_id)

    # @todo - fix the decorator. Test is not picked up with the decorator.
    # @parent_base.skip_if_no_service_admin_available
    def test_update_tenant_name(self):
        '''Test to verify update on tenant name for a NAST tenant.

        The update tenant name functionality can be turned ON/OFF by a
        feature flag. This test verifies
        1. the tenant name updates are reflected in Authentication & Validate
           Token API calls when update is allowed.
        2. the tenant name updates do not take place when update is not
            allowed.
        '''
        if not self.test_config.run_service_admin_tests:
            self.skipTest('Skipping Service Admin Tests per config value')
        before = self.identity_admin_client.get_tenant(
            tenant_id=self.tenant_id)
        before_tenant = responses.Tenant(before.json())

        # Add User
        request_object = factory.get_add_user_request_object()
        resp = self.identity_admin_client.add_user(
            request_object=request_object)
        self.assertEqual(resp.status_code, 201)

        user = responses.User(resp.json())
        self.user_ids.append(user.id)

        # Add Role
        role_object = factory.get_add_role_request_object()
        resp = self.identity_admin_client.add_role(request_object=role_object)
        role = responses.Role(resp.json())
        self.role_ids.append(role.id)

        # Add role to user for tenant
        resp = self.identity_admin_client.add_role_to_user_for_tenant(
            user_id=user.id, role_id=role.id, tenant_id=self.tenant_id)

        new_name = self.generate_random_string(
            pattern=const.TENANT_NAME_PATTERN)

        tenant_update_object = requests.Tenant(
            tenant_name=new_name, tenant_id=self.tenant_id)
        resp = self.identity_admin_client.update_tenant(
            tenant_id=self.tenant_id, request_object=tenant_update_object)
        self.assertEqual(resp.status_code, 200)

        after = self.identity_admin_client.get_tenant(
            tenant_id=self.tenant_id)
        updated_tenant = responses.Tenant(after.json())

        if self.update_tenant_name_feature_flag:
            self.assertEqual(updated_tenant.name, new_name)
        else:
            self.assertEqual(updated_tenant.name, before_tenant.name)

        # Get Auth Token for User
        user_client = client.IdentityAPIClient(
            url=self.url,
            serialize_format=self.test_config.serialize_format,
            deserialize_format=self.test_config.deserialize_format)
        resp = user_client.get_auth_token(
            user=user.name, password=user.password)
        auth_resp = responses.Access(resp.json())

        # Auth as Tenant with token
        tenant_with_token_obj = requests.TenantWithTokenAuth(
            tenant_id=self.tenant_id, token_id=auth_resp.access.token.id)
        resp = user_client.auth_as_tenant_with_token(
            request_object=tenant_with_token_obj)

        auth_tenant_with_token = responses.Access(resp.json())
        tenant_id_after_update = [
            item.tenant_id
            for item in auth_tenant_with_token.access.user.roles if
            item.id == role.id][0]
        self.assertEqual(tenant_id_after_update, before_tenant.id)
        if self.update_tenant_name_feature_flag:
            self.assertEqual(
                auth_tenant_with_token.access.token.tenant.name, new_name)
        else:
            self.assertEqual(
                auth_tenant_with_token.access.token.tenant.name,
                before_tenant.name)

        # Auth as user with password
        resp = user_client.get_auth_token(
            user=user.name, password=user.password)
        auth_user_with_password = responses.Access(resp.json())
        tenant_id_after_update = [
            item.tenant_id
            for item in auth_user_with_password.access.user.roles if
            item.id == role.id][0]
        self.assertEqual(tenant_id_after_update, before_tenant.id)

        # Validate Token for Tenant
        resp = self.identity_admin_client.validate_token(
            token_id=auth_resp.access.token.id)
        validate_token_resp = responses.Access(resp.json())
        tenant_id_after_update = [
            item.tenant_id
            for item in validate_token_resp.access.user.roles if
            item.id == role.id][0]
        self.assertEqual(tenant_id_after_update, before_tenant.id)

    def test_update_tenant_name_one_call_user(self):
        '''Test to verify update on tenant name for a MOSSO tenant.

        The update tenant name functionality can be turned ON/OFF by a
        feature flag. This test verifies
        1. the tenant name updates are reflected in Authentication & Validate
           Token API calls when update is allowed.
        2. the tenant name updates do not take place when update is not
            allowed.
        '''

        # Add user with One Call Logic
        one_call_req_obj = factory.get_add_user_one_call_request_object()
        resp = self.identity_admin_client.add_user(
            request_object=one_call_req_obj)
        self.assertEqual(resp.status_code, 201)

        one_call_user = responses.User(resp.json())
        self.user_ids.append(one_call_user.id)

        mosso_tenant_id = [
            item.tenant_id
            for item in one_call_user.roles if
            item.name == 'compute:default'][0]
        self.tenant_ids.append(mosso_tenant_id)
        nast_tenant_id = [
            item.tenant_id
            for item in one_call_user.roles if
            item.name == 'object-store:default'][0]
        self.tenant_ids.append(nast_tenant_id)
        self.domain_ids.append(one_call_user.domain_id)

        # update one_call_user tenant
        before = self.identity_admin_client.get_tenant(
            tenant_id=one_call_user.domain_id)
        one_call_tenant = responses.Tenant(before.json())

        new_one_call_tenant_name = 'changed' + self.generate_random_string(
            pattern=const.MOSSO_TENANT_ID_PATTERN)

        tenant_update_object = requests.Tenant(
            tenant_name=new_one_call_tenant_name,
            tenant_id=one_call_req_obj.domain_id)
        resp = self.identity_admin_client.update_tenant(
            tenant_id=one_call_user.domain_id,
            request_object=tenant_update_object)
        self.assertEqual(resp.status_code, 200)

        after = self.identity_admin_client.get_tenant(
            tenant_id=one_call_user.domain_id)
        updated_one_call_tenant = responses.Tenant(after.json())

        if self.update_tenant_name_feature_flag:
            self.assertEqual(
                updated_one_call_tenant.name, new_one_call_tenant_name)
        else:
            self.assertEqual(
                updated_one_call_tenant.name, one_call_req_obj.domain_id)

        # Auth as one_call_user with password
        user_client = client.IdentityAPIClient(
            url=self.url,
            serialize_format=self.test_config.serialize_format,
            deserialize_format=self.test_config.deserialize_format)

        resp = user_client.get_auth_token(
            user=one_call_user.name, password=one_call_user.password)
        auth_one_call_user_with_password = responses.Access(resp.json())

        # Auth response will/not reflect updated tenant name based on flag.
        if self.update_tenant_name_feature_flag:
            self.assertEqual(
                auth_one_call_user_with_password.access.token.tenant.name,
                updated_one_call_tenant.name)
        else:
            self.assertEqual(
                auth_one_call_user_with_password.access.token.tenant.name,
                one_call_tenant.name)

    def tearDown(self):
        # Delete all resources created in the tests
        for user_id in self.user_ids:
            self.identity_admin_client.delete_user(user_id=user_id)
        for role_id in self.role_ids:
            self.identity_admin_client.delete_role(role_id=role_id)
        for tenant_id in self.tenant_ids:
            self.identity_admin_client.delete_tenant(tenant_id=tenant_id)

        super(TestUpdateTenant, self).tearDown()
