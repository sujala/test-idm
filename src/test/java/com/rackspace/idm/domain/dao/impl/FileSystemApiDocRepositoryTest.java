package com.rackspace.idm.domain.dao.impl;

import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * Created with IntelliJ IDEA.
 * User: ryan5034
 * Date: 7/5/12
 * Time: 5:39 PM
 * To change this template use File | Settings | File Templates.
 */
public class FileSystemApiDocRepositoryTest {

    FileSystemApiDocRepository fileSystemApiDocRepository;
    FileSystemApiDocRepository spy;

    @Before
    public void setUp() throws Exception {
        fileSystemApiDocRepository = new FileSystemApiDocRepository();
        spy = spy(fileSystemApiDocRepository);
    }

    @Test
    public void getContent_pathIsBlank_returnsBlankString() throws Exception {
        assertThat("returns blank string",fileSystemApiDocRepository.getContent(""),equalTo(""));
    }

    @Test
    public void getContent_invalidPath_returnsBlankString() throws Exception {
        assertThat("returns blank string",fileSystemApiDocRepository.getContent("hello"),equalTo(""));
    }

    @Test
    public void getContent__returnsBlankString() throws Exception {
        assertThat("returns blank string",fileSystemApiDocRepository.getContent("hello"),equalTo(""));
    }

    @Test
    public void convertStringToStream_inputStreamIsNull_returnsBlankString() throws Exception {
        assertThat("returns blank string",fileSystemApiDocRepository.convertStreamToString(null),equalTo(""));
    }

    @Test
    public void convertStringToStream_emptyInputStream_returnsBlankString() throws Exception {
        String body = "";
        assertThat("returns blank string",fileSystemApiDocRepository.convertStreamToString(new ByteArrayInputStream(body.getBytes())),equalTo(""));
    }

    @Test
    public void convertStringToStream_nonEmptyInputStream_returnsStringOfInputStream() throws Exception {
        String body = "test";
        assertThat("returns string",fileSystemApiDocRepository.convertStreamToString(new ByteArrayInputStream(body.getBytes())),equalTo("test"));
    }

    @Test
    public void getContent_withIoExceptionReturnsBlankString() throws Exception {
        when(spy.convertStreamToString(any(InputStream.class))).thenThrow(new IOException());
        assertThat("returns blank string", spy.getContent("somePath"), equalTo(""));
    }
}
