package com.rackspace.idm.api.resource.mapper

import com.rackspace.idm.domain.entity.AuthenticatedByMethodGroup
import com.rackspace.idm.domain.entity.LdapTokenRevocationRecord
import com.rackspace.idm.domain.sql.entity.SqlTokenRevocationRecord
import com.rackspace.idm.domain.sql.mapper.impl.TokenRevocationMapper
import spock.lang.Shared
import spock.lang.Specification

class TokenRevocationMapperTest extends Specification {

    @Shared TokenRevocationMapper mapper = new TokenRevocationMapper()

    def "test ldap to sql for revocation by token - toSql"() {
        given:
        LdapTokenRevocationRecord ldapTrr = new LdapTokenRevocationRecord();
        ldapTrr.setId(getRandomUUID())
        ldapTrr.setTargetToken(getRandomUUID())
        ldapTrr.setCreateTimestamp(getRandomDate())
        ldapTrr.setTargetIssuedToId(getRandomUUID())
        ldapTrr.setTargetCreatedBefore(getRandomDate())

        when:
        SqlTokenRevocationRecord sqlTrr = mapper.toSQL(ldapTrr)

        then:
        sqlTrr.getId() == ldapTrr.getId()
        sqlTrr.getTargetToken() == ldapTrr.getTargetToken()
        sqlTrr.getCreateTimestamp() == ldapTrr.getCreateTimestamp()
        sqlTrr.getTargetIssuedToId() == ldapTrr.getTargetIssuedToId()
        sqlTrr.getTargetCreatedBefore() == ldapTrr.getTargetCreatedBefore()
        sqlTrr.getTargetAuthenticatedByMethodGroups() != null
        sqlTrr.getTargetAuthenticatedByMethodGroups().isEmpty()
    }

    def "test ldap to sql for revocation by auth-by values - toSql"() {
        given:
        LdapTokenRevocationRecord ldapTrr = new LdapTokenRevocationRecord();
        ldapTrr.setId(getRandomUUID())
        ldapTrr.setCreateTimestamp(getRandomDate())
        ldapTrr.setTargetIssuedToId(getRandomUUID())
        ldapTrr.setTargetCreatedBefore(getRandomDate())
        def authByGroups = [AuthenticatedByMethodGroup.PASSWORD, AuthenticatedByMethodGroup.PASSWORD_PASSCODE]
        ldapTrr.setTargetAuthenticatedByMethodGroups(authByGroups)

        when:
        SqlTokenRevocationRecord sqlTrr = mapper.toSQL(ldapTrr)

        then:
        sqlTrr.getId() == ldapTrr.getId()
        sqlTrr.getTargetToken() == null
        sqlTrr.getCreateTimestamp() == ldapTrr.getCreateTimestamp()
        sqlTrr.getTargetIssuedToId() == ldapTrr.getTargetIssuedToId()
        sqlTrr.getTargetCreatedBefore() == ldapTrr.getTargetCreatedBefore()
        sqlTrr.getTargetAuthenticatedByMethodGroups() == ldapTrr.getTargetAuthenticatedByMethodGroups()
    }

    def "test ldap to sql for revocation by token - fromSql"() {
        given:
        SqlTokenRevocationRecord sqlTrr = new SqlTokenRevocationRecord();
        sqlTrr.setId(getRandomUUID())
        sqlTrr.setTargetToken(getRandomUUID())
        sqlTrr.setCreateTimestamp(getRandomDate())
        sqlTrr.setTargetIssuedToId(getRandomUUID())
        sqlTrr.setTargetCreatedBefore(getRandomDate())

        when:
        LdapTokenRevocationRecord ldapTrr = mapper.fromSQL(sqlTrr)

        then:
        sqlTrr.getId() == ldapTrr.getId()
        sqlTrr.getTargetToken() == ldapTrr.getTargetToken()
        sqlTrr.getCreateTimestamp() == ldapTrr.getCreateTimestamp()
        sqlTrr.getTargetIssuedToId() == ldapTrr.getTargetIssuedToId()
        sqlTrr.getTargetCreatedBefore() == ldapTrr.getTargetCreatedBefore()
        sqlTrr.getTargetAuthenticatedByMethodGroups() != null
        sqlTrr.getTargetAuthenticatedByMethodGroups().isEmpty()
    }

    def "test ldap to sql for revocation by auth-by values - fromSql"() {
        given:
        SqlTokenRevocationRecord sqlTrr = new SqlTokenRevocationRecord();
        sqlTrr.setId(getRandomUUID())
        sqlTrr.setCreateTimestamp(getRandomDate())
        sqlTrr.setTargetIssuedToId(getRandomUUID())
        sqlTrr.setTargetCreatedBefore(getRandomDate())
        def authByGroups = [AuthenticatedByMethodGroup.PASSWORD, AuthenticatedByMethodGroup.PASSWORD_PASSCODE]
        sqlTrr.setTargetAuthenticatedByMethodGroups(authByGroups)

        when:
        LdapTokenRevocationRecord ldapTrr = mapper.fromSQL(sqlTrr)

        then:
        sqlTrr.getId() == ldapTrr.getId()
        sqlTrr.getTargetToken() == null
        sqlTrr.getCreateTimestamp() == ldapTrr.getCreateTimestamp()
        sqlTrr.getTargetIssuedToId() == ldapTrr.getTargetIssuedToId()
        sqlTrr.getTargetCreatedBefore() == ldapTrr.getTargetCreatedBefore()
        sqlTrr.getTargetAuthenticatedByMethodGroups() == ldapTrr.getTargetAuthenticatedByMethodGroups()
    }

    def getRandomUUID(prefix='') {
        String.format("%s%s", prefix, UUID.randomUUID().toString().replace('-', ''))
    }

    def getRandomDate() {
        return new Date().plus(new Random().nextInt(365))
    }

}
