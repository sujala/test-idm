#!/usr/bin/python3
import argparse
import requests
import os
from urllib.parse import urljoin


parser = argparse.ArgumentParser()
parser.add_argument('id_admin_token', type=str,
                    help='x-auth-token of identity admin user')
parser.add_argument('--url', type=str, help='identity endpoint',
                    default='http://localhost:8080/idm/cloud/')
args = parser.parse_args()
# Set the identity endpoint in 'identity_url'
identity_url = args.url
id_admin_token = args.id_admin_token

headers = {
    'content-type': 'application/json',
    'accept': 'application/json',
    'x-auth-token': id_admin_token
}


def get_absolute_path(file_name):
    return os.path.join(parent_dir, file_name)


parent_dir = os.path.abspath(os.path.dirname(__file__))
input_file_name = 'test_accounts.txt'
abs_input_file_path = get_absolute_path(input_file_name)
output_dom_file_name = 'domains_not_terminated.txt'
abs_output_dom_file_path = get_absolute_path(output_dom_file_name)
output_tenant_file_name = 'tenants_not_terminated.txt'
abs_output_tenant_file_path = get_absolute_path(output_tenant_file_name)
output_dom_not_found_file_name = 'domains_not_found.txt'
abs_output_dom_not_found_file_path = get_absolute_path(
    output_dom_not_found_file_name)

try:
    with open(abs_input_file_path) as f:
        accounts = f.read().splitlines()
        domain_url = urljoin(identity_url,
                             'v2.0/RAX-AUTH/domains/{}')
        try:
            with open(abs_output_dom_file_path, 'w') as f_dom, open(
                abs_output_tenant_file_path, 'w') as f_tenant, open(
                  abs_output_dom_not_found_file_path, 'w') as f_dom_not_found:

                f_dom_not_found.write("Domains not found in Identity are:\n")
                f_dom.write(
                    "Domains not terminated properly in Identity are:\n")
                f_tenant.write(
                    "Tenants not terminated properly in Identity are:\n")
                for account in accounts:
                    get_dom_resp = requests.get(domain_url.format(account),
                                                headers=headers)
                    if get_dom_resp.status_code == 200:
                        enabled = get_dom_resp.json()['RAX-AUTH:domain'][
                            'enabled']
                        if enabled:
                            f_dom.write(account + "\n")

                        get_tenants_in_domain_url = urljoin(
                            identity_url, (
                                'v2.0/RAX-AUTH/domains/{}/tenants'))
                        get_tenants_for_domain_resp = requests.get(
                            url=get_tenants_in_domain_url.format(account),
                            headers=headers)
                        if get_tenants_for_domain_resp.status_code == 200:
                            tenants_in_domain = (
                                get_tenants_for_domain_resp.json()[
                                  'tenants'])
                            for tenant in tenants_in_domain:
                                if tenant['enabled']:
                                    f_tenant.write(tenant['id'] + "\n")
                    elif get_dom_resp.status_code == 404:
                        f_dom_not_found.write(account + "\n")
                    elif get_dom_resp.status_code == 401:
                        print(
                            "Auth token provided is not valid or has expired")
                        raise Exception
        except IOError:
            print("There was an error opening one of the output files")
except IOError:
    print("There was an error opening the input file")
