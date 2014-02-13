package com.rackspace.idm.api.resource.cloud.v20.multifactor;

public interface SessionIdReaderWriter {
    SessionId readEncoded(String encodedSessionId);
    String writeEncoded(SessionId sessionIdToEncode);
}
