#!/usr/bin/env python
import argparse
import csv
import perf_constants as const
import requests


def get_token(user_name, password=const.TEST_PASSWORD, alt_url=None):
    if alt_url:
        baseurl = alt_url
    # auth to check
    print("Authing as user")
    authdata = {"auth":
                {"passwordCredentials":
                 {"username": user_name,
                  "password": password}
                 }
                }
    headers_auth = {'Accept': 'application/json',
                    'Content-Type': 'application/json'}
    t_url = "{0}/v2.0/tokens".format(baseurl)
    print("authdata: {0}".format(authdata))
    r = requests.post(url=t_url, json=authdata,
                      headers=headers_auth, verify=False)
    print(t_url)
    print(r)
    print(r.json())
    return r.json()["access"]["token"]["id"]


def move_domain_to_rcn(domain_id):

    rcn = 'RCN-TEST3'

    move_domain_to_rcn_url = baseurl + (
        "/v2.0/RAX-AUTH/domains/{domain_id}/rcn/{rcn}".format(
            domain_id=domain_id, rcn=rcn))
    resp = requests.put(
        url=move_domain_to_rcn_url, headers=headers, verify=False)
    assert resp.status_code == 204


if __name__ == '__main__':
    parser = argparse.ArgumentParser()

    parser.add_argument(
        "-s", "--server_url",
        default="http://localhost:8082/idm/cloud",
        help="Server URL")
    parser.add_argument(
        "-i", "--input_file",
        default="/users/test.csv"
    )

    args = parser.parse_args()
    baseurl = args.server_url
    input_file_name = args.input_file

    sa_token = get_token(user_name="keystone_service_admin",
                         password="Auth1234", alt_url=baseurl)

    sa_headers = {'Accept': 'application/json', 'Content-Type': 'application/json',
                  'X-Auth-Token': sa_token}

    list_roles_by_name_url = baseurl + (
        "/v2.0/OS-KSADM/roles?roleName=identity:domain-rcn-switch")
    list_roles_resp = requests.get(
        url=list_roles_by_name_url, headers=sa_headers)
    assert list_roles_resp.status_code == 200
    role_id = list_roles_resp.json()["roles"][0]["id"]
    id_admin_id = "173189"

    add_role_to_user_url = baseurl + (
        "/v2.0/users/{user_id}/roles/OS-KSADM/{role_id}".format(
          user_id=id_admin_id, role_id=role_id))
    add_role_resp = requests.put(url=add_role_to_user_url, headers=sa_headers)
    assert add_role_resp.status_code == 200

    id_admin_token = get_token('auth', 'auth123', alt_url=baseurl)
    headers = {'Accept': 'application/json', 'Content-Type': 'application/json',
               'X-Auth-Token': id_admin_token}

    with open(input_file_name, 'r') as user_data_file:
        reader = csv.DictReader(user_data_file)

        for line in reader:
            print line['domainid']
            move_domain_to_rcn(line['domainid'])
