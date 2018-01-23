package com.rackspace.idm.event;

import com.sun.jersey.spi.container.ContainerRequest;
import lombok.Getter;
import lombok.Setter;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Consolidates information about the request along with helper APIs. Must be threadsafe as could be processed by a different thread in a asynchronous manner
 */
@Getter
@Setter
public class IdentityApiResourceRequest {
    private Method apiMethod;
    private ContainerRequest containerRequest;

    public IdentityApiResourceRequest(Method apiMethod, ContainerRequest containerRequest) {
        if (apiMethod.getAnnotation(IdentityApi.class) == null) {
            throw new IllegalArgumentException("Method must be annotated with IdentityApi");
        }

        this.apiMethod = apiMethod;
        this.containerRequest = containerRequest;
    }

    public IdentityApi getIdentityApiAnnotation() {
        IdentityApi identityApi = apiMethod.getAnnotation(IdentityApi.class);
        return identityApi;
    }

    public ReportableQueryParams getReportableQueryParamsAnnotation() {
        return apiMethod.getAnnotation(ReportableQueryParams.class);
    }

    public SecureResourcePath getSecureResourcePathAnnotation() {
        SecureResourcePath secureResourcePath = apiMethod.getAnnotation(SecureResourcePath.class);
        return secureResourcePath;
    }

    /**
     * Get the resource type of the request as specified by the {@link IdentityApi} annotation. Returns null if the
     * annotation doesn't exist on the resource.
     *
     * @return
     */
    public ApiResourceType getResourceType() {
        IdentityApi identityApi = getIdentityApiAnnotation();
        return identityApi != null ? identityApi.apiResourceType() : null;
    }

    /**
     * Whether or not the resource associated with the request is marked as being deprecated
     *
     * @return
     */
    public boolean isResourceDeprecated() {
        return apiMethod.getAnnotation(Deprecated.class) != null;
    }

    /**
     * Lowercases all values included in {@link ReportableQueryParams#securedQueryParams()}
     *
     * @return
     */
    public Set<String> getSecuredReportableParamNames() {
        ReportableQueryParams reportableQueryParams = getReportableQueryParamsAnnotation();

        Set<String> finalParams = Collections.emptySet();
        if (reportableQueryParams != null) {
            finalParams = lowerCaseParams(reportableQueryParams.securedQueryParams());
        }

        return finalParams;
    }

    /**
     * Lowercases all values included in {@link ReportableQueryParams#includedQueryParams()}}
     *
     * @return
     */
    public Set<String> getIncludedReportableParamNames() {
        ReportableQueryParams reportableQueryParams = getReportableQueryParamsAnnotation();

        Set<String> finalParams = Collections.emptySet();
        if (reportableQueryParams != null) {
            String[] paramAr = reportableQueryParams.includedQueryParams();
            finalParams = lowerCaseParams(reportableQueryParams.includedQueryParams());

            // Must remove any that are secured
            finalParams.removeAll(getSecuredReportableParamNames());
        }

        return finalParams;
    }

    /**
     * Lowercases all values included in {@link ReportableQueryParams#unsecuredQueryParams()}
     *
     * @return
     */
    public Set<String> getUnsecuredReportableParamNames() {
        ReportableQueryParams reportableQueryParams = getReportableQueryParamsAnnotation();

        Set<String> finalParams = Collections.emptySet();
        if (reportableQueryParams != null) {
            finalParams = lowerCaseParams(reportableQueryParams.unsecuredQueryParams());

            // Must remove any that are secured or included
            finalParams.removeAll(getSecuredReportableParamNames());
            finalParams.removeAll(getIncludedReportableParamNames());
        }

        return finalParams;
    }

    private Set<String> lowerCaseParams(String[] strings) {
        Set<String> loweredParams = new HashSet<>();
        for (String s : strings) {
            loweredParams.add(s.toLowerCase());
        }

        return loweredParams;
    }
}
