package com.rackspace.idm.api.resource;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.tuckey.web.filters.urlrewrite.utils.StringUtils;

import com.rackspace.idm.api.error.ApiError;
import com.rackspace.idm.domain.entity.FilterParam;
import com.rackspace.idm.domain.entity.FilterParam.FilterParamName;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspace.idm.validation.InputValidator;
import com.sun.jersey.core.provider.EntityHolder;

public class ParentResource {

    private final InputValidator inputValidator;
    
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    
    @Autowired
    public ParentResource(InputValidator inputValidator) {
    	this.inputValidator = inputValidator;
    }
    
    protected void validateRequestBody(EntityHolder<?> holder) {
        if (!holder.hasEntity()) {
            throw new BadRequestException("Request body missing.");
        }
    }
    
    protected void validateDomainObject(Object inputParam) {
        ApiError err = inputValidator.validate(inputParam);
        if (err != null) {
            throw new BadRequestException(err.getMessage());
        }
    }
    
	protected FilterBuilder createFilterBuilder() {
		return new FilterBuilder();
	}
	
    protected Logger getLogger() {
        return logger;
    }
    
	protected class FilterBuilder {
		List<FilterParam> filters = new ArrayList<FilterParam> ();
		
		public void addFilter(FilterParamName paramName, String value) {
			if (!StringUtils.isBlank(value)) {
				filters.add(new FilterParam(paramName,value));
			}
		}
		
		public FilterParam[] getFilters() {
			return filters.toArray(new FilterParam[] {});
		}
	}
}
