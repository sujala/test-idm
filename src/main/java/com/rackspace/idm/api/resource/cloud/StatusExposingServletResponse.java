package com.rackspace.idm.api.resource.cloud;

import com.sun.jersey.core.util.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.TeeOutputStream;
import org.springframework.mock.web.DelegatingServletOutputStream;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.*;
import java.util.zip.GZIPInputStream;

/**
 * Created with IntelliJ IDEA.
 * User: jacob
 * Date: 1/4/13
 * Time: 3:33 PM
 * To change this template use File | Settings | File Templates.
 */

public class StatusExposingServletResponse extends HttpServletResponseWrapper {
    private int httpStatus;
    private ByteArrayOutputStream stream = new ByteArrayOutputStream();

    public StatusExposingServletResponse(HttpServletResponse response) throws IOException {
        super(response);
    }

    @Override
    public void sendError(int sc) throws IOException {
        httpStatus = sc;
        super.sendError(sc);
    }

    @Override
    public void sendError(int sc, String msg) throws IOException {
        httpStatus = sc;
        super.sendError(sc, msg);
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        return new DelegatingServletOutputStream(
                new TeeOutputStream(super.getOutputStream(), stream)
        );
    }

    @Override
    public void setStatus(int sc) {
        httpStatus = sc;
        super.setStatus(sc);
    }

    public String getBody() throws IOException {
        byte[] bytes = stream.toByteArray();

        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
            GZIPInputStream gzis = new GZIPInputStream(bais);

            StringWriter writer = new StringWriter();
            IOUtils.copy(gzis, writer, "UTF-8");
            return writer.toString();
        } catch (IOException e){
        }
        return new String(bytes, "UTF-8");
    }

    public int getStatus() {
        return httpStatus;
    }
}
