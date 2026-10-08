package com.pet.platform.identity.application.bootstrap;

import com.pet.platform.identity.application.IdentityNames;
import com.fasterxml.jackson.annotation.JsonIgnoreType;
import java.util.Arrays;

/** 短生命周期初始化输入；不会作为HTTP DTO或日志内容。 */
@JsonIgnoreType
public final class BootstrapRequest implements AutoCloseable {
    private final String tenantCode,tenantName,loginName,storeCode,storeName;
    private final char[] password;
    public BootstrapRequest(String tenantCode,String tenantName,String loginName,char[] password,String storeCode,String storeName) {
        this.tenantCode=IdentityNames.tenantCode(tenantCode);this.tenantName=IdentityNames.name(tenantName);this.loginName=IdentityNames.loginName(loginName);
        if ((storeCode==null)!=(storeName==null)) throw new IllegalArgumentException("门店编码和名称须同时提供");
        this.storeCode=storeCode==null ? null : IdentityNames.tenantCode(storeCode);this.storeName=storeName==null ? null : IdentityNames.name(storeName);
        this.password=password==null ? null : password.clone();
    }
    public String tenantCode() { return tenantCode; }
    public String tenantName() { return tenantName; }
    public String loginName() { return loginName; }
    public String storeCode() { return storeCode; }
    public String storeName() { return storeName; }
    char[] password() { return password; }
    @Override public void close() { if(password!=null) Arrays.fill(password,'\0'); }
    @Override public String toString() { return "BootstrapRequest[受限初始化输入]"; }
}
