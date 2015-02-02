package com.rackspace.idm.domain.entity

import org.apache.commons.collections.CollectionUtils
import org.apache.commons.lang.StringUtils
import spock.lang.Specification


class LdapTokenRevocationRecordTest extends Specification {

    def "verify flattening and unflattening of authenticated by"() {
        TokenRevocationRecord record = new LdapTokenRevocationRecord();

        when:
        record.setTargetAuthenticatedByMethodGroups(unflattened)

        then:
        def actualFlattened = record.getInternalTargetAuthenticatedBy()
        CollectionUtils.isEqualCollection(actualFlattened, expectedFlattened)

        when:
        List<AuthenticatedByMethodGroup> actualUnflattened = record.getTargetAuthenticatedByMethodGroups()

        then:
        verifyUnflattened(unflattened, actualUnflattened)

        where:
        unflattened                                                                 | expectedFlattened
        []                                                                          | []
        [AuthenticatedByMethodGroup.NULL]                                           | [LdapTokenRevocationRecord.AUTHENTICATED_BY_EMPTY_LIST_SUBSTITUTE]
        [AuthenticatedByMethodGroup.ALL]                                            | [LdapTokenRevocationRecord.AUTHENTICATED_BY_ALL_SUBSTITUTE]
        [AuthenticatedByMethodGroup.PASSWORD]                                       | [AuthenticatedByMethodEnum.PASSWORD.value]
        [AuthenticatedByMethodGroup.PASSWORD, AuthenticatedByMethodGroup.APIKEY]    | [AuthenticatedByMethodEnum.PASSWORD.value, AuthenticatedByMethodEnum.APIKEY.value]
        [AuthenticatedByMethodGroup.PASSWORD_PASSCODE]                              | [AuthenticatedByMethodEnum.PASSWORD.value + "," + AuthenticatedByMethodEnum.PASSCODE.value]
        [AuthenticatedByMethodGroup.PASSWORD_PASSCODE, AuthenticatedByMethodGroup.PASSWORD_PASSCODE] | [AuthenticatedByMethodEnum.PASSWORD.value + "," + AuthenticatedByMethodEnum.PASSCODE.value, AuthenticatedByMethodEnum.PASSWORD.value + "," + AuthenticatedByMethodEnum.PASSCODE.value]
    }

    def void verifyUnflattened(List<AuthenticatedByMethodGroup> expected, List<AuthenticatedByMethodGroup> actual) {
        assert expected.size() == actual.size()
        for (int i=0; i<expected.size(); i++) {
            assert expected.get(i).matches(actual.get(i))
        }
    }
}
