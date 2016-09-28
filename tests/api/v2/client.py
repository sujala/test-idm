"""Identity API Base Client."""

import types
from urlparse import urlparse
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

        # If the response is xml, adds a json() method to the object.
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

    def validate_token(self, token_id, belongs_to=None,
                       requestslib_kwargs=None):
        """
        Returns response object from validate token api call
        GET /v2.0/tokens/{token_id}
        In case of XML response, add a json() method to the response object
        that will create a JSON equivalent of the XML response
        """
        url = self.url + const.VALIDATE_TOKENS_URL.format(token_id)

        params = {'belongsTo': belongs_to} if belongs_to else None

        resp = self.request('GET', url, params=params,
                            requestslib_kwargs=requestslib_kwargs)
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

    def add_group(self, request_object, requestslib_kwargs=None):
        """
        Add a new Group
        """
        return self.request(method='POST', url=self.url+const.GROUPS_URL,
                            request_entity=request_object,
                            requestslib_kwargs=requestslib_kwargs)

    def delete_group(self, group_id, requestslib_kwargs=None):
        """
        Delete Group
        """
        url = self.url + const.GROUPS_URL + "/" + group_id
        return self.request(method='DELETE', url=url,
                            requestslib_kwargs=requestslib_kwargs)

    def add_role(self, request_object, requestslib_kwargs=None):
        """
        Add a new Role
        """
        return self.request(method='POST', url=self.url+const.ROLES_URL,
                            request_entity=request_object,
                            requestslib_kwargs=requestslib_kwargs)

    def delete_role(self, role_id, requestslib_kwargs=None):
        """
        Delete role
        """
        url = self.url + const.ROLES_URL + "/" + role_id
        return self.request(method='DELETE', url=url,
                            requestslib_kwargs=requestslib_kwargs)

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

    def list_unboundid_config(self):
        """
        Get current config of unboundid
        """
        parsedurl = urlparse(self.url)
        url = "{0}://{1}{2}".format(parsedurl.scheme, parsedurl.netloc,
                                    const.UNBOUNDID_CONFIG_URL)
        return self.request('GET', url)

    def get_role_by_name(self, role_name):
        """
        Get role by name
        """
        url = self.url + const.ROLES_URL
        params = {'roleName': role_name}
        return self.request('GET', url, params=params)

    def upgrade_user_to_cloud(self, auth_token, request_object,
                              requestslib_kwargs=None):
        headers = {const.X_AUTH_TOKEN: auth_token,
                   const.ACCEPT: const.CONTENT_TYPE_VALUE.format(const.JSON)}
        """
        Upgrade user
        SEE: https://one.rackspace.com/pages/viewpage.action?title=
        3.3.x+Demo&spaceKey=auth
        e.g., groups = [{'name': 'ImAGroup'}]
        roles = [{'name: 'ImARole'}]
        """
        url = self.url+const.UPGRADE_USER_TO_CLOUD_URL
        return self.request('PUT', url=url,
                            request_entity=request_object,
                            requestslib_kwargs=requestslib_kwargs,
                            headers=headers)

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

    def add_tenant(self, tenant, requestslib_kwargs=None):
        """Return response object from the add tenant api call

        POST /tenants
        @todo: In case of XML response, add a json() method to the response
        object that will create a JSON equivalent of the XML response
        """
        url = self.url + const.ADD_TENANT_URL
        resp = self.request('POST', url, request_entity=tenant,
                            requestslib_kwargs=requestslib_kwargs)

        return resp

    def update_tenant(self, tenant_id,
                      request_object, requestslib_kwargs=None):
        """Return response object from the update tenant api call

        POST /tenants/{tenant_id}
        @todo: In case of XML response, add a json() method to the response
        object that will create a JSON equivalent of the XML response
        """
        url = self.url + const.UPDATE_TENANT_URL.format(tenant_id=tenant_id)
        resp = self.request('POST', url, request_entity=request_object,
                            requestslib_kwargs=requestslib_kwargs)
        return resp

    def get_tenant(self, tenant_id):
        """Return response object from the get tenant api call

        GET /tenants/{tenant_id}
        @todo: In case of XML response, add a json() method to the response
        object that will create a JSON equivalent of the XML response
        """
        url = self.url + const.GET_TENANT_URL.format(tenant_id=tenant_id)
        resp = self.request('GET', url)
        return resp

    def delete_tenant(self, tenant_id):
        """Return response object from the delete tenant api call

        DELETE /tenants/{tenant_id}
        """
        url = self.url + const.DELETE_TENANT_URL.format(tenant_id=tenant_id)
        resp = self.request('DELETE', url)
        return resp

    def list_tenants(self, requestslib_kwargs=None):
        """Return response object from the list tenants call

        GET /v2.0/tenants
        @todo:
        In case of XML response, add a json() method to the response object
        that will create a JSON equivalent of the XML response
        """
        url = self.url + const.LIST_TENANTS
        resp = self.request('GET', url,
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
                resp_json[const.USER][const.RAX_AUTH_DOMAIN_ID] = (
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
                        const.RAX_AUTH_DOMAIN_ID] = child.attrib[domainId_key]

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

    def add_service(self, request_object,
                    requestslib_kwargs=None):
        """Return response object from the add service api call

        POST /OS-KSADM/services
        """
        # TODO: In case of XML response, add a json() method to the response
        # object that will create a JSON equivalent of the XML response

        url = self.url + const.SERVICE_URL
        resp = self.request('POST', url, request_entity=request_object,
                            requestslib_kwargs=requestslib_kwargs)
        return resp

    def delete_service(self, service_id, requestslib_kwargs=None):
        """Return response object from the delete service api call

        DELETE /v2.0/OS-KSADM/services/{serviceId}
        """
        url = self.url + const.DELETE_SERVICE_URL.format(
            service_id=service_id)
        resp = self.request('DELETE', url,
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
        request_object = requests.Tenant(
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

    def auth_with_saml(self, saml, base64_url_encode=False, new_url=False,
                       content_type=None, accept_type='json',
                       requestslib_kwargs=None):
        """
        Returns response object from saml auth api call for federated users
        POST call
        legacy url = '/v2.0/RAX-AUTH/saml-tokens'
        new url = '/v2.0/RAX-AUTH/federation/saml/auth'
        In case of XML response, add a json() method to the response object
        that will create a JSON equivalent of the XML response
        """

        if not new_url:
            url = self.url + const.LEGACY_FED_AUTH_URL
            headers = {
                'Accept': 'application/{0}'.format(accept_type),
                'Content-Type': 'application/{0}'.format(
                    content_type or 'xml')
            }
        else:
            url = self.url + const.NEW_FED_AUTH_URL
            headers = {
                'Accept': 'application/{0}'.format(accept_type)
            }
            if base64_url_encode:
                headers['Content-Type'] = 'application/{0}'.format(
                    content_type or 'x-www-form-urlencoded'
                )
            else:
                headers['Content-Type'] = 'application/{0}'.format(
                    content_type or 'xml'
                )

        resp = self.request('POST', url, data=saml, headers=headers,
                            requestslib_kwargs=requestslib_kwargs)
        return resp

    def logout_with_saml(self, saml=None,
                         content_type='application/x-www-form-urlencoded',
                         accept_type='json', requestslib_kwargs=None):
        """
        Returns response object from saml logout api call for federated users
        POST '/v2.0/RAX-AUTH/federation/saml/logout'
        In case of XML response, add a json() method to the response object
        that will create a JSON equivalent of the XML response
        """
        url = self.url + const.FED_LOGOUT_URL

        headers = {
            'accept': 'application/{0}'.format(accept_type),
            'Content-Type': content_type
        }
        resp = self.request('POST', url, data=saml, headers=headers,
                            requestslib_kwargs=requestslib_kwargs)
        return resp

    def get_admins_for_a_user(self, user_id, requestslib_kwargs=None):
        url = self.url + const.ADMINS_OF_A_USER_URL.format(user_id=user_id)

        resp = self.request('GET', url,
                            requestslib_kwargs=requestslib_kwargs)
        return resp

    def update_domain(self, domain_id, request_object,
                      requestslib_kwargs=None):
        """Return response object from the update domain api call

        PUT /v2.0/RAX-AUTH/domains/{domain_id}
        In case of XML response, add a json() method to the response object
        that will create a JSON equivalent of the XML response
        """
        url = self.url + '/RAX-AUTH/domains/' + domain_id
        resp = self.request('PUT', url, request_entity=request_object,
                            requestslib_kwargs=requestslib_kwargs)
        return resp

    def add_role_to_user(self, role_id, user_id, requestslib_kwargs=None):
        """Return response object from the add role to user api call

        PUT /v2.0/users/{user_id}/roles/OS-KSADM/{role_id}
        In case of XML response, add a json() method to the response object
        that will create a JSON equivalent of the XML response
        """
        url = self.url + const.ADD_ROLE_TO_USER_URL.format(user_id=user_id,
                                                           role_id=role_id)
        resp = self.request('PUT', url,
                            requestslib_kwargs=requestslib_kwargs)

        if self.deserialize_format == 'xml':
            def json(self):
                resp_json = {}
                resp_json[const.ROLE] = {}

                root_string = resp.text.encode('ascii')
                root = objectify.fromstring(root_string)

                resp_json[const.ROLE][const.ID] = root.attrib[const.ID]
                resp_json[const.ROLE][const.ROLE_NAME] = root.attrib[
                    const.ROLE_NAME]
                resp_json[const.ROLE][const.DESCRIPTION] = root.attrib[
                    const.DESCRIPTION]
                resp_json[const.ROLE][const.SERVICE_ID] = root.attrib[
                    const.SERVICE_ID]

                admin_role_key = \
                    '{' + const.XMLNS_RAX_AUTH + '}' + const.ADMINISTRATOR_ROLE
                resp_json[const.ROLE][const.RAX_AUTH_ADMINISTRATOR_ROLE] = (
                    root.attrib[admin_role_key])

                propagate_key = '{' + const.XMLNS_RAX_AUTH + '}' + (
                    const.PROPAGATE)
                if root.attrib[propagate_key] == 'true':
                    resp_json[const.ROLE][const.RAX_AUTH_PROPAGATE] = True
                else:
                    resp_json[const.ROLE][const.RAX_AUTH_PROPAGATE] = False
                return resp_json
            resp.json = types.MethodType(json, resp, type(resp))

        return resp

    def verify_cors(self, method, url, headers=None, requestslib_kwargs=None):
        """
        Support calls to verify CORS headers enable in Identity
        For now CORS headers only allow for POST /tokens
        :param method:
        :param url:
        :param requestslib_kwargs:
        :return:
        """
        url = self.url + url
        resp = self.request(method=method, url=url, headers=headers,
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
