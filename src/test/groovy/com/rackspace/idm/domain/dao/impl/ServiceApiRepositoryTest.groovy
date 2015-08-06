package com.rackspace.idm.domain.dao.impl

import com.rackspace.idm.domain.dao.ServiceApiDao;
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Specification
import org.springframework.test.context.ContextConfiguration
import com.rackspace.idm.domain.entity.ServiceApi

/**
 * Created by IntelliJ IDEA.
 * User: jorge
 * Date: 11/7/12
 * Time: 3:15 PM
 * To change this template use File | Settings | File Templates.
 */
@ContextConfiguration(locations = "classpath:app-config.xml")
class ServiceApiRepositoryTest extends Specification {

    @Autowired
    private ServiceApiDao serviceApiDao

    def "list serviceApis"(){
        when:
        List<ServiceApi> baseUrlList = serviceApiDao.getServiceApis().collect()

        then:
        baseUrlList != null

    }
}
