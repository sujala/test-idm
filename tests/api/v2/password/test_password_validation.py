# -*- coding: utf-8 -*
import ddt

from nose.plugins.attrib import attr
from qe_coverage.opencafe_decorators import tags, unless_coverage

from tests.api.v2 import base
from tests.api.v2.schema import password as password_json

from tests.package.johny.v2.models import requests


@ddt.ddt
class TestPasswordValidation(base.TestBaseV2):
    """Tests for password validation endpoint."""

    @unless_coverage
    def setUp(self):
        super(TestPasswordValidation, self).setUp()

    @tags('positive', 'p0', 'smoke')
    @attr(type='smoke_alpha')
    @ddt.data('Password', 'Str0ngPassw#rd')
    def test_password_validation(self, password):

        pwd_validation_obj = requests.PasswordValidation(
            password='Password')

        resp = self.identity_admin_client.validate_password(
            request_object=pwd_validation_obj)
        self.assertEqual(resp.status_code, 200)
        self.assertSchema(resp, password_json.password_validation)

    @unless_coverage
    def tearDown(self):
        super(TestPasswordValidation, self).tearDown()
