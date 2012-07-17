package com.rackspace.idm.api.resource;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertThat;

/**
 * Created by IntelliJ IDEA.
 * User: Hector
 * Date: 8/31/11
 * Time: 11:38 AM
 */
public class EncoderTest {
    Encoder encoder;

    @Before
    public void setUp() throws Exception {
        encoder = new Encoder();
    }

    @Test
    public void encode_encodesCorrectly() throws Exception {
        String string = "{tokenId}";
        String encodedUrl = encoder.encode(string);
        assertThat("encode url", encodedUrl, Matchers.equalTo("%7BtokenId%7D"));
    }

    @Test
    public void encode_urlIsNull_returnsNull() throws Exception {
        String encodeUrl = encoder.encode(null);
        assertThat("encode url", encodeUrl, Matchers.equalTo(null));
    }
}
