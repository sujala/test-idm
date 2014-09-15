package com.rackspace.idm.api.security;

import com.rackspace.idm.domain.entity.EndUser;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.service.IdentityUserService;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DefaultRequestContextHolder implements RequestContextHolder {

    /**
     * The request context that is tied to the current thread.
     */
    @Getter
    @Autowired
    private RequestContext requestContext;

    @Autowired
    private IdentityUserService identityUserService;

    private static final String USER_ID = "UserId";
    private static final String USER = "User";
    private static final String SPRING_CONTEXT_ERROR = "Spring request context error getting %s with %s = %s";

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    /**
     * Checks the context to see if the user has already been loaded. If not, loads the user and sets it in the context
     * for future retrieval.
     *
     * <p>
     *     Note: This is meant to be used against a single user only. As such, once the context has been loaded with the target user any
     *     attempt to use this method to load a different user will result in an exception. This method exists more to support
     *     refactoring existing service calls to start using the RequestContext and to start populating the request context
     *     with more information.
     * </p>
     *
     * @param userId
     * @return
     * @throws java.lang.IllegalStateException if the user stored in the context has a different userId than specified
     */
    @Override
    public EndUser getTargetEndUser(String userId) {
        EndUser endUser = requestContext.getTargetEndUser();
        if (endUser == null) {
            endUser = identityUserService.getEndUserById(userId);
            requestContext.setTargetEndUser(endUser);
        }

        // This check is to verify the Spring Scope "Request" is working properly. This should never
        // fail, but because it's obviously a security concern we should verify it.
        if (endUser != null && !endUser.getId().equalsIgnoreCase(userId)) {
            String errMsg = String.format(SPRING_CONTEXT_ERROR, USER, USER_ID, userId);
            logger.error(errMsg);
            throw new IllegalStateException(errMsg);
        }

        return endUser;
    }

    /**
     * Checks the context to see if the user has already been loaded. If not, loads the user and sets it in the context
     * for future retrieval.
     *
     * <p>
     *     Note: This is meant to be used against a single user only. As such, once the context has been loaded with the target user any
     *     attempt to use this method to load a different user will result in an exception. This method exists more to support
     *     refactoring existing service calls to start using the RequestContext and to start populating the request context
     *     with more information.
     * </p>
     *
     * @param userId
     * @return
     *
     * @throws com.rackspace.idm.exception.NotFoundException If there is no user in the context yet, and the specified userId is not found
     * @throws java.lang.IllegalStateException if the user stored in the context has a different userId than specified
     */
    @Override
    public EndUser getAndCheckTargetEndUser(String userId) {
        EndUser endUser = requestContext.getTargetEndUser();
        if (endUser == null) {
            endUser = identityUserService.checkAndGetEndUserById(userId);
            requestContext.setTargetEndUser(endUser);
        }

        // This check is to verify the Spring Scope "Request" is working properly. This should never
        // fail, but because it's obviously a security concern we should verify it.
        if (!endUser.getId().equalsIgnoreCase(userId)) {
            String errMsg = String.format(SPRING_CONTEXT_ERROR, USER, USER_ID, userId);
            logger.error(errMsg);
            throw new IllegalStateException(errMsg);
        }

        return endUser;
    }

    @Override
    public User getTargetUser(String userId) {
        return (User) getTargetEndUser(userId);
    }

    @Override
    public User checkAndGetTargetUser(String userId) {
        return (User) getAndCheckTargetEndUser(userId);
    }
}
