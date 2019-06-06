# -*- coding: utf-8 -*
from qe_coverage.opencafe_decorators import tags, unless_coverage
import pytest
from tests.api.utils import func_helper
from tests.api.v2 import base

from tests.api import base as parent_base
from tests.package.johny import constants as const
from tests.package.johny.v2.models import requests
from tests.api.v2.models import factory


class TestLockDownAbilityToCreateDomains(base.TestBaseV2):

    @classmethod
    @unless_coverage
    def setUpClass(cls):
        super(TestLockDownAbilityToCreateDomains, cls).setUpClass()
        req_obj = requests.Domain(
            domain_name=cls.generate_random_string(const.DOMAIN_PATTERN),
            description=cls.generate_random_string(const.DESC_PATTERN),
            enabled=True)
        # Create Domain with Role identity:rs-domain-admin
        resp = cls.identity_admin_client.add_domain(req_obj)
        assert resp.status_code == 201
        cls.domain_id = resp.json()[const.RAX_AUTH_DOMAIN][const.ID]

    @unless_coverage
    def setUp(self):
        super(TestLockDownAbilityToCreateDomains, self).setUp()
        self.user_ids = []

    @tags('positive', 'p0', 'smoke')
    @pytest.mark.smoke
    def test_identity_admin_can_create_admin_user_with_role(self):
        user_name = self.generate_random_string()
        request_input = requests.UserAdd(user_name=user_name,
                                         domain_id=self.domain_id)
        resp = self.identity_admin_client.add_user(
                            request_object=request_input)
        self.assertEqual(resp.status_code, 201)
        self.user_ids.append(resp.json()[const.USER][const.ID])

    @tags('positive', 'p0', 'smoke')
    @pytest.mark.smoke
    def test_identity_admin_can_create_user_with_role_for_one_user_call(self):
        user_name = self.generate_random_string()
        req_obj = factory.get_add_user_one_call_request_object(
            username=user_name, domainid=self.domain_id)
        resp = self.identity_admin_client.add_user(request_object=req_obj)
        self.assertEqual(resp.status_code, 201)
        user_id = resp.json()[const.USER][const.ID]
        self.user_ids.append(user_id)

    @tags('positive', 'p0', 'smoke')
    @pytest.mark.smoke
    def test_ia_can_add_user_with_one_user_call_for_nonexist_domain(self):
        # verify identity admin with role can create user
        # with one user call for non existing domain
        user_name = self.generate_random_string()
        domain_id = func_helper.generate_randomized_domain_id(
            client=self.identity_admin_client)
        # Identity admin has role identity:rs-domain-admin
        req_obj = factory.get_add_user_one_call_request_object(
            username=user_name, domainid=domain_id)
        resp = self.identity_admin_client.add_user(
            request_object=req_obj)
        self.assertEqual(resp.status_code, 201)

    @tags('positive', 'p0', 'regression')
    @parent_base.skip_if_no_service_admin_available
    def test_ia_cannot_add_user_with_one_user_call_for_nonexist_domain(self):
        # verify identity admin with no role cannot create user
        # with one user call for non existing domain
        user_name = self.generate_random_string()
        domain_id = func_helper.generate_randomized_domain_id(
            client=self.identity_admin_client)
        # Test Identity admin has no role identity:rs-domain-admin
        test_identity_admin_client = self.generate_client(
            parent_client=self.service_admin_client,
            additional_input_data={'domain_id': domain_id})
        req_obj = factory.get_add_user_one_call_request_object(
            username=user_name, domainid=domain_id)
        resp = test_identity_admin_client.add_user(
            request_object=req_obj)
        self.assertEqual(resp.status_code, 403)

    @tags('positive', 'p0', 'regression')
    @parent_base.skip_if_no_service_admin_available
    def test_identity_admin_cannot_create_domain_without_role(self):
        test_domain_id = func_helper.generate_randomized_domain_id(
            client=self.identity_admin_client)
        # Test Identity admin has no role identity:rs-domain-admin
        test_identity_admin_client = self.generate_client(
            parent_client=self.service_admin_client,
            additional_input_data={'domain_id': test_domain_id})
        req_obj = requests.Domain(
            domain_name=self.generate_random_string(const.DOMAIN_PATTERN),
            description=self.generate_random_string(const.DESC_PATTERN),
            enabled=True)
        resp = test_identity_admin_client.add_domain(req_obj)
        self.assertEqual(resp.status_code, 403)

    @base.base.log_tearDown_error
    @unless_coverage
    def tearDown(self):
        super(TestLockDownAbilityToCreateDomains, self).tearDown()
        for user_id in self.user_ids:
            resp = self.identity_admin_client.delete_user(user_id=user_id)
            assert resp.status_code in [204, 404], (
                'User with ID {0} failed to delete'.format(user_id))

    @classmethod
    @unless_coverage
    @base.base.log_tearDown_error
    def tearDownClass(cls):
        disable_domain_req = requests.Domain(enabled=False)
        cls.identity_admin_client.update_domain(
            domain_id=cls.domain_id, request_object=disable_domain_req)
        resp = cls.identity_admin_client.delete_domain(
            domain_id=cls.domain_id)
        assert resp.status_code == 204, (
                'Domain with ID {0} failed to delete'.format(
                    cls.domain_id))
        super(TestLockDownAbilityToCreateDomains, cls).tearDownClass()
