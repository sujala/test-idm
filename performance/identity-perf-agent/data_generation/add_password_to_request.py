#!/usr/bin/env python

"""
This scripts modifies two json files in
../src/test/resources/request-bodies/identity/v2, create_user_body_v2.json
and tokens_body_password_v2.json. The script adds the password to use when
sending the contents of those files as JSON payloads.
"""

import json
import os

import perf_constants as const


abs_path = os.path.abspath(os.path.dirname(__file__))
file_path = abs_path + '/../src/test/resources/request-bodies/identity/v2/create_user_body_v2.json'
with open(file_path, 'r') as input_file:
    data = json.load(input_file)
data['user']['OS-KSADM:password'] = const.TEST_PASSWORD
with open(file_path, 'w') as output_file:
    data = json.dump(data, output_file, indent=4)

file_path = abs_path + '/../src/test/resources/request-bodies/identity/v2/tokens_body_password_v2.json'
with open(file_path, 'r') as input_file:
    data = json.load(input_file)
data['auth']['passwordCredentials']['password'] = const.TEST_PASSWORD
with open(file_path, 'w') as output_file:
    data = json.dump(data, output_file, indent=4)
