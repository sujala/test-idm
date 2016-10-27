#!/usr/bin/env python
"""Some helper functions can be used cross tests"""
import urlparse
from tests.api.utils import hotpie


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
