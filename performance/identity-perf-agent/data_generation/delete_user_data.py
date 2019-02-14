#!/usr/bin/env python
import csv
import requests
import copy
from multiprocessing import Pool
from contextlib import contextmanager
import argparse
import perf_constants as const
import os


@contextmanager
def terminating(thing):
    try:
        yield thing
    finally:
        thing.terminate()

# get users
user_by_id = dict()
headers = None

# Authenticate User Admin
def get_token(user_name, password=const.TEST_PASSWORD, alt_url=None):
    if alt_url:
        baseurl = alt_url
    # auth to check
    print "Authing as user"
    authdata = {"auth":
                {"passwordCredentials":
                 {"username": user_name,
                  "password": password}
                 }
                }
    headers_auth = {'Accept': 'application/json',
                    'Content-Type': 'application/json'}
    t_url = "{0}/v2.0/tokens".format(baseurl)
    print "authdata: {0}".format(authdata)
    r = requests.post(url=t_url, json=authdata,
                      headers=headers_auth, verify=False)
    print t_url
    print r
    print r.json()
    return r.json()["access"]["token"]["id"]


def delete_user(row):
    print row
    # delete user
    status = "Success"
    message = None
    try:
        del_user = requests.delete(url="{0}/v2.0/users/{1}".format(baseurl,
                                                                   row["userid"]),
                                   headers=headers)
        if del_user.status_code != 404 and del_user.status_code != 204:
            status = "Failure"
            message = del_user.text
        if "status" in row:
            if row["status"] == "Success":
                print "user already deleted. Skipping."
                return copy.copy(row)

        disable_data = {"RAX-AUTH:domain": {
                        "name": row["mossoid"],
                        "enabled": False}}
        # need to disable domain before deleting
        requests.put(url="{0}/v2.0/RAX-AUTH/domains/{1}".format(baseurl,
                                                                row["mossoid"]),
                     headers=headers, json=disable_data)
        del_domain = requests.delete(url="{0}/v2.0/RAX-AUTH/domains/{1}"
                                         "".format(baseurl, row["mossoid"]),
                                     headers=headers)
        if del_domain.status_code != 404 and del_domain.status_code != 204:
            status = "Failure"
            message = del_domain.text
        del_nast_tenant = requests.delete(url="{0}/v2.0/tenants/{1}"
                                              "".format(baseurl, row["mossoid"]),
                                          headers=headers)
        if (del_nast_tenant.status_code != 404 and
                del_nast_tenant.status_code != 204):
            status = "Failure"
            if not message:
                message = del_nast_tenant.text
        del_mosso_tenant = requests.delete(url="{0}/v2.0/tenants/{1}"
                                               "".format(baseurl, row["nastid"]),
                                           headers=headers)
        if (del_mosso_tenant.status_code != 404 and
                del_mosso_tenant.status_code != 204):
            status = "Failure"
            if not message:
                message = del_mosso_tenant.text
        del_group = requests.delete(url="{0}/v2.0/RAX-GRPADM/groups/{1}"
                                        "".format(baseurl, row["groupid"]),
                                    headers=headers)
        if del_group.status_code != 404 and del_group.status_code != 204:
            status = "Failure"
            if not message:
                message = del_group.text
    except:
        status = "Failure"
        message = "Uknown Exception"
    new_row = copy.copy(row)
    new_row["status"] = status
    new_row["message"] = message
    return new_row

if __name__ == '__main__':
    # do arg stuff
    parser = argparse.ArgumentParser()
    parser.add_argument("-p", "--num_processes", default=1, type=int,
                        help="Number of process to run")
    parser.add_argument("-i", "--input_csv_file", default="users_data.csv",
                        help="Input CSV file")
    parser.add_argument("-o", "--output_csv_file",
                        default="users_data_out.csv",
                        help="Output CSV file")
    parser.add_argument(
        "-s", "--server_url",
        default="https://staging.identity-internal.api.rackspacecloud.com",
        help="Server URL")
    args = parser.parse_args()

    proc_count = args.num_processes
    input_file_name = args.input_csv_file
    output_file_name = args.output_csv_file
    baseurl = args.server_url

    admin_token = get_token(user_name="keystone_identity_admin",
                            password="Auth1234", alt_url=baseurl)
    print admin_token
    headers = {'Accept': 'application/json', 'Content-Type': 'application/json',
            'X-Auth-Token': admin_token}

    print "Running {0} processes".format(proc_count)
    # write all the info to a file:
    # username, userid, password, apikey, nastid, mossoid
    with open(input_file_name, 'r') as user_data_file:
        with open(output_file_name, 'w') as user_data_out_file:
            reader = csv.DictReader(user_data_file)
            if "status" in reader.fieldnames:
                fieldnames = reader.fieldnames
            else:
                fieldnames = copy.copy(reader.fieldnames)
                fieldnames.append("status")
                fieldnames.append("message")
            writer = csv.DictWriter(user_data_out_file, fieldnames)
            writer.writeheader()
            rows = []
            for row in reader:
                rows.append(row)
            with terminating(Pool(processes=proc_count)) as p:
                result_rows = p.map(delete_user, rows)
            for new_row in result_rows:
                writer.writerow(new_row)
    os.remove(input_file_name)
