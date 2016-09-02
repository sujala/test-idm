"""Identity API Base Client."""

import types

from lxml import objectify

from cafe.engine.http import client
from tests.api import constants as const
from tests.api.v2.models import requests


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

    def list_credentials(self, user_id):
        """
        Get list of creds for a particular user_id
        """
        url = self.url + const.CREDENTIALS_URL.format(user_id=user_id)
        resp = self.request('GET', url)
        return resp

    def add_password_credentials(self, user_id, request_object,
                                 requestslib_kwargs=None):
        """
        Add password to account
        """
        url = self.url + const.CREDENTIALS_URL.format(
                user_id=user_id)

        return self.request(method='POST', url=url,
                            request_entity=request_object,
                            requestslib_kwargs=requestslib_kwargs)

    def update_apikey(self, user_id, request_object, requestslib_kwargs=None):
        """
        Update API key on account
        """
        url = self.url+const.APIKEY_URL
        url = url.format(user_id=user_id)
        return self.request(method='POST', url=url,
                            request_entity=request_object,
                            requestslib_kwargs=requestslib_kwargs)

    def reset_api_key(self, user_id):
        """
        Reset API key
        """
        url = self.url+const.APIKEY_URL+"/"+const.RAX_AUTH+"/reset"
        url = url.format(user_id=user_id)
        return self.request('POST', url)

    def get_api_key(self, user_id):
        """
        Get API key
        """
        url = self.url+const.APIKEY_URL
        url = url.format(user_id=user_id)
        return self.request('GET', url)

    def delete_api_key(self, user_id):
        """
        Delete API key
        """
        url = self.url+const.APIKEY_URL
        url = url.format(user_id=user_id)
        return self.request('DELETE', url)

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

    def list_domains(self, headers=None, requestslib_kwargs=None):
        """Return response object from the list domains api call

        GET /v2.0/RAX-AUTH/domains

        @todo:
        In case of XML response, add a json() method to the response object
        that will create a JSON equivalent of the XML response
        """
        url = self.url + const.DOMAIN_URL
        resp = self.request('GET', url,
                            headers=headers,
                            requestslib_kwargs=requestslib_kwargs)
        return resp

    def update_user(self, user_id, request_object,
                    requestslib_kwargs=None):

        """
        Return response object from update user api call
        POST /v2.0/users/{user_id}
        In case of XML response, add a json() method to the response object
        that will create a JSON equivalent of the XML response
        """
        url = self.url + const.UPDATE_USER_URL.format(user_id=user_id)

        resp = self.request('POST', url, request_entity=request_object,
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
                    password_key = '{' + const.XMLNS_OS_KSADM + '}' + (
                        const.PASSWORD)
                    resp_json[const.USER][const.NS_PASSWORD] = (
                        root.attrib[password_key])

                defaultRegion_key = (
                    '{' + const.XMLNS_RAX_AUTH + '}' + const.DEFAULT_REGION)
                resp_json[const.USER][const.RAX_AUTH_DEFAULT_REGION] = (
                    root.attrib[defaultRegion_key])

                mFE_key = '{' + const.XMLNS_RAX_AUTH + '}' + (
                    const.MULTI_FACTOR_ENABLED
                )
                if root.attrib[mFE_key] == 'true':
                    resp_json[const.USER][const.MULTI_FACTOR_ENABLED] = True
                else:
                    resp_json[const.USER][const.MULTI_FACTOR_ENABLED] = False

                domainId_key = '{' + const.XMLNS_RAX_AUTH + '}' + (
                    const.DOMAINID
                )
                resp_json[const.USER][const.RAX_AUTH_DOMAIN] = (
                    root.attrib[domainId_key])
                return resp_json
            resp.json = types.MethodType(json, resp, type(resp))

        return resp

    def list_users(self, option=None, requestslib_kwargs=None):
        """Return response object from list users api call

        GET /v2.0/users or /v2.0/users{?name,email}
        ex: /v2.0/users?email=test@blah.com
            /v2.0/users?name=blahblah (get user by name)
        list of users were created by a given user token
        """
        url = self.url + const.USER_URL
        resp = self.request(method="GET", url=url, params=option,
                            requestslib_kwargs=requestslib_kwargs)
        if self.deserialize_format == const.XML:
            def json(self):
                resp_json = {}
                resp_json[const.USERS] = {}

                root_string = resp.text.encode(const.ASCII)
                root = objectify.fromstring(root_string)
                for child in root:
                    resp_json[const.USERS][const.USER] = {}
                    resp_json[const.USERS][const.USER][const.USERNAME] = (
                        child.attrib[const.USERNAME])

                    # Needed because json expects boolean
                    if child.attrib[const.ENABLED] == 'true':
                        resp_json[const.USERS][const.USER][const.ENABLED] = (
                            True)
                    else:
                        resp_json[const.USERS][const.USER][const.ENABLED] = (
                            False)

                    resp_json[const.USERS][const.USER][const.EMAIL] = (
                        child.attrib[const.EMAIL])
                    resp_json[const.USERS][const.USER][const.ID] = (
                        child.attrib[const.ID])

                    domainId_key = '{' + const.XMLNS_RAX_AUTH + '}' + (
                        const.DOMAINID)
                    resp_json[const.USERS][const.USER][
                        const.RAX_AUTH_DOMAIN] = child.attrib[domainId_key]

                    defaultRegion_key = (
                        '{' + const.XMLNS_RAX_AUTH + '}' +
                        const.DEFAULT_REGION)
                    resp_json[const.USERS][const.USER][
                        const.RAX_AUTH_DEFAULT_REGION] = (
                        child.attrib[defaultRegion_key])
                return resp_json
            resp.json = types.MethodType(json, resp, type(resp))
        return resp

    def add_endpoint_template(self, request_object, requestslib_kwargs=None):
        """Return response object from the create endpoint template api call

        POST /OS-KSCATALOG/endpointTemplates
        """
        # TODO: In case of XML response, add a json() method to the response
        # object that will create a JSON equivalent of the XML response

        url = self.url + const.ENDPOINT_TEMPLATE_URL
        resp = self.request('POST', url, request_entity=request_object,
                            requestslib_kwargs=requestslib_kwargs)
        return resp

    def update_endpoint_template(self, template_id, request_object,
                                 requestslib_kwargs=None):
        """Return response object from the update endpoint template api call

        PUT /OS-KSCATALOG/endpointTemplates
        """
        # TODO: In case of XML response, add a json() method to the response
        # object that will create a JSON equivalent of the XML response

        url = self.url + const.UPDATE_ENDPOINT_TEMPLATE_URL.format(
            template_id=str(template_id))
        resp = self.request('PUT', url, request_entity=request_object,
                            requestslib_kwargs=requestslib_kwargs)
        return resp

    def delete_endpoint_template(self, template_id, requestslib_kwargs=None):
        """Return response object from the delete endpoint template api call

        DELETE /OS-KSCATALOG/endpointTemplates/{template_id}
        """
        url = self.url + const.DELETE_ENDPOINT_TEMPLATE_URL.format(
            template_id=str(template_id))
        resp = self.request('DELETE', url,
                            requestslib_kwargs=requestslib_kwargs)
        return resp

    def get_endpoint_template(self, template_id, requestslib_kwargs=None):
        """Return response object from the get endpoint template api call

        GET /OS-KSCATALOG/endpointTemplates/{template_id}
        """
        # TODO: In case of XML response, add a json() method to the response
        # object that will create a JSON equivalent of the XML response

        url = self.url + const.GET_ENDPOINT_TEMPLATE_URL.format(
            template_id=str(template_id))
        resp = self.request('GET', url,
                            requestslib_kwargs=requestslib_kwargs)
        return resp

    def list_endpoint_templates(self, requestslib_kwargs=None):
        """Return response object from the list endpoint templates api call

        GET /OS-KSCATALOG/endpointTemplates
        """
        # TODO: In case of XML response, add a json() method to the response
        # object that will create a JSON equivalent of the XML response

        url = self.url + const.LIST_ENDPOINT_TEMPLATES_URL
        resp = self.request('GET', url,
                            requestslib_kwargs=requestslib_kwargs)
        return resp

    def add_service(self, service_id, name, service_type, description,
                    requestslib_kwargs=None):
        """Return response object from the add service api call

        POST /OS-KSADM/services
        """
        # TODO: In case of XML response, add a json() method to the response
        # object that will create a JSON equivalent of the XML response

        url = self.url + const.SERVICE_URL
        request_object = requests.AddService(
            service_id=service_id, service_name=name,
            service_type=service_type, service_description=description)
        resp = self.request('POST', url, request_entity=request_object,
                            requestslib_kwargs=requestslib_kwargs)
        return resp

    def add_tenant_by_name(self, tenant_name, tenant_id=None, enabled=None,
                           description=None, requestslib_kwargs=None):
        """
        Returns response object for add tenant api call

        POST /v2.0/tenants
        """
        # TODO: In case of XML response, add a json() method to the response
        # object that will create a JSON equivalent of the XML response

        url = self.url + const.TENANTS_URL
        request_object = requests.AddTenant(
            tenant_name=tenant_name, tenant_id=tenant_id,
            enabled=enabled, description=description)
        resp = self.request('POST', url, request_entity=request_object,
                            requestslib_kwargs=requestslib_kwargs)
        return resp

    def add_endpoint_to_tenant(self, tenant_id, endpoint_template_id,
                               requestslib_kwargs=None):
        """
        POST /v2.0/tenants/{tenantId}/OS-KSCATALOG/endpoints
        """
        # TODO: In case of XML response, add a json() method to the response
        # object that will create a JSON equivalent of the XML response

        url = self.url + const.ADD_ENDPOINT_TO_TENANT_URL.format(
            tenant_id=tenant_id)
        request_object = requests.AddEndpointToTenant(
            endpoint_template_id=endpoint_template_id)
        resp = self.request('POST', url, request_entity=request_object,
                            requestslib_kwargs=requestslib_kwargs)
        return resp

    def delete_endpoint_from_tenant(self, tenant_id, endpoint_template_id,
                                    requestslib_kwargs=None):
        """
        DELETE /v2.0/tenants/{tenantId}/OS-KSCATALOG/
            endpoints/{endpoint_template_id}
        """
        url = self.url + const.DELETE_ENDPOINT_FROM_TENANT_URL.format(
            tenant_id=str(tenant_id),
            endpoint_template_id=str(endpoint_template_id))
        resp = self.request('DELETE', url,
                            requestslib_kwargs=requestslib_kwargs)
        return resp

    def list_endpoints_for_token(self, token, requestslib_kwargs=None):
        """Return response object from the list endpoints for token api call

        GET /v2.0/tokens/{token}/endpoints
        """
        # TODO: In case of XML response, add a json() method to the response
        # object that will create a JSON equivalent of the XML response

        url = self.url + const.LIST_ENDPOINTS_FOR_TOKEN_URL.format(
            token_id=token)
        resp = self.request('GET', url,
                            requestslib_kwargs=requestslib_kwargs)
        return resp


    ''' THE CODE BELOW IS NOT USED IN ANY OF THE TESTS YET.
    COMMENTING THIS OUT, SO WE CAN RESURRECT THESE CLIENT METHODS WHENEVER
    WE ADD TESTS FOR THE CORRESPONDING ENDPOINTS.

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

                resp_json[const.ROLE][const.USERS] = root.attrib[const.USERS]
                return resp_json
            resp.json = types.MethodType(json, resp, type(resp))

        return resp


'''
