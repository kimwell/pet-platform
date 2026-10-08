package com.pet.platform.shared.exception;

import com.pet.platform.shared.api.ErrorCode;

public class ResourceNotFoundException extends BusinessException {
    public ResourceNotFoundException() { super(ErrorCode.RESOURCE_NOT_FOUND); }
}
