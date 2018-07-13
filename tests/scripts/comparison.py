import json
import logging
import os
import requests
import urllib3


urllib3.disable_warnings(
    urllib3.exceptions.InsecureRequestWarning)

LOG_PATH_LOCATION = os.getenv(
    "LOG_PATH_LOCATION", os.getcwd())

logging.basicConfig(
    format='%(levelname)s:%(message)s',
    filename='{}/results.log'.format(LOG_PATH_LOCATION),
    level=logging.DEBUG)


PROPERTY_FILE_LOCATION = os.getenv("PROPERTY_FILE_LOCATION", os.path.join(
    os.curdir, "properties.json"))

ENVIRONMENT_FILE_LOCATION = os.getenv(
    "ENVIRONMENT_FILE_LOCATION", os.path.join(os.curdir, "environments.json"))


def is_prop_ignored(prop):
    return 'action' in prop and prop['action'] == 'ignore'


def does_region_match(prop, region):
    if 'region' in prop:
        return str.upper(region) == str.upper(prop['region'])
    else:
        return True


def does_environment_match(prop, environment):
    if 'environment' in prop:
        return str.upper(environment) == str.upper(prop['environment'])
    else:
        return True


def does_visibility_match(prop, environment):
    if 'visibility' in prop:
        return str.upper(environment) == str.upper(prop['visibility'])
    else:
        return True


def get_user_token(user, password, endpoint, environment_key):
    response = requests.post(
        "{}/{}".format(endpoint, "v2.0/tokens"),
        json={
            "auth": {
                "passwordCredentials": {
                    "username": user,
                    "password": password
                }
            }
        },
        verify=False)
    response.raise_for_status()

    return response.json()['access']['token']['id']


def get_props_from_server(token, environment_key, environments_json):
    response = requests.get(
        "{}/{}".format(
            environments_json[environment_key]['endpoint'],
            "devops/props"),
        headers={'x-auth-token': token},
        verify=False)
    response.raise_for_status()

    return response.json()['properties']


def get_server_prop(server_properties, prop):
    return next(
        (s_prop for s_prop in server_properties if s_prop[
            'name'] == prop['name'] and s_prop['versionAdded'] == prop[
            'versionAdded']), None)


def check_key_is_equivalent(key, prop, server_prop, server_name, error_list):
    if key in prop:
        local_property = prop[key]
        server_property = server_prop[key]
        if isinstance(local_property, list) and isinstance(
                server_property, list):
            local_property = set(local_property)
            server_property = set(server_property)

        if local_property != server_property:
            logging.error({
                "{}:{}".format(prop['name'], key): {
                    "local": prop[key],
                    server_name: server_prop[key]
                }
            })
            error_list.append({
                "{}:{}".format(prop['name'], key): {
                    "local": prop[key],
                    server_name: server_prop[key]
                }
            })


def compare_environment_configurations(environments_json, environment,
                                       visibility, region):
    error_list = []
    environment_key = environment + "_" + visibility + "_" + region
    logging.debug("Servers in list %s",
                  environments_json[environment_key]['servers'])

    user = os.getenv("{}_DEVOPS_USER".format(environment))
    password = os.getenv("{}_DEVOPS_PASSWORD".format(environment))

    logging.debug("Retrieve token for read-props user")
    token = get_user_token(
        user, password, environments_json[environment_key]['endpoint'],
        environment_key)

    logging.debug("Load the properties data for user")
    server_properties = get_props_from_server(token, environment_key,
                                              environments_json)

    logging.debug("Get server name from property 'ae.node.name.for.signoff'")
    server_name = next(
        (prop['value'] for prop in server_properties if prop[
            'name'] == 'ae.node.name.for.signoff'), None)

    logging.warn("Compare for %s %s", environment_key, server_name)
    if server_name is not None:
        logging.debug(
            "Check that server %s should be checked", server_name)
        if server_name in environments_json[environment_key]['servers']:
            logging.debug(
                "Run the comparison for property version to current state ("
                "aggregate failures)")
            with open(PROPERTY_FILE_LOCATION, 'r') as f:
                prop_file_as_text = f.read()
                prop_file_as_json = json.loads(prop_file_as_text)
                for prop in prop_file_as_json['properties']:
                    if not is_prop_ignored(prop) and does_region_match(
                        prop, region) and does_environment_match(
                            prop, environment) and does_visibility_match(
                                prop, environment):
                        logging.debug(
                            ('retrieve prop by name and compare defaultValue,'
                             ' reloadable, source, value, and versionAdded'))
                        server_prop = get_server_prop(server_properties, prop)
                        logging.debug('compare %s with %s', prop, server_prop)
                        if server_prop is None:
                            logging.debug(
                                ('%s not found. make sure that the version is'
                                 ' not yet released to environment'),
                                environments_json[environment_key][
                                    'versionReleased'])
                            if prop['versionAdded'] != environments_json[
                                    environment_key]['versionReleased']:
                                error_list.append(
                                    {
                                        "{}:{}:not-found".format(
                                            prop['name'], server_name): (
                                            "local prop not "
                                            "found in {}").format(server_name)
                                    })
                        else:
                            check_key_is_equivalent(
                                'defaultValue', prop, server_prop, server_name,
                                error_list)
                            check_key_is_equivalent(
                                'reloadable', prop, server_prop, server_name,
                                error_list)
                            check_key_is_equivalent(
                                'source', prop, server_prop, server_name,
                                error_list)
                            check_key_is_equivalent(
                                'value', prop, server_prop, server_name,
                                error_list)
                            check_key_is_equivalent(
                                'versionAdded', prop, server_prop, server_name,
                                error_list)
                            logging.debug("Remove %s", server_prop['name'])
                            server_properties.remove(server_prop)
            logging.debug(
                ("Remove the server from the server list and run again if the"
                 " server list is not empty"))
            environments_json[environment_key]['servers'].remove(server_name)

        if len(environments_json[environment_key]['servers']) > 0:
            error_list = error_list + compare_environment_configurations(
                environments_json, environment, visibility, region)
            logging.warn("current error list: {}".format(error_list))
    else:
        logging.debug("Server name is empty")
        error_list = error_list + compare_environment_configurations(
            environments_json, environment, visibility, region)
        logging.warn("current error list: {}".format(error_list))

    return error_list


if __name__ == '__main__':
    logging.debug(
        ("start comparison of property variables (prop file location: %s) "
         "(env file location: %s"),
        PROPERTY_FILE_LOCATION,
        ENVIRONMENT_FILE_LOCATION)

    error_list = []
    environments_json = None
    with open(ENVIRONMENT_FILE_LOCATION, 'r') as f:
        environments_json = json.loads(f.read())

    environment_tuple_list = [
        tuple(e.split('_')) for e in environments_json.keys()]
    for environment in environment_tuple_list:
        error_list = error_list + compare_environment_configurations(
            environments_json, *environment)
    results = {
        "status": "success",
        "error_list": []
    }
    if len(error_list) > 0:
        logging.debug("Comparison for %s failed!", environment)
        results['status'] = 'failure'
        for error in error_list:
            logging.debug(error)
            results['error_list'].append(error)
    logging.debug("finish comparison of property variables")
    print(json.dumps(results))
