package com.rackspace.idm.documentation.util;

import java.lang.reflect.Type;

import javax.xml.datatype.XMLGregorianCalendar;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class XmlGregorianCalendarSerializer implements JsonSerializer<XMLGregorianCalendar> {

	public JsonElement serialize(XMLGregorianCalendar src, Type typeOfSrc, JsonSerializationContext context) {
	    return new JsonPrimitive(src.toString());
	}
}
