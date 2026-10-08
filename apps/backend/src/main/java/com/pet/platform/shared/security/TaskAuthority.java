package com.pet.platform.shared.security;

import com.pet.platform.shared.tenancy.TenantContext;

/** 身份模块实现的可信任务重验端口；证明只能从当前真实会话捕获。 */
public interface TaskAuthority {
    interface Proof { }
    Proof capture(TenantContext context);
    TenantContext revalidate(TenantContext captured,Proof proof);
}
