#!/usr/bin/env python
"""
OATH HOTP + TOTP Implementation in python.

Based on http://tools.ietf.org/html/rfc4226

Parameter and function names kept inline with the RFC
(e.g. HOTP, Truncate, K, C etc)
"""

import base64
import hashlib
import hmac
import random
import struct
import time
import unittest


def new_six_digit_totp_from_b32secret(secret):
    """
    Brute force wait for TOTP to cycle. Returns 6 digit TOTP from base 32
    secret
    """
    totp_1 = six_digit_totp_from_b32secret(secret)
    totp_2 = six_digit_totp_from_b32secret(secret)
    while (totp_1 == totp_2):
        totp_2 = six_digit_totp_from_b32secret(secret)
    return totp_2


def six_digit_totp_from_b32secret(secret):
    """
    Get TOTP (6 digit integer) from base 32 encoded secret.
    """
    return TOTP(base64.b32decode(secret.rstrip()))


def HOTP(K, C, digits=6):
    """
    HOTP accepts key K and counter C
    optional digits parameter can control the response length

    returns the OATH integer code with {digits} length
    """
    C_bytes = struct.pack(b"!Q", C)
    hmac_sha1 = hmac.new(key=K, msg=C_bytes,
                         digestmod=hashlib.sha1).hexdigest()
    return Truncate(hmac_sha1)[-digits:]


def TOTP(K, digits=6, window=30):
    """
    TOTP is a time-based variant of HOTP.
    It accepts only key K, since the counter is derived from the current time
    optional digits parameter can control the response length
    optional window parameter controls the time window in seconds

    returns the OATH integer code with {digits} length
    """
    C = int(time.time() / window)
    return HOTP(K, C, digits=digits)


def Truncate(hmac_sha1):
    """
    Truncate represents the function that converts an HMAC-SHA-1
    value into an HOTP value as defined in Section 5.3.
    http://tools.ietf.org/html/rfc4226#section-5.3
    """
    offset = int(hmac_sha1[-1], 16)
    binary = int(hmac_sha1[(offset * 2):((offset * 2) + 8)], 16) & 0x7fffffff
    return str(binary)


def generate_16char_base32(pre):
    """
    Encode a string of 16 length in base32
    with a given string - maximum length of 10 chars -
    Note: 10 char = 16 char string in base32

    Warning: Decoding is not a part of this example
    since it adds a random integer to the end of string
    """
    prefix = str(pre)
    prefix_len = len(prefix)
    if prefix_len > 10:
        return None
    remaining = 10 - prefix_len
    random_int_str = ''
    if remaining != 0:
        random_from = 10 ** (remaining-1)
        random_to = (10 ** remaining) - 1
        random_int = random.randint(random_from, random_to)
        random_int_str = str(random_int)
    str_to_encode = prefix + random_int_str
    encoded_str = base64.b32encode(str_to_encode)
    return encoded_str


class HotpTest(unittest.TestCase):
    """
    a very simple test case for HOTP.
    Based on test vectors from http://www.ietf.org/rfc/rfc4226.txt
    """
    def setUp(self):
        self.key_string = b'12345678901234567890'

    def test_hotp_vectors(self):
        hotp_result_vector = ['755224', '287082', '359152',
                              '969429', '338314', '254676',
                              '287922', '162583', '399871',
                              '520489']
        for i, r in enumerate(hotp_result_vector):
            self.assertEqual(HOTP(self.key_string, i), r)

    def test_totp(self):
        """
        a simple test for TOTP.
        since TOTP depends on the time window, we cannot predict the value.
        However, if we execute it several times, we should expect the
        same response most of the time.
        We only expect the value to change
        once or not at all within a reasonable time window.
        """
        value = TOTP(self.key_string, digits=8, window=20)
        value_changes = 0  # counting the number of changes to TOTP value
        for i in range(0, 100000):
            new_totp = TOTP(self.key_string, digits=8, window=20)
            if new_totp != value:
                value_changes += 1
                value = new_totp
        self.assertTrue(value_changes <= 1)

if __name__ == '__main__':
    unittest.main()
