#!/usr/bin/env python
import argparse
from contextlib import contextmanager
import csv
import glob
import os
from multiprocessing import Pool
import requests
import random
import string
import sys
import traceback
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


def name_generator(size=18, start_char=string.ascii_lowercase,
                   chars=string.ascii_lowercase + string.digits):
    start = random.choice(start_char)
    SR = random.SystemRandom()
    end = ''.join(SR.choice(chars) for _ in range(size - 1))
    return start + end


def get_token(user_name, password=const.TEST_PASSWORD, alt_url=None, debug=False):
    if alt_url:
        baseurl = alt_url
    # auth to check
    if debug:
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
    r = requests.post(url=t_url, json=authdata,
                      headers=headers_auth, verify=False)
    if r.status_code < 200 or r.status_code >= 300 or debug:
        print("authdata: {0}".format(authdata))
        print(r.json())
    if r.status_code < 200 or r.status_code >= 300:
        raise Exception("Non-success: {}".format(r))
    return r.json()["access"]["token"]["id"]


def add_default_user(number, debug=False):
    try:
        user_name = name_generator()
        create_user_data = {"user": {
                            "enabled": True,
                            "username": user_name,
                            "OS-KSADM:password": const.TEST_PASSWORD}}
        u_url = "{0}/v2.0/users".format(baseurl)
        r = requests.post(url=u_url,
                          json=create_user_data, headers=ua_headers, verify=False)
        if r.status_code != 201 or debug:
            print(r.json())
        assert r.status_code == 201
        result_data = r.json()
        user_data = dict()
        user_data["username"] = result_data["user"]["username"]
        user_data["userid"] = result_data["user"]["id"]
        user_data["password"] = const.TEST_PASSWORD
        user_data["domainId"] = domain_id
        time.sleep(0.1)
        return user_data
    finally:
        pass


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument("-p", "--num_processes", default=1, type=int,
                        help="Number of process to run")
    parser.add_argument("-i", "--input_dir",
                        help="directory where user-admin csv files are stored")
    parser.add_argument("-m", "--num_of_domains", default="1",
                        help="number of domains to test with")
    parser.add_argument("-n", "--num_def_users",
                        help="number of default users")
    parser.add_argument("-o", "--users_in_domain_output_csv_file",
                        default="users_in_dom/users_in_domain_data.csv",
                        help="Output CSV file")
    parser.add_argument(
        "-s", "--server_url", default="http://localhost:8082/idm/cloud",
        help="Server URL")
    parser.add_argument('--debug', action='store_true',
                        help='Print out diagnostic information.')

    args = parser.parse_args()
    proc_count = args.num_processes
    user_admins_dir = args.input_dir
    num_of_dom = args.num_of_domains
    num_of_default_users = args.num_def_users
    baseurl = args.server_url
    users_in_dom_output_file_name = args.users_in_domain_output_csv_file

    with open(users_in_dom_output_file_name, 'w') as default_user_data_file:
        fieldnames = ['username', 'userid', 'password', 'domainId']
        writer = csv.DictWriter(default_user_data_file, fieldnames=fieldnames)
        writer.writeheader()

        files = glob.glob(os.path.join(user_admins_dir, "*.csv"))
        for afile_name in files:
            with open(afile_name, "r") as afile:
                reader = csv.DictReader(afile)
                count = 0
                for row in reader:
                    if count < int(num_of_dom):
                        user_admin_token = get_token(
                            user_name=row['username'],
                            password=row['password'], alt_url=baseurl, debug=args.debug)

                        ua_headers = {'Accept': 'application/json',
                                      'Content-Type': 'application/json',
                                      'X-Auth-Token': user_admin_token}
                        domain_id = row['domainid']

                        def add_default_user_with_debug(number):
                            add_default_user(number, debug=args.debug)

                        with terminating(Pool(processes=proc_count)) as p:
                            result_rows = p.map(add_default_user_with_debug, range(
                                int(num_of_default_users)))

                        for user_data in result_rows:
                            if user_data:
                                writer.writerow(user_data)

                        count += 1
                    else:
                        break
