# -*- coding: utf-8 -*
from munch import Munch

from nose.plugins.attrib import attr

from tests.api.utils import func_helper
from tests.api.v2 import base
from tests.package.johny import constants as const
from tests.package.johny.v2.models import requests


class DelegationAgreementsCrdTests(base.TestBaseV2):
    """
    Create/Read/Delete tests for Delegation Agreements
    """

    @classmethod
    def setUpClass(cls):
        super(DelegationAgreementsCrdTests, cls).setUpClass()
        cls.rcn = cls.test_config.da_rcn
        cls.domain_ids = []
        domain_id = cls.create_domain_with_rcn()
        additional_input_data = {
            'domain_id': domain_id
        }
        cls.user_admin_client = cls.generate_client(
            parent_client=cls.identity_admin_client,
            additional_input_data=additional_input_data)

        domain_id_2 = cls.create_domain_with_rcn()
        additional_input_data = {'domain_id': domain_id_2}
        cls.user_admin_client_2 = cls.generate_client(
            parent_client=cls.identity_admin_client,
            additional_input_data=additional_input_data)
        cls.user_admin_2_id = cls.user_admin_client_2.default_headers[
            const.X_USER_ID]

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

    def test_delegation_agreement_crd(self):
        da_name = self.generate_random_string(
            pattern=const.DELEGATION_AGREEMENT_NAME_PATTERN)
        da_resp = self.call_create_delegation_agreement(
            client=self.user_admin_client, delegate_id=self.user_admin_2_id,
            da_name=da_name)
        self.assertEqual(da_resp.status_code, 201)
        # TODO: Add schema validations once contracts are finalized for
        # Delegation agreements
        da_id = da_resp.json()[const.RAX_AUTH_DELEGATION_AGREEMENT][const.ID]

        self.user_admin_client.add_user_delegate_to_delegation_agreement(
            da_id, self.user_admin_2_id)

        get_resp = self.user_admin_client.get_delegation_agreement(
            da_id=da_id)
        self.assertEqual(get_resp.status_code, 200)
        self.assertEqual(
            get_resp.json()[const.RAX_AUTH_DELEGATION_AGREEMENT][const.NAME],
            da_name)
        get_resp = self.user_admin_client.delete_delegation_agreement(
            da_id=da_id)
        self.assertEqual(get_resp.status_code, 204)
        get_resp = self.user_admin_client.get_delegation_agreement(
            da_id=da_id)
        self.assertEqual(get_resp.status_code, 404)

    @attr(type='regression')
    def test_list_delegation_agreements(self):

        # Create two DAs for same principal & see if list shows both
        da_1_resp = self.call_create_delegation_agreement(
            client=self.user_admin_client, delegate_id=self.user_admin_2_id)
        da_1_resp_parsed = Munch.fromDict(da_1_resp.json())
        da_1_id = da_1_resp_parsed[const.RAX_AUTH_DELEGATION_AGREEMENT].id

        self.user_admin_client.add_user_delegate_to_delegation_agreement(
            da_1_id, self.user_admin_2_id)

        da_2_resp = self.call_create_delegation_agreement(
            client=self.user_admin_client, delegate_id=self.user_admin_2_id)
        da_2_resp_parsed = Munch.fromDict(da_2_resp.json())
        da_2_id = da_2_resp_parsed[const.RAX_AUTH_DELEGATION_AGREEMENT].id

        self.user_admin_client.add_user_delegate_to_delegation_agreement(
            da_2_id, self.user_admin_2_id)

        list_da_resp = self.user_admin_client.list_delegation_agreements()
        self.validate_list_delegation_agreements_resp(
            list_da_resp=list_da_resp, da_1_id=da_1_id, da_2_id=da_2_id)

        # Call list DAs as principal with query param & see both DAs are
        # returned
        option = {
            const.RELATIONSHIP: const.QUERY_PARAM_PRINCIPAL
        }
        list_da_resp = self.user_admin_client.list_delegation_agreements(
            option=option)
        self.validate_list_delegation_agreements_resp(
            list_da_resp=list_da_resp, da_1_id=da_1_id, da_2_id=da_2_id)

        # Call list DAs as delegate with query param 'delegate' & see both DAs
        # are returned
        option = {
            const.RELATIONSHIP: const.QUERY_PARAM_DELEGATE
        }
        list_da_resp = self.user_admin_client_2.list_delegation_agreements(
            option=option)
        self.validate_list_delegation_agreements_resp(
            list_da_resp=list_da_resp, da_1_id=da_1_id, da_2_id=da_2_id)

    def validate_delegation_agreements_crud(self, allow_sub_agreements):
        da_name = self.generate_random_string(
            pattern=const.DELEGATION_AGREEMENT_NAME_PATTERN)
        da_resp = self.call_create_delegation_agreement(
            client=self.user_admin_client, delegate_id=self.user_admin_2_id,
            da_name=da_name, allow_sub_agreements=allow_sub_agreements)
        self.assertEqual(da_resp.status_code, 201)

        self.assertEqual(
            da_resp.json()[const.RAX_AUTH_DELEGATION_AGREEMENT][
                const.ALLOW_SUB_AGREEMENTS],
            allow_sub_agreements
        )

        # TODO: Add schema validations once contracts are finalized for
        # Delegation agreements
        da_id = da_resp.json()[const.RAX_AUTH_DELEGATION_AGREEMENT][const.ID]
        get_resp = self.user_admin_client.get_delegation_agreement(
            da_id=da_id)
        self.assertEqual(get_resp.status_code, 200)
        self.assertEqual(
            get_resp.json()[const.RAX_AUTH_DELEGATION_AGREEMENT][const.NAME],
            da_name)

        self.assertEqual(
            get_resp.json()[const.RAX_AUTH_DELEGATION_AGREEMENT][
                const.ALLOW_SUB_AGREEMENTS],
            allow_sub_agreements
        )

        get_resp = self.user_admin_client.delete_delegation_agreement(
            da_id=da_id)
        self.assertEqual(get_resp.status_code, 204)
        get_resp = self.user_admin_client.get_delegation_agreement(
            da_id=da_id)
        self.assertEqual(get_resp.status_code, 404)

    def validate_list_delegation_agreements_resp(
            self, list_da_resp, da_1_id, da_2_id):

        self.assertEqual(list_da_resp.status_code, 200)
        da_ids_from_resp = [da[const.ID] for da in list_da_resp.json()[
            const.RAX_AUTH_DELEGATION_AGREEMENTS]]
        self.assertIn(da_1_id, da_ids_from_resp)
        self.assertIn(da_2_id, da_ids_from_resp)

    def call_create_delegation_agreement(self, client, delegate_id,
                                         da_name=None,
                                         allow_sub_agreements=None):
        if not da_name:
            da_name = self.generate_random_string(
                pattern=const.DELEGATION_AGREEMENT_NAME_PATTERN)
        da_req = requests.DelegationAgreements(da_name=da_name)
        da_resp = client.create_delegation_agreement(request_object=da_req)
        da_id = da_resp.json()[const.RAX_AUTH_DELEGATION_AGREEMENT][const.ID]

        client.add_user_delegate_to_delegation_agreement(
            da_id, delegate_id)

        return da_resp

    @classmethod
    @base.base.log_tearDown_error
    def tearDownClass(cls):
        super(DelegationAgreementsCrdTests, cls).tearDownClass()
        resp = cls.identity_admin_client.delete_user(
            user_id=cls.user_admin_client.default_headers[const.X_USER_ID])
        assert resp.status_code == 204, (
            'User with ID {0} failed to delete'.format(
              cls.user_admin_client.default_headers[const.X_USER_ID]))
        resp = cls.identity_admin_client.delete_user(
            user_id=cls.user_admin_2_id)
        assert resp.status_code == 204, (
            'User with ID {0} failed to delete'.format(cls.user_admin_2_id))

        disable_domain_req = requests.Domain(enabled=False)
        for domain_id in cls.domain_ids:
            cls.identity_admin_client.update_domain(
                domain_id=domain_id, request_object=disable_domain_req)

            resp = cls.identity_admin_client.delete_domain(
                domain_id=domain_id)
            assert resp.status_code == 204, (
                'Domain with ID {0} failed to delete'.format(domain_id))
