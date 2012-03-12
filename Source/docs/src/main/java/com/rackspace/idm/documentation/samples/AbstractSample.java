package com.rackspace.idm.documentation.samples;

import javax.xml.bind.JAXBElement;


public abstract class AbstractSample {

	public abstract JAXBElement<? extends Object> getJaxbObject();
	
	/**
	 * gets the name of the sample file to be written to.
	 * specific samples can override this method
	 * @return
	 */
	public String getSampleName() {
		return this.getClass().getSimpleName();
	}
	
	/**
	 * flag to indicate if the xml sample file should be overriden.
	 * specific samples can override this method.
	 * @return
	 */
	public boolean overwriteXMLSample() {
		return true;
	}
	
	/**
	 * flag to indicate if the json sample file should be overriden.
	 * specific samples can override this method.
	 * @return
	 */
	public boolean overwriteJSONSample() {
		return true;
	}
}
