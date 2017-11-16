package com.rackspace.idm.event

import spock.lang.Specification
import spock.lang.Unroll

class NewRelicCustomAttributesEnumTest extends Specification {

    @Unroll
    def "amIInListWithWildcardSupport: Returns true for all values when wildcard present: enumVal: #enumVal; enabledList: #enabledList"() {
        expect:
        enumVal.amIInListWithWildcardSupport(enabledList)

        where:
        [enumVal, enabledList] << [NewRelicCustomAttributesEnum.values().flatten(), [["*"] as Set, ["hello", "*"] as Set, ["*", "*"] as Set]].combinations()
    }

    /**
     * Mix up the enabled lists to include other values, different casing of same values, etc
     * @return
     */
    @Unroll
    def "amIInListWithWildcardSupport: Returns true for all values when present: enumVal: #enumVal; enabledList: #enabledList"() {
        expect:
        enumVal.amIInListWithWildcardSupport(enabledList)

        where:
        enumVal << [NewRelicCustomAttributesEnum.CALLER_ID, NewRelicCustomAttributesEnum.EVENT_ID,NewRelicCustomAttributesEnum.EFFECTIVE_CALLER_ID]
        enabledList << [[enumVal.newRelicAttributeName] as Set, ["hello", enumVal.newRelicAttributeName.toLowerCase()] as Set, [enumVal.newRelicAttributeName, enumVal.newRelicAttributeName] as Set]
    }

    def "amIInListWithWildcardSupport: Returns false when list does not include attribute name or wildcard"() {
        expect:
        !NewRelicCustomAttributesEnum.CALLER_ID.amIInListWithWildcardSupport(["hello", "world", NewRelicCustomAttributesEnum.CALLER_TOKEN.newRelicAttributeName] as Set)
    }



}
