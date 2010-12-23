package com.rackspace.idm.docs;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import com.rackspace.idm.jaxb.ObjectFactory;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.json.JettisonMappedXmlDriver;
import com.thoughtworks.xstream.io.json.JsonWriter;

public class SampleGenerator {

    protected ObjectFactory of = new ObjectFactory();
    protected XStream xstream = new XStream(new JettisonMappedXmlDriver());

    public SampleGenerator() {
        xstream.setMode(XStream.NO_REFERENCES);
    }

    protected void marshalToFiles(Object objToMarshal, String objectAlias)
        throws JAXBException, IOException {
        // XML
//        JAXBContext.newInstance(objToMarshal.getClass()).createMarshaller()
//            .marshal(objToMarshal, new File("samples/" + objectAlias + ".xml"));
//
//        // JSON
//        xstream.alias(objectAlias, objToMarshal.getClass());
//        xstream.marshal(objToMarshal, new JsonWriter(new FileWriter(new File(
//            "samples/" + objectAlias + ".json"))));

    }

}