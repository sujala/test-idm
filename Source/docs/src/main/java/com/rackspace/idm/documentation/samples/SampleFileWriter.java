package com.rackspace.idm.documentation.samples;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.datatype.XMLGregorianCalendar;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.rackspace.idm.documentation.util.XmlGregorianCalendarSerializer;

/**
 * This class is responsible for writing serializable entity objects to file
 *
 */
public class SampleFileWriter {

	private static JAXBContext jaxbContext;

	// default location is current directory that application is run
	private String fileOutputLocation = "."; 
	
	public void writeSample(final AbstractSample sample) throws Throwable {
		JAXBContext context = getJaxbContext();
		
		// write xml
		if (sample.overwriteXMLSample()) {
			File file = new File(fileOutputLocation + "/" + sample.getSampleName() + ".xml");
			
			Marshaller marshaller = context.createMarshaller();
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			marshaller.marshal(sample.getJaxbObject(), file);
		}
		
		// write json
		if (sample.overwriteJSONSample()) {
			GsonBuilder builder = new GsonBuilder();
			builder.registerTypeAdapter(XMLGregorianCalendar.class, new XmlGregorianCalendarSerializer());
			Gson gson = builder.setPrettyPrinting().create();
			
			String jsonOutput = gson.toJson(sample.getJaxbObject().getValue());
			
			FileWriter fstream = new FileWriter(fileOutputLocation + "/" + sample.getSampleName() + ".json");
			BufferedWriter out = new BufferedWriter(fstream);
			out.write(jsonOutput);
			out.close();
		}
	}


	private JAXBContext getJaxbContext() throws Throwable {
		if (jaxbContext == null) {
			jaxbContext = JAXBContext.newInstance("com.rackspace.docs.identity.api.ext.rax_ga.v1");
		}
		
		return jaxbContext;
	}
	
	public String getFileOutputLocation() {
		return fileOutputLocation;
	}

	public void setFileOutputLocation(String fileOutputLocation) {
		this.fileOutputLocation = fileOutputLocation;
	}
}
