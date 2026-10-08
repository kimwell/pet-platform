package com.pet.platform.identity.application.bootstrap;

/** 专用命令端口；普通Spring应用不装配此能力。 */
public interface BootstrapWriter {
    BootstrapResult initialize(String tenantCode,String tenantName,String loginName,String encodedPassword,String storeCode,String storeName);
}
