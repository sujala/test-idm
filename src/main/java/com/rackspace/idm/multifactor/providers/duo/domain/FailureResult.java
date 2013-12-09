package com.rackspace.idm.multifactor.providers.duo.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * When a duo request fails Duo Security will provide a code, message, and message_detail with more information about the error. The exact meaning and usefulness of these
 * values varies with the specific duo request made (e.g. ping vs create phone)
*/
@Getter
@AllArgsConstructor
public class FailureResult {

    /**
     * The code returned by Duo Security when a request fails.
     */
    private int code;

    /**
     * The message returned by Duo Security when a request fails.
     */
    private String message;

    /**
     * The detailed message returned by Duo Security when a request fails.
     */
    private String message_detail;
}
