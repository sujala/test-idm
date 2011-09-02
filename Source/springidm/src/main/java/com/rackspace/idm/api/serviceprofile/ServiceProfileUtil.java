package com.rackspace.idm.api.serviceprofile;

import javax.ws.rs.core.MediaType;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.rackspace.idm.jaxb.Link;
import com.rackspace.idm.jaxb.MediaTypeList;
import com.rackspace.idm.jaxb.Relation;

@Component
@Scope(value="singleton")
public class ServiceProfileUtil {

	Link createLink(final Relation rel, final String type, final String href, final String title) {
		Link link = new Link();
		link.setRel(rel);
		link.setHref(href);
		link.setType(type);
		link.setTitle(title);
		return link;
	}

	/**
	 * helper method that creates media type list from single media type
	 * 
	 * @param mediaType
	 * @return
	 */
	MediaTypeList createMediaTypeList(com.rackspace.idm.jaxb.MediaType mediaType) {
		MediaTypeList mediaTypes = new MediaTypeList();
		mediaTypes.getMediaTypes().add(mediaType);
		return mediaTypes;
	}

	com.rackspace.idm.jaxb.MediaType createMediaType(final String describedByLink, final String type) {
		com.rackspace.idm.jaxb.MediaType mediaType = new com.rackspace.idm.jaxb.MediaType();
		mediaType.setBase(MediaType.APPLICATION_XML);
		mediaType.setType(type);
		mediaType.getLinks().add(
				createLink(Relation.DESCRIBEDBY, MediaType.APPLICATION_XML,
						describedByLink, null));
		return mediaType;
	}
}
