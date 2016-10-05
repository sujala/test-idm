"""Config Parser for API Tests."""

from cafe.engine.models import data_interfaces


class IdentityConfig(data_interfaces.ConfigSectionInterface):
    """Defines the config values for Identity."""
    SECTION_NAME = 'identity'

    @property
    def base_url(self):
        """identity endpoint."""
        return self.get('base_url')

    @property
    def cloud_url(self):
        """
        identity cloud url, which gets appended to the base url
        when hitting individual node.
        """
        return self.get('cloud_url')

    @property
    def api_version(self):
        """identity API version."""
        return self.get('api_version')

    @property
    def service_admin_auth_token(self):
        """Auth Token for a Service Admin."""
        return self.get('service_admin_auth_token')

    @property
    def identity_admin_user_name(self):
        """Identity Admin User."""
        return self.get('identity_admin_user_name')

    @property
    def identity_admin_password(self):
        """API Key for the Identity Admin User."""
        return self.get('identity_admin_password')


class TestConfig(data_interfaces.ConfigSectionInterface):
    """Defines the config values specific to test execution."""
    SECTION_NAME = 'test_configuration'

    @property
    def serialize_format(self):
        """Format of Requests - can be json or xml."""
        return self.get('serialize_format')

    @property
    def deserialize_format(self):
        """Format of Responses - can be json or xml."""
        return self.get('deserialize_format')

    @property
    def run_hypothesis_tests(self):
        """Flag to indicate if hypothesis tests should be run."""
        run_hypothesis_tests = self.get('run_hypothesis_tests')
        if run_hypothesis_tests.lower() == 'true':
            return True
        else:
            return False

    @property
    def run_service_admin_tests(self):
        """Flag to indicate if tests that require service admin privileges
        should be run."""
        run_service_admin_tests = self.get('run_service_admin_tests')
        if run_service_admin_tests.lower() == 'true':
            return True
        else:
            return False
