#!/usr/bin/env python
# -*- coding: utf-8 -*-

#import ssl
import os
import socket
import subprocess
import shutil

LB_SERVERS = (
'cert-edir.dfw1.corp.rackspace.com',
'edir.sat.corp.rackspace.com',
'edir.dfw.corp.rackspace.com',
'edir.iad.corp.rackspace.com', 
'edir.ord.corp.rackspace.com',
'edir.lon.corp.rackspace.com',
'edir.hkg.corp.rackspace.com',
)

SERVERS = (
'172.17.16.83',
'172.17.16.85',
'cert-edir1.dfw1.corp.rackspace.com',
'cert-edir2.dfw1.corp.rackspace.com',
'edir2.sat1.corp.rackspace.com',
'edir3.sat1.corp.rackspace.com',
'edir2.dfw1.corp.rackspace.com',
'edir3.dfw1.corp.rackspace.com',
'edir1.hkg1.corp.rackspace.com',
'edir2.hkg1.corp.rackspace.com',
'edir2.iad1.corp.rackspace.com',
'edir3.iad1.corp.rackspace.com',
'edir2.lon3.corp.rackspace.com',
'edir3.lon3.corp.rackspace.com',
'edir1.ord1.corp.rackspace.com',
'edir2.ord1.corp.rackspace.com',
)

PORT = 636

SCRIPT_DIR = os.path.dirname(__file__)
CERT_BEGIN_STR = '-----BEGIN CERTIFICATE-----'
CERT_END_STR = '-----END CERTIFICATE-----'
CACERT_PATH = os.path.join(os.pardir, 'src', 'main', 'resources', 'cacerts')
CACERT_TEST_PATH = os.path.join(os.pardir, 'src', 'test', 'resources', 'cacerts')
# STORE_PWD='TIthM,H,D,aJ'

def pem_fname(svr):
    svr_namesegs = svr.split('.')
    return ''.join((svr_namesegs[1], '-', svr_namesegs[0], '.pem'))

for svr in SERVERS:
    try:
        openssl_cmd = ''.join(('openssl s_client -connect ', svr, ':ldaps'))
        print(openssl_cmd)
        openssl = subprocess.Popen(
            openssl_cmd,
            shell=True,
            stdin=subprocess.PIPE,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE
        )
        stdout, stderr = openssl.communicate('GET /')
        begin_idx = stdout.find(CERT_BEGIN_STR)
        end_idx = stdout.find(CERT_END_STR)
        if begin_idx > -1 and end_idx > -1:
            pub_key = stdout[begin_idx:(end_idx + len(CERT_END_STR))]
            pem_filename = ''.join((svr, '.pem'))
            pem_fpath = os.path.join(SCRIPT_DIR, pem_filename)
            pemfile = open(pem_fpath, 'w')
            pemfile.write(pub_key)
            pemfile.close()
            subprocess.call(' '.join((
                                'keytool -import -alias',
                                svr,
                                '-keystore',
                                CACERT_PATH,
                                '-file',
                                pem_fpath)),
                            shell=True)
        else:
            print(''.join(('Error getting public key for ', svr, ':', str(PORT))))
    except Exception as ex:
        print(''.join(('Could not connect to ', svr)))
        print(ex)
shutil.copy2(CACERT_PATH, CACERT_TEST_PATH)