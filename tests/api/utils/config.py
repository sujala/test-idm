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
        """Password for the Identity Admin User."""
        return self.get('identity_admin_password')

    @property
    def identity_admin_apikey(self):
        """API Key for the Identity Admin User."""
        return self.get('identity_admin_apikey')

    @property
    def racker_username(self):
        """Racker Username."""
        return self.get('racker_username')

    @property
    def racker_password(self):
        """Password for the Racker User."""
        return self.get('racker_password')


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
    def domain_id(self):
        """Domain id to test user groups with."""
        return self.get('domain_id')

    @property
    def use_domain_for_user_groups(self):
        """Checks to see if Johny should use specified domain
           or generate one."""
        use_explicit_domain = self.get('domain_id')
        if use_explicit_domain is None or len(use_explicit_domain) == 0:
            return False
        else:
            return True

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

    @property
    def run_local_and_jenkins_only(self):
        """Flag to indicate if tests only run against local and jenkins"""
        run_local_and_jenkins_only = self.get('run_local_and_jenkins_only')
        if run_local_and_jenkins_only.lower() == 'true':
            return True
        else:
            return False
