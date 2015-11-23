package com.rackspace.idm.exception;

import com.rackspace.idm.ErrorCodes;
import lombok.Getter;

public class MigrationReadOnlyIdmException extends IdmException {

    public MigrationReadOnlyIdmException() {
        super(ErrorCodes.ERROR_CODE_MIGRATION_READ_ONLY_ENTITY_MESSAGE, ErrorCodes.ERROR_CODE_MIGRATION_READ_ONLY_ENTITY_CODE);
    }

    public MigrationReadOnlyIdmException(Throwable cause) {
        super(ErrorCodes.ERROR_CODE_MIGRATION_READ_ONLY_ENTITY_MESSAGE, ErrorCodes.ERROR_CODE_MIGRATION_READ_ONLY_ENTITY_CODE, cause);
    }
}
