package com.rackspace.idm.exceptions;

public class XACMLRequestCreationException extends Exception {

	String errorMessage; 
	
	public XACMLRequestCreationException(String message) {
		this.errorMessage = message;
	}
	
}
