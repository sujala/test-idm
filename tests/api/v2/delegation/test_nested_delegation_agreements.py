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
        self.sub_users = []

    @attr(type='regression')
    def test_crud_nested_da(self):

        parent_nest_level = 2
        _, parent_da_id = self.call_create_delegation_agreement(
            client=self.user_admin_client,
            delegate_id=self.user_admin_2_id,
            sub_agreement_nest_level=parent_nest_level)
        get_parent_da_resp = self.user_admin_client.get_delegation_agreement(
            da_id=parent_da_id)
        # Checking that 'parent da id' attribute is not returned for the
        # first DA
        self.assertSchema(get_parent_da_resp, da_schema.add_da)

        # create nested da with level - 1
        nested_da_name = self.generate_random_string(
            pattern=const.DELEGATION_AGREEMENT_NAME_PATTERN)
        da_req = requests.DelegationAgreements(
            da_name=nested_da_name,
            sub_agreement_nest_level=(parent_nest_level - 1),
            parent_da_id=parent_da_id)
        nested_da_resp = self.user_admin_client_2.create_delegation_agreement(
            request_object=da_req)
        self.validate_nested_da_response(nested_da_resp)
        nest_level = nested_da_resp.json()[
            const.RAX_AUTH_DELEGATION_AGREEMENT][const.SUBAGREEMENT_NEST_LEVEL]
        self.assertEqual(nest_level, parent_nest_level - 1)

        nested_da_id = nested_da_resp.json()[
            const.RAX_AUTH_DELEGATION_AGREEMENT][const.ID]

        # Validate get da response
        get_nested_da_resp = self.user_admin_client_2.get_delegation_agreement(
            da_id=nested_da_id)
        self.validate_nested_da_response(get_nested_da_resp, resp_code=200)
        nest_level = get_nested_da_resp.json()[
            const.RAX_AUTH_DELEGATION_AGREEMENT][const.SUBAGREEMENT_NEST_LEVEL]
        self.assertEqual(nest_level, parent_nest_level - 1)

        # list da's
        list_da_resp = self.user_admin_client_2.list_delegation_agreements()
        self.assertEqual(list_da_resp.status_code, 200)
        self.assertSchema(list_da_resp, da_schema.list_da)
        das = [da for da in list_da_resp.json()[
            const.RAX_AUTH_DELEGATION_AGREEMENTS]]
        for da in das:
            if da[const.ID] == nested_da_id:
                self.assertIn(const.PARENT_DELEGATION_AGREEMENT_ID, da)
                self.assertEqual(
                    da[const.PARENT_DELEGATION_AGREEMENT_ID], parent_da_id)
                break

        self.validate_update_nested_da(
            nested_da_name=nested_da_name, nested_da_id=nested_da_id,
            parent_nest_level=parent_nest_level)

        # Delete parent DA
        resp = self.user_admin_client.delete_delegation_agreement(
            da_id=parent_da_id)
        self.assertEqual(resp.status_code, 204)

        # Get Parent DA
        resp = self.user_admin_client.get_delegation_agreement(
            da_id=parent_da_id)
        self.assertEqual(resp.status_code, 404)

        # Get Child DA - will be deleted due to parent DA delete.
        resp = self.user_admin_client.get_delegation_agreement(
            da_id=nested_da_id)
        self.assertEqual(resp.status_code, 404)

    def validate_update_nested_da(self, nested_da_name, nested_da_id,
                                  parent_nest_level):

        # update with an invalid nest-level
        da_req = requests.DelegationAgreements(
            da_name=nested_da_name,
            sub_agreement_nest_level=(parent_nest_level + 1))
        update_resp = self.user_admin_client_2.update_delegation_agreement(
            da_id=nested_da_id, request_object=da_req
        )
        self.assertEqual(update_resp.status_code, 400)
        self.assertEqual(
            update_resp.json()[const.BAD_REQUEST][const.MESSAGE],
            ("Error code: 'GEN-007'; subAgreementNestLevel value must "
             "be between 0 and {0}".format(parent_nest_level - 1)))

        # update with a valid nest-level
        da_req = requests.DelegationAgreements(
            da_name=nested_da_name,
            sub_agreement_nest_level=(parent_nest_level - 1))
        update_resp = self.user_admin_client_2.update_delegation_agreement(
            da_id=nested_da_id, request_object=da_req
        )
        self.assertEqual(update_resp.status_code, 200)
        self.assertSchema(update_resp, da_schema.add_da)
        self.assertEqual(
            update_resp.json()[const.RAX_AUTH_DELEGATION_AGREEMENT][
                const.SUBAGREEMENT_NEST_LEVEL],
            (parent_nest_level - 1))
        get_nested_da_resp = self.user_admin_client_2.get_delegation_agreement(
            da_id=nested_da_id)
        self.assertEqual(get_nested_da_resp.status_code, 200)
        self.assertEqual(
            get_nested_da_resp.json()[const.RAX_AUTH_DELEGATION_AGREEMENT][
                const.SUBAGREEMENT_NEST_LEVEL], 1)
        self.assertSchema(get_nested_da_resp, da_schema.add_da)
        if parent_nest_level > 1:
            self.assertTrue(
                get_nested_da_resp.json()[
                    const.RAX_AUTH_DELEGATION_AGREEMENT][
                    const.ALLOW_SUB_AGREEMENTS])

    def validate_nested_da_response(self, nested_da_resp, resp_code=201):

        self.assertEqual(nested_da_resp.status_code, resp_code)
        modified_schema = copy.deepcopy(da_schema.add_da)
        modified_schema['properties'][const.RAX_AUTH_DELEGATION_AGREEMENT][
            'required'] += [const.PARENT_DELEGATION_AGREEMENT_ID]
        self.assertSchema(nested_da_resp, modified_schema)

    @attr('skip_at_gate')
    def test_auth_under_nested_da(self):

        parent_nest_level = 2
        _, parent_da_id = self.call_create_delegation_agreement(
            client=self.user_admin_client, delegate_id=self.user_admin_2_id,
            sub_agreement_nest_level=parent_nest_level)

        # create nested da with level - 1
        nested_da_name = self.generate_random_string(
            pattern=const.DELEGATION_AGREEMENT_NAME_PATTERN)
        da_req = requests.DelegationAgreements(
            da_name=nested_da_name,
            # sub_agreement_nest_level=(parent_nest_level - 1),
            parent_da_id=parent_da_id)
        nested_da_resp = self.user_admin_client_2.create_delegation_agreement(
            request_object=da_req)
        nested_da_id = nested_da_resp.json()[
            const.RAX_AUTH_DELEGATION_AGREEMENT][const.ID]
        self.validate_nested_da_response(nested_da_resp)

        # Add a sub user in Domain 2
        sub_user_name = self.generate_random_string(
            pattern=const.SUB_USER_PATTERN)
        additional_input_data = {
            'user_name': sub_user_name}
        sub_user_client = self.generate_client(
            parent_client=self.user_admin_client_2,
            additional_input_data=additional_input_data)
        sub_user_id = sub_user_client.default_headers[const.X_USER_ID]
        # for sub-user cleanup
        self.sub_users.append(sub_user_id)

        # add sub-user in domain 2 as delegate to nested DA
        self.user_admin_client_2.add_user_delegate_to_delegation_agreement(
            da_id=nested_da_id, user_id=sub_user_id)

        delegation_auth_req = requests.AuthenticateWithDelegationAgreement(
            token=sub_user_client.default_headers[const.X_AUTH_TOKEN],
            delegation_agreement_id=nested_da_id)

        resp = self.identity_admin_client.get_auth_token(delegation_auth_req)
        self.assertEqual(resp.status_code, 200)
        self.assertEqual(
            resp.json()[const.ACCESS][const.USER][const.RAX_AUTH_DOMAIN_ID],
            self.domain_id)

    @attr(type='regression')
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
        _, parent_da_id = self.call_create_delegation_agreement(
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
        ua_client_2 = self.user_admin_client_2
        resp = ua_client_2.add_user_delegate_to_delegation_agreement(
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
        self.assertEqual(
            nested_da_resp_2.json()[const.FORBIDDEN][const.MESSAGE],
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

        for user_id in self.sub_users:
            resp = self.identity_admin_client.delete_user(user_id=user_id)
            assert resp.status_code == 204, (
                'Subuser with ID {0} failed to delete'.format(user_id))

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
