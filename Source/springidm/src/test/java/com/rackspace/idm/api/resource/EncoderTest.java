package com.rackspace.idm.api.resource;

import org.hamcrest.Matchers;
import org.junit.Test;

import static org.junit.Assert.assertThat;

/**
 * Created by IntelliJ IDEA.
 * User: Hector
 * Date: 8/31/11
 * Time: 11:38 AM
 */
public class EncoderTest {

    @Test
    public void encode_encodesCorrectly() throws Exception {
        String string = "{tokenId}";
        String encodedUrl = Encoder.encode(string);
        assertThat("encode url", encodedUrl, Matchers.equalTo("%7BtokenId%7D"));
    }

    @Test
    public void encode_urlIsNull_returnsNull() throws Exception {
        String encodeUrl = Encoder.encode(null);
        assertThat("encode url", encodeUrl, Matchers.equalTo(null));
    }
}
