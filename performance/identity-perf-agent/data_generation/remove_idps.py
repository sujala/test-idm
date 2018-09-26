#!/usr/bin/env python
import argparse
import requests
import time

from create_user_data import get_token


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument("-f", "--file_name",
                        default="../localhost/data/identity/issuers_of_fed_test.dat",
                        help="File with domain ids, but with providers")

    parser.add_argument(
        "-s", "--server_url",
        default="https://staging.identity-internal.api.rackspacecloud.com",
        help="Server URL")

    parser.add_argument(
        "-n", "--identity_admin_user_name",
        default="auth",
        help="identity admin name")

    parser.add_argument(
        "-p", "--identity_admin_user_password",
        default="auth123",
        help="identity admin password")

    args = parser.parse_args()
    file_name = args.file_name
    server_url = args.server_url
    ia_user_name = args.identity_admin_user_name
    ia_password = args.identity_admin_user_password

    ia_token = get_token(ia_user_name, ia_password,
                         alt_url="{0}".format(server_url))
    headers = {
        'x-auth-token': ia_token,
        'accept': 'application/json'
    }

    f = open(file_name, 'r')
    issuers = []
    for line in f:
        issuers.append(line)
    f.close()

    for issuer in issuers:
        params = {
            'issuer': issuer
        }
        # get idps
        resp = requests.get('{}/v2.0/RAX-AUTH/federation/identity-providers'.format(
            server_url), headers=headers, params=params)
        if resp.status_code == 200:
            # get only the idp that's for perf
            for idp in resp.json()["RAX-AUTH:identityProviders"]:
                print("INFO::::start deleting {}".format(idp))
                if idp['description'] == 'Performance Test IDP, Domain is in issuer and auth url':
                    resp2 = requests.delete('{0}/v2.0/RAX-AUTH/federation/identity-providers/{1}'.format(
                        server_url, idp['id']), headers=headers)
                    if resp2.status_code == 204:
                        print("DELETED {}".format(idp))
                        time.sleep(5)
                        continue
                    else:
                        print("ERROR:: {} is not deleted".format(idp))
                else:
                    print("did not delete because not a performance idp: {}".format(idp))
        else:
            print "ERROR:: did not return idps"
            print resp.status_code
