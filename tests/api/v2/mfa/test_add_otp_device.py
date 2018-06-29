# -*- coding: utf-8 -*
import ddt
from nose.plugins.attrib import attr
from qe_coverage.opencafe_decorators import unless_coverage

from tests.api.utils import func_helper
from tests.api.v2 import base
from tests.package.johny import constants as const
from tests.package.johny.v2.models import requests


@ddt.ddt
class AddOtpDeviceForUser(base.TestBaseV2):

    @classmethod
    @unless_coverage
    def setUpClass(cls):

        super(AddOtpDeviceForUser, cls).setUpClass()
        cls.domain_id = func_helper.generate_randomized_domain_id(
            client=cls.identity_admin_client,
            pattern=const.DOMAIN_PATTERN)
        cls.user_admin_client = cls.generate_client(
            parent_client=cls.identity_admin_client,
            additional_input_data={'domain_id': cls.domain_id})

    @unless_coverage
    def setUp(self):
        super(AddOtpDeviceForUser, self).setUp()

    @unless_coverage
    @ddt.data("", "  ", None)
    @attr('skip_at_gate')
    def test_add_otp_device_with_invalid_name(self, device_name):

        otp_req = requests.OTPDeviceAdd(device_name=device_name)
        resp = self.user_admin_client.create_otp_device(
            user_id=self.user_admin_client.default_headers[const.X_USER_ID],
            request_object=otp_req)
        self.assertEqual(resp.status_code, 400)
        self.assertEqual(resp.json()[const.BAD_REQUEST][const.MESSAGE],
                         "Must provide a name for an OTP device")

    @unless_coverage
    def tearDown(self):
        super(AddOtpDeviceForUser, self).tearDown()

    @classmethod
    @unless_coverage
    def tearDownClass(cls):
        cls.delete_client(client=cls.user_admin_client,
                          parent_client=cls.identity_admin_client)
        super(AddOtpDeviceForUser, cls).tearDownClass()
