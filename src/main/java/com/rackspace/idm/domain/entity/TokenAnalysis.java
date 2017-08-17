package com.rackspace.idm.domain.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Represents a generic password policy.
 */
@JsonRootName(value = "tokenAnalysis")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@Setter
public class TokenAnalysis {
    private static final Logger logger = LoggerFactory.getLogger(TokenAnalysis.class);

    private Boolean tokenDecryptable;
    private Boolean tokenValid;
    private Boolean tokenExpired;
    private Boolean tokenRevoked;

    private Token token;

    private User user;

    private ImpersonatedUser impersonatedUser;

    private List<Trr> trrs;

    private ExceptionReport decryptionException;

    private static ObjectMapper mapper;

    private static final String IMPERSONATION = "IMPERSONATION";
    private static final String USER = "USER";
    private static final String RACKER = "RACKER";
    private static final String PROVISIONED_USER = "PROVISIONED_USER";
    private static final String FEDERATED_USER = "FEDERATED_USER";
    private static final String UNKNOWN = "UNKNOWN";

    static {
        mapper = new ObjectMapper().registerModule(new Jdk8Module()).registerModule(new JavaTimeModule());
        mapper.enable(DeserializationFeature.UNWRAP_ROOT_VALUE);
        mapper.enable(SerializationFeature.WRAP_ROOT_VALUE);
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    public TokenAnalysis() {
    }

    public static TokenAnalysis fromException(java.lang.Exception ex) {
        TokenAnalysis tokenAnalysis = new TokenAnalysis();
        tokenAnalysis.setTokenDecryptable(false);
        tokenAnalysis.setTokenRevoked(false);
        tokenAnalysis.setTokenExpired(false);
        tokenAnalysis.setTokenValid(false);
        tokenAnalysis.decryptionException = ExceptionReport.fromException(ex);

        return tokenAnalysis;
    }

    public static TokenAnalysis fromEntities(ScopeAccess token, BaseUser user, BaseUser imp, Domain userDomain, Domain impersonatedUserDomain, List<LdapTokenRevocationRecord> trrs) {
        TokenAnalysis tokenAnalysis = new TokenAnalysis();

        Token tokenFromSA = Token.fromScopeAccess(token);

        if(IMPERSONATION.equals(tokenFromSA.getType()) && imp != null && impersonatedUserDomain != null) {
            tokenAnalysis.setImpersonatedUser(ImpersonatedUser.fromEntityImpersonatedUser(imp, impersonatedUserDomain));
        }
        tokenAnalysis.setToken(tokenFromSA);
        tokenAnalysis.setUser(User.fromEntityUser(user, userDomain));
        tokenAnalysis.setTrrs(Trr.fromTokenRevocationRecords(trrs));

        tokenAnalysis.setTokenDecryptable(tokenAnalysis.token != null);
        tokenAnalysis.setTokenExpired(token != null && token.isAccessTokenExpired());
        tokenAnalysis.setTokenRevoked(!CollectionUtils.isEmpty(trrs));

        tokenAnalysis.setTokenValid(BooleanUtils.isTrue(tokenAnalysis.getTokenDecryptable())
                && BooleanUtils.isFalse(tokenAnalysis.getTokenExpired())
                && BooleanUtils.isFalse(tokenAnalysis.getTokenRevoked()));

        return tokenAnalysis;
    }

    public String toJson() throws JsonProcessingException {
        String json = mapper.writeValueAsString(this);
        return json;
    }

    public static TokenAnalysis fromJson(String json) throws IOException {
        if (StringUtils.isEmpty(json)) {
            return null;
        }
        TokenAnalysis tokenAnalysis = mapper.readValue(json, TokenAnalysis.class);
        return tokenAnalysis;
    }

    @Getter
    @Setter
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ExceptionReport {
        private String message;

        public static ExceptionReport fromException(java.lang.Exception ex) {
            ExceptionReport exception = new ExceptionReport();

            if (ex != null) {
                exception.setMessage(ex.getMessage());
            }

            return exception;
        }

        public ExceptionReport() {
        }
    }


    @Getter
    @Setter
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Token {
        private String token;

        private Date expiration;

        private Date creation;

        private String scope;

        private String type;

        private List<String> authenticatedBy;

        public static Token fromScopeAccess(ScopeAccess sa) {
            Token token = new Token();

            if (sa != null) {
                if(sa instanceof ImpersonatedScopeAccess){
                    token.setType(IMPERSONATION);
                } else if (sa instanceof UserScopeAccess) {
                    token.setType(USER);
                } else if (sa instanceof RackerScopeAccess) {
                    token.setType(RACKER);
                }
                token.setToken(sa.getAccessTokenString());
                token.setExpiration(sa.getAccessTokenExp());
                token.setCreation(sa.getCreateTimestamp());
                token.setScope(sa.getScope());
                token.setAuthenticatedBy(sa.getAuthenticatedBy());
            }

            return token;
        }

        public Token() {
        }
    }

    @JsonRootName(value = "user")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Getter
    @Setter
    public static class User {
        private String type;
        private String id;
        private String username;
        private String domain;
        private Boolean enabled;
        private String federatedIdp;
        private Boolean domainEnabled;

        public User() {
        }

        public static User fromEntityUser(BaseUser entityUser, Domain domain) {
            User user = new User();

            if (entityUser != null) {
                user.id = entityUser.getId();
                user.username = entityUser.getUsername();
                user.domain = entityUser.getDomainId();
                user.enabled = !entityUser.isDisabled();
                user.domainEnabled = domain != null ? domain.getEnabled() : null;

                if (entityUser instanceof Racker) {
                    user.type = RACKER;
                } else if (entityUser instanceof com.rackspace.idm.domain.entity.User) {
                    user.type = PROVISIONED_USER;
                } else if (entityUser instanceof FederatedUser) {
                    user.type = FEDERATED_USER;
                    FederatedUser federatedUser = (FederatedUser) entityUser;
                    user.federatedIdp = federatedUser.getFederatedIdpUri();
                } else {
                    user.type = UNKNOWN;
                }
            }
            return user;
        }
    }

    @JsonRootName(value = "impersonatedUser")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Getter
    @Setter
    public static class ImpersonatedUser {
        private String type;
        private String id;
        private String username;
        private String domain;
        private Boolean enabled;
        private String federatedIdp;
        private Boolean domainEnabled;

        public ImpersonatedUser() {
        }

        public static ImpersonatedUser fromEntityImpersonatedUser(BaseUser entityUser, Domain domain) {
            ImpersonatedUser impersonatedUser = new ImpersonatedUser();

            if (entityUser != null) {
                impersonatedUser.id = entityUser.getId();
                impersonatedUser.username = entityUser.getUsername();
                impersonatedUser.domain = entityUser.getDomainId();
                impersonatedUser.enabled = !entityUser.isDisabled();
                impersonatedUser.domainEnabled = domain != null ? domain.getEnabled() : null;

                if (entityUser instanceof Racker) {
                    impersonatedUser.type = RACKER;
                } else if (entityUser instanceof com.rackspace.idm.domain.entity.User) {
                    impersonatedUser.type = PROVISIONED_USER;
                } else if (entityUser instanceof FederatedUser) {
                    impersonatedUser.type = FEDERATED_USER;
                    FederatedUser federatedUser = (FederatedUser) entityUser;
                    impersonatedUser.federatedIdp = federatedUser.getFederatedIdpUri();
                } else {
                    impersonatedUser.type = UNKNOWN;
                }
            }
            return impersonatedUser;
        }
    }

    @Getter
    @Setter
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Trr {
        private String id;
        private String targetUserId;
        private String tokenAuthenticatedByGroups;
        private Date   tokenCreatedBefore;
        private String token;
        private String identityProviderId;

        public static Trr fromTokenRevocationRecord(LdapTokenRevocationRecord ldapTokenRevocationRecord) {
            Trr trr = new Trr();

            if (ldapTokenRevocationRecord != null) {
                trr.setId(ldapTokenRevocationRecord.getId());
                trr.setTargetUserId(ldapTokenRevocationRecord.getTargetIssuedToId());
                trr.setTokenCreatedBefore(ldapTokenRevocationRecord.getTargetCreatedBefore());
                trr.setTokenAuthenticatedByGroups(StringUtils.join(TokenRevocationRecordUtil.getAuthByStringsFromAuthByGroups(ldapTokenRevocationRecord.getTargetAuthenticatedByMethodGroups()), ","));
                trr.setToken(ldapTokenRevocationRecord.getTargetToken());
                trr.setIdentityProviderId(ldapTokenRevocationRecord.getIdentityProviderId());
            }

            return trr;
        }

        public static List<Trr> fromTokenRevocationRecords(List<LdapTokenRevocationRecord> ldapTokenRevocationRecords) {
            List<Trr> trrs = new ArrayList<>(ldapTokenRevocationRecords.size());

            if (!CollectionUtils.isEmpty(ldapTokenRevocationRecords)) {
                for (LdapTokenRevocationRecord tokenRevocationRecord : ldapTokenRevocationRecords) {
                    trrs.add(fromTokenRevocationRecord(tokenRevocationRecord));
                }
            }

            return trrs;
        }

        public Trr() {
        }
    }
}
