#!/usr/bin/env python

"""
This script creates users within a Customer Identity instance.
See `create_user_data.py -h` for usage information.
"""


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
import perf_constants as const


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


def get_token(user_name, password=const.TEST_PASSWORD, alt_url=None,
              debug=False):
    if alt_url:
        baseurl = alt_url
    # auth to check
    if debug:
        print("Authing as user")
    authdata = {
        "auth": {
            "passwordCredentials": {
                "username": user_name,
                "password": password}}}
    headers_auth = {
        'Accept': 'application/json',
        'Content-Type': 'application/json'}
    t_url = "{0}/v2.0/tokens".format(baseurl)
    if debug:
        print("authdata: {0}".format(authdata))
    r = requests.post(url=t_url, json=authdata,
                      headers=headers_auth, verify=False)
    if r.status_code < 200 or r.status_code >= 300 or debug:
        print("authdata: {0}".format(authdata))
        print(t_url)
        print(r)
        print(r.json())
    if r.status_code < 200 or r.status_code >= 300:
        raise Exception("Non-success: {}".format(r))
    return r.json()["access"]["token"]["id"]


def get_api_key(baseurl, headers, user_id, debug=False):
    # get api key
    a_url = "{0}/v2.0/users/{1}/OS-KSADM/credentials/" \
            "RAX-KSKEY:apiKeyCredentials".format(baseurl, user_id)
    r = requests.get(url=a_url,
                     headers=headers, verify=False)
    if r.status_code < 200 or r.status_code >= 300 or debug:
        print(a_url)
        print(r)
        print(r.text)
    if r.status_code < 200 or r.status_code >= 300:
        raise Exception("Non-success: {}".format(r))
    apikey = r.json()["RAX-KSKEY:apiKeyCredentials"]["apiKey"]
    return apikey


def add_default_user_unpack(args):
    return add_default_user(*args)


def add_default_user(username, password, debug=False):
    try:
        user_name = name_generator()
        create_user_data = {
            "user": {
                "enabled": True,
                "username": user_name,
                "OS-KSADM:password": const.TEST_PASSWORD}}
        u_url = "{0}/v2.0/users".format(baseurl)
        user_admin_token = get_token(
            user_name=username, password=password, alt_url=baseurl,
            debug=debug)

        ua_headers = {
            'Accept': 'application/json',
            'Content-Type': 'application/json',
            'X-Auth-Token': user_admin_token
        }

        r = requests.post(url=u_url,
                          json=create_user_data,
                          headers=ua_headers,
                          verify=False)
        result_data = r.json()
        user_data = dict()
        user_data["username"] = result_data["user"]["username"]
        user_data["userid"] = result_data["user"]["id"]
        user_data["password"] = const.TEST_PASSWORD
        apikey = get_api_key(baseurl, headers, user_data["userid"],
                             debug=debug)
        user_data["apikey"] = apikey
        time.sleep(0.1)
        return user_data
    finally:
        pass


def add_user(number, debug=False):
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
        if r.status_code < 200 or r.status_code >= 300 or debug:
            print(g_url)
            print(r)
            print(r.text)
        if r.status_code < 200 or r.status_code >= 300:
            raise Exception("Non-success: {}".format(r))


        user_name = name_generator()
        create_user_data = {"user": {
            "enabled": True,
            "RAX-KSGRP:groups": [{"name": group_name}],
            "RAX-KSQA:secretQA": {
                "answer": "There is no meaning",
                "question": "What is the meaning of it all"},
            "RAX-AUTH:domainId": domain_id,
            "username": user_name,
            "OS-KSADM:password": const.TEST_PASSWORD,
            "email": "identity_perf_test_{0}@rackspace.com"
                     "".format(domain_id)}}
        u_url = "{0}/v2.0/users".format(baseurl)
        r = requests.post(url=u_url,
                          json=create_user_data, headers=headers, verify=False)
        if r.status_code < 200 or r.status_code >= 300 or debug:
            print(u_url)
            print(r)
            print(r.text)
        if r.status_code < 200 or r.status_code >= 300:
            raise Exception("Non-success: {}".format(r))

        result_data = r.json()
        user_id = result_data["user"]["id"]

        user_data = dict()
        user_data["username"] = result_data["user"]["username"]
        user_data["userid"] = result_data["user"]["id"]
        user_data["domainid"] = domain_id
        user_data["groupid"] = result_data["user"]["RAX-KSGRP:groups"][0]["id"]
        user_data["password"] = const.TEST_PASSWORD
        user_data["nastid"] = [role["tenantId"] for
                               role in result_data["user"]["roles"] if
                               role["name"] == "object-store:default"][0]
        user_data["mossoid"] = [role["tenantId"] for
                                role in result_data["user"]["roles"] if
                                role["name"] == "compute:default"][0]
        get_token(user_name=user_data["username"], alt_url=baseurl,
                  debug=debug)
        # get api key
        apikey = get_api_key(baseurl, headers, user_id, debug=debug)
        user_data["apikey"] = apikey
        if debug:
            print(user_data)
        time.sleep(0.1)
        return user_data
    finally:
        pass


def add_admin_user(number, alt_url=None, debug=False):
    if debug:
        print("alt:{0}".format(alt_url))
    if alt_url:
        baseurl = alt_url
    try:
        user_name = name_generator()
        create_user_data = {
            "user": {
                "enabled": True,
                "username": user_name,
                "OS-KSADM:password": const.TEST_PASSWORD,
                "email": "identity_perf_test_admin_{0}@rackspace.com"
                         "".format(user_name)}}
        admin_token = get_token(user_name="keystone_service_admin",
                                password="Auth1234", alt_url=baseurl,
                                debug=debug)
        if debug:
            print(admin_token)

        sa_headers = {
            'Accept': 'application/json',
            'Content-Type': 'application/json',
            'X-Auth-Token': admin_token}

        u_url = "{0}/v2.0/users".format(baseurl)
        r = requests.post(url=u_url,
                          json=create_user_data,
                          headers=sa_headers,
                          verify=False)
        result_data = r.json()
        user_data = dict()
        user_data["username"] = result_data["user"]["username"]
        user_data["userid"] = result_data["user"]["id"]
        user_data["password"] = const.TEST_PASSWORD
        apikey = get_api_key(baseurl, sa_headers, user_data["userid"],
                             debug=debug)
        user_data["apikey"] = apikey
        time.sleep(0.1)
        return user_data
    finally:
        pass


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
    parser.add_argument("-d", "--default_user_output_csv_file",
                        default="default_users_data.csv",
                        help="Output CSV file")

    parser.add_argument("-f", "--admin_output_csv_file",
                        default="admin_users_data.csv",
                        help="Output CSV file")
    parser.add_argument(
        "-s", "--server_url",
        default="https://staging.identity-internal.api.rackspacecloud.com",
        help="Server URL")
    parser.add_argument("-i", "--admin_username",
                        default="keystone_identity_admin",
                        help="username of an admin user")
    parser.add_argument('--debug', action='store_true',
                        help='Print out diagnostic information.')
    args = parser.parse_args()

    proc_count = args.num_processes
    output_file_name = args.output_csv_file

    admin_output_file_name = args.admin_output_csv_file
    default_user_output_file_name = args.default_user_output_csv_file
    num_users = args.num_users
    num_admin_users = args.num_users
    baseurl = args.server_url
    admin_username = args.admin_username

    # authdata = {"auth":
    #             {"passwordCredentials":
    #              {"username": "IAperf11",
    #               "password": const.TEST_PASSWORD}
    #              }
    #             }

    # localhost - needs to be an identity admin, not a service admin
    admin_token = get_token(user_name=admin_username,
                            password="Auth1234", alt_url=baseurl,
                            debug=args.debug)
    if args.debug:
        print(admin_token)

    headers = {
        'Accept': 'application/json',
        'Content-Type': 'application/json',
        'X-Auth-Token': admin_token}

    if args.debug:
        print("Running {0} processes".format(proc_count))
        print(headers)


    def add_user_debug(number):
        return add_user(number, debug=args.debug)


    with open(output_file_name, 'w') as user_data_file:
        with terminating(Pool(processes=proc_count)) as p:
            result_rows = p.map(add_user_debug, range(num_users))
        # print result_rows
        fieldnames = ['username', 'userid', 'password', 'apikey', 'nastid',
                      'mossoid', 'groupid', 'domainid']
        writer = csv.DictWriter(user_data_file, fieldnames=fieldnames)
        writer.writeheader()
        for user_data in result_rows:
            if user_data:
                writer.writerow(user_data)

    with open(admin_output_file_name, 'w') as admin_user_data_file:
        add_admin_partial = partial(add_admin_user, alt_url=baseurl,
                                    debug=args.debug)
        with terminating(Pool(processes=proc_count)) as p:
            result_rows = p.map(add_admin_partial, range(num_admin_users))
        # print result_rows
        fieldnames = ['username', 'userid', 'password', 'apikey']
        a_writer = csv.DictWriter(admin_user_data_file, fieldnames=fieldnames)
        a_writer.writeheader()
        for user_data in result_rows:
            if user_data:
                a_writer.writerow(user_data)

    def add_default_user_unpack_with_debug(_args):
        return add_default_user(*_args, debug=args.debug)

    with open(default_user_output_file_name, 'w') as default_user_data_file:

        with open(output_file_name, 'r') as user_data_file:
            reader = csv.DictReader(user_data_file)
            user_admin_creds = []
            for line in reader:
                user_admin_creds.append((line['username'], line['password']))
            with terminating(Pool(processes=proc_count)) as p:
                result_rows = p.map(add_default_user_unpack_with_debug,
                                    user_admin_creds)
            fieldnames = ['username', 'userid', 'password', 'apikey']
            writer = csv.DictWriter(default_user_data_file,
                                    fieldnames=fieldnames)
            writer.writeheader()
            for user_data in result_rows:
                if user_data:
                    writer.writerow(user_data)
