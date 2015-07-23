package com.rackspace.idm.exception;

import com.rackspace.idm.ErrorCodes;
import lombok.Getter;

public class MigrationReadOnlyIdmException extends IdmException {

    @Getter
    private String errorCode = ErrorCodes.ERROR_CODE_MIGRATION_READ_ONLY_ENTITY_CODE;

    public MigrationReadOnlyIdmException() {
        super(ErrorCodes.ERROR_CODE_MIGRATION_READ_ONLY_ENTITY_MESSAGE);
    }

    public MigrationReadOnlyIdmException(Throwable cause) {
        super(ErrorCodes.ERROR_CODE_MIGRATION_READ_ONLY_ENTITY_MESSAGE, cause);
    }

    @Override
    public String getMessage()  {
        return ErrorCodes.generateErrorCodeFormattedMessage(errorCode, super.getMessage());
    }
}
