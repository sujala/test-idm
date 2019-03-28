#!/usr/bin/env python
import glob
import csv
import json
import os
import random
import argparse
import socket
import struct


def get_users(directory):
    users = []
    files = glob.glob(os.path.join(directory, "*.csv"))
    for afile_name in files:
        # print afile_name
        with open(afile_name, "r") as afile:
            reader = csv.DictReader(afile)
            for row in reader:
                users.append(row)
    print ("{0} users loaded".format(len(users)))
    return users


def load_ip_addresses(address_count):
    result = list()
    for i in range(address_count):
        result.append(socket.inet_ntoa(struct.pack('>I',
                                       random.randint(1, 0xffffffff))))
    return result


def get_col_mapping(config_file):
    mappings = dict()
    if "column_mappings" in config_file:
        mappings = config_file["column_mappings"]
    mappings = {key: (key if key not in config_file["column_mappings"]
                      else config_file["column_mappings"][key]) for key
                in config_file["columns"]}
    return mappings


def generate_files(users_dir, user_config_file, output_dir,
                   include_all=False):
    output_file_marker = 0
    user_marker = 0

    ips = load_ip_addresses(400000)
    print ("# ips: {0}".format(len(ips)))
    print(user_config_file)
    with open(user_config_file, "r") as config_file_handle:
        config_file = json.load(config_file_handle)
        # Need a try/except
        print ("Starting with {0}".format(
            config_file[output_file_marker]["name"]))
        output_file = open(
            os.path.join(output_dir,
                         config_file[output_file_marker]["name"]), "w")
        mappings = get_col_mapping(config_file[output_file_marker])
        print ("mappings:{0}".format(mappings))
        print ("keys: {0}".format([key for key in config_file[output_file_marker]["columns"]]))
        fieldnames = [mappings[key] for key in config_file[output_file_marker]["columns"]]

        users = get_users(users_dir)
        user_iterator = iter(users)

        writer = csv.DictWriter(
            output_file,
            fieldnames=fieldnames)
        writer.writeheader()
        # convert to json
        try:
            while(True):
                if user_marker >= config_file[output_file_marker]["users"]:
                    output_file_marker += 1
                    output_file.close()
                    user_marker = 0
                    if output_file_marker >= len(config_file):
                        print ("No more output files")
                        exit(0)
                    print("switching output files: {0}"
                          "".format(config_file[output_file_marker]["name"]))

                    output_file = open(
                        os.path.join(output_dir,
                                     config_file[output_file_marker]["name"]),
                        "w")
                    mappings = get_col_mapping(config_file[output_file_marker])
                    fieldnames = [mappings[key] for key in config_file[output_file_marker]["columns"]]
                    print ("mappings:{0}".format(mappings))
                    writer = csv.DictWriter(
                        output_file,
                        fieldnames=fieldnames)
                    writer.writeheader()

                # write (mapped) columns to output file
                if include_all:
                    # This is if you do not need randomness but rather want
                    # all the users included in the data file
                    # user = user_iterator.next()
                    user = next(user_iterator)
                else:
                    user = random.choice(users)
                output = dict()
                output = {mappings[key]: user[key] for key in user
                          if key in mappings}
                if "ipaddress" in config_file[output_file_marker]["columns"]:
                    output["ipaddress"] = random.choice(ips)
                writer.writerow(output)
                user_marker += 1
        finally:
            if output_file:
                output_file.close()


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument("-u", "--users_dir", default="users",
                        help="Directory that contains users "
                             "(assume [users dir]/*.csv")
    parser.add_argument("-c", "--config_file", default="file_config.json",
                        help="File containing user config")
    parser.add_argument("-o", "--output_dir", default=".",
                        help="Directory that will contain the result files.")
    parser.add_argument("-i", "--include_all", default="false",
                        help="Flag to indicate if to use randomness or not.")

    args = parser.parse_args()
    users_dir = args.users_dir
    config_file = args.config_file
    output_dir = args.output_dir
    include_all = args.include_all
    if include_all in ["false", "False"]:
        include_all = False
    else:
        include_all = True
    print("generate_files({0},{1},{2})".format(users_dir,
                                               config_file, output_dir))
    generate_files(users_dir, config_file, output_dir, include_all)
