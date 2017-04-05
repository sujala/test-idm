"""Identity API Base Client."""
from cafe.engine.http import client
from tests.package.johny import constants as const


class IdentityAPIClient(client.AutoMarshallingHTTPClient):
    """Client methods for the identity api v1.0 endpoints.
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

    def authenticate(self, x_auth_user, x_auth_key):
        headers = {const.X_AUTH_USER: x_auth_user,
                   const.X_AUTH_KEY: x_auth_key}
        # resp = self.get(self.base_url, headers=headers)
        resp = self.request(method='GET', url=self.url,
                            headers=headers)
        return resp

    def authenticate_storage(self, x_storage_user, x_storage_pass):
        headers = {const.X_STORAGE_USER: x_storage_user,
                   const.X_STORAGE_PASS: x_storage_pass}
        resp = self.get(self.url, headers=headers)
        return resp
