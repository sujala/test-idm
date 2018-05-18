# -*- coding: utf-8 -*
import copy

from nose.plugins.attrib import attr

from tests.api.v2.delegation import delegation
from tests.api.v2.schema import delegation as da_schema
from tests.package.johny import constants as const
from tests.package.johny.v2.models import requests


class NestedDelegationAgreementsTests(delegation.TestBaseDelegation):
    """
    Create/Read/Delete tests for Delegation Agreements
    """
    @classmethod
    def setUpClass(cls):
        super(NestedDelegationAgreementsTests, cls).setUpClass()
        cls.user_admin_2_id = cls.user_admin_client_2.default_headers[
            const.X_USER_ID]

    def setUp(self):
        super(NestedDelegationAgreementsTests, self).setUp()
        self.group_ids = []

    @attr(type='regression')
    def test_create_nested_da(self):

        parent_da_id = self.call_create_delegation_agreement(
            client=self.user_admin_client, delegate_id=self.user_admin_2_id,
            sub_agreement_nest_level=2)

        # create nested da with level - 1
        nested_da_name = self.generate_random_string(
            pattern=const.DELEGATION_AGREEMENT_NAME_PATTERN)
        da_req = requests.DelegationAgreements(
            da_name=nested_da_name, sub_agreement_nest_level=1,
            parent_da_id=parent_da_id)
        nested_da_resp = self.user_admin_client_2.create_delegation_agreement(
            request_object=da_req)
        nest_level = nested_da_resp.json()[
            const.RAX_AUTH_DELEGATION_AGREEMENT][const.SUBAGREEMENT_NEST_LEVEL]
        self.validate_nested_da_response(nested_da_resp)
        self.assertEqual(nest_level, 1)

    @attr(type='regression')
    def validate_nested_da_response(self, nested_da_resp):

        self.assertEqual(nested_da_resp.status_code, 201)
        modified_schema = copy.deepcopy(da_schema.add_da)
        modified_schema['properties'][const.RAX_AUTH_DELEGATION_AGREEMENT][
            'required'] += [const.PARENT_DELEGATION_AGREEMENT_ID]
        self.assertSchema(nested_da_resp, modified_schema)

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
        return da_id

    def test_nested_da_when_principal_is_user_group(self):

        # create user groups for domain
        group_one = self.create_and_add_user_group_to_domain(
            self.user_admin_client_2, self.domain_id_2)
        self.group_ids.append((group_one.id, self.domain_id_2))

        resp = self.user_admin_client_2.add_user_to_user_group(
            domain_id=self.domain_id_2, group_id=group_one.id,
            user_id=self.user_admin_2_id)
        self.assertEqual(resp.status_code, 204)

        # create parent DA when user group is a delegate
        parent_da_id = self.call_create_delegation_agreement(
            client=self.user_admin_client, delegate_id=group_one.id,
            user_delegate=False, sub_agreement_nest_level=1)

        # create nested da for which the user group is the principal
        nested_da_name = self.generate_random_string(
            pattern=const.DELEGATION_AGREEMENT_NAME_PATTERN)
        da_req = requests.DelegationAgreements(
            da_name=nested_da_name, sub_agreement_nest_level=0,
            parent_da_id=parent_da_id, principal_type=const.USER_GROUP,
            principal_id=group_one.id)
        nested_da_resp = self.user_admin_client_2.create_delegation_agreement(
            request_object=da_req)
        nested_da_id = nested_da_resp.json()[
            const.RAX_AUTH_DELEGATION_AGREEMENT][const.ID]
        self.validate_nested_da_response(nested_da_resp)

        # This is attempting to create a cyclic agreements as we are adding
        # principal of 1st DA as a delegate for the 3rd DA being created
        ua_clien_2 = self.user_admin_client_2
        resp = ua_clien_2.add_user_delegate_to_delegation_agreement(
            nested_da_id,
            self.user_admin_client.default_headers[const.X_USER_ID])
        self.assertEqual(resp.status_code, 204)

        # Trying to create another nested DA, which should fail
        nested_da_name_2 = self.generate_random_string(
            pattern=const.DELEGATION_AGREEMENT_NAME_PATTERN)
        da_req = requests.DelegationAgreements(
            da_name=nested_da_name_2, sub_agreement_nest_level=1,
            parent_da_id=nested_da_id)
        nested_da_resp_2 = self.user_admin_client.create_delegation_agreement(
            request_object=da_req)
        self.assertEqual(nested_da_resp_2.status_code, 403)
        self.assertEqual(nested_da_resp_2.json()[
                             const.FORBIDDEN][const.MESSAGE],
                         "Error code: 'GEN-006'; The parent agreement does not"
                         " allow nested agreements")

    @delegation.base.base.log_tearDown_error
    def tearDown(self):
        super(NestedDelegationAgreementsTests, self).tearDown()
        for group_id, domain_id in self.group_ids:
            resp = self.identity_admin_client.delete_user_group_from_domain(
                group_id=group_id, domain_id=domain_id)
            self.assertEqual(
                resp.status_code, 204,
                msg='User group with ID {0} failed to delete'.format(
                    group_id))

    @classmethod
    @delegation.base.base.log_tearDown_error
    def tearDownClass(cls):
        resp = cls.identity_admin_client.delete_user(
            user_id=cls.user_admin_client.default_headers[const.X_USER_ID])
        assert resp.status_code == 204, (
            'User with ID {0} failed to delete'.format(
              cls.user_admin_client.default_headers[const.X_USER_ID]))
        resp = cls.identity_admin_client.delete_user(
            user_id=cls.user_admin_2_id)
        assert resp.status_code == 204, (
            'User with ID {0} failed to delete'.format(cls.user_admin_2_id))
        super(NestedDelegationAgreementsTests, cls).tearDownClass()
