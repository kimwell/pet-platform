package com.pet.platform.shared.exception;

import com.pet.platform.shared.api.ErrorCode;

public final class ConflictException extends BusinessException {
    public ConflictException() { this(ErrorCode.BUSINESS_STATE_CONFLICT); }

    public ConflictException(ErrorCode code) {
        super(code);
        if (code.status().value() != 409) {
            throw new IllegalArgumentException("冲突异常必须使用已登记的 409 错误代码");
        }
    }
}
