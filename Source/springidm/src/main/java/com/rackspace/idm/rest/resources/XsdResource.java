package com.rackspace.idm.rest.resources;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.URL;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.springframework.stereotype.Component;

import com.rackspace.idm.exceptions.NotFoundException;

@Produces({MediaType.APPLICATION_XML})
@Component
public class XsdResource {
    
    public XsdResource() {}
    
    @GET
    @Path("{filename}")
    public Response getXsdFile(@PathParam("filename") String filename) {
        try {
            ClassLoader loader = this.getClass().getClassLoader();
            URL url = loader.getResource("xsd/" + filename);
            return Response.ok().type(MediaType.APPLICATION_XML).entity(new FileInputStream(url.toString())).build();
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            throw new NotFoundException("File Not Found");
        }
        
    }
}
