# -*- coding: utf-8 -*
from nose.plugins.attrib import attr

from tests.api.utils import func_helper
from tests.api.v2 import base
from tests.api.v2.models import factory, responses
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
        input_data = {
            'domain_id': domain_id_2
        }
        req_object = factory.get_add_user_request_object(
            input_data=input_data)
        resp = cls.identity_admin_client.add_user(req_object)
        cls.user_admin_2 = responses.User(resp.json())

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

    @attr(type='smoke_alpha')
    def test_delegation_agreement_crd(self):
        da_name = self.generate_random_string(
            pattern=const.DELEGATION_AGREEMENT_NAME_PATTERN)
        da_req = requests.DelegationAgreements(
            da_name=da_name, delegate_id=self.user_admin_2.id)
        da_resp = self.user_admin_client.create_delegation_agreement(
            request_object=da_req)
        self.assertEqual(da_resp.status_code, 201)
        # TODO: Add schema validations once contracts are finalized for
        # Delegation agreements
        da_id = da_resp.json()[const.RAX_AUTH_DELEGATION_AGREEMENT][const.ID]
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
            user_id=cls.user_admin_2.id)
        assert resp.status_code == 204, (
            'User with ID {0} failed to delete'.format(cls.user_admin_2.id))

        disable_domain_req = requests.Domain(enabled=False)
        for domain_id in cls.domain_ids:
            cls.identity_admin_client.update_domain(
                domain_id=domain_id, request_object=disable_domain_req)

            resp = cls.identity_admin_client.delete_domain(
                domain_id=domain_id)
            assert resp.status_code == 204, (
                'Domain with ID {0} failed to delete'.format(domain_id))
