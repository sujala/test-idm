# -*- coding: utf-8 -*
import ddt
from nose.plugins.attrib import attr
from qe_coverage.opencafe_decorators import tags, unless_coverage
from random import randrange
from tests.api.utils import header_validation

from tests.api.v1_1 import base
from tests.api.v1_1 import requests
from tests.package.johny import constants as const


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
        self.user_info = self.create_user_get_info()
        self.user_ids = []
        self.user_ids.append(self.user_info['id'])

    def create_user_get_info(self):
        user_id = self.generate_random_string(pattern=const.USER_NAME_PATTERN)
        key = self.generate_random_string(pattern=const.API_KEY_PATTERN)
        mosso_id = randrange(start=const.CONTACT_ID_MIN,
                             stop=const.CONTACT_ID_MAX)
        enabled = True

        auth_obj = requests.User(id=user_id, key=key, mossoId=mosso_id,
                                 enabled=enabled)
        resp = self.identity_admin_client.add_user(request_object=auth_obj)
        self.assertEqual(resp.status_code, 201)
        userId = resp.entity.id
        apiKey = resp.entity.key
        mossoId = resp.entity.mossoId
        nastId = resp.entity.nastId
        return {'id': userId, 'key': apiKey, 'mosso_id': mossoId,
                'nast_id': nastId}

    def validate_resp_token(self, token):
        resp = self.identity_admin_client.validate_token(token_id=token)
        self.assertEqual(resp.status_code, 200)
        header_validation.validate_header_not_present('response-source')(resp)

    @tags('positive', 'p0', 'smoke')
    @attr(type='smoke')
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
    @attr('skip_at_gate')
    def test_auth_with_invalid_key(self):
        username = self.user_info['id']
        key = "invalid"

        auth_resp = self.identity_admin_client.auth_user_key(
            user_name=username, key=key)
        self.assertEqual(auth_resp.status_code, 401)
        header_validation.validate_header_not_present(
                [const.X_USER_NAME, const.X_TENANT_ID])(auth_resp)

    @tags('positive', 'p0', 'smoke')
    @attr(type='smoke')
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
    @attr('skip_at_gate')
    def test_auth_with_invalid_key_and_mosso(self):
        key = 'invalid'
        mosso_id = self.user_info['mosso_id']

        auth_resp = self.identity_admin_client.auth_mosso_key(
            mosso_id=mosso_id, key=key)
        self.assertEqual(auth_resp.status_code, 401)
        header_validation.validate_header_not_present(
            [const.X_USER_NAME, const.X_TENANT_ID])(auth_resp)

    @tags('positive', 'p0', 'smoke')
    @attr(type='smoke')
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
    @attr('skip_at_gate')
    def test_auth_with_invalid_key_and_nast(self):
        key = 'invalid'
        nast_id = self.user_info['nast_id']

        auth_resp = self.identity_admin_client.auth_nast_key(
            nast_id=nast_id, key=key)
        self.assertEqual(auth_resp.status_code, 401)
        header_validation.validate_header_not_present(
            [const.X_USER_NAME, const.X_TENANT_ID])(auth_resp)

    @tags('positive', 'p0', 'smoke')
    @attr(type='smoke')
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
    @attr('skip_at_gate')
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
        for id_ in self.user_ids:
            self.identity_admin_client.delete_user(user_id=id_)
        super(TestAuthAndValidationV11, self).tearDown()

    @classmethod
    @unless_coverage
    def tearDownClass(cls):
        super(TestAuthAndValidationV11, cls).tearDownClass()
