package com.rackspace.idm.multifactor

import spock.lang.Specification


class PhoneNumberGeneratorTest extends Specification {

    def "generates valid phone numbers"() {
        List<String> errors = new ArrayList<String>();
        int runs = 10000;

        when:
        for (int i=0; i<runs; i++) {
            try {
                PhoneNumberGenerator.randomUSNumber();
            } catch (Exception e) {
                errors.add(e.getMessage());
            }
        }

        then:
        errors.size() == 0
    }
}
