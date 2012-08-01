package com.rackspace.idm.api.resource.cloud;

import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HttpHeadersAcceptXml implements HttpHeaders {
	
	private HttpHeaders headers;
	
	public HttpHeadersAcceptXml(HttpHeaders headers) {
		this.headers = headers;
	}
	
	@Override
	public MultivaluedMap<String, String> getRequestHeaders() {
		MultivaluedMap<String, String> result = headers.getRequestHeaders();
		if (result.containsKey(HttpHeaders.ACCEPT)) {
			result.putSingle(HttpHeaders.ACCEPT, MediaType.APPLICATION_XML);
			return result;
		}
		return result;
	}
	
	@Override
	public List<String> getRequestHeader(String name) {
		if (HttpHeaders.ACCEPT.equalsIgnoreCase(name)) {
			List<String> result = new ArrayList<String>();
			result.add(MediaType.APPLICATION_XML);
			return result;
		}
		
		return headers.getRequestHeader(name);
	}
	
	@Override
	public MediaType getMediaType() {
		return headers.getMediaType();
	}
	
	@Override
	public Locale getLanguage() {
		return headers.getLanguage();
	}
	
	@Override
	public Map<String, Cookie> getCookies() {
		return headers.getCookies();
	}
	
	@Override
	public List<MediaType> getAcceptableMediaTypes() {
		List<MediaType> result = new ArrayList<MediaType>();
		result.add(MediaType.APPLICATION_XML_TYPE);
		return result;
	}
	
	@Override
	public List<Locale> getAcceptableLanguages() {
		return headers.getAcceptableLanguages();
	}
}
