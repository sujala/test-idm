package com.rackspace.idm.api.filter

import com.rackspace.idm.domain.entity.AuthorizationContext
import com.rackspace.idm.domain.entity.UserScopeAccess
import com.rackspace.idm.domain.service.AuthorizationService
import com.rackspace.idm.domain.service.ScopeAccessService
import com.unboundid.util.Debug
import org.apache.commons.configuration.BaseConfiguration
import org.apache.commons.configuration.Configuration
import org.joda.time.DateTime
import org.springframework.http.HttpStatus
import org.springframework.mock.web.MockFilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import spock.lang.Specification

import javax.servlet.FilterChain


class LdapLoggingFilterTest extends Specification {

    def "when enable property does not exist no headers added"() {
        setup:
        LdapLoggingFilter filter = new LdapLoggingFilter();
        Configuration config = new BaseConfiguration();
        filter.globalConfig = config;
        FilterChain fc = new MockFilterChain();
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        when:
            filter.doFilterInternal(request, response, fc)

        then:
            !Debug.debugEnabled()
            response.getHeaderNames().size() == 0

    }

    def "when enable property set to false debug is disabled"() {
        setup:
        LdapLoggingFilter filter = new LdapLoggingFilter();
        Configuration config = new BaseConfiguration();
        config.setProperty(LdapLoggingFilter.UNBOUND_LOG_ALLOW_PROP_NAME, false)
        filter.globalConfig = config;
        FilterChain fc = new MockFilterChain();
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        when:
        filter.doFilterInternal(request, response, fc)

        then:
        !Debug.debugEnabled()
        response.getHeaderNames().size() == 0
    }

    def "when enable property set to true but request does not include header not enabled"() {
        setup:
        LdapLoggingFilter filter = new LdapLoggingFilter();
        Configuration config = new BaseConfiguration();
        config.setProperty(LdapLoggingFilter.UNBOUND_LOG_ALLOW_PROP_NAME, true)
        filter.globalConfig = config;
        FilterChain fc = new MockFilterChain();
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        when:
        filter.doFilterInternal(request, response, fc)

        then:
        !Debug.debugEnabled()
        response.getHeaderNames().size() == 0
    }

    def "when enabled without token 401 returned"() {
        setup:
        LdapLoggingFilter filter = new LdapLoggingFilter();
        filter.globalConfig = createLoggingConfig(true)
        FilterChain fc = new MockFilterChain();
        MockHttpServletRequest request = LoggingRequestBuilder.instance.addLoggingHeader().build()
        MockHttpServletResponse response = new MockHttpServletResponse();

        when:
        filter.doFilterInternal(request, response, fc)

        then:
        !Debug.debugEnabled()
        response.getHeaderNames().size() == 1
        response.getHeader(LdapLoggingFilter.HEADER_X_LDAP_LOG_LOCATION) == null
        response.getStatus() == HttpStatus.UNAUTHORIZED.value()
    }

    def "when logging enabled and requested but invalid token supplied results in 401"() {
        setup:
        LdapLoggingFilter filter = new LdapLoggingFilter();
        filter.globalConfig = createLoggingConfig(true)
        filter.scopeAccessService = Mock(ScopeAccessService)
        filter.authorizationService = Mock(AuthorizationService)

        FilterChain fc = new MockFilterChain();
        MockHttpServletRequest request = LoggingRequestBuilder.instance.addLoggingHeader().addLoggingTokenHeader("invalid").build()
        MockHttpServletResponse response = new MockHttpServletResponse();

        when:
        filter.doFilterInternal(request, response, fc)

        then:
        filter.scopeAccessService.getScopeAccessByAccessToken( _) >> null
        filter.authorizationService.getAuthorizationContext(_) >> new AuthorizationContext()
        response.getHeaderNames().size() == 1
        response.getHeader(LdapLoggingFilter.HEADER_X_LDAP_LOG_LOCATION) == null
        response.getStatus() == HttpStatus.UNAUTHORIZED.value()
        !Debug.debugEnabled()
    }

    def "when logging enabled and not requested but invalid token supplied results in no error"() {
        setup:
        LdapLoggingFilter filter = new LdapLoggingFilter();
        filter.globalConfig = createLoggingConfig(true)
        FilterChain fc = new MockFilterChain();
        MockHttpServletRequest request = LoggingRequestBuilder.instance.addLoggingHeader(false).addLoggingTokenHeader("invalid").build()
        MockHttpServletResponse response = new MockHttpServletResponse();

        when:
        filter.doFilterInternal(request, response, fc)

        then:
        !Debug.debugEnabled()
        response.getHeaderNames().size() == 0
    }

    def "when logging enabled and requested and expired token supplied results in 401"() {
        setup:
        LdapLoggingFilter filter = new LdapLoggingFilter();
        filter.globalConfig = createLoggingConfig(true)
        filter.scopeAccessService = Mock(ScopeAccessService)
        filter.authorizationService = Mock(AuthorizationService)
        FilterChain fc = new MockFilterChain();
        MockHttpServletRequest request = LoggingRequestBuilder.instance.addLoggingHeader().addLoggingTokenHeader("invalid").build()
        MockHttpServletResponse response = new MockHttpServletResponse();
        UserScopeAccess scopeAccess = new UserScopeAccess();
        scopeAccess.setAccessTokenExpired()

        when:
        filter.doFilterInternal(request, response, fc)

        then:
        filter.scopeAccessService.getScopeAccessByAccessToken( _) >> scopeAccess
        filter.authorizationService.getAuthorizationContext(_) >> new AuthorizationContext()
        response.getStatus() == HttpStatus.UNAUTHORIZED.value()
        !Debug.debugEnabled()
        response.getHeaderNames().size() == 1
        response.getHeader(LdapLoggingFilter.HEADER_X_LDAP_LOG_LOCATION) == null
    }

    def "when logging enabled and requested and non-sa token supplied results in 403"() {
        setup:
        LdapLoggingFilter filter = new LdapLoggingFilter();
        filter.globalConfig = createLoggingConfig(true)
        filter.scopeAccessService = Mock(ScopeAccessService)
        filter.authorizationService = Mock(AuthorizationService)
        FilterChain fc = new MockFilterChain();
        MockHttpServletRequest request = LoggingRequestBuilder.instance.addLoggingHeader().addLoggingTokenHeader("does not matter").build()
        MockHttpServletResponse response = new MockHttpServletResponse();
        UserScopeAccess scopeAccess = new UserScopeAccess();
        scopeAccess.setAccessTokenExp(new DateTime().plusDays(1).toDate())
        scopeAccess.setAccessTokenString("abc")

        when:
        filter.doFilterInternal(request, response, fc)

        then:
        filter.scopeAccessService.getScopeAccessByAccessToken(_) >> scopeAccess
        filter.authorizationService.authorizeCloudServiceAdmin(_) >> false
        filter.authorizationService.getAuthorizationContext(_) >> new AuthorizationContext()
        response.getStatus() == HttpStatus.FORBIDDEN.value()
        !Debug.debugEnabled()
        response.getHeaderNames().size() == 1
        response.getHeader(LdapLoggingFilter.HEADER_X_LDAP_LOG_LOCATION) == null
    }

    def "when logging enabled and requested and sa token supplied results in headers"() {
        setup:
        LdapLoggingFilter filter = new LdapLoggingFilter();
        filter.globalConfig = createLoggingConfig(true)
        filter.scopeAccessService = Mock(ScopeAccessService)
        filter.authorizationService = Mock(AuthorizationService)
        FilterChain fc = new MockFilterChain();
        MockHttpServletRequest request = LoggingRequestBuilder.instance.addLoggingHeader().addLoggingTokenHeader("does not matter").build()
        MockHttpServletResponse response = new MockHttpServletResponse();
        UserScopeAccess scopeAccess = new UserScopeAccess();
        scopeAccess.setAccessTokenExp(new DateTime().plusDays(1).toDate())
        scopeAccess.setAccessTokenString("abc")

        when:
        filter.doFilterInternal(request, response, fc)

        then:
        filter.scopeAccessService.getScopeAccessByAccessToken(_) >> scopeAccess
        filter.authorizationService.authorizeCloudServiceAdmin(_) >> true
        filter.authorizationService.getAuthorizationContext(_) >> new AuthorizationContext()
        response.getStatus() == HttpStatus.OK.value()
        !Debug.debugEnabled()
        response.getHeaderNames().size() == 1
        response.getHeader(LdapLoggingFilter.HEADER_X_LDAP_LOG_LOCATION) != null
    }

    def "when logging enabled, requested, replace log request, and sa token supplied results in log as response"() {
        setup:
        LdapLoggingFilter filter = new LdapLoggingFilter();
        filter.globalConfig = createLoggingConfig(true)
        filter.scopeAccessService = Mock(ScopeAccessService)
        filter.authorizationService = Mock(AuthorizationService)
        FilterChain fc = new MockFilterChain();
        MockHttpServletRequest request = LoggingRequestBuilder.instance.addLoggingHeader().addReplaceResponseHeader().addLoggingTokenHeader("does not matter").build()
        MockHttpServletResponse response = new MockHttpServletResponse();
        UserScopeAccess scopeAccess = new UserScopeAccess();
        scopeAccess.setAccessTokenExp(new DateTime().plusDays(1).toDate())
        scopeAccess.setAccessTokenString("abc")

        when:
        filter.doFilterInternal(request, response, fc)

        then:
        filter.scopeAccessService.getScopeAccessByAccessToken(_) >> scopeAccess
        filter.authorizationService.authorizeCloudServiceAdmin(_) >> true
        response.getStatus() == HttpStatus.OK.value()
        !Debug.debugEnabled()
        response.getHeaderNames().size() == 3
        response.getHeader(LdapLoggingFilter.HEADER_X_LDAP_LOG_LOCATION) != null
        response.getHeader(LdapLoggingFilter.HEADER_X_LDAP_LOG_SERVICE_STATUS_CODE) != null
        response.getContentAsString().contains("<message>** START  null **</message>") //arbitary but shows log working

    }

    private static class LoggingRequestBuilder {

        MockHttpServletRequest request = new MockHttpServletRequest();

        public static LoggingRequestBuilder getInstance() {
            return new LoggingRequestBuilder();
        }

        def LoggingRequestBuilder addLoggingHeader(boolean value = true) {
            request.addHeader(LdapLoggingFilter.HEADER_CREATE_LDAP_LOG, value);
            return this;
        }

        def LoggingRequestBuilder addReplaceResponseHeader(boolean value = true) {
            request.addHeader(LdapLoggingFilter.HEADER_X_RETURN_LDAP_LOG, value);
            return this;
        }

        def LoggingRequestBuilder addLoggingTokenHeader(String value) {
            request.addHeader(LdapLoggingFilter.HEADER_X_LDAP_LOG_TOKEN, value);
            return this;
        }
        def MockHttpServletRequest build() {
            return request;
        }
    }

    def Configuration createLoggingConfig(boolean allowLogging) {
        Configuration config = new BaseConfiguration();
        config.setProperty(LdapLoggingFilter.UNBOUND_LOG_ALLOW_PROP_NAME, allowLogging)
        return config;
    }
}
