#!/usr/bin/env python

# flake8: noqa
# following code is modified from: https://gist.github.com/ril3y/1165038
# updated to include DER encoded PKCS8 private key

import os
# from Crypto.Hash import SHA
# from Crypto.PublicKey import RSA
# from Crypto.Signature import PKCS1_v1_5 as pk
from cryptography.hazmat.backends import default_backend
from cryptography.hazmat.primitives import serialization, hashes
from cryptography.hazmat.primitives.asymmetric import rsa
from cryptography import x509
from cryptography.x509.oid import NameOID
# from OpenSSL import crypto
import datetime
from tests.api.base import TestBase
from tests.package.johny import constants


def get_current_datetime_as_str(str_to_append=None):
    temp = str(datetime.datetime.utcnow()).replace(' ', 'T')
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
                            organization_unit='IdentityQE',
                            common_name=None):

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
    key = rsa.generate_private_key(
    public_exponent=65537,
    key_size=1024,
    backend=default_backend()
)

    subject = issuer = x509.Name([
    x509.NameAttribute(NameOID.COUNTRY_NAME, unicode(country, 'utf-8')),
    x509.NameAttribute(NameOID.STATE_OR_PROVINCE_NAME, unicode(state, 'utf-8')),
    x509.NameAttribute(NameOID.LOCALITY_NAME, unicode(locality, 'utf-8')),
    x509.NameAttribute(
        NameOID.ORGANIZATION_NAME, unicode(organization, 'utf-8')),
    x509.NameAttribute(
        NameOID.COMMON_NAME, unicode(organization_unit, 'utf-8')),
    ])
    cert = x509.CertificateBuilder().subject_name(
        subject
    ).issuer_name(
        issuer
    ).public_key(
        key.public_key()
    ).not_valid_before(
       datetime.datetime.utcnow()
    ).not_valid_after(
        # Our certificate will be valid for 10 days
        datetime.datetime.utcnow() + datetime.timedelta(days=10)
    ).serial_number(
        x509.random_serial_number()
    ).add_extension(
        x509.SubjectAlternativeName(
            [x509.DNSName(common_name or TestBase.generate_random_string(
             pattern=constants.UPPER_CASE_LETTERS))]),
        critical=False,
    ).sign(key, hashes.SHA1(), default_backend())

    cert_contents = cert.public_bytes(serialization.Encoding.PEM)
    key_contents = key.private_bytes(
        encoding=serialization.Encoding.DER,
        format=serialization.PrivateFormat.PKCS8,
        encryption_algorithm=serialization.NoEncryption())

    if create_files:
        with open(cert_path, 'wt+') as cert_handler:
            cert_handler.write(cert_contents)
        with open(key_path, 'wt+') as key_handler:
            key_handler.write(key_contents)

    der_key_path = None
    if make_der_pkcs8_private_key:
        der_pkcs_key = key_contents

        der_key_path = '{0}{1}'.format(partial_path, '.pkcs8')

        if create_files:
            with open(der_key_path, 'wt+') as key_handler:
                key_handler.write(der_pkcs_key)

    cert_contents_cleanup = cert_contents.\
        replace('-----BEGIN CERTIFICATE-----', '').\
        replace('-----END CERTIFICATE-----', '').\
        replace('\n', '')

    fingerprint = cert.fingerprint(hashes.SHA1())

    return (cert_contents_cleanup, cert_path, key_path,
            der_key_path, fingerprint)
