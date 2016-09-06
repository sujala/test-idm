"""Identity API Base Client."""

import types

from lxml import objectify

from cafe.engine.http import client
from tests.api.v2.models import requests
from tests.api import constants as const


class IdentityAPIClient(client.AutoMarshallingHTTPClient):
    """Client methods for the identity api endpoints."""

    def __init__(self, url, serialize_format, deserialize_format):
        super(IdentityAPIClient, self).__init__()
        self.url = url
        self.default_headers[const.CONTENT_TYPE] = (
            const.CONTENT_TYPE_VALUE.format(serialize_format))
        self.default_headers[const.ACCEPT_ENCODING] = (
            const.ACCEPT_ENCODING_VALUE.format(deserialize_format))
        self.default_headers[const.ACCEPT] = (
            const.ACCEPT_ENCODING_VALUE.format(deserialize_format))

        self.serialize_format = serialize_format
        self.deserialize_format = deserialize_format

    def get_auth_token(self, user, password=None, api_key=None,
                       headers=None, requestslib_kwargs=None):
        if api_key:
            request_object = requests.AuthenticateWithApiKey(
                user_name=user, api_key=api_key)
        else:
            request_object = requests.AuthenticateWithPassword(
                user_name=user, password=password)

        url = self.url + const.TOKEN_URL
        resp = self.request(method='POST', url=url,
                            request_entity=request_object,
                            headers=headers,
                            requestslib_kwargs=requestslib_kwargs)

        # If the response is xml, adds a json() menthod to the object.
        # The json() method should return a json equivalent of the
        # xml response.
        if self.deserialize_format == const.XML:
            def json(self):
                # NOTE: Partially implemented for the purpose of extracting
                # auth token. Needs to be revisited when implementing tests for
                # the endpoint
                resp_json = {}
                resp_json[const.ACCESS] = {}

                root_string = resp.text.encode(const.ASCII)
                root = objectify.fromstring(root_string)
                token = root[const.TOKEN].attrib

                services = root[const.SERVICE_CATALOG]
                serviceCatalog = []

                for service in services.getchildren():
                    item = {}
                    item[const.NAME] = service.attrib[const.NAME]
                    item[const.TYPE] = service.attrib[const.TYPE]
                    links = []
                    for endpoint in service.endpoint:
                        links.append(endpoint.attrib)
                    item[const.ENDPOINTS] = links
                    serviceCatalog.append(item)

                resp_json[const.ACCESS][const.SERVICE_CATALOG] = serviceCatalog
                resp_json[const.ACCESS][const.TOKEN] = token
                resp_json[const.ACCESS][const.USER] = user
                return resp_json
            resp.json = types.MethodType(json, resp, type(resp))

        return resp

    def add_user(self, request_object, requestslib_kwargs=None):
        """Return response object from the add user api call

        POST /v2.0/users
        In case of XML response, add a json() method to the response object
        that will create a JSON equivalent of the XML response
        """
        url = self.url + const.USER_URL

        # This is needed because json expects boolean values, while xml
        # expects strings. The tests always pass in boolean, and the xml client
        # has to translate it to string

        resp = self.request(method='POST', url=url,
                            request_entity=request_object,
                            requestslib_kwargs=requestslib_kwargs)

        # If the response is xml, adds a json() menthod to the object.
        # The json() method should return a json equivalent of the
        # xml response.
        if self.deserialize_format == const.XML:
            def json(self):
                resp_json = {}
                resp_json[const.USER] = {}

                root_string = resp.text.encode(const.ASCII)
                root = objectify.fromstring(root_string)
                resp_json[const.USER][const.USERNAME] = (
                    root.attrib[const.USERNAME])
                # Needed because json expects boolean
                if root.attrib[const.ENABLED] == 'true':
                    resp_json[const.USER][const.ENABLED] = True
                else:
                    resp_json[const.USER][const.ENABLED] = False

                if const.EMAIL in request_object:
                    resp_json[const.USER][const.EMAIL] = (
                        root.attrib[const.EMAIL])
                resp_json[const.USER][const.ID] = root.attrib[const.ID]

                if const.PASSWORD not in request_object:
                    password_key = ('{' + const.XMLNS_OS_KSADM + '}'
                                    + const.PASSWORD)
                    resp_json[const.USER][const.PASSWORD] = (
                        root.attrib[password_key])

                defaultRegion_key = (
                    '{' + const.XMLNS_RAX_AUTH + '}' + const.DEFAULT_REGION)
                resp_json[const.USER][const.DEFAULT_REGION] = root.attrib[
                    defaultRegion_key]

                mFE_key = (
                    '{' + const.XMLNS_RAX_AUTH + '}' +
                    const.MULTI_FACTOR_ENABLED)
                if root.attrib[mFE_key] == 'true':
                    resp_json[const.USER][const.MULTI_FACTOR_ENABLED] = True
                else:
                    resp_json[const.USER][const.MULTI_FACTOR_ENABLED] = False

                domainId_key = (
                    '{' + const.XMLNS_RAX_AUTH + '}' + const.DOMAINID)
                resp_json[const.USER][const.DOMAIN] = root.attrib[
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
        url = self.url + const.GET_USER_URL.format(user_id=user_id)
        resp = self.request(method='GET', url=url,
                            requestslib_kwargs=requestslib_kwargs)

        # If the response is xml, adds a json() menthod to the object.
        # The json() method should return a json equivalent of the
        # xml response.
        if self.deserialize_format == const.XML:
            def json(self):
                resp_json = {}
                resp_json[const.USER] = {}

                root_string = resp.text.encode(const.ASCII)
                root = objectify.fromstring(root_string)
                resp_json[const.USER][const.USERNAME] = (
                    root.attrib[const.USERNAME])
                # Needed because json expects boolean
                if root.attrib[const.ENABLED] == 'true':
                    resp_json[const.USER][const.ENABLED] = True
                else:
                    resp_json[const.USER][const.ENABLED] = False

                resp_json[const.USER][const.EMAIL] = root.attrib[const.EMAIL]
                resp_json[const.USER][const.ID] = root.attrib[const.ID]

                domainId_key = (
                    '{' + const.XMLNS_RAX_AUTH + '}' + const.DOMAINID)
                resp_json[const.USER][const.DOMAIN] = root.attrib[
                    domainId_key]

                defaultRegion_key = (
                    '{' + const.XMLNS_RAX_AUTH + '}' + const.DEFAULT_REGION)
                resp_json[const.USER][const.RAX_AUTH_DEFAULT_REGION] = (
                    root.attrib[defaultRegion_key])

                return resp_json
            resp.json = types.MethodType(json, resp, type(resp))

        return resp

    def delete_user(self, user_id, requestslib_kwargs=None):
        """Return response object from the delete user api call

        DELETE /v2.0/users/{user_id}
        """
        url = self.url + const.DELETE_USER_URL.format(user_id=user_id)
        resp = self.request(method='DELETE', url=url,
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
        url = self.url + const.domain_url
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
        resp = self.request(method='POST', url=url,
                            request_entity=request_object,
                            requestslib_kwargs=requestslib_kwargs)
        # If the response is xml, adds a json() menthod to the object.
        # The json() method should return a json equivalent of the
        # xml response.
        if self.deserialize_format == const.XML:
            def json(self):
                resp_json = {}
                resp_json['role'] = {}

                root_string = resp.text.encode(const.ASCII)
                root = objectify.fromstring(root_string)

                resp_json[const.ROLE][const.ID] = root.attrib[const.ID]
                resp_json[const.ROLE][const.NAME] = root.attrib[const.NAME]
                resp_json[const.ROLE][const.DESCRIPTION] = (
                    root.attrib[const.DESCRIPTION])
                resp_json[const.ROLE][const.SERVICE_ID] = (
                    root.attrib[const.SERVICE_ID])

                admin_role_key = \
                    '{' + const.XMLNS_RAX_AUTH + '}' + const.ADMINISTRATOR_ROLE
                resp_json[const.ROLE][const.RAX_AUTH_ADMINISTRATOR_ROLE] = (
                    root.attrib[admin_role_key])

                propagate_key = '{' + const.XMLNS_RAX_AUTH + '}' +
                    const.PROPAGATE
                if root.attrib[propagate_key] == 'true':
                    resp_json[const.ROLE][const.RAX_AUTH_PROPAGATE] = True
                else:
                    resp_json[const.ROLE][const.RAX_AUTH_PROPAGATE] = False
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
        if self.deserialize_format == const.XML:
            def json(self):
                resp_json = {}
                resp_json[const.ROLE] = {}

                root_string = resp.text.encode(const.ASCII)
                root = objectify.fromstring(root_string)

                resp_json[const.ROLE][const.ID] = root.attrib[const.ID]
                resp_json[const.ROLE][const.NAME] = root.attrib[const.NAME]
                resp_json[const.ROLE][const.DESCRIPTION] = (
                    root.attrib[const.DESCRIPTION])
                resp_json[const.ROLE][const.SERVICE_ID] = (
                    root.attrib[const.SERVICE_ID])

                admin_role_key = (
                    '{' + const.XMLNS_RAX_AUTH + '}' +
                    const.ADMINISTRATOR_ROLE)
                resp_json[const.ROLE][const.RAX_AUTH_ADMINISTRATOR_ROLE] = (
                    root.attrib[admin_role_key])

                propagate_key = '{' + const.XMLNS_RAX_AUTH + '}' +
                    const.PROPAGATE
                if root.attrib[propagate_key] == 'true':
                    resp_json[const.ROLE][const.RAX_AUTH_PROPAGATE] = True
                else:
                    resp_json[const.ROLE][const.RAX_AUTH_PROPAGATE] = False
            resp.json = types.MethodType(json, resp, type(resp))

        return resp

    def delete_role(self, role_id, requestslib_kwargs=None):
        """Return response object from the delete role api call

        DELETE /v2.0/OS-KSADM/roles/{role_id}
        In case of XML response, add a json() method to the response object
        that will create a JSON equivalent of the XML response
        """
        url = self.url + '/OS-KSADM/roles/' + role_id
        resp = self.request(const.DELETE, url,
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
        if self.deserialize_format == const.XML:
            def json(self):
                resp_json = {}
                resp_json[const.ROLE] = {}

                root_string = resp.text.encode(const.ASCII)
                root = objectify.fromstring(root_string)

                resp_json[const.ROLE][const.ID] = root.attrib[const.ID]
                resp_json[const.ROLE][const.NAME] = root.attrib[const.NAME]
                resp_json[const.ROLE][const.DESCRIPTION] = (
                    root.attrib[const.DESCRIPTION])
                resp_json[const.ROLE][const.SERVICE_ID] = (
                    root.attrib[const.SERVICE_ID])

                admin_role_key = \
                    '{' + const.XMLNS_RAX_AUTH + '}' + const.ADMINISTRATOR_ROLE
                resp_json[const.ROLE][const.RAX_AUTH_ADMINISTRATOR_ROLE] = (
                    root.attrib[admin_role_key])

                propagate_key = '{' + const.XMLNS_RAX_AUTH + '}' + 'propagate'
                if root.attrib[propagate_key] == 'true':
                    resp_json[const.ROLE][const.RAX_AUTH_PROPAGATE] = True
                else:
                    resp_json[const.ROLE][const.RAX_AUTH_PROPAGATE] = False
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
        if self.deserialize_format == const.XML:
            def json(self):
                resp_json = {}
                resp_json[const.ROLE] = {}

                root_string = resp.text.encode(const.ASCII)
                root = objectify.fromstring(root_string)

                resp_json[const.ROLE]['users'] = root.attrib['users']
                return resp_json
            resp.json = types.MethodType(json, resp, type(resp))

        return resp
    '''
