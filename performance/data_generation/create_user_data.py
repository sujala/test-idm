#!/usr/bin/env python
import argparse
from contextlib import contextmanager
import csv
from multiprocessing import Pool
import requests
import random
import string
import sys
import traceback
from functools import partial
import time


@contextmanager
def terminating(thing):
    try:
        yield thing
    finally:
        thing.terminate()


# get users
user_by_id = dict()
headers = None
baseurl = None
# Authenticate User Admin


def name_generator(size=18, start_char=string.ascii_lowercase,
                   chars=string.ascii_lowercase + string.digits):
    start = random.choice(start_char)
    SR = random.SystemRandom()
    end = ''.join(SR.choice(chars) for _ in range(size - 1))
    return start + end


def get_token(user_name, password="Password1", alt_url=None):
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


def get_api_key(baseurl, headers, user_id):
    # get api key
    a_url = "{0}/v2.0/users/{1}/OS-KSADM/credentials/RAX-KSKEY:apiKeyCredentials".format(baseurl, user_id)
    r = requests.get(url=a_url,
                     headers=headers, verify=False)
    print a_url
    print r
    print r.text
    apikey = r.json()["RAX-KSKEY:apiKeyCredentials"]["apiKey"]
    return apikey


def add_user(number):
    try:
        group_name = name_generator() + "perf_test_group"
        domain_id = random.randrange(11111111, 99999999)
        group_data = {"RAX-KSGRP:group": {
                      "name": group_name,
                      "description": "Group for performance testing: User"
                                     " domain: {0}".format(domain_id)
                      }
                      }
        g_url = "{0}/v2.0/RAX-GRPADM/groups".format(baseurl)
        r = requests.post(url=g_url,
                          json=group_data, headers=headers, verify=False)
        print g_url
        print r
        print r.text

        user_name = name_generator()
        region = "ORD"
        create_user_data = {"user": {
                            "enabled": True,
                            "RAX-KSGRP:groups": [{"name": group_name}],
                            "RAX-KSQA:secretQA": {
                                "answer": "There is no meaning",
                                "question": "What is the meaning of it all"},
                            "RAX-AUTH:domainId": domain_id,
                            "username": user_name,
                            "OS-KSADM:password": "Password1",
                            "email": "identity_perf_test_{0}@rackspace.com"
                            "".format(domain_id),
                            "RAX-AUTH:defaultRegion": region}}
        u_url = "{0}/v2.0/users".format(baseurl)
        r = requests.post(url=u_url,
                          json=create_user_data, headers=headers, verify=False)
        print u_url
        print r
        print r.text
        result_data = r.json()
        user_id = result_data["user"]["id"]

        user_data = dict()
        user_data["username"] = result_data["user"]["username"]
        user_data["userid"] = result_data["user"]["id"]
        user_data["domainid"] = domain_id
        user_data["groupid"] = result_data["user"]["RAX-KSGRP:groups"][0]["id"]
        user_data["password"] = "Password1"
        user_data["nastid"] = [role["tenantId"] for
                               role in result_data["user"]["roles"] if
                               role["name"] == "object-store:default"][0]
        user_data["mossoid"] = [role["tenantId"] for
                                role in result_data["user"]["roles"] if
                                role["name"] == "compute:default"][0]
        get_token(user_name=user_data["username"], alt_url=baseurl)
        # get api key
        apikey = get_api_key(baseurl, headers, user_id)
        user_data["apikey"] = apikey
        print user_data
        time.sleep(0.1)
        return user_data
    except Exception as excp:
        print(excp)
        e = sys.exc_info()
        print("user error: {0},{1},{2}".format(e[0], e[1], e[2]))
        traceback.print_tb(e[2], limit=1, file=sys.stdout)
        return ""


def add_admin_user(number, alt_url=None):
    print "alt:{0}".format(alt_url)
    if alt_url:
        baseurl = alt_url
    try:
        user_name = name_generator()
        region = "ORD"
        create_user_data = {"user": {
                            "enabled": True,
                            "username": user_name,
                            "OS-KSADM:password": "Password1",
                            "email": "identity_perf_test_{0}@rackspace.com"
                            "".format(user_name),
                            "RAX-AUTH:defaultRegion": region}}
        admin_token = get_token(user_name="keystone_service_admin",
                                password="Auth1234", alt_url=baseurl)
        print admin_token

        sa_headers = {'Accept': 'application/json', 'Content-Type': 'application/json',
                      'X-Auth-Token': admin_token}

        u_url = "{0}/v2.0/users".format(baseurl)
        r = requests.post(url=u_url,
                          json=create_user_data, headers=sa_headers, verify=False)
        result_data = r.json()
        user_data = dict()
        user_data["username"] = result_data["user"]["username"]
        user_data["userid"] = result_data["user"]["id"]
        user_data["password"] = "Password1"
        apikey = get_api_key(baseurl, sa_headers, user_data["userid"])
        user_data["apikey"] = apikey
        time.sleep(0.1)
        return user_data
    except Exception as excp:
        print(excp)
        e = sys.exc_info()
        print("user error: {0},{1},{2}".format(e[0], e[1], e[2]))
        traceback.print_tb(e[2], limit=1, file=sys.stdout)
        return ""


# write all the info to a file:
# username, userid, password, apikey, nastid, mossoid
if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument("-p", "--num_processes", default=1, type=int,
                        help="Number of process to run")
    parser.add_argument("-u", "--num_users", default=1, type=int,
                        help="Number of users to create")
    parser.add_argument("-a", "--num_admin_users", default=5, type=int,
                        help="Number of admin users to create")
    parser.add_argument("-o", "--output_csv_file",
                        default="users_data.csv",
                        help="Output CSV file")

    parser.add_argument("-f", "--admin_output_csv_file",
                        default="admin_users_data.csv",
                        help="Output CSV file")
    parser.add_argument(
        "-s", "--server_url",
        default="https://staging.identity-internal.api.rackspacecloud.com",
        help="Server URL")

    args = parser.parse_args()

    proc_count = args.num_processes
    output_file_name = args.output_csv_file

    admin_output_file_name = args.admin_output_csv_file
    num_users = args.num_users
    num_admin_users = args.num_users
    baseurl = args.server_url

    # authdata = {"auth":
    #             {"passwordCredentials":
    #              {"username": "IAperf11",
    #               "password": "Password1"}
    #              }
    #             }

    # localhost - needs to be an identity admin, not a service admin
    admin_token = get_token(user_name="keystone_identity_admin",
                            password="Auth1234", alt_url=baseurl)
    print admin_token

    headers = {'Accept': 'application/json', 'Content-Type': 'application/json',
               'X-Auth-Token': admin_token}

    print "Running {0} processes".format(proc_count)
    print headers
    with open(output_file_name, 'w') as user_data_file:
        with terminating(Pool(processes=proc_count)) as p:
            result_rows = p.map(add_user, range(num_users))
        # print result_rows
        fieldnames = ['username', 'userid', 'password', 'apikey', 'nastid',
                      'mossoid', 'groupid', 'domainid']
        writer = csv.DictWriter(user_data_file, fieldnames=fieldnames)
        writer.writeheader()
        for user_data in result_rows:
            if user_data:
                writer.writerow(user_data)

    with open(admin_output_file_name, 'w') as admin_user_data_file:
        add_admin_partial = partial(add_admin_user, alt_url=baseurl)
        with terminating(Pool(processes=proc_count)) as p:
            result_rows = p.map(add_admin_partial, range(num_admin_users))
        # print result_rows
        fieldnames = ['username', 'userid', 'password', 'apikey']
        a_writer = csv.DictWriter(admin_user_data_file, fieldnames=fieldnames)
        a_writer.writeheader()
        for user_data in result_rows:
            if user_data:
                a_writer.writerow(user_data)
