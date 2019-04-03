#!/usr/bin/env python
import json
import os


import perf_constants as const

abs_path = os.path.abspath(os.path.dirname(__file__))
file_path = abs_path + '/../src/test/resources/request-bodies/identity/v2/change_password_v2.json'
with open(file_path, 'r') as input_file:
    data = json.load(input_file)

data['RAX-AUTH:changePasswordCredentials']['password'] = const.TEST_PASSWORD
data['RAX-AUTH:changePasswordCredentials']['newPassword'] = const.COMPRIMISED_PASSWORD

with open(file_path, 'w') as output_file:
    data = json.dump(data, output_file, indent=4)

