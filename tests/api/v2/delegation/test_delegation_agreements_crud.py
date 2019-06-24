# -*- coding: utf-8 -*
import pytest
from qe_coverage.opencafe_decorators import tags, unless_coverage

from tests.api.v2.delegation import delegation
from tests.api.v2.schema import delegation as da_schema
from tests.package.johny import constants as const
from tests.package.johny.v2.models import requests


class DelegationAgreementsCrudTests(delegation.TestBaseDelegation):
    """
    Create/Read/Delete tests for Delegation Agreements
    """
    @classmethod
    @unless_coverage
    def setUpClass(cls):
        super(DelegationAgreementsCrudTests, cls).setUpClass()
        cls.user_admin_2_id = cls.user_admin_client_2.default_headers[
            const.X_USER_ID]

    @unless_coverage
    def setUp(self):
        super(DelegationAgreementsCrudTests, self).setUp()

    @tags('positive', 'p0', 'regression')
    @pytest.mark.regression
    def test_delegation_agreement_crud(self):
        # assert that the subAgreements attribute is false
        self.validate_delegation_agreements_crud(
            sub_agreement_nest_level=0,
            client=self.user_admin_client)

    @tags('positive', 'p0', 'regression')
    @pytest.mark.regression
    def test_delegation_agreement_crud_with_sub_agreements(self):
        # assert that the subAgreements attribute is true
        self.validate_delegation_agreements_crud(
            sub_agreement_nest_level=1,
            client=self.user_admin_client)

    @tags('positive', 'p0', 'regression')
    @pytest.mark.regression
    def test_delegation_agreement_crud_rcn_admin(self):
        # assert that the subAgreements attribute is false
        self.validate_delegation_agreements_crud(
            client=self.rcn_admin_client,
            sub_agreement_nest_level=0)

    @tags('positive', 'p0', 'regression')
    @pytest.mark.regression
    def test_list_delegation_agreements(self):

        da_1_id, da_2_id = self.create_multiple_das_for_principal()

        list_da_resp = self.user_admin_client.list_delegation_agreements()
        self.validate_list_delegation_agreements_resp(
            list_da_resp=list_da_resp, da_1_id=da_1_id, da_2_id=da_2_id)

        list_da_resp = self.rcn_admin_client.list_delegation_agreements()
        self.validate_list_delegation_agreements_resp(
            list_da_resp=list_da_resp, da_1_id=da_1_id, da_2_id=da_2_id)

        # Call list DAs as principal with query param 'delegate' & see both DAs
        # are returned
        option = {
            const.RELATIONSHIP: const.QUERY_PARAM_PRINCIPAL
        }
        list_da_resp = self.user_admin_client.list_delegation_agreements(
            option=option)
        self.validate_list_delegation_agreements_resp(
            list_da_resp=list_da_resp, da_1_id=da_1_id, da_2_id=da_2_id)

        # Call list DAs as delegate with query param 'delegate' & see both DAs
        # are returned
        da_1_id, da_2_id = self.create_multiple_das_for_principal()
        option = {
            const.RELATIONSHIP: const.QUERY_PARAM_DELEGATE
        }
        list_da_resp = self.user_admin_client_2.list_delegation_agreements(
            option=option)
        self.validate_list_delegation_agreements_resp(
            list_da_resp=list_da_resp, da_1_id=da_1_id, da_2_id=da_2_id)

    def create_multiple_das_for_principal(self):

        # Create two DAs for same principal
        _, da_1_id = self.call_create_delegation_agreement(
            client=self.user_admin_client, delegate_id=self.user_admin_2_id)

        self.user_admin_client.add_user_delegate_to_delegation_agreement(
            da_1_id, self.user_admin_2_id)

        _, da_2_id = self.call_create_delegation_agreement(
            client=self.user_admin_client, delegate_id=self.user_admin_2_id)

        self.user_admin_client.add_user_delegate_to_delegation_agreement(
            da_2_id, self.user_admin_2_id)

        return da_1_id, da_2_id

    def validate_delegation_agreements_crud(
            self, sub_agreement_nest_level, client):
        # Create DA
        da_name = self.generate_random_string(
            pattern=const.DELEGATION_AGREEMENT_NAME_PATTERN)
        da_resp, da_id = self.call_create_delegation_agreement(
            client=client,
            delegate_id=self.user_admin_2_id,
            da_name=da_name,
            sub_agreement_nest_level=sub_agreement_nest_level)

        self.assertEqual(da_resp.status_code, 201)
        self.assertEqual(
            da_resp.json()[const.RAX_AUTH_DELEGATION_AGREEMENT][
                const.SUBAGREEMENT_NEST_LEVEL],
            sub_agreement_nest_level
        )
        # TODO: Add schema validations once contracts are finalized for
        # Add User to DA
        self.user_admin_client.add_user_delegate_to_delegation_agreement(
            da_id, self.user_admin_2_id)

        # Read DA
        get_resp = client.get_delegation_agreement(da_id=da_id)
        self.assertEqual(get_resp.status_code, 200)
        self.assertEqual(
            get_resp.json()[const.RAX_AUTH_DELEGATION_AGREEMENT][const.NAME],
            da_name)

        self.assertEqual(
            get_resp.json()[const.RAX_AUTH_DELEGATION_AGREEMENT][
                const.SUBAGREEMENT_NEST_LEVEL],
            sub_agreement_nest_level
        )

        # Update DA
        update_da_name = self.generate_random_string(
            pattern=const.DELEGATION_AGREEMENT_NAME_PATTERN)
        da_req = requests.DelegationAgreements(
            da_name=update_da_name)

        update_resp = client.update_delegation_agreement(
            da_id=da_id, request_object=da_req)

        self.assertEqual(update_resp.status_code, 200)
        self.assertEqual(
            update_resp.json()[
                const.RAX_AUTH_DELEGATION_AGREEMENT][const.NAME],
            update_da_name)

        # Delete DA
        delete_resp = client.delete_delegation_agreement(da_id=da_id)
        self.assertEqual(delete_resp.status_code, 204)

        get_resp = self.user_admin_client.get_delegation_agreement(
            da_id=da_id)
        self.assertEqual(get_resp.status_code, 404)

    def validate_list_delegation_agreements_resp(
            self, list_da_resp, da_1_id, da_2_id):

        self.assertEqual(list_da_resp.status_code, 200)
        self.assertSchema(list_da_resp, json_schema=da_schema.list_da)
        da_ids_from_resp = [da[const.ID] for da in list_da_resp.json()[
            const.RAX_AUTH_DELEGATION_AGREEMENTS]]
        self.assertIn(da_1_id, da_ids_from_resp)
        self.assertIn(da_2_id, da_ids_from_resp)

    @unless_coverage
    def tearDown(self):
        super(DelegationAgreementsCrudTests, self).tearDown()

    @classmethod
    @delegation.base.base.log_tearDown_error
    @unless_coverage
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
        super(DelegationAgreementsCrudTests, cls).tearDownClass()
