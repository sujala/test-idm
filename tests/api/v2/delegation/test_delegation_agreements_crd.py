# -*- coding: utf-8 -*
from nose.plugins.attrib import attr

from tests.api.utils import func_helper
from tests.api.v2 import base
from tests.api.v2.models import responses
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
        cls.domain_id = func_helper.generate_randomized_domain_id(
            client=cls.identity_admin_client)
        dom_req = requests.Domain(
            domain_name=cls.domain_id, domain_id=cls.domain_id, rcn=cls.rcn)
        add_dom_resp = cls.identity_admin_client.add_domain(dom_req)
        assert add_dom_resp.status_code == 201, (
            'domain was not created successfully')
        additional_input_data = {
            'domain_id': cls.domain_id
        }
        cls.user_admin_client = cls.generate_client(
            parent_client=cls.identity_admin_client,
            additional_input_data=additional_input_data)

        sub_user_name = cls.generate_random_string(
            pattern=const.SUB_USER_PATTERN)
        request_object = requests.UserAdd(user_name=sub_user_name)
        sub_user_resp = cls.user_admin_client.add_user(request_object)
        assert sub_user_resp.status_code == 201, 'sub user creation failed'
        cls.sub_user = responses.User(sub_user_resp.json())

    @attr(type='smoke_alpha')
    def test_delegation_agreement_crd(self):
        da_name = self.generate_random_string(
            pattern=const.DELEGATION_AGREEMENT_NAME_PATTERN)
        da_req = requests.DelegationAgreements(
            da_name=da_name, delegate_id=self.sub_user.id)
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
        resp = cls.user_admin_client.delete_user(cls.sub_user.id)
        assert resp.status_code == 204, (
            'Subuser with ID {0} failed to delete'.format(cls.sub_user_id))
        resp = cls.identity_admin_client.delete_user(
            user_id=cls.user_admin_client.default_headers[const.X_USER_ID])
        assert resp.status_code == 204, (
            'User with ID {0} failed to delete'.format(
                cls.user_admin_client.default_headers[const.X_USER_ID]))

        disable_domain_req = requests.Domain(enabled=False)
        cls.identity_admin_client.update_domain(
            domain_id=cls.domain_id, request_object=disable_domain_req)

        resp = cls.identity_admin_client.delete_domain(
            domain_id=cls.domain_id)
        assert resp.status_code == 204, (
            'Domain with ID {0} failed to delete'.format(cls.domain_id))
