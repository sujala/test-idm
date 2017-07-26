package com.rackspace.idm.api.resource.cloud

import com.rackspace.idm.api.filter.MultiReadHttpServletRequest
import org.apache.commons.configuration.Configuration
import org.apache.commons.mail.ByteArrayDataSource
import org.mockito.Mock
import spock.lang.Shared
import spock.lang.Specification

import javax.servlet.FilterChain
import javax.servlet.ServletInputStream
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse


class LoggerFilterTest extends Specification {

    @Shared LoggerFilter filter
    @Shared def request, response, filterChain
    @Shared ServletInputStream inputStream

    def setupSpec() {
        filter = new LoggerFilter()
    }

    def setup() {
        request = Mock(HttpServletRequest)
        response = Mock(HttpServletResponse)

        filterChain = Mock(FilterChain)

        filter.config = Mock(Configuration)
        filter.analyticsLogger = Mock(AnalyticsLogger)

        inputStream = Mock(ServletInputStream)
    }

    def "test analytic logger feature flag - true" () {
        when:
        filter.doFilter(request, response, filterChain)

        then:
        1 * filter.config.getBoolean("analytics.logger.enabled", false) >> true
        1 * request.getInputStream() >> inputStream
        inputStream.read(_) >> -1
        1 * filter.analyticsLogger.log(_,_,_,_,_,_,_,_,_,_,_,_,_)
    }

    def "test analytic logger feature flag - false" () {
        when:
        filter.doFilter(request, response, filterChain)

        then:
        1 * filter.config.getBoolean("analytics.logger.enabled", false) >> false
        1 * request.getInputStream() >> inputStream
        inputStream.read(_) >> -1
        0 * filter.analyticsLogger.log(_,_,_,_,_,_,_,_,_,_,_,_,_)
    }
}
