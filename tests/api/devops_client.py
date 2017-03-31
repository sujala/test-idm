from cafe.engine.http import client
from tests.package.johny import constants as const


class IdentityDevopsClient(client.AutoMarshallingHTTPClient):

    """Client methods for the identity devops api endpoints."""

    def __init__(self, devops_url, serialize_format, deserialize_format):
        super(IdentityDevopsClient, self).__init__()
        self.devops_url = devops_url
        self.default_headers[const.CONTENT_TYPE] = (
            const.CONTENT_TYPE_VALUE.format(serialize_format))
        self.default_headers[const.ACCEPT_ENCODING] = (
            const.ACCEPT_ENCODING_VALUE.format(deserialize_format))
        self.default_headers[const.ACCEPT] = (
            const.ACCEPT_ENCODING_VALUE.format(deserialize_format))

        self.serialize_format = serialize_format
        self.deserialize_format = deserialize_format

    def get_devops_properties(self, prop_name=None, version=None):
        """
        Returns a response object for get devops properties api call
        GET /devops/props
        :param prop_name: Config property name to look for
        :param version: identity release version to look for all config
        properties which got added in that release
        In case of XML response, add a json() method to the response object
        that will create a JSON equivalent of the XML response
        """
        url = self.devops_url + const.DEVOPS_PROPS_URL

        params = {}
        if prop_name:
            params['name'] = prop_name
        if version:
            params['versions'] = version

        resp = self.request('GET', url, params=params)
        return resp

    def get_feature_flag(self, flag_name):
        """
        Convienence function to make getting a feature flag easier.
        Returns the value of the flag which should be boolean indicating
        if it's on or off
        """
        resp = self.get_devops_properties(
            prop_name=flag_name)
        assert(resp.status_code == 200)
        return resp.json()[const.PROPERTIES][0][const.VALUE]

    def create_devops_prop(self, request_object, requestslib_kwargs=None):
        url = "{0}{1}".format(self.devops_url,
                              const.DEVOPS_PROPS_URL)
        resp = self.request(method='POST', url=url,
                            request_entity=request_object,
                            requestslib_kwargs=requestslib_kwargs)
        return resp

    def update_devops_prop(self, devops_props_id, request_object,
                           requestslib_kwargs=None):
        url = "{0}{1}/{2}".format(self.devops_url,
                                  const.DEVOPS_PROPS_URL,
                                  devops_props_id)
        return self.request(method='PUT', url=url,
                            request_entity=request_object,
                            requestslib_kwargs=requestslib_kwargs)

    def list_devops_props(self, queryparams=None, requestslib_kwargs=None):
        url = "{0}{1}".format(self.devops_url,
                              const.DEVOPS_PROPS_URL)
        resp = self.request(method='GET', url=url, params=queryparams,
                            requestslib_kwargs=requestslib_kwargs)
        return resp

    def delete_devops_prop(self, devops_props_id, requestslib_kwargs=None):
        url = "{0}{1}/{2}".format(self.devops_url,
                                  const.DEVOPS_PROPS_URL,
                                  devops_props_id)
        resp = self.request(method='DELETE', url=url,
                            requestslib_kwargs=requestslib_kwargs)
        return resp
