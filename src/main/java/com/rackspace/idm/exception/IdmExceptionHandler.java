package com.rackspace.idm.exception;

import javax.ws.rs.core.Response;

public interface IdmExceptionHandler {
    Response.ResponseBuilder badRequestExceptionResponse(String message);

    Response.ResponseBuilder notAuthenticatedExceptionResponse(String message);

    Response.ResponseBuilder forbiddenExceptionResponse(String errMsg);

    Response.ResponseBuilder notFoundExceptionResponse(String message);

    Response.ResponseBuilder notImplementedExceptionResponse();

    Response.ResponseBuilder tenantConflictExceptionResponse(String message);

    Response.ResponseBuilder userDisabledExceptionResponse(String message);

    Response.ResponseBuilder conflictExceptionResponse(String message);

    Response.ResponseBuilder unrecoverableExceptionResponse(String message);

    Response.ResponseBuilder serviceExceptionResponse();

    Response.ResponseBuilder exceptionResponse(Exception ex);

    Response.ResponseBuilder serviceUnavailableExceptionResponse(String message);

    int exceptionToHttpStatus(Exception ex);
}
