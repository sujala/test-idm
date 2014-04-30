package com.rackspace.idm.helpers

import org.apache.commons.lang.math.RandomUtils
import org.springframework.stereotype.Component

import javax.annotation.PostConstruct

@Component
class CloudTestUtils {

    private Random random

    @PostConstruct
    def init() {
        random = new Random()
    }

    def getRandomUUID(prefix='') {
        String.format("%s%s", prefix, UUID.randomUUID().toString().replace('-', ''))
    }

    def getRandomInteger() {
        ((Integer)(100000 + random.nextFloat() * 900000))
    }

    def getRandomIntegerString() {
        String.valueOf(getRandomInteger())
    }



}
