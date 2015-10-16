package com.rackspace.idm.domain.sql.entity;

import com.rackspace.idm.domain.entity.AuthenticatedByMethodGroup;
import com.rackspace.idm.domain.entity.TokenRevocationRecordUtil;
import com.rackspace.idm.domain.entity.TokenRevocationRecord;
import lombok.Data;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Transformer;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
@Entity
@Table(name = "revocation_event")
public class SqlTokenRevocationRecord implements TokenRevocationRecord {

    @Id
    @Column(name = "id", length = 64)
    private String id;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "revoked_at", columnDefinition = "DATETIME")
    private Date createTimestamp;

    @OneToOne(mappedBy = "tokenRevocationRecord", cascade = CascadeType.ALL)
    private SqlTokenRevocationRecordTokenRax accessToken;

    @Column(name = "user_id")
    private String targetIssuedToId;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "issued_before", columnDefinition = "DATETIME")
    private Date targetCreatedBefore;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "tokenRevocationRecord")
    private List<SqlTokenRevocationRecordAuthenticatedByRax> sqlTokenRevocationRecordAuthenticatedBy;

    @Override
    public String getTargetToken() {
        return accessToken == null ? null : accessToken.getToken();
    }

    @Override
    public void setTargetToken(String accessTokenString) {
        SqlTokenRevocationRecordTokenRax tokenRax = new SqlTokenRevocationRecordTokenRax();
        tokenRax.setToken(accessTokenString);
        tokenRax.setId(TokenRevocationRecordUtil.getNextId());
        tokenRax.setTokenRevocationRecord(this);
        accessToken = tokenRax;
    }

    @Override
    public List<AuthenticatedByMethodGroup> getTargetAuthenticatedByMethodGroups() {
        if(CollectionUtils.isEmpty(sqlTokenRevocationRecordAuthenticatedBy)) {
            return new ArrayList<AuthenticatedByMethodGroup>();
        }
        
        List<String> authByList = new ArrayList<String>(CollectionUtils.collect(sqlTokenRevocationRecordAuthenticatedBy, new Transformer<SqlTokenRevocationRecordAuthenticatedByRax, String>() {

            @Override
            public String transform(SqlTokenRevocationRecordAuthenticatedByRax input) {
                return input.getAuthenticatedBy();
            }

        }));

        return TokenRevocationRecordUtil.getAuthdByGroupsFromAuthByStrings(authByList);
    }

    @Override
    public void setTargetAuthenticatedByMethodGroups(List<AuthenticatedByMethodGroup> authenticatedByMethodGroups) {
        List<String> authByStrings = TokenRevocationRecordUtil.getAuthByStringsFromAuthByGroups(authenticatedByMethodGroups);
        sqlTokenRevocationRecordAuthenticatedBy = new ArrayList<SqlTokenRevocationRecordAuthenticatedByRax>();

        for(String authBy : authByStrings) {
            SqlTokenRevocationRecordAuthenticatedByRax authByRax = new SqlTokenRevocationRecordAuthenticatedByRax();
            authByRax.setAuthenticatedBy(authBy);
            authByRax.setTokenRevocationRecord(this);
            authByRax.setId(TokenRevocationRecordUtil.getNextId());
            sqlTokenRevocationRecordAuthenticatedBy.add(authByRax);
        }
    }

}
