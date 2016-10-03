#!/usr/bin/env python
import subprocess

"""
This has methods to access and search repose docker container log
such as:
 search and return log lines that match search pattern
"""


def clean_log(container_name='localhost_reposeB_1',
              path_to_logfile=None):
    docker_cmd = ['docker', 'exec', container_name]
    docker_cmd.extend(['truncate', path_to_logfile, '--size', '0'])
    search_result = subprocess.check_output(docker_cmd)
    return search_result


def search_string(container_name='localhost_reposeB_1',
                  search_pattern=None,
                  path_to_logfile=None):
    """
    Search return all matching lines from the log
    :param container_name:
    :param search_pattern:
    :param path_to_logfile:
    :return:
    """
    docker_cmd = ['docker', 'exec', container_name]
    search_format = '/' + search_pattern + '/'
    docker_cmd.extend(['awk', search_format, path_to_logfile])
    search_result = subprocess.check_output(docker_cmd)
    return search_result
