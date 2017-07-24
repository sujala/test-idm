package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_auth.v1.IdentityProviders
import com.rackspace.idm.api.resource.cloud.v20.json.readers.JSONReaderForRaxAuthIdentityProviders
import spock.lang.Shared
import testHelpers.RootServiceTest

class JSONReaderForIdentityProvidersTest extends RootServiceTest{

    @Shared
    JSONReaderForRaxAuthIdentityProviders jsonReader

    String identityProvidersJSON = "{\n" +
            "  \"RAX-AUTH:identityProviders\": [\n" +
            "    {\n" +
            "      \"id\": \"dedicated\",\n" +
            "      \"federationType\": \"DOMAIN\",\n" +
            "      \"issuer\": \"http://my.rackspace.com\",\n" +
            "      \"description\": \"Identity provider for dedicated\",\n" +
            "      \"approvedDomainGroup\": \"GLOBAL\"\n" +
            "    },\n" +
            "    {\n" +
            "      \"id\": \"9ffce6860b5b411a9d97ca630def3062\",\n" +
            "      \"federationType\": \"RACKER\",\n" +
            "      \"issuer\": \"893e610cfe5e4928ae01adcf1cffd456\",\n" +
            "      \"description\": \"blah\"\n" +
            "    },\n" +
            "    {\n" +
            "      \"approvedDomainIds\": [\n" +
            "        \"983452\"\n" +
            "      ],\n" +
            "      \"id\": \"a5b055835def48b097f6daafe8a1d236\",\n" +
            "      \"federationType\": \"DOMAIN\",\n" +
            "      \"issuer\": \"https://my.issuer.com\",\n" +
            "      \"description\": \"A description\"\n" +
            "    }" +
            "]" +
            "}";

    String emptyIdentityProvidersJSON = "{" +
            "\"RAX-AUTH:identityProviders\" : [" +
            "]" +
            "}";


    def setupSpec() {
        jsonReader = new JSONReaderForRaxAuthIdentityProviders()
    }

    def "can read identityproviders with multiple results"() throws Exception {
        InputStream inputStream = new BufferedInputStream(new ByteArrayInputStream(identityProvidersJSON.getBytes()));

        when: "process"
        IdentityProviders identityProviders = jsonReader.readFrom(IdentityProviders.class, null, null, null, null, inputStream);

        then:
        identityProviders.getIdentityProvider().get(2).getApprovedDomainIds().getApprovedDomainId() != null
        identityProviders.getIdentityProvider().get(2).getApprovedDomainIds().getApprovedDomainId().get(0) == "983452"
    }

    def "can read identityproviders with no results"() throws Exception {
        InputStream inputStream = new BufferedInputStream(new ByteArrayInputStream(emptyIdentityProvidersJSON.getBytes()));

        when: "process"
        IdentityProviders identityProviders = jsonReader.readFrom(IdentityProviders.class, null, null, null, null, inputStream);

        then:
        identityProviders.getIdentityProvider() != null
        identityProviders.getIdentityProvider().size() == 0
    }
}
