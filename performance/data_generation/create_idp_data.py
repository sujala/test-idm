import argparse
import csv
import requests
import os
import json
import simplejson

from create_user_data import get_token
from create_user_data import add_admin_user


def add_role(headers, baseurl, user_id, role_name):
    print "headers:{0}".format(headers)
    l_url = baseurl + "/v2.0/OS-KSADM/roles"
    list_resp = requests.get(headers=headers, url=l_url,
                             params={"roleName": role_name})
    print(list_resp)
    mapping_rules_role_id = list_resp.json()["roles"][0]["id"]
    url = (baseurl + "/v2.0/users/{userId}/roles/OS-KSADM/{roleId}"
                     "".format(userId=user_id, roleId=mapping_rules_role_id))
    r = requests.put(url=url, headers=headers)
    print("add role r: {0}, {1}".format(r, r.text))


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument("-f", "--file_name",
                        default="../identity-perf-agent/localhost/data/identity/dom_users_for_fed.dat",
                        help="File with domain ids, but with providers")

    parser.add_argument(
        "-s", "--server_url",
        default="https://staging.identity-internal.api.rackspacecloud.com",
        help="Server URL")

    parser.add_argument(
        "-n", "--identity_service_user_name",
        default="keystone_service_admin",
        help="service admin name")

    parser.add_argument(
        "-p", "--identity_service_user_password",
        default="Auth1234",
        help="service admin password")

    args = parser.parse_args()

    file_name = args.file_name
    server_url = args.server_url
    s_user_name = args.identity_service_user_name
    s_password = args.identity_service_user_password

    result = []
    cert_file = "sample_keys/fed-origin.crt"
    sa_token = get_token(s_user_name, s_password,
                         alt_url="{0}".format(server_url))
    # create an admin user
    au = add_admin_user(1, alt_url=server_url)
    print "added admin user: {0}".format(au)
    headers = {'Accept': 'application/json',
               'Content-Type': 'application/json',
               'X-AUTH-TOKEN': sa_token}
    add_role(headers, server_url, au["userid"],
             'identity:identity-provider-manager')
    au_token = get_token(au["username"], au["password"],
                         alt_url="{0}".format(server_url))
    with open(file_name, 'r') as read_data:
        with open(cert_file, 'r') as read_cert:
            a_reader = csv.DictReader(read_data)
            print cert_file
            print os.path.isfile(cert_file)
            cert = ''.join(map(lambda s: s.strip(),
                               read_cert.readlines()[1:-1]))
            print(cert)
            idp_url = (server_url + "/v2.0/RAX-AUTH/federation"
                                    "/identity-providers")
            for row in a_reader:
                headers = {'Accept': 'application/json',
                           'Content-Type': 'application/json',
                           'X-AUTH-TOKEN': au_token}
                create_idp_data = {"RAX-AUTH:identityProvider": {
                    "name": "perf-idp-" + row["domainid"],
                    "issuer": "https://perf-{0}.issuer.com".format(row["domainid"]),
                    "description": "Performance Test IDP, Domain is in issuer and auth url",
                    "federationType": "DOMAIN",
                    "authenticationUrl": "https://perf-{0}.login.url".format(row["domainid"]),
                    "approvedDomainIds": [
                        "{0}".format(row["domainid"])
                    ],
                    "publicCertificates": [{
                        "pemEncoded": "{0}".format(cert)}]}}
                print "raw dict: {0}".format(create_idp_data)
                print "simplejson: {0}".format(simplejson.dumps(create_idp_data))
                print "json: {0}".format(json.dumps(create_idp_data))
                print "simplejson: {0}, no ascii".format(simplejson.dumps(create_idp_data, ensure_ascii=False))
                print headers, create_idp_data
                r = requests.post(url=idp_url, json=create_idp_data,
                                  headers=headers, verify=False)
                print idp_url
                print r
                print r.text
