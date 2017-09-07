#!/usr/bin/env python
"""Some helper functions can be used cross tests"""
from strgen import StringGenerator
import urlparse

from tests.api.utils import hotpie
from tests.package.johny import constants as const
from tests.package.johny.v2.models import requests


def parse_secret_from_otp_device(otp_response):
    """Parse secret code from create otp device response
        Used in authenticate with otp device
    """
    key_uri = otp_response.json()["RAX-AUTH:otpDevice"]["keyUri"]
    o = urlparse.urlparse(key_uri)
    return urlparse.parse_qs(o.query)["secret"][0]


def get_oath_from_secret(secret):
    """Obtain Oath from the secret, using hotpie
        used in authenticate with otp device
    """
    return hotpie.six_digit_totp_from_b32secret(secret)


def add_otp_device(user_id, client):
    opt_object = requests.OTPDeviceAdd(
        device_name=StringGenerator(const.OTP_NAME_PATTERN).render())
    resp = client.create_otp_device(
        user_id=user_id, request_object=opt_object)
    assert resp.status_code == 201
    device_id = resp.json()[const.NS_OTP_DEVICE][const.ID]
    return device_id, resp


def setup_mfa_for_user(user_id, client):

    device_id, resp = add_otp_device(user_id=user_id, client=client)
    secret = parse_secret_from_otp_device(otp_response=resp)
    code = get_oath_from_secret(secret=secret)
    # verify otp device
    verify_obj = requests.OTPDeviceVerify(code=code)
    resp = client.verify_otp_device(
        user_id=user_id, otp_device_id=device_id,
        request_object=verify_obj
    )
    assert resp.status_code == 204
    # update mfa enabled=true
    update_obj = requests.MFAUpdate(enabled=True)
    resp = client.update_mfa(user_id=user_id, request_object=update_obj)
    assert resp.status_code == 204
    return secret


def generate_randomized_domain_id(
        client, pattern=const.NUMERIC_DOMAIN_ID_PATTERN, non_existing=True):
    """
    Provides the ability to generate non existing domain id
    :param client: Client to make the call to GET DOMAIN
    :param pattern: Pattern to provide to random string generator
    :param non_existing: If set to True, this function provides a domain ID
      until it finds one which is non-existing. If False, it simply generates
      a random string but doesn't check if a domain exists with that ID
    """
    domain_id = StringGenerator(pattern=pattern).render()
    if non_existing:
        while True:
            get_domain_resp = client.get_domain(domain_id)
            if get_domain_resp.status_code != 200:
                break
            domain_id = StringGenerator(pattern=pattern).render()
    return domain_id
