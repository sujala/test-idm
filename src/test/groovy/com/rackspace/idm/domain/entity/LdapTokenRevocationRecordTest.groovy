package com.rackspace.idm.domain.entity

import org.apache.commons.collections.CollectionUtils
import spock.lang.Specification


class LdapTokenRevocationRecordTest extends Specification {

    def "verify flattening and unflattening of authenticated by"() {
        TokenRevocationRecord record = new LdapTokenRevocationRecord();

        when:
        record.setTargetAuthenticatedBy(unflattened)

        then:
        def actualFlattened = record.getInternalTargetAuthenticatedBy()
        CollectionUtils.isEqualCollection(actualFlattened, expectedFlattened)

        when:
        List<Set<String>> actualUnflattened = record.getTargetAuthenticatedBy()

        then:
        verifyUnflattened(unflattened, actualUnflattened)

        where:
        unflattened                             | expectedFlattened
        []                                      | []
        [[] as Set]                             | [LdapTokenRevocationRecord.AUTHENTICATED_BY_EMPTY_SET_SUBSTITUTE]
        [["A"] as Set]                          | ["A"]
        [["A"] as Set, ["B"] as Set]            | ["A","B"]
        [["A", "B"] as Set]                     | ["A,B"]
        [["A", "B"] as Set, ["C", "D"] as Set]  | ["A,B", "C,D"]
        [["A", "B"] as Set, [] as Set]          | ["A,B", LdapTokenRevocationRecord.AUTHENTICATED_BY_EMPTY_SET_SUBSTITUTE]
        [["A", "B"] as Set, [] as Set, [TokenRevocationRecord.AUTHENTICATED_BY_WILDCARD_VALUE] as Set] | ["A,B", LdapTokenRevocationRecord.AUTHENTICATED_BY_EMPTY_SET_SUBSTITUTE, TokenRevocationRecord.AUTHENTICATED_BY_WILDCARD_VALUE]
    }

    def void verifyUnflattened(List<Set<String>> expected, List<Set<String>> actual) {
        assert expected.size() == actual.size()
        for (int i=0; i<expected.size(); i++) {
            assert CollectionUtils.isEqualCollection(expected.get(i), actual.get(i))
        }
    }
}
