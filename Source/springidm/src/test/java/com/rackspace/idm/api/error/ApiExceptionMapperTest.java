package com.rackspace.idm.api.error;

import com.rackspace.idm.exception.*;
import org.junit.Test;

import javax.ws.rs.core.Response;
import java.util.ResourceBundle;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Created by IntelliJ IDEA.
 * User: Hector
 * Date: 11/3/11
 * Time: 2:55 PM
 */
public class ApiExceptionMapperTest {
    ResourceBundle resourceBundle = mock(ResourceBundle.class);
    ApiExceptionMapper apiExceptionMapper = new ApiExceptionMapper(resourceBundle);

    @Test
    public void toResponse_NotProvisionedException_returns403() throws Exception {
        Response response = apiExceptionMapper.toResponse(new NotProvisionedException());
        assertThat("response code", response.getStatus(), equalTo(403));
    }

    @Test
    public void toResponse_NumberFormatException_returns400() throws Exception {
        Response response = apiExceptionMapper.toResponse(new NumberFormatException());
        assertThat("response code", response.getStatus(), equalTo(400));
    }

    @Test
    public void toResponse_BadRequestException_returns400() throws Exception {
        Response response = apiExceptionMapper.toResponse(new BadRequestException());
        assertThat("response code", response.getStatus(), equalTo(400));
    }

    @Test
    public void toResponse_ClassCastException_returns400() throws Exception {
        Response response = apiExceptionMapper.toResponse(new ClassCastException());
        assertThat("response code", response.getStatus(), equalTo(400));
    }

    @Test
    public void toResponse_PermissionConflictException_returns409() throws Exception {
        Response response = apiExceptionMapper.toResponse(new PermissionConflictException());
        assertThat("response code", response.getStatus(), equalTo(409));
    }

    @Test
    public void toResponse_BaseUrlConflictException_returns409() throws Exception {
        Response response = apiExceptionMapper.toResponse(new BaseUrlConflictException());
        assertThat("response code", response.getStatus(), equalTo(409));
    }

    @Test
    public void toResponse_DuplicateClientGroupException_returns409() throws Exception {
        Response response = apiExceptionMapper.toResponse(new DuplicateClientGroupException());
        assertThat("response code", response.getStatus(), equalTo(409));
    }

    @Test
    public void toResponse_CustomerConflictException_returns409() throws Exception {
        Response response = apiExceptionMapper.toResponse(new CustomerConflictException());
        assertThat("response code", response.getStatus(), equalTo(409));
    }

    @Test
    public void toResponse_UserDisabledException_returns403() throws Exception {
        Response response = apiExceptionMapper.toResponse(new UserDisabledException());
        assertThat("response code", response.getStatus(), equalTo(403));
    }

    @Test
    public void toResponse_PasswordValidationException_returns400() throws Exception {
        Response response = apiExceptionMapper.toResponse(new PasswordValidationException());
        assertThat("response code", response.getStatus(), equalTo(400));
    }

    @Test
    public void toResponse_PasswordSelfUpdateTooSoonException_returns409() throws Exception {
        Response response = apiExceptionMapper.toResponse(new PasswordSelfUpdateTooSoonException());
        assertThat("response code", response.getStatus(), equalTo(409));
    }

    @Test
    public void toResponse_StalePasswordException_returns409() throws Exception {
        Response response = apiExceptionMapper.toResponse(new StalePasswordException(""));
        assertThat("response code", response.getStatus(), equalTo(409));
    }

    @Test
    public void toResponse_NotAuthenticatedException_returns401() throws Exception {
        Response response = apiExceptionMapper.toResponse(new NotAuthenticatedException());
        assertThat("response code", response.getStatus(), equalTo(401));
    }

    @Test
    public void toResponse_NotAuthorizedException_returns401() throws Exception {
        Response response = apiExceptionMapper.toResponse(new NotAuthorizedException());
        assertThat("response code", response.getStatus(), equalTo(401));
    }

    @Test
    public void toResponse_CloudAdminAuthorizationException_returns405() throws Exception {
        Response response = apiExceptionMapper.toResponse(new CloudAdminAuthorizationException());
        assertThat("response code", response.getStatus(), equalTo(405));
    }

    @Test
    public void toResponse_ForbiddenException_returns403() throws Exception {
        Response response = apiExceptionMapper.toResponse(new ForbiddenException());
        assertThat("response code", response.getStatus(), equalTo(403));
    }

    @Test
    public void toResponse_NotFoundException_returns404() throws Exception {
        Response response = apiExceptionMapper.toResponse(new NotFoundException());
        assertThat("response code", response.getStatus(), equalTo(404));
    }

    @Test
    public void toResponse_com_sun_jersey_api_NotFoundException_returns404() throws Exception {
        Response response = apiExceptionMapper.toResponse(new com.sun.jersey.api.NotFoundException());
        assertThat("response code", response.getStatus(), equalTo(404));
    }

    @Test
    public void toResponse_DuplicateUsernameException_returns409() throws Exception {
        Response response = apiExceptionMapper.toResponse(new DuplicateUsernameException());
        assertThat("response code", response.getStatus(), equalTo(409));
    }

    @Test
    public void toResponse_DuplicateClientException_returns409() throws Exception {
        Response response = apiExceptionMapper.toResponse(new DuplicateClientException());
        assertThat("response code", response.getStatus(), equalTo(409));
    }

    @Test
    public void toResponse_ClientConflictException_returns409() throws Exception {
        Response response = apiExceptionMapper.toResponse(new ClientConflictException());
        assertThat("response code", response.getStatus(), equalTo(409));
    }

    @Test
    public void toResponse_IdmException_returns500() throws Exception {
        Response response = apiExceptionMapper.toResponse(new IdmException());
        assertThat("response code", response.getStatus(), equalTo(500));
    }
}
