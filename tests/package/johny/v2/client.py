"""Identity API Base Client."""

import types
from urlparse import urlparse
from lxml import objectify

from cafe.engine.http import client

from .. import constants as const
from ..v2.models import requests


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

    def get_auth_token(self, request_object, headers=None,
                       requestslib_kwargs=None):
        """Returns response object from auth"""
        url = self.url + const.TOKEN_URL
        resp = self.request(method='POST', url=url,
                            request_entity=request_object,
                            headers=headers,
                            requestslib_kwargs=requestslib_kwargs)

        if self.deserialize_format == const.XML:
            def json(self):
                root_string = resp.text.encode(const.ASCII)
                root = objectify.fromstring(root_string)
                token = root[const.TOKEN]

                resp_json = {}
                resp_json[const.ACCESS] = {}
                resp_json[const.ACCESS][const.TOKEN] = {}

                resp_json[const.ACCESS][const.TOKEN][const.EXPIRES] = \
                    token.attrib[const.EXPIRES]
                resp_json[const.ACCESS][const.TOKEN][const.ID] = \
                    token.attrib[const.ID]
                if const.TENANT in token.attrib:
                    tenant = root[const.TOKEN][const.TENANT]
                    resp_json[const.ACCESS][const.TOKEN][const.TENANT] = {}
                    resp_json[
                        const.ACCESS][const.TOKEN][const.TENANT][const.ID] = \
                        tenant.attrib[const.ID]
                    resp_json[const.ACCESS][const.TOKEN][const.TENANT][const.NAME] = tenant.attrib[const.NAME] # noqa

                resp_json[const.ACCESS][const.SERVICE_CATALOG] = [
                    'not_implemented']

                user = root[const.USER]
                resp_json[const.ACCESS][const.USER] = {}
                resp_json[const.ACCESS][const.USER][const.RAX_AUTH_DEFAULT_REGION] = 1 # noqa
                resp_json[const.ACCESS][const.USER][const.ID] = \
                    unicode(user.attrib[const.ID])
                resp_json[const.ACCESS][const.USER][const.ROLES] = []
                resp_json[const.ACCESS][const.USER][const.NAME] = \
                    user.attrib[const.NAME]

                return resp_json
            resp.json = types.MethodType(json, resp, type(resp))

        return resp

    def auth_with_mfa_cred(self, session_id, pass_code, tenant_id=None,
                           tenant_name=None, headers=None,
                           requestslib_kwargs=None):
        if not headers:
            headers = {}
        headers[const.X_SESSION_ID] = session_id
        req_obj = requests.AuthenticateWithMFA(
            pass_code=pass_code, tenant_id=tenant_id, tenant_name=tenant_name)
        url = self.url + const.TOKEN_URL
        resp = self.request(method='POST', url=url, request_entity=req_obj,
                            headers=headers,
                            requestslib_kwargs=requestslib_kwargs)
        # TODO: add equivalent xml to json response
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

    def list_groups(self, user_id, requestslib_kwargs=None):
        """
        List Group
        """
        url = self.url + const.LIST_GROUPS_URL.format(user_id=user_id)
        return self.request(method='GET', url=url,
                            requestslib_kwargs=requestslib_kwargs)

    def revoke_token(self, token_id='', requestslib_kwargs=None):
        """
        Returns response object from revoke token api call
        - A user can revoke their own authentication token by submitting the
        DELETE request without specifying the tokenId parameter.
        DELETE /v2.0/tokens/
        - Identity and User administrators can revoke the token for another
        user by including the tokenId parameter in the request
        DELETE /v2.0/tokens/{token_id}
        Delete user token
        :param token_id:
        :param requestlib_kwargs:
        :return:
        """
        url = self.url + const.DELETE_TOKEN_URL.format(token_id=token_id)
        resp = self.request(method='DELETE', url=url,
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
        url = self.url + const.CREDENTIALS_URL.format(user_id=user_id)

        return self.request(method='POST', url=url,
                            request_entity=request_object,
                            requestslib_kwargs=requestslib_kwargs)

    def update_api_key(self, user_id, request_object, requestslib_kwargs=None):
        """
        Update API key on account
        """
        url = self.url + const.UPDATE_USER_API_CRED_URL
        url = url.format(user_id=user_id)
        return self.request(method='POST', url=url,
                            request_entity=request_object,
                            requestslib_kwargs=requestslib_kwargs)

    def reset_api_key(self, user_id):
        """
        Reset API key
        """
        url = self.url + const.RESET_USER_API_KEY_URL
        url = url.format(user_id=user_id)
        return self.request('POST', url)

    def add_group(self, request_object, requestslib_kwargs=None):
        """
        Add a new Group
        """
        return self.request(method='POST', url=self.url + const.GROUPS_URL,
                            request_entity=request_object,
                            requestslib_kwargs=requestslib_kwargs)

    def delete_group(self, group_id, requestslib_kwargs=None):
        """
        Delete Group
        """
        url = self.url + const.GROUPS_URL + "/" + group_id
        return self.request(method='DELETE', url=url,
                            requestslib_kwargs=requestslib_kwargs)

    def get_api_key(self, user_id):
        """
        Get API key
        """
        url = self.url + const.GET_USER_API_CRED_URL
        url = url.format(user_id=user_id)
        resp = self.request('GET', url)

        if self.deserialize_format == const.XML:
            def json(self):
                root_string = resp.text.encode(const.ASCII)
                root = objectify.fromstring(root_string)

                resp_json = {}
                resp_json[const.NS_API_KEY_CREDENTIALS] = {}

                resp_json[const.NS_API_KEY_CREDENTIALS][const.USERNAME] = \
                    root.attrib[const.USERNAME]
                resp_json[const.NS_API_KEY_CREDENTIALS][const.API_KEY] = \
                    root.attrib[const.API_KEY]
                return resp_json
            resp.json = types.MethodType(json, resp, type(resp))
        return resp

    def delete_api_key(self, user_id):
        """
        Delete API key
        """
        url = self.url + const.DELETE_USER_API_CRED_URL
        url = url.format(user_id=user_id)
        return self.request('DELETE', url)

    def list_unboundid_config(self):
        """
        Get current config of unboundid
        """
        parsed_url = urlparse(self.url)
        url = "{0}://{1}{2}{3}".format(parsed_url.scheme,
                                       parsed_url.netloc,
                                       const.DEVOPS_URL,
                                       const.DEVOPS_PROPS_URL)
        return self.request('GET', url)

    def analyze_token(self):
        """Analyze Tokens."""
        parsed_url = urlparse(self.url)
        url = "{0}://{1}{2}{3}".format(parsed_url.scheme,
                                       parsed_url.netloc,
                                       const.DEVOPS_URL,
                                       const.ANALYZE_TOKEN_URL)
        return self.request('GET', url)

    def create_otp_device(self, user_id, request_object,
                          requestslib_kwargs=None):
        """
        Add OTP (one time password) device to user
        POST /v2.0/users/{user_id}/RAX-AUTH/multi-factor/otp-devices
        """
        url = self.url + const.ADD_OTP_DEVICE_URL.format(user_id=user_id)
        resp = self.request(method='POST', url=url,
                            request_entity=request_object,
                            requestslib_kwargs=requestslib_kwargs)

        if self.deserialize_format == const.XML:
            def json(self):
                resp_json = {}
                resp_json[const.NS_OTP_DEVICE] = {}
                root_string = resp.text.encode(const.ASCII)
                root = objectify.fromstring(root_string)
                resp_json[const.NS_OTP_DEVICE][const.ID] = (
                    root.attrib[const.ID]
                )
                resp_json[const.NS_OTP_DEVICE][const.KEY_URI] = (
                    root.attrib[const.KEY_URI]
                )
                resp_json[const.NS_OTP_DEVICE][const.QR_CODE] = (
                    root.attrib[const.QR_CODE]
                )
                resp_json[const.NS_OTP_DEVICE][const.NAME] = (
                    root.attrib[const.NAME]
                )
                return resp_json
            resp.json = types.MethodType(json, resp, type(resp))
        return resp

    def verify_otp_device(self, user_id, otp_device_id, request_object,
                          requestslib_kwargs=None):
        """
        Verify OTP device
        Sends the mobile passcode from the mobile passcode application to the
        Identity service to pair the OTP device instance with the account
        POST /v2.0/users/{userId}/RAX-AUTH/multi-factor/otp-devices/\
              {deiviceId}/verify
        :param user_id:
        :return: 204 no content
        """
        url = self.url + const.VERIFY_OTP_DEVICE_URL.format(
            user_id=user_id, device_id=otp_device_id)
        resp = self.request(method='POST', url=url,
                            request_entity=request_object,
                            requestslib_kwargs=requestslib_kwargs)

        return resp

    def delete_otp_device(self, user_id, otp_device_id,
                          requestslib_kwargs=None):
        """
        Remove Otp device from user account
        DELETE '/v2.0/users/{userId}/RAX-AUTH/multi-factor/otp-devices/\
                {otpDeviceId}'
        :param user_id:
        :param otp_device_id:
        :return: 204 no content
        """
        url = self.url + const.DELETE_OTP_DEVICE_URL.format(
            user_id=user_id, device_id=otp_device_id
        )
        resp = self.request(method='DELETE', url=url,
                            requestslib_kwargs=requestslib_kwargs)
        return resp

    def update_mfa(self, user_id, request_object, requestslib_kwargs=None):
        """
        Updates the multi-factor authentication settings for the
        specified account.
        PUT /v2.0/users/{userId}/RAX-AUTH/multi-factor
        :param user_id:
        :return: 204 no response body
        """
        url = self.url + const.UPDATE_MFA_URL.format(user_id=user_id)
        resp = self.request(method='PUT', url=url,
                            request_entity=request_object,
                            requestslib_kwargs=requestslib_kwargs)
        return resp

    def get_role_by_name(self, role_name):
        """
        Get role by name
        """
        url = self.url + const.ROLES_URL
        params = {'roleName': role_name}
        return self.request('GET', url, params=params)

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

                if request_object.email:
                    resp_json[const.USER][const.EMAIL] = (
                        root.attrib[const.EMAIL])
                resp_json[const.USER][const.ID] = root.attrib[const.ID]

                if not request_object.password:
                    password_key = ('{' + const.XMLNS_OS_KSADM + '}'
                                    + const.PASSWORD)
                    resp_json[const.USER][const.NS_PASSWORD] = (
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
                    '{' + const.XMLNS_RAX_AUTH + '}' + const.DOMAIN_ID)
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
                    '{' + const.XMLNS_RAX_AUTH + '}' + const.DOMAIN_ID)
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

    def add_domain(self, request_object, requestslib_kwargs=None):
        """Return response object from the add domain api call

        POST /domains
        @todo: In case of XML response, add a json() method to the response
        object that will create a JSON equivalent of the XML response
        """
        url = self.url + const.DOMAIN_URL
        resp = self.request('POST', url, request_entity=request_object,
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

    def list_tenants_in_domain(self, domain_id, requestslib_kwargs=None):
        """Return response object from the list tenants in domain call
        @todo:
        In case of XML response, add a json() method to the response object
        that will create a JSON equivalent of the XML response
        """
        url = self.url + const.LIST_TENANTS_IN_DOMAIN_URL.format(
            domainId=domain_id)
        resp = self.request('GET', url,
                            requestslib_kwargs=requestslib_kwargs)

        return resp

    def list_users_for_tenant(self, tenant_id, option=None,
                              requestslib_kwargs=None):
        """ Return response object from the list users on tenant call
        GET /v2.0/tenants/{tenant_id}/users{?marker,limit}
        """
        url = self.url + const.LIST_USERS_FOR_TENANT_URL.format(
            tenant_id=tenant_id)
        resp = self.request(method='GET', url=url, params=option,
                            requestslib_kwargs=requestslib_kwargs)
        return resp

    def list_roles_for_user_on_tenant(self, tenant_id, user_id,
                                      requestslib_kwargs=None):
        """ Return response object from list roles for specific user on
        specific tenant

        GET /v2.0/tenants/{tenant_id}/users/{user_id}/roles
        """
        url = self.url + const.LIST_ROLES_FOR_USER_ON_TENANT_URL.format(
            tenant_id=tenant_id, user_id=user_id)
        resp = self.request(method='GET', url=url,
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
                    const.DOMAIN_ID
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
                        const.DOMAIN_ID)
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

    def list_services(self, option=None, requestslib_kwargs=None):
        """
        Return response object from list services api call

        GET /v2.0//OS-KSADM/services{?name,limit,marker}
        :param option:
        :param requestslib_kwargs:
        :return:
        """
        url = self.url + const.SERVICE_URL
        resp = self.request(method='GET', url=url, params=option,
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
        return resp

    def add_role_to_user_for_tenant(self, tenant_id, user_id, role_id,
                                    requestslib_kwargs=None):
        """ Return response object from the add role to user on tenant
            no response body
        PUT /v2.0/tenants/{tenant_id}/users/{user_id}/roles/OS-KSADM/(role_id}
        :return: 204 no response body
        """
        url = self.url + const.ADD_ROLE_TO_USER_FOR_TENANT_URL.format(
            tenant_id=tenant_id, user_id=user_id, role_id=role_id
        )
        resp = self.request(method='PUT', url=url,
                            requestslib_kwargs=requestslib_kwargs)
        return resp

    def delete_role_from_user_for_tenant(self, tenant_id, user_id, role_id,
                                         requestslib_kwargs=None):
        """ Return response object from the delete role to user on tenant
            no response body
        DELETE /v2.0/tenants/{tenant_id}/users/{user_id}/roles/OS-KSADM/{
        role_id}
        :return: 204 no response body
        """
        url = self.url + const.DEL_ROLE_FROM_USER_FOR_TENANT_URL.format(
            tenant_id=tenant_id, user_id=user_id, role_id=role_id
        )
        resp = self.request(method='DELETE', url=url,
                            requestslib_kwargs=requestslib_kwargs)
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

    def delete_role_from_user(self, role_id, user_id, requestslib_kwargs=None):
        """Delete role from user
        DELETE /v2.0/users/{userId}/roles/OS-KSADM/{roleid}
        """
        url = self.url + const.DELETE_ROLE_FR_USER_URL.format(user_id=user_id,
                                                              role_id=role_id)
        resp = self.request(method='DELETE', url=url,
                            requestslib_kwargs=requestslib_kwargs)
        return resp

    def add_tenant_to_domain(self, domain_id, tenant_id,
                             requestslib_kwargs=None):
        """ Return response object from add a tenant to a domain
        PUT /v2.0/RAX-AUTH/domains/{domainId}/tenants/{tenantId}
        :return:
        """
        url = self.url + const.ADD_TENANT_TO_DOMAIN_URL.format(
            domain_id=domain_id, tenant_id=tenant_id)
        resp = self.request(method='PUT', url=url,
                            requestslib_kwargs=requestslib_kwargs)
        return resp

    def get_tenants_in_domain(self, domain_id, requestslib_kwargs=None):
        """ Return response object from get tenants in domain
        GET '/v2.0/RAX-AUTH/domains/{domainId}/tenants'
        :param domain_id:
        :return:
        """
        url = self.url + const.GET_TENANTS_IN_DOMAIN_URL.format(
            domain_id=domain_id)
        resp = self.request(method='GET', url=url,
                            requestslib_kwargs=requestslib_kwargs)
        return resp

    def delete_tenant_from_domain(self, domain_id, tenant_id,
                                  requestslib_kwargs=None):
        """ Return response object from add a tenant to a domain
        DELETE /v2.0/RAX-AUTH/domains/{domainId}/tenants/{tenantId}
        :return:
        """
        url = self.url + const.DELETE_TENANT_FROM_DOMAIN_URL.format(
            domain_id=domain_id, tenant_id=tenant_id)
        resp = self.request(method='DELETE', url=url,
                            requestslib_kwargs=requestslib_kwargs)
        return resp

    def add_tenant_type_to_endpoint_mapping_rule(
            self, request_object, requestslib_kwargs=None):
        """
        Return response object from create tenant type to endpoints
        mapping rule
        POST OS-KSCATALOG/endpointTemplates/RAX-AUTH/rules
        In case of XML response, add a json() method to the response object
        that will create a JSON equivalent of the XML response
        """

        url = self.url + const.TENANT_TYPE_TO_ENDPOINT_MAPPING_RULES_URL

        resp = self.request(method='POST', url=url,
                            request_entity=request_object,
                            requestslib_kwargs=requestslib_kwargs)
        return resp

    def get_tenant_type_to_endpoint_mapping_rule(
            self, rule_id, response_detail=None, requestslib_kwargs=None):
        """
        Return response object from get a rule for tenant type to endpoints
        mapping
        GET OS-KSCATALOG/endpointTemplates/RAX-AUTH/rules/{rule_id}
        This api call supports a query param 'responseDetail', with
        two possible values 'minimum' and 'basic'
        @todo: In case of XML response, add a json() method to the response
        object that will create a JSON equivalent of the XML response
        """

        url = self.url + (
            const.GET_TENANT_TYPE_TO_ENDPOINT_MAPPING_RULE_URL.format(
                rule_id=rule_id))
        params = None
        if response_detail is not None:
            params = {const.RESPONSE_DETAIL: response_detail}

        resp = self.request(method='GET', url=url, params=params,
                            requestslib_kwargs=requestslib_kwargs)

        return resp

    def delete_tenant_type_to_endpoint_mapping_rule(
            self, rule_id, requestslib_kwargs=None):
        """
        Delete specified rule for tenant type to endpoints mapping
        DELETE OS-KSCATALOG/endpointTemplates/RAX-AUTH/rules/{rule_id}
        """

        url = self.url + (
            const.DELETE_TENANT_TYPE_TO_ENDPOINT_MAPPING_RULE_URL.format(
                rule_id=rule_id))

        resp = self.request(
            method='DELETE', url=url, requestslib_kwargs=requestslib_kwargs)

        return resp

    def list_tenant_type_to_endpoint_mapping_rules(
            self, requestslib_kwargs=None):
        """
        Return response object from list rules for tenant type to endpoints
        mapping
        GET OS-KSCATALOG/endpointTemplates/RAX-AUTH/rules
        @todo: In case of XML response, add a json() method to the response
        object that will create a JSON equivalent of the XML response
        """

        url = self.url + (
            const.TENANT_TYPE_TO_ENDPOINT_MAPPING_RULES_URL)

        resp = self.request(method='GET', url=url,
                            requestslib_kwargs=requestslib_kwargs)

        return resp

    def add_role(self, request_object, requestslib_kwargs=None):
        """Return response object from the add role api call

        POST /v2.0/OS-KSADM/roles
        In case of XML response, add a json() method to the response object
        that will create a JSON equivalent of the XML response
        """
        url = self.url + const.ROLE_URL
        resp = self.request(method='POST', url=url,
                            request_entity=request_object,
                            requestslib_kwargs=requestslib_kwargs)

        # If the response is xml, adds a json() menthod to the object.
        # The json() method should return a json equivalent of the
        # xml response.
        if self.deserialize_format == 'xml':
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

                propagate_key = '{' + const.XMLNS_RAX_AUTH + '}' + (
                    const.PROPAGATE)
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
        url = self.url + const.GET_ROLE_URL.format(role_id=role_id)
        resp = self.request(method='GET', url=url,
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

                propagate_key = '{' + const.XMLNS_RAX_AUTH + '}' + (
                    const.PROPAGATE)
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
        url = self.url + const.DELETE_ROLE_URL.format(role_id=role_id)
        resp = self.request(method='DELETE', url=url,
                            requestslib_kwargs=requestslib_kwargs)
        return resp

    def list_roles(self, option=None, requestslib_kwargs=None):
        """Return response object from the list roles api call

        GET /v2.0/OS-KSADM/roles{?limit,marker}
        """
        url = self.url + const.ROLE_URL
        resp = self.request(method='GET', url=url, params=option,
                            requestslib_kwargs=requestslib_kwargs)

        if self.deserialize_format == const.XML:
            def json(self):
                resp_json = {}
                resp_json[const.ROLES] = []

                root_string = resp.text.encode(const.ASCII)
                root = objectify.fromstring(root_string)

                for child in root:
                    item = {}
                    item[const.ID] = child.attrib[const.ID]
                    item[const.NAME] = child.attrib[const.NAME]
                    item[const.DESCRIPTION] = child.attrib[const.DESCRIPTION]
                    item[const.SERVICE_ID] = child.attrib[const.SERVICE_ID]

                    propagate = '{' + const.XMLNS_RAX_AUTH + '}' + (
                        const.PROPAGATE)
                    if child.attrib[propagate] == 'true':
                        item[const.RAX_AUTH_PROPAGATE] = True
                    else:
                        item[const.RAX_AUTH_PROPAGATE] = False
                    resp_json[const.ROLES].append(item)

            resp.json = types.MethodType(json, resp, type(resp))
        return resp

    def list_roles_for_user(self, user_id, apply_rcn_roles=None,
                            requestslib_kwargs=None):
        """
        List global roles assigned to a user
        GET /v2.0/users/{userId}/roles
        :return return a list of roles assigned to a user
        """
        url = self.url + const.LIST_USER_ROLES_URL.format(user_id=user_id)
        params = None
        if apply_rcn_roles:
            params = {'apply_rcn_roles': apply_rcn_roles}
        resp = self.request(method='GET', url=url, params=params,
                            requestslib_kwargs=requestslib_kwargs)
        if self.deserialize_format == const.XML:
            def json(self):
                resp_json = {}
                resp_json[const.ROLES] = []

                root_string = resp.text.encode(const.ASCII)
                root = objectify.fromstring(root_string)

                for child in root:
                    item = {}
                    item[const.ID] = child.attrib[const.ID]
                    item[const.NAME] = child.attrib[const.NAME]
                    item[const.DESCRIPTION] = child.attrib[const.DESCRIPTION]
                    item[const.SERVICE_ID] = child.attrib[const.SERVICE_ID]

                    propagate = '{' + const.XMLNS_RAX_AUTH + '}' + (
                        const.PROPAGATE)
                    if child.attrib[propagate] == 'true':
                        item[const.RAX_AUTH_PROPAGATE] = True
                    else:
                        item[const.RAX_AUTH_PROPAGATE] = False
                    resp_json[const.ROLES].append(item)

            resp.json = types.MethodType(json, resp, type(resp))
        return resp

    def get_users_for_role(self, role_id, requestslib_kwargs=None):
        """Gets users who have the specified role.
        GET /v2.0/OS-KSADM/roles/{roleId}/RAX-AUTH/users
        :return list of users associated with specific role
        """
        url = self.url + const.GET_USERS_FOR_ROLE_URL.format(role_id=role_id)
        resp = self.request(method='GET', url=url,
                            requestslib_kwargs=requestslib_kwargs)
        if self.deserialize_format == 'xml':
            def json(self):
                resp_json = {}
                resp_json[const.USERS] = []

                root_string = resp.text.encode(const.ASCII)
                root = objectify.fromstring(root_string)

                for child in root:
                    item = {}
                    item[const.ID] = child.attrib[const.ID]
                    item[const.USERNAME] = child.attrib[const.USERNAME]
                    item[const.EMAIL] = child.attrib[const.EMAIL]
                    item[const.RAX_AUTH_DOMAIN_ID] = child.attrib[
                        const.RAX_AUTH_DOMAIN_ID
                    ]
                    item[const.RAX_AUTH_DEFAULT_REGION] = child.attrib[
                        const.RAX_AUTH_DEFAULT_REGION
                    ]
                    if child.attrib[const.RAX_AUTH_MULTI_FACTOR_ENABLED] == (
                            'true'):
                        item[const.RAX_AUTH_MULTI_FACTOR_ENABLED] = True
                    else:
                        item[const.RAX_AUTH_MULTI_FACTOR_ENABLED] = False
                    if child.attrib[const.ENABLED] == 'true':
                        item[const.ENABLED] = True
                    else:
                        item[const.ENABLED] = False

                    resp_json[const.USERS].append(item)
            resp.json = types.MethodType(json, resp, type(resp))
        return resp

    def get_domain(self, domain_id, requestslib_kwargs=None):
        """Get a domain
        """
        url = self.url + const.GET_DOMAIN_URL.format(domain_id=domain_id)
        resp = self.request(method='GET', url=url,
                            requestslib_kwargs=requestslib_kwargs)
        return resp

    def delete_domain(self, domain_id, requestslib_kwargs=None):
        """Delete a domain
        :return 204 no response body
        """
        url = self.url + const.DELETE_DOMAIN_URL.format(domain_id=domain_id)
        resp = self.request(method='DELETE', url=url,
                            requestslib_kwargs=requestslib_kwargs)
        return resp

    def list_users_in_domain(self, domain_id,
                             enabled=None, requestslib_kwargs=None):
        """Return response object from list users in domain
        GET '/v2.0//RAX-AUTH/domains/{domain_id}/users{?enabled}
        :param domain_id:
        :param requestslib_kwargs:
        :return:
        """
        url = self.url + const.LIST_USERS_IN_DOMAIN_URL.format(
            domain_id=domain_id)
        if enabled is True:
            enabled = {'enabled': True}
        elif enabled is False:
            enabled = {'enabled': False}
        else:
            enabled = None
        resp = self.request(method='GET', url=url, params=enabled,
                            requestslib_kwargs=requestslib_kwargs)
        if self.deserialize_format == const.XML:
            def json(self):
                resp_json = {}
                resp_json[const.USERS] = []

                root_string = resp.text.encode(const.ASCII)
                root = objectify.fromstring(root_string)
                user = root.user

                item = {}
                item[const.ID] = user.attrib[const.ID]
                item[const.USERNAME] = user.attrib[const.USERNAME]
                item[const.EMAIL] = user.attrib[const.EMAIL]
                if user.attrib[const.ENABLED] == 'true':
                    item[const.ENABLED] = True
                else:
                    item[const.ENABLED] = False

                resp_json[const.USERS].append(item)
                return resp_json

            resp.json = types.MethodType(json, resp, type(resp))

        return resp

    def create_idp(self, request_object, requestslib_kwargs=None):
        """Return response object from the create idp api call
        """
        url = self.url + const.IDP_URL

        resp = self.request(method='POST', url=url,
                            request_entity=request_object,
                            requestslib_kwargs=requestslib_kwargs)
        return resp

    def update_idp(self, idp_id, request_object, requestslib_kwargs=None):
        """Return response object from the update idp api call
        """
        url = self.url + const.IDP_RUD_URL.format(idp_id=idp_id)

        resp = self.request(method='PUT', url=url,
                            request_entity=request_object,
                            requestslib_kwargs=requestslib_kwargs)
        return resp

    def get_idp(self, idp_id, requestslib_kwargs=None):
        """Return response object from the get_idp api call
        """
        url = self.url + const.IDP_RUD_URL.format(idp_id=idp_id)

        resp = self.request(method='GET', url=url,
                            requestslib_kwargs=requestslib_kwargs)
        return resp

    def list_idp(self, option=None, requestslib_kwargs=None):
        """Return response object from the list idp api call
        """
        url = self.url + const.IDP_URL

        resp = self.request(method='GET', url=url, params=option,
                            requestslib_kwargs=requestslib_kwargs)
        return resp

    def delete_idp(self, idp_id, requestslib_kwargs=None):
        """Return response object from the delete provider api call
        """
        url = self.url + const.IDP_RUD_URL.format(idp_id=idp_id)

        resp = self.request(method='DELETE', url=url,
                            requestslib_kwargs=requestslib_kwargs)
        return resp

    def add_certificate(self, idp_id, request_object, requestslib_kwargs=None):
        """Return response object from the add certificate api call
        """
        url = self.url + const.CERTIFICATE_ADD_URL.format(idp_id=idp_id)

        resp = self.request(method='PUT', url=url,
                            request_entity=request_object,
                            requestslib_kwargs=requestslib_kwargs)
        return resp

    def delete_certificate(self, idp_id, certificate_id,
                           requestslib_kwargs=None):
        """Return response object from the delete certificate api call
        """
        url = self.url + const.IDP_RUD_URL.format(idp_id=idp_id,
                                                  cert_id=certificate_id)

        resp = self.request(method='DELETE', url=url,
                            requestslib_kwargs=requestslib_kwargs)
        return resp

    def add_idp_mapping(self, idp_id, request_data, content_type=const.JSON,
                        requestslib_kwargs=None):
        """Return response object from the add certificate api call
        """
        url = self.url + const.IDP_MAPPING_CR_URL.format(idp_id=idp_id)
        headers = {}
        if not requestslib_kwargs:
            requestslib_kwargs = {}
        if content_type == const.YAML:
            requestslib_kwargs["data"] = request_data
            headers[const.CONTENT_TYPE] = const.YAML_CONTENT_TYPE_VALUE
        elif content_type == const.JSON:
            requestslib_kwargs["json"] = request_data
            headers[const.CONTENT_TYPE] = const.CONTENT_TYPE_VALUE.format(
                const.JSON)

        # Note that there is always a default mapping, so no POST
        resp = self.request(method='PUT', url=url, headers=headers,
                            requestslib_kwargs=requestslib_kwargs)
        return resp

    def get_idp_mapping(self, idp_id, headers=None, requestslib_kwargs=None):
        """Return response object from the get_idp api call
        """
        url = self.url + const.IDP_MAPPING_CR_URL.format(idp_id=idp_id)

        resp = self.request(method='GET', url=url, headers=headers,
                            requestslib_kwargs=requestslib_kwargs)
        return resp

    def get_idp_metadata(self, idp_id, requestslib_kwargs=None):
        """Return response object from the get_idp_metadata api call
        """
        url = self.url + const.IDP_METADATA_RUD_URL.format(idp_id=idp_id)

        resp = self.request(method='GET', url=url,
                            requestslib_kwargs=requestslib_kwargs)
        return resp

    def update_idp_metadata(self, idp_id, request_object,
                            requestslib_kwargs=None):
        """Return response object from the update_idp_metadata api call
        """
        url = self.url + const.IDP_METADATA_RUD_URL.format(idp_id=idp_id)

        resp = self.request(method='PUT', url=url,
                            request_entity=request_object,
                            requestslib_kwargs=requestslib_kwargs)
        return resp

    def impersonate_user(self, request_data, requestslib_kwargs=None):
        """
        Return response object from impersonation call
        """
        url = self.url + const.IMPERSONATION_URL

        resp = self.request(
            method='POST', url=url, request_entity=request_data,
            requestslib_kwargs=requestslib_kwargs)

        return resp

    def add_tenant_type(self, tenant_type, requestslib_kwargs=None):
        """Return response object from the add tenant type api call

        POST RAX-AUTH/tenant-types
        """
        url = self.url + const.TENANT_TYPE_URL
        resp = self.request('POST', url, request_entity=tenant_type,
                            requestslib_kwargs=requestslib_kwargs)

        return resp

    def delete_tenant_type(self, name):
        """Return response object from the delete tenant type api call

        DELETE RAX-AUTH/tenant-types/{name}
        """
        url = self.url + const.RD_TENANT_TYPE_URL.format(name=name)
        resp = self.request('DELETE', url)
        return resp

    def get_tenant_type(self, name):
        """Return response object from the get tenant type api call

        GET RAX-AUTH/tenant-types/{name}
        """
        url = self.url + const.RD_TENANT_TYPE_URL.format(name=name)
        resp = self.request('GET', url)
        return resp

    def list_tenant_types(self):
        """Return response object from the list tenant types api call

        GET RAX-AUTH/tenant-types
        """
        url = self.url + const.TENANT_TYPE_URL
        resp = self.request('GET', url)
        return resp

    def add_update_password_policy(
            self, domain_id, request_object, requestslib_kwargs=None):
        """Return response object from the add/update password policy api call

        PUT {{AUTH_URL}}/v2.0/RAX-AUTH/domains/{{USER_DOMAIN}}/passwordPolicy
        """
        url = self.url + const.PASSWORD_POLICY_URL.format(domain_id=domain_id)
        return self.request('PUT', url, request_entity=request_object,
                            requestslib_kwargs=requestslib_kwargs)

    def get_password_policy(self, domain_id, requestslib_kwargs=None):
        """Return response object from the add/update password policy api call

        GET {{AUTH_URL}}/v2.0/RAX-AUTH/domains/{{USER_DOMAIN}}/passwordPolicy
        """
        url = self.url + const.PASSWORD_POLICY_URL.format(domain_id=domain_id)
        return self.request('GET', url, requestslib_kwargs=requestslib_kwargs)

    def delete_password_policy(self, domain_id, requestslib_kwargs=None):
        """Return response object from the add/update password policy api call

        DELETE
        {AUTH_URL}}/v2.0/RAX-AUTH/domains/{{USER_DOMAIN}}/passwordPolicy
        """
        url = self.url + const.PASSWORD_POLICY_URL.format(domain_id=domain_id)
        return self.request(
            'DELETE', url, requestslib_kwargs=requestslib_kwargs)

    def change_password(self, request_object, requestslib_kwargs=None):
        """Return response object from the change password api call

        POST
        {AUTH_URL}}/v2.0/RAX-AUTH/domains/{{USER_DOMAIN}}/passwordPolicy
        """
        url = self.url + const.CHANGE_PASSWORD_URL
        return self.request('POST', url, request_entity=request_object,
                            requestslib_kwargs=requestslib_kwargs)

    def change_administrators(self, domain_id, request_object,
                              requestslib_kwargs=None):
        """Return response object from list users in domain
        PUT
        {{AUTH_URL}}/v2.0/RAX-AUTH/domains/{{USER_DOMAIN}/domainAdministratorChange
        """
        url = self.url + const.ADMIN_CHANGE_URL.format(domain_id=domain_id)
        return self.request('PUT', url, request_entity=request_object,
                            requestslib_kwargs=requestslib_kwargs)

    def move_domain_to_rcn(self, domain_id, rcn, requestslib_kwargs=None):
        """
        PUT /v2.0/RAX-AUTH/domains/{domain_id}/rcn/{rcn}
        """
        url = self.url + const.MOVE_DOMAIN_TO_RCN_URL.format(
            domain_id=domain_id, rcn=rcn)
        return self.request('PUT', url, requestslib_kwargs=requestslib_kwargs)

    def add_user_group_to_domain(self, domain_id, request_object,
                                 requestslib_kwargs=None):
        """
        POST /v2.0/RAX-AUTH/domains/{domain_id}/groups
        """
        url = self.url + const.ADD_USER_GROUP_TO_DOMAIN_URL.format(
            domain_id=domain_id)
        return self.request(
            'POST', url, request_entity=request_object,
            requestslib_kwargs=requestslib_kwargs)

    def list_user_groups_for_domain(self, domain_id, option=None,
                                    requestslib_kwargs=None):
        """
        GET /v2.0/RAX-AUTH/domains/{domain_id}/groups
        """
        url = self.url + const.LIST_USER_GROUPS_FOR_DOMAIN_URL.format(
            domain_id=domain_id)
        return self.request('GET', url, params=option,
                            requestslib_kwargs=requestslib_kwargs)

    def get_user_group_for_domain(
            self, domain_id, group_id, requestslib_kwargs=None):
        """
        GET /v2.0/RAX-AUTH/domains/{domain_id}/groups/{group_id}
        """
        url = self.url + const.RD_USER_GROUP_TO_DOMAIN_URL.format(
            domain_id=domain_id, group_id=group_id)
        return self.request('GET', url, requestslib_kwargs=requestslib_kwargs)

    def add_tenant_role_assignments_to_user_group(
            self, domain_id, group_id, request_object,
            requestslib_kwargs=None):
        """
        PUT RAX-AUTH/domains/{domainId}/groups/{groupId}/roles
        """
        url = self.url + (
            const.ADD_TENANT_ROLE_ASSIGNMENTS_TO_USER_GROUP_URL.format(
                domain_id=domain_id, group_id=group_id))
        return self.request('PUT', url, request_entity=request_object,
                            requestslib_kwargs=requestslib_kwargs)

    def delete_user_group_from_domain(
            self, domain_id, group_id, requestslib_kwargs=None):
        """
        DELETE /v2.0/RAX-AUTH/domains/{domain_id}/groups/{group_id}
        """
        url = self.url + const.RD_USER_GROUP_TO_DOMAIN_URL.format(
            domain_id=domain_id, group_id=group_id)
        return self.request(
            'DELETE', url, requestslib_kwargs=requestslib_kwargs)

    def delete_tenant_role_assignments_from_user_group(
            self, domain_id, group_id, role_id, requestslib_kwargs=None):
        """
        DELETE RAX-AUTH/domains/{domainId}/groups/{groupId}/roles/{role_id}
        """
        url = self.url + (
            const.DELETE_TENANT_ROLE_ASSIGNMENTS_FROM_USER_GROUP_URL.format(
                domain_id=domain_id, group_id=group_id, role_id=role_id))
        return self.request(
            'DELETE', url, requestslib_kwargs=requestslib_kwargs)

    def list_tenant_role_assignments_to_user_group(
            self, domain_id, group_id, requestslib_kwargs=None):
        """
        GET RAX-AUTH/domains/{domainId}/groups/{groupId}/roles
        """
        url = self.url + (
            const.LIST_TENANT_ROLE_ASSIGNMENTS_FOR_USER_GROUP_URL.format(
                domain_id=domain_id, group_id=group_id))
        return self.request('GET', url, requestslib_kwargs=requestslib_kwargs)

    def add_user_to_user_group(
            self, domain_id, group_id, user_id, requestslib_kwargs=None):
        """
        PUT /v2.0/RAX-AUTH/domains/{domain_id}/groups/{group_id}/
        users/{user_id}
        """
        url = self.url + const.ADD_USER_TO_USER_GROUP_URL.format(
            domain_id=domain_id, group_id=group_id, user_id=user_id)
        return self.request('PUT', url, requestslib_kwargs=requestslib_kwargs)

    def update_user_group(self, domain_id, group_id,
                          request_object, requestslib_kwargs=None):
        """
        PUT /v2.0/RAX-AUTH/domains/{domain_id}/groups/{group_id}
        """
        url = self.url + const.UPDATE_USER_GROUP_URL.format(
            domain_id=domain_id, group_id=group_id)
        return self.request(
            'PUT', url, request_entity=request_object,
            requestslib_kwargs=requestslib_kwargs)

    def list_users_in_user_group_for_domain(
            self, domain_id, group_id, requestslib_kwargs=None):
        """
        GET /v2.0/RAX-AUTH/domains/{domain_id}/groups/{group_id}/users
        """
        url = self.url + const.LIST_USERS_IN_USER_GROUP_FOR_DOMAIN_URL.format(
            domain_id=domain_id, group_id=group_id)
        return self.request('GET', url, requestslib_kwargs=requestslib_kwargs)

    def delete_user_from_user_group(
            self, domain_id, group_id, user_id, requestslib_kwargs=None):
        """
        DELETE /v2.0/RAX-AUTH/domains/{domain_id}/groups/{group_id}/
        users/{user_id}
        """
        url = self.url + const.DELETE_USER_FROM_USER_GROUP_URL.format(
            domain_id=domain_id, group_id=group_id, user_id=user_id)
        return self.request(
            'DELETE', url, requestslib_kwargs=requestslib_kwargs)

    def add_role_to_user_group_for_tenant(
            self, domain_id, group_id, role_id, tenant_id,
            requestslib_kwargs=None):
        """
        PUT RAX-AUTH/domains/{domainId}/groups/{groupId}/roles/{roleId}/
        tenants/{tenantId}
        """
        url = self.url + (
            const.ADD_ROLE_TO_USER_GROUP_ON_TENANT_URL.format(
                domain_id=domain_id, group_id=group_id, role_id=role_id,
                tenant_id=tenant_id))
        return self.request('PUT', url, requestslib_kwargs=requestslib_kwargs)

    def delete_role_from_user_group_on_tenant(
            self, domain_id, group_id, role_id, tenant_id,
            requestslib_kwargs=None):
        """
        DELETE RAX-AUTH/domains/{domainId}/groups/{groupId}/roles/{roleId}/
        tenants/{tenantId}
        """
        url = self.url + (
            const.DELETE_ROLE_FROM_USER_GROUP_ON_TENANT_URL.format(
                domain_id=domain_id, group_id=group_id, role_id=role_id,
                tenant_id=tenant_id))
        return self.request(
            'DELETE', url, requestslib_kwargs=requestslib_kwargs)
