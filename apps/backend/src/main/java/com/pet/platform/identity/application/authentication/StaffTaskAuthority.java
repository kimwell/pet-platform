package com.pet.platform.identity.application.authentication;

import com.pet.platform.shared.security.TaskAuthority;
import com.pet.platform.shared.api.ErrorCode;
import com.pet.platform.shared.exception.BusinessException;
import com.pet.platform.shared.tenancy.*;
import java.util.*;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.*;

/** 私有会话引用不包含Token/CSRF；执行前验证真实设备、数据库版本与当前授权交集。 */
@Component
public final class StaffTaskAuthority implements TaskAuthority {
    private record Claim(UUID tenant,UUID employee,UUID session,long security,long tenantSecurity) implements Proof {
        @Override public String toString(){return "Claim[受限任务证明]";}
    }
    private final StaffAuthentication auth;
    private final StaffSessionPort sessions;
    public StaffTaskAuthority(StaffAuthentication auth,StaffSessionPort sessions){this.auth=auth;this.sessions=sessions;}
    @Override public Proof capture(TenantContext context) {
        if(!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes))throw error(ErrorCode.PERMISSION_DENIED);
        var current=StaffHttpAuthentication.current(attributes.getRequest());
        if(current==null || !context.tenantId().equals(current.identity().tenantId()) || !context.principalId().equals(current.identity().employeeId()) || !context.sessionId().equals(current.session().sessionId()))throw error(ErrorCode.PERMISSION_DENIED);
        if(current.identity().passwordChangeRequired())throw error(ErrorCode.PASSWORD_CHANGE_REQUIRED);
        return new Claim(context.tenantId(),context.principalId(),current.session().sessionId(),current.session().securityVersion(),current.session().tenantSecurityVersion());
    }
    @Override public TenantContext revalidate(TenantContext captured,Proof proof) {
        if(!(proof instanceof Claim claim) || !claim.tenant().equals(captured.tenantId()) || !claim.employee().equals(captured.principalId()))throw error(ErrorCode.PERMISSION_DENIED);
        var current=auth.loadForSession(claim.tenant(),claim.employee()).orElseThrow(() -> error(ErrorCode.SESSION_REVOKED));
        if(current.securityVersion()!=claim.security() || current.tenantSecurityVersion()!=claim.tenantSecurity() || current.passwordChangeRequired())throw error(ErrorCode.SESSION_REVOKED);
        if(!sessions.isActive(claim.tenant(),claim.employee(),claim.session()))throw error(ErrorCode.SESSION_REVOKED);
        var permission=current.grants().get(captured.permissionCode());
        if(permission==null)throw error(ErrorCode.PERMISSION_DENIED);
        var scope=intersection(captured.dataScope(),permission);
        var stores=new HashSet<>(captured.authorizedStoreIds());stores.retainAll(current.authorizedStoreIds());
        if(captured.currentStoreId()!=null && (!stores.contains(captured.currentStoreId()) || (!scope.types().contains(DataScopeType.TENANT) && !scope.storeIds().contains(captured.currentStoreId()))))throw error(ErrorCode.PERMISSION_DENIED);
        return new TenantContext(captured.tenantId(),captured.principalType(),captured.principalId(),captured.sessionId(),current.authorizationVersion(),captured.permissionCode(),scope,stores,captured.currentStoreId(),captured.traceId(),captured.purpose());
    }
    private static DataScope intersection(DataScope a,DataScope b) {
        if(a.types().contains(DataScopeType.TENANT))return b;
        if(b.types().contains(DataScopeType.TENANT))return a;
        var types=new HashSet<>(a.types());types.retainAll(b.types());if(types.isEmpty())throw error(ErrorCode.PERMISSION_DENIED);
        var stores=new HashSet<>(a.storeIds());stores.retainAll(b.storeIds());
        return new DataScope(a.tenantId(),a.principalType(),a.principalId(),types,types.contains(DataScopeType.STORES)?stores:Set.of());
    }
    private static BusinessException error(ErrorCode code){return new BusinessException(code);}
}
