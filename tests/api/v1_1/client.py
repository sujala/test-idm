"""Identity API Base Client."""
from cafe.engine.http import client
from tests.api.v1_1 import requests
from tests.api.v1_1 import response
from tests.package.johny import constants as const


class IdentityAPIClient(client.AutoMarshallingHTTPClient):
    """Client methods for the identity api v1.1 endpoints.
    """

    def __init__(self, url, serialize_format, deserialize_format):
        super(IdentityAPIClient, self).__init__()
        self.url = url
        self.default_headers[const.CONTENT_TYPE] = (
            const.CONTENT_TYPE_VALUE.format(serialize_format))
        self.default_headers[const.ACCEPT] = (
            const.ACCEPT_ENCODING_VALUE.format(deserialize_format))

        self.serialize_format = serialize_format
        self.deserialize_format = deserialize_format

    def auth_user_key(self, user_name, key, headers=None,
                      requestslib_kwargs=None):
        """Returns response object from auth"""
        url = self.url + const.V11_AUTH_URL
        user_key_cred = requests.Credentials(user_name=user_name, key=key)
        resp = self.request(method='POST', url=url,
                            request_entity=user_key_cred,
                            headers=headers,
                            response_entity_type=response.Auth,
                            requestslib_kwargs=requestslib_kwargs)

        return resp

    def auth_user_password(self, user_name, password, headers=None,
                           requestslib_kwargs=None):
        url = self.url + const.V11_AUTH_ADMIN_URL
        user_key_cred = requests.PasswordCredentials(user_name=user_name,
                                                     password=password)
        resp = self.request(method='POST', url=url,
                            request_entity=user_key_cred,
                            headers=headers,
                            response_entity_type=response.Auth,
                            requestslib_kwargs=requestslib_kwargs)

        return resp

    def auth_mosso_key(self, mosso_id, key, headers=None,
                       requestslib_kwargs=None):
        """Returns response object from auth"""
        url = self.url + const.V11_AUTH_ADMIN_URL
        mosso_key_cred = requests.MossoCredentials(mosso_id=mosso_id, key=key)
        resp = self.request(method='POST', url=url,
                            request_entity=mosso_key_cred,
                            headers=headers,
                            response_entity_type=response.Auth,
                            requestslib_kwargs=requestslib_kwargs)

        return resp

    def auth_nast_key(self, nast_id, key, headers=None,
                      requestslib_kwargs=None):
        """Returns response object from auth"""
        url = self.url + const.V11_AUTH_ADMIN_URL
        nast_key_cred = requests.NastCredentials(nast_id=nast_id, key=key)
        resp = self.request(method='POST', url=url,
                            request_entity=nast_key_cred,
                            headers=headers,
                            response_entity_type=response.Auth,
                            requestslib_kwargs=requestslib_kwargs)

        return resp

    def validate_token(self, token_id, belongs_to=None, type=None,
                       headers=None,
                       requestslib_kwargs=None):
        """
        Returns response object from validate token api call
        GET /v1.1/auth/token/{token_id}?type=CLOUD
        In case of XML response, add a json() method to the response object
        that will create a JSON equivalent of the XML response
        """
        url = self.url + const.V11_TOKEN_VALIDATION_URL.format(
            token_id=token_id)
        params = {}
        params['belongsTo'] = belongs_to if belongs_to else None
        params['type'] = type if type else None

        resp = self.request(method='GET', url=url, params=params,
                            response_entity_type=response.Token,
                            headers=headers,
                            requestslib_kwargs=requestslib_kwargs)
        return resp

    def add_user(self, request_object, headers=None, requestslib_kwargs=None):
        """
        POST '/v1.1/users'
        """
        url = self.url + const.V11_USER_URL
        resp = self.request(method='POST', url=url,
                            request_entity=request_object,
                            response_entity_type=response.User,
                            headers=headers,
                            requestslib_kwargs=requestslib_kwargs)
        return resp

    def get_user(self, user_id, headers=None, requestslib_kwargs=None):
        """
        GET '/v1.1/users/{user_id}
        """
        url = self.url + const.V11_GET_USER_URL.format(user_id=user_id)
        resp = self.request(method='GET', url=url,
                            response_entity_type=response.User,
                            headers=headers,
                            requestslib_kwargs=requestslib_kwargs)
        return resp

    def delete_user(self, user_id, requestslib_kwargs=None):
        """
        DELETE '/v1.1/users/{user_id}
        """
        url = self.url + const.V11_DELETE_USER_URL.format(user_id=user_id)
        resp = self.request(method='DELETE', url=url,
                            requestslib_kwargs=requestslib_kwargs)
        return resp
