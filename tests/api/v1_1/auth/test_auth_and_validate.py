# -*- coding: utf-8 -*
import ddt
import pytest
from qe_coverage.opencafe_decorators import tags, unless_coverage
from tests.api.utils import header_validation
from tests.api.utils import func_helper

from tests.api.v1_1 import base
from tests.package.johny import constants as const

from tests.package.johny.v2.models import requests
from tests.api.v2.models import responses as responses


@ddt.ddt
class TestAuthAndValidationV11(base.TestBaseV1):

    """
        AuthAndValidateTokens for v1.1
    """

    @classmethod
    @unless_coverage
    def setUpClass(cls):
        super(TestAuthAndValidationV11, cls).setUpClass()

    @unless_coverage
    def setUp(self):
        super(TestAuthAndValidationV11, self).setUp()
        self.user_ids = []
        self.tenant_ids = []
        self.user_info = self.create_user_get_info()

    def create_user_get_info(self):
        # Create a userAdmin with tenantId
        username = self.generate_random_string(
            pattern=const.USER_ADMIN_PATTERN)
        domain_id = func_helper.generate_randomized_domain_id(
            client=self.identity_admin_client_v2)
        input_data = {'email': const.EMAIL_RANDOM,
                      'secret_qa': {
                          const.SECRET_QUESTION: self.generate_random_string(
                              pattern=const.LOWER_CASE_LETTERS),
                          const.SECRET_ANSWER: self.generate_random_string(
                              pattern=const.UPPER_CASE_LETTERS)},
                      'domain_id': domain_id}
        req_obj = requests.UserAdd(user_name=username, **input_data)

        resp = self.identity_admin_client_v2.add_user(request_object=req_obj)
        self.assertEqual(resp.status_code, 201)
        create_user_with_tenant_resp = responses.User(resp.json())
        self.user_ids.append(create_user_with_tenant_resp.id)
        self.domain_id = domain_id

        password = resp.json()[const.USER][const.NS_PASSWORD]

        # Get user's tenant ID
        auth_obj = requests.AuthenticateWithPassword(user_name=username,
                                                     password=password)
        resp = self.identity_admin_client_v2.get_auth_token(
            request_object=auth_obj)
        self.assertEqual(resp.status_code, 200)
        auth_user_pass_resp = responses.Access(resp.json())
        self.tenant_ids.append(auth_user_pass_resp.access.token.tenant.id)

        user_id = create_user_with_tenant_resp.id

        # Get apikey
        resp = self.identity_admin_client_v2.get_api_key(user_id)
        self.assertEqual(resp.status_code, 200)
        api_key = (resp.json()[const.NS_API_KEY_CREDENTIALS]
                   [const.API_KEY])

        return {'id': create_user_with_tenant_resp.user_name,
                'key': api_key,
                'mosso_id': domain_id,
                'nast_id': const.NAST_PREFIX + domain_id}

    def validate_resp_token(self, token):
        resp = self.identity_admin_client.validate_token(token_id=token)
        self.assertEqual(resp.status_code, 200)
        header_validation.validate_header_not_present('response-source')(resp)

    @tags('positive', 'p0', 'smoke')
    @pytest.mark.smoke
    def test_auth_with_key_and_validate_token(self):
        username = self.user_info['id']
        key = self.user_info['key']

        auth_resp = self.identity_admin_client.auth_user_key(
            user_name=username, key=key)
        self.assertEqual(auth_resp.status_code, 200)
        header_validation.validate_header_not_present('response-source')(
            auth_resp)
        header_validation.validate_header_tenant_id(
            value=str(self.user_info['mosso_id']))(auth_resp)

        # This check is added to verify repose removes the header. If this
        # test is run against identity directly, it will fail.
        self.assertHeaders(auth_resp, (
            header_validation.validate_username_header_not_present))

        token_id = auth_resp.entity.token.id
        # validate token
        self.validate_resp_token(token=token_id)

    @tags('negative', 'p1', 'regression')
    @pytest.mark.skip_at_gate
    def test_auth_with_invalid_key(self):
        username = self.user_info['id']
        key = "invalid"

        auth_resp = self.identity_admin_client.auth_user_key(
            user_name=username, key=key)
        self.assertEqual(auth_resp.status_code, 401)
        header_validation.validate_header_not_present(
                [const.X_USER_NAME, const.X_TENANT_ID])(auth_resp)

    @tags('positive', 'p0', 'smoke')
    @pytest.mark.smoke
    def test_auth_with_key_and_mosso_and_validate_token(self):
        key = self.user_info['key']
        mosso_id = self.user_info['mosso_id']

        auth_resp = self.identity_admin_client.auth_mosso_key(
            mosso_id=mosso_id, key=key)
        self.assertEqual(auth_resp.status_code, 200)

        header_validation.validate_header_not_present('response-source')(
            auth_resp)
        header_validation.validate_header_tenant_id(
            value=str(self.user_info['mosso_id']))(auth_resp)

        # This check is added to verify repose removes the header. If this
        # test is run against identity directly, it will fail.
        self.assertHeaders(auth_resp, (
            header_validation.validate_username_header_not_present))

        token_id = auth_resp.entity.token.id
        # validate token
        self.validate_resp_token(token=token_id)

    @tags('negative', 'p1', 'regression')
    @pytest.mark.skip_at_gate
    def test_auth_with_invalid_key_and_mosso(self):
        key = 'invalid'
        mosso_id = self.user_info['mosso_id']

        auth_resp = self.identity_admin_client.auth_mosso_key(
            mosso_id=mosso_id, key=key)
        self.assertEqual(auth_resp.status_code, 401)
        header_validation.validate_header_not_present(
            [const.X_USER_NAME, const.X_TENANT_ID])(auth_resp)

    @tags('positive', 'p0', 'smoke')
    @pytest.mark.smoke
    def test_auth_with_key_and_nast_and_validate_token(self):
        key = self.user_info['key']
        nast_id = self.user_info['nast_id']

        auth_resp = self.identity_admin_client.auth_nast_key(
            nast_id=nast_id, key=key)
        self.assertEqual(auth_resp.status_code, 200)
        header_validation.validate_header_not_present('response-source')(
            auth_resp)
        header_validation.validate_header_tenant_id(
            value=str(self.user_info['nast_id']))(auth_resp)

        # This check is added to verify repose removes the header. If this
        # test is run against identity directly, it will fail.
        self.assertHeaders(auth_resp, (
            header_validation.validate_username_header_not_present))

        token_id = auth_resp.entity.token.id
        # validate token
        self.validate_resp_token(token=token_id)

    @tags('negative', 'p1', 'regression')
    @pytest.mark.skip_at_gate
    def test_auth_with_invalid_key_and_nast(self):
        key = 'invalid'
        nast_id = self.user_info['nast_id']

        auth_resp = self.identity_admin_client.auth_nast_key(
            nast_id=nast_id, key=key)
        self.assertEqual(auth_resp.status_code, 401)
        header_validation.validate_header_not_present(
            [const.X_USER_NAME, const.X_TENANT_ID])(auth_resp)

    @tags('positive', 'p0', 'smoke')
    @pytest.mark.smoke
    def test_auth_with_username_password(self):
        admin_password = self.identity_config.identity_admin_password
        admin_username = self.identity_config.identity_admin_user_name

        auth_resp = self.identity_admin_client.auth_user_password(
            user_name=admin_username, password=admin_password)
        self.assertEqual(auth_resp.status_code, 200)
        header_validation.validate_header_not_present('response-source')(
            auth_resp)

        # This check is added to verify repose removes the header. If this
        # test is run against identity directly, it will fail.
        self.assertHeaders(auth_resp, (
            header_validation.validate_username_header_not_present))

        # Not validating tenant_id value, because this is a pre-existing user
        # and we don't know the tenant_id
        header_validation.basic_header_validations(
            response=auth_resp, header=const.X_TENANT_ID)

        token_id = auth_resp.entity.token.id
        # validate token
        self.validate_resp_token(token=token_id)

    @tags('negative', 'p0', 'regression')
    @pytest.mark.skip_at_gate
    def test_auth_with_username_and_invalid_password(self):
        admin_password = 'invalid'
        admin_username = self.identity_config.identity_admin_user_name

        auth_resp = self.identity_admin_client.auth_user_password(
            user_name=admin_username, password=admin_password)
        self.assertEqual(auth_resp.status_code, 401)
        header_validation.validate_header_not_present(
            [const.X_USER_NAME, const.X_TENANT_ID])(auth_resp)

    @unless_coverage
    def tearDown(self):
        for user_id in self.user_ids:
            resp = self.identity_admin_client_v2.delete_user(user_id=user_id)
            self.assertEqual(
                resp.status_code, 204,
                msg='User with ID {0} failed to delete'.format(user_id))
        for tenant_id in self.tenant_ids:
            resp = self.identity_admin_client_v2.delete_tenant(
                tenant_id=tenant_id)
            self.assertEqual(
                resp.status_code, 204,
                msg='Tenant with ID {0} failed to delete'.format(
                    tenant_id))
        super(TestAuthAndValidationV11, self).tearDown()

    @classmethod
    @unless_coverage
    def tearDownClass(cls):
        super(TestAuthAndValidationV11, cls).tearDownClass()
