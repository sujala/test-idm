package com.rackspace.idm.api.resource.cloud.v20.multifactor;

import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
class V1SessionIdVersion1 implements SessionIdVersion {
    private String prefix = "v1.00"; //must be exact 5 characters long
    private String separator = "|";
    private String authenticatedByDelimiter = ",";

    @Override
    public String getPrefix() {
        return prefix;
    }

    public String getPlaintext(SessionId session) {
        Assert.notNull(session);
        Assert.hasText(session.getUserId());
        Assert.notNull(session.getCreatedDate());
        Assert.notNull(session.getExpirationDate());
        Assert.notEmpty(session.getAuthenticatedBy());

        StringBuilder plaintextBuilder = new StringBuilder();
        plaintextBuilder.append(prefix).append(separator);
        plaintextBuilder.append(session.getUserId()).append(separator);
        plaintextBuilder.append(EncryptedSessionIdReaderWriter.RFC822DATEFORMAT.print(session.getCreatedDate())).append(separator);
        plaintextBuilder.append(EncryptedSessionIdReaderWriter.RFC822DATEFORMAT.print(session.getExpirationDate())).append(separator);
        plaintextBuilder.append(StringUtils.join(session.getAuthenticatedBy(), authenticatedByDelimiter));

        String plaintext = plaintextBuilder.toString();
        return plaintext;
    }

    public SessionId getSessionId(String plaintext) {
        Assert.hasText(plaintext);

        String[] split = StringUtils.split(plaintext, separator);
        Assert.isTrue(split.length == 5, "Invalid sessionId format");
        Assert.isTrue(prefix.equals(split[0]), "Invalid sessionId format");

        V1SessionId sessionId = new V1SessionId();

        sessionId.setVersion(split[0]);
        sessionId.setUserId(split[1]);
        sessionId.setCreatedDate(EncryptedSessionIdReaderWriter.RFC822DATEFORMAT.parseDateTime(split[2]));
        sessionId.setExpirationDate(EncryptedSessionIdReaderWriter.RFC822DATEFORMAT.parseDateTime(split[3]));

        List<String> authBy = new ArrayList<String>();
        if (split[4] != null) {
            authBy.addAll(Arrays.asList(StringUtils.split(split[4], authenticatedByDelimiter)));
        }
        sessionId.setAuthenticatedBy(authBy);

        return sessionId;
    }


}
