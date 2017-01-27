#!/usr/bin/env python

# flake8: noqa
# following code is modified from: https://gist.github.com/ril3y/1165038
# updated to include DER encoded PKCS8 private key

import os
# from Crypto.Hash import SHA
from Crypto.PublicKey import RSA
# from Crypto.Signature import PKCS1_v1_5 as pk
from OpenSSL import crypto
from datetime import datetime
from tests.api.base import TestBase


def get_current_datetime_as_str(str_to_append=None):
    temp = str(datetime.utcnow()).replace(' ', 'T')
    if str_to_append:
        temp = '{0}{1}'.format(temp, str_to_append)
    return temp


def convert_datetime_to_asn1_generalized_str(date_time):
    return date_time.strftime('%Y%m%d%H%M%SZ')


def create_self_signed_cert(cert_path=None, key_path=None, create_files=True,
                            make_der_pkcs8_private_key=True,
                            not_before_datetime=None,
                            not_after_datetime=None, not_before_seconds=None,
                            not_after_seconds=None, country='US',
                            state='Texas', locality='Austin',
                            organization='Rackspace',
                            organization_unit='IdentityQE', common_name=None):

    # set paths for cert and key
    temp_timestamp = get_current_datetime_as_str()
    dir_path = os.path.join(os.path.dirname(__file__), 'keys')
    if not cert_path or not key_path:
        if not os.path.exists(dir_path):
            os.makedirs(dir_path)
    partial_path = os.path.join(dir_path, temp_timestamp)
    if create_files:
        cert_path = cert_path or '{0}{1}'.format(partial_path, '.crt')
        key_path = key_path or '{0}{1}'.format(partial_path, '.pem')

    # create a key pair
    k = crypto.PKey()
    k.generate_key(crypto.TYPE_RSA, 1024)

    # create a X.509 self-signed cert
    cert = crypto.X509()
    cert.get_subject().C = country
    cert.get_subject().ST = state
    cert.get_subject().L = locality
    cert.get_subject().O = organization
    cert.get_subject().OU = organization_unit
    cert.get_subject().CN = common_name or TestBase.generate_random_string()
    cert.set_serial_number(1000)

    if not_before_datetime:
        if not isinstance(not_before_datetime, datetime):
            not_before_datetime = convert_datetime_to_asn1_generalized_str(
                not_before_datetime
            )
        cert.setNotBefore(not_before_datetime)
    if not_after_datetime:
        if not isinstance(not_after_datetime, datetime):
            not_after_datetime = convert_datetime_to_asn1_generalized_str(
                not_after_datetime
            )
        cert.setNotAfter(not_after_datetime)

    if not_before_seconds is not None:
        cert.gmtime_adj_notBefore(not_before_seconds)
    if not_after_seconds is not None:
        cert.gmtime_adj_notAfter(not_after_seconds)

    if not_before_datetime is None and not_before_seconds is None:
        cert.gmtime_adj_notBefore(0)

    if not_after_datetime is None and not_after_seconds is None:
        cert.gmtime_adj_notAfter(86400)

    cert.set_issuer(cert.get_subject())
    cert.set_pubkey(k)
    cert.sign(k, 'sha1')

    cert_contents = crypto.dump_certificate(crypto.FILETYPE_PEM, cert)
    key_contents = crypto.dump_privatekey(crypto.FILETYPE_PEM, k)

    if create_files:
        with open(cert_path, 'wt+') as cert_handler:
            cert_handler.write(cert_contents)
        with open(key_path, 'wt+') as key_handler:
            key_handler.write(key_contents)

    der_key_path = None
    if make_der_pkcs8_private_key:
        der_pkcs_key = RSA.importKey(key_contents).exportKey(format='DER',
                                                             pkcs=8)
        der_key_path = '{0}{1}'.format(partial_path, '.pkcs8')

        if create_files:
            with open(der_key_path, 'wt+') as key_handler:
                key_handler.write(der_pkcs_key)

    cert_contents_cleanup = cert_contents.\
        replace('-----BEGIN CERTIFICATE-----', '').\
        replace('-----END CERTIFICATE-----', '').\
        replace('\n', '')

    fingerprint = cert.digest('sha1').replace(':', '').lower()

    return (cert_contents_cleanup, cert_path, key_path,
            der_key_path, fingerprint)
