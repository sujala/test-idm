package com.rackspace.idm.docs;

import java.io.IOException;

import javax.xml.bind.JAXBException;

import com.rackspace.idm.jaxb.Token;

public class TokenSampleGenerator extends SampleGenerator {
    public static void main(String[] args) throws JAXBException, IOException {
        TokenSampleGenerator tsg = new TokenSampleGenerator();
        tsg.marshalToFiles(tsg.getToken(), "token");
    }

    private Token getToken() {
        Token token = of.createToken();
        token.setExpiresIn(new Integer(3600));
        token.setId("309487987f0892397a9439875900b");
        return token;
    }
}
