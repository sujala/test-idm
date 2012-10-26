package com.rackspace.idm.api.resource.pagination;

import spock.lang.Shared
import spock.lang.Specification
import com.rackspace.idm.domain.entity.User
import org.apache.commons.configuration.Configuration
import org.springframework.test.context.ContextConfiguration
import com.sun.jersey.api.spring.Autowire
import org.springframework.beans.factory.annotation.Autowired

/**
 * Created with IntelliJ IDEA.
 * User: jacob
 * Date: 10/10/12
 * Time: 5:24 PM
 * To change this template use File | Settings | File Templates.
 */

/*
        Configuration config = Mock();
        userPaginator.config = config;
        config.getInt("ldap.paging.limit.default") >> globalLimitDefault
        config.getInt("ldap.paging.limit.max") >> globalLimitMax
*/

@ContextConfiguration(locations = "classpath:app-config.xml")
public class DefaultPaginatorTest extends Specification {

    @Shared def randomness = UUID.randomUUID()
    @Shared def globalOffset = 0
    @Shared def globalLimitDefault = 10
    @Shared def globalLimitMax = 50

    @Autowired
    private DefaultPaginator<User> userPaginator;

    def setupSpec() {
    }

    def "setting limit to 0 sets to default"() {
        given:
        Configuration config = Mock();
        userPaginator.config = config;
        config.getInt("ldap.paging.offset.default") >> globalLimitDefault
        config.getInt("ldap.paging.limit.max") >> globalLimitMax

        when:
        userPaginator.limit(0)

        then:
        userPaginator.limit == globalLimitDefault
    }

    def "setting limit to negative value sets to 1"() {
        given:
        Configuration config = Mock();
        userPaginator.config = config;
        config.getInt("ldap.paging.limit.default") >> globalLimitDefault
        config.getInt("ldap.paging.limit.max") >> globalLimitMax

        when:
        userPaginator.limit(-5)

        then:
        userPaginator.limit() == globalLimitDefault
    }

    def "setting limit above max sets value to max"() {
        given:
        Configuration config = Mock();
        userPaginator.config = config;
        config.getInt("ldap.paging.offset.default") >> globalLimitDefault
        config.getInt("ldap.paging.limit.max") >> globalLimitMax

        when:
        userPaginator.limit(100)

        then:
        userPaginator.limit() == globalLimitMax
    }

    def "setting limit to max sets value to max"() {
        given:
        Configuration config = Mock();
        userPaginator.config = config;
        config.getInt("ldap.paging.offset.default") >> globalLimitDefault
        config.getInt("ldap.paging.limit.max") >> globalLimitMax

        when:
        userPaginator.limit(globalLimitMax)

        then:
        userPaginator.limit() == globalLimitMax
    }

    def "setting limit to default sets value to default"() {
        given:
        Configuration config = Mock();
        userPaginator.config = config;
        config.getInt("ldap.paging.offset.default") >> globalLimitDefault
        config.getInt("ldap.paging.limit.max") >> globalLimitMax

        when:
        userPaginator.limit(globalLimitDefault)

        then:
        userPaginator.limit() == globalLimitDefault
    }
}