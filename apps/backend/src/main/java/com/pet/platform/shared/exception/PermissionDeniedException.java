package com.pet.platform.shared.exception;

import com.pet.platform.shared.api.ErrorCode;

public final class PermissionDeniedException extends BusinessException {
    public PermissionDeniedException() { super(ErrorCode.PERMISSION_DENIED); }
}
