"""Identity API Base Client."""

import types

from lxml import objectify

from cafe.engine.http import client
from tests.api.v2.models import requests


XMLNS = "http://docs.openstack.org/identity/api/v2.0"
XMLNS_OS_KSADM = "http://docs.openstack.org/identity/api/ext/OS-KSADM/v1.0"
XMLNS_RAX_AUTH = "http://docs.rackspace.com/identity/api/ext/RAX-AUTH/v1.0"
XMLNS_RAX_KSGRP = "http://docs.rackspace.com/identity/api/ext/RAX-KSGRP/v1.0"
XMLNS_RAX_KSQA = "http://docs.rackspace.com/identity/api/ext/RAX-KSQA/v1.0"
XMLNS_RAX_KSKEY = "http://docs.rackspace.com/identity/api/ext/RAX-KSKEY/v1.0"


class IdentityAPIClient(client.AutoMarshallingHTTPClient):
    """Client methods for the identity api endpoints."""

    def __init__(self, url, serialize_format, deserialize_format):
        super(IdentityAPIClient, self).__init__()
        self.url = url
        self.default_headers['Content-Type'] = 'application/{0}'.format(
            serialize_format)
        self.default_headers['Accept-Encoding'] = 'application/{0}'.format(
            deserialize_format)
        self.default_headers['Accept'] = 'application/{0}'.format(
            deserialize_format)

        self.serialize_format = serialize_format
        self.deserialize_format = deserialize_format

    def get_auth_token(self, user, password=None, api_key=None,
                       requestslib_kwargs=None):
        if api_key:
            request_object = requests.AuthenticateWithApiKey(
                user_name=user, api_key=api_key)
        else:
            request_object = requests.AuthenticateWithPassword(
                user_name=user, password=password)

        url = self.url + '/tokens'
        resp = self.request('POST', url, request_entity=request_object,
                            requestslib_kwargs=requestslib_kwargs)

        # If the response is xml, adds a json() menthod to the object.
        # The json() method should return a json equivalent of the
        # xml response.
        if self.deserialize_format == 'xml':
            def json(self):
                # NOTE: Partially implemented for the purpose of extracting
                # auth token. Needs to be revisited when implementing tests for
                # the endpoint
                resp_json = {}
                resp_json['access'] = {}

                root_string = resp.text.encode('ascii')
                root = objectify.fromstring(root_string)
                token = root['token'].attrib

                services = root['serviceCatalog']
                serviceCatalog = []

                for service in services.getchildren():
                    item = {}
                    item['name'] = service.attrib['name']
                    item['type'] = service.attrib['type']
                    links = []
                    for endpoint in service.endpoint:
                        links.append(endpoint.attrib)
                    item['endpoints'] = links
                    serviceCatalog.append(item)

                resp_json['access']['serviceCatalog'] = serviceCatalog
                resp_json['access']['token'] = token
                resp_json['access']['user'] = user
                return resp_json
            resp.json = types.MethodType(json, resp, type(resp))

        return resp

    def add_user(self, user_name, email=None, domain_id=None, contact_id=None,
                 default_region=None, token_format=None, password=None,
                 enabled=True, display_name=None,
                 requestslib_kwargs=None):
        """Return response object from the add user api call

        POST /v2.0/users
        In case of XML response, add a json() method to the response object
        that will create a JSON equivalent of the XML response
        """
        url = self.url + '/users'

        # This is needed because json expects boolean values, while xml
        # expects strings. The tests always pass in boolean, and the xml client
        # has to translate it to string
        if self.serialize_format == 'xml':
            if enabled:
                enabled = 'true'
            else:
                enabled = 'false'
        request_object = requests.AddUser(
            user_name=user_name, domain_id=domain_id, contact_id=contact_id,
            default_region=default_region, token_format=token_format,
            password=password, email=email, enabled=enabled,
            display_name=display_name)
        resp = self.request('POST', url, request_entity=request_object,
                            requestslib_kwargs=requestslib_kwargs)

        # If the response is xml, adds a json() menthod to the object.
        # The json() method should return a json equivalent of the
        # xml response.
        if self.deserialize_format == 'xml':
            def json(self):
                resp_json = {}
                resp_json['user'] = {}

                root_string = resp.text.encode('ascii')
                root = objectify.fromstring(root_string)
                resp_json['user']['username'] = root.attrib['username']
                # Needed because json expects boolean
                if root.attrib['enabled'] == 'true':
                    resp_json['user']['enabled'] = True
                else:
                    resp_json['user']['enabled'] = False

                if email:
                    resp_json['user']['email'] = root.attrib['email']
                resp_json['user']['id'] = root.attrib['id']

                if not password:
                    password_key = '{' + XMLNS_OS_KSADM + '}' + 'password'
                    resp_json['user']['OS-KSADM:password'] = (
                        root.attrib[password_key])

                defaultRegion_key = (
                    '{' + XMLNS_RAX_AUTH + '}' + 'defaultRegion')
                resp_json['user']['RAX-AUTH:defaultRegion'] = root.attrib[
                    defaultRegion_key]

                mFE_key = '{' + XMLNS_RAX_AUTH + '}' + 'multiFactorEnabled'
                if root.attrib[mFE_key] == 'true':
                    resp_json['user']['multiFactorEnabled'] = True
                else:
                    resp_json['user']['multiFactorEnabled'] = False

                domainId_key = '{' + XMLNS_RAX_AUTH + '}' + 'domainId'
                resp_json['user']['RAX-AUTH:domainId'] = root.attrib[
                    domainId_key]
                return resp_json
            resp.json = types.MethodType(json, resp, type(resp))

        return resp

    def get_user(self, user_id, requestslib_kwargs=None):
        """Return response object from the get user api call

        GET /v2.0/users/{user_id}
        In case of XML response, add a json() method to the response object
        that will create a JSON equivalent of the XML response
        """
        url = self.url + '/users/' + user_id
        resp = self.request('GET', url,
                            requestslib_kwargs=requestslib_kwargs)

        # If the response is xml, adds a json() menthod to the object.
        # The json() method should return a json equivalent of the
        # xml response.
        if self.deserialize_format == 'xml':
            def json(self):
                resp_json = {}
                resp_json['user'] = {}

                root_string = resp.text.encode('ascii')
                root = objectify.fromstring(root_string)
                resp_json['user']['username'] = root.attrib['username']
                # Needed because json expects boolean
                if root.attrib['enabled'] == 'true':
                    resp_json['user']['enabled'] = True
                else:
                    resp_json['user']['enabled'] = False

                resp_json['user']['email'] = root.attrib['email']
                resp_json['user']['id'] = root.attrib['id']

                domainId_key = '{' + XMLNS_RAX_AUTH + '}' + 'domainId'
                resp_json['user']['RAX-AUTH:domainId'] = (
                    root.attrib[domainId_key])

                defaultRegion_key = (
                    '{' + XMLNS_RAX_AUTH + '}' + 'defaultRegion')
                resp_json['user']['RAX-AUTH:defaultRegion'] = root.attrib[
                    defaultRegion_key]
                return resp_json
            resp.json = types.MethodType(json, resp, type(resp))

        return resp

    def delete_user(self, user_id, requestslib_kwargs=None):
        """Return response object from the delete user api call

        DELETE /v2.0/users/{user_id}
        """
        url = self.url + '/users/' + user_id
        resp = self.request('DELETE', url,
                            requestslib_kwargs=requestslib_kwargs)
        return resp

    ''' THE CODE BELOW IS NOT USED IN ANY OF THE TESTS YET.
    COMMENTING THIS OUT, SO WE CAN RESURRECT THESE CLIENT METHODS WHENEVER
    WE ADD TESTS FOR THE CORRESPONDING ENDPOINTS.

    def list_domains(self, requestslib_kwargs=None):
        """Return response object from the list domains api call

        GET /v2.0/RAX-AUTH/domains

        @todo:
        In case of XML response, add a json() method to the response object
        that will create a JSON equivalent of the XML response
        """
        url = self.url + '/RAX-AUTH/domains'
        resp = self.request('GET', url, requestslib_kwargs=requestslib_kwargs)
        return resp

    def add_role(self, role_name, role_id=None, role_description=None,
                 requestslib_kwargs=None):
        """Return response object from the add role api call

        POST /v2.0/OS-KSADM/roles
        In case of XML response, add a json() method to the response object
        that will create a JSON equivalent of the XML response
        """
        url = self.url + '/OS-KSADM/roles'
        request_object = requests.AddRole(
            role_name=role_name, role_id=role_id,
            role_description=role_description)
        resp = self.request('POST', url, request_entity=request_object,
                            requestslib_kwargs=requestslib_kwargs)
        # If the response is xml, adds a json() menthod to the object.
        # The json() method should return a json equivalent of the
        # xml response.
        if self.deserialize_format == 'xml':
            def json(self):
                resp_json = {}
                resp_json['role'] = {}

                root_string = resp.text.encode('ascii')
                root = objectify.fromstring(root_string)

                resp_json['role']['id'] = root.attrib['id']
                resp_json['role']['name'] = root.attrib['name']
                resp_json['role']['description'] = root.attrib['description']
                resp_json['role']['serviceId'] = root.attrib['serviceId']

                admin_role_key = \
                    '{' + XMLNS_RAX_AUTH + '}' + 'administratorRole'
                resp_json['role']['RAX-AUTH:administratorRole'] = root.attrib[
                    admin_role_key]

                propagate_key = '{' + XMLNS_RAX_AUTH + '}' + 'propagate'
                if root.attrib[propagate_key] == 'true':
                    resp_json['role']['RAX-AUTH:propagate'] = True
                else:
                    resp_json['role']['RAX-AUTH:propagate'] = False
                return resp_json
            resp.json = types.MethodType(json, resp, type(resp))

        return resp

    def get_role(self, role_id, requestslib_kwargs=None):
        """Return response object from the get role api call

        GET /v2.0/OS-KSADM/roles/{role_id}
        In case of XML response, add a json() method to the response object
        that will create a JSON equivalent of the XML response
        """
        url = self.url + '/OS-KSADM/roles/' + role_id
        resp = self.request('GET', url,
                            requestslib_kwargs=requestslib_kwargs)
        if self.deserialize_format == 'xml':
            def json(self):
                resp_json = {}
                resp_json['role'] = {}

                root_string = resp.text.encode('ascii')
                root = objectify.fromstring(root_string)

                resp_json['role']['id'] = root.attrib['id']
                resp_json['role']['name'] = root.attrib['name']
                resp_json['role']['description'] = root.attrib['description']
                resp_json['role']['serviceId'] = root.attrib['serviceId']

                admin_role_key = (
                    '{' + XMLNS_RAX_AUTH + '}' + 'administratorRole')
                resp_json['role']['RAX-AUTH:administratorRole'] = root.attrib[
                    admin_role_key]

                propagate_key = '{' + XMLNS_RAX_AUTH + '}' + 'propagate'
                if root.attrib[propagate_key] == 'true':
                    resp_json['role']['RAX-AUTH:propagate'] = True
                else:
                    resp_json['role']['RAX-AUTH:propagate'] = False
            resp.json = types.MethodType(json, resp, type(resp))

        return resp

    def delete_role(self, role_id, requestslib_kwargs=None):
        """Return response object from the delete role api call

        DELETE /v2.0/OS-KSADM/roles/{role_id}
        In case of XML response, add a json() method to the response object
        that will create a JSON equivalent of the XML response
        """
        url = self.url + '/OS-KSADM/roles/' + role_id
        resp = self.request('DELETE', url,
                            requestslib_kwargs=requestslib_kwargs)
        return resp

    def add_role_to_user(self, role_id, user_id, requestslib_kwargs=None):
        """Return response object from the add role to user api call

        PUT /v2.0/users/{user_id}/roles/OS-KSADM/{role_id}
        In case of XML response, add a json() method to the response object
        that will create a JSON equivalent of the XML response
        """
        url = self.url + '/users/' + user_id + '/roles/OS-KSADM/' + role_id
        resp = self.request('PUT', url,
                            requestslib_kwargs=requestslib_kwargs)
        if self.deserialize_format == 'xml':
            def json(self):
                resp_json = {}
                resp_json['role'] = {}

                root_string = resp.text.encode('ascii')
                root = objectify.fromstring(root_string)

                resp_json['role']['id'] = root.attrib['id']
                resp_json['role']['name'] = root.attrib['name']
                resp_json['role']['description'] = root.attrib['description']
                resp_json['role']['serviceId'] = root.attrib['serviceId']

                admin_role_key = \
                    '{' + XMLNS_RAX_AUTH + '}' + 'administratorRole'
                resp_json['role']['RAX-AUTH:administratorRole'] = root.attrib[
                    admin_role_key]

                propagate_key = '{' + XMLNS_RAX_AUTH + '}' + 'propagate'
                if root.attrib[propagate_key] == 'true':
                    resp_json['role']['RAX-AUTH:propagate'] = True
                else:
                    resp_json['role']['RAX-AUTH:propagate'] = False
                return resp_json
            resp.json = types.MethodType(json, resp, type(resp))

        return resp

    def list_roles(self, requestslib_kwargs=None):
        """Return response object from the list roles api call

        GET /v2.0/OS-KSADM/roles
        @todo:
        In case of XML response, add a json() method to the response object
        that will create a JSON equivalent of the XML response
        """
        url = self.url + '/OS-KSADM/roles'
        resp = self.request('GET', url,
                            requestslib_kwargs=requestslib_kwargs)
        return resp.status_code, resp.json()

    def get_users_with_role(self, role_id, requestslib_kwargs=None):
        """Return response object from the list roles api call

        GET /v2.0/OS-KSADM/roles
        @todo:
        In case of XML response, add a json() method to the response object
        that will create a JSON equivalent of the XML response
        """
        url = self.url + '/OS-KSADM/roles/' + role_id + '/RAX-AUTH/users'
        resp = self.request('GET', url,
                            requestslib_kwargs=requestslib_kwargs)
        # If the response is xml, adds a json() menthod to the object.
        # The json() method should return a json equivalent of the
        # xml response.
        if self.deserialize_format == 'xml':
            def json(self):
                resp_json = {}
                resp_json['role'] = {}

                root_string = resp.text.encode('ascii')
                root = objectify.fromstring(root_string)

                resp_json['role']['users'] = root.attrib['users']
                return resp_json
            resp.json = types.MethodType(json, resp, type(resp))

        return resp
    '''
