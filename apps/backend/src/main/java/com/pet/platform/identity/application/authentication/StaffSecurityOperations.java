package com.pet.platform.identity.application.authentication;

import com.pet.platform.identity.application.PasswordService;
import com.pet.platform.shared.api.ErrorCode;
import com.pet.platform.shared.exception.BusinessException;
import com.pet.platform.shared.observability.TraceContext;
import com.pet.platform.shared.tenancy.*;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** 四个固定敏感用例；当前密码重新确认，不签发可复用的二次证明。 */
@Service
public final class StaffSecurityOperations {
    public enum Operation { CHANGE_PASSWORD, RESET_PASSWORD, LOGOUT_ALL, REVOKE_SESSIONS }
    private static final Logger LOG=LoggerFactory.getLogger(StaffSecurityOperations.class);
    private final StaffSecurityStore store;
    private final StaffAuthentication authentication;
    private final PasswordService passwords;
    private final StaffSessionPort sessions;
    public StaffSecurityOperations(StaffSecurityStore store,StaffAuthentication auth,PasswordService passwords,StaffSessionPort sessions){this.store=store;authentication=auth;this.passwords=passwords;this.sessions=sessions;}

    public boolean execute(StaffHttpAuthentication.Authenticated current,Operation op,UUID requestedTarget,Long expectedVersion,String confirmation,String replacement,String ip) {
        UUID actor=current.identity().employeeId(),tenant=current.identity().tenantId();
        boolean admin=op==Operation.RESET_PASSWORD || op==Operation.REVOKE_SESSIONS;
        UUID target=admin?Objects.requireNonNull(requestedTarget):actor;
        sessions.limitSensitive(ip,tenant,actor,target);
        char[] confirm=confirmation==null?null:confirmation.toCharArray(),next=replacement==null?null:replacement.toCharArray();
        try {
            if(admin && !current.identity().grants().containsKey(op==Operation.RESET_PASSWORD?"identity:user:reset-password":"identity:user:revoke-sessions"))throw failure(ErrorCode.PERMISSION_DENIED);
            store.transaction(tenant,() -> {
                store.lockTenant();
                // 固定UUID顺序锁两行，避免 A管理B/B管理A 死锁。
                var locked=new HashMap<UUID,StaffSecurityStore.Credential>();
                for(UUID id:new TreeSet<>(Set.copyOf(Arrays.asList(actor,target))))locked.put(id,store.lockEmployee(id).orElseThrow(() -> failure(ErrorCode.RESOURCE_NOT_FOUND)));
                var operator=locked.get(actor);var subject=locked.get(target);
                var authority=authentication.loadForSession(tenant,actor).orElseThrow(() -> failure(ErrorCode.SESSION_REVOKED));
                requireSession(current,authority);
                if(!sessions.isActive(tenant,actor,current.session().sessionId()))throw failure(ErrorCode.SESSION_REVOKED);
                if(!operator.active || operator.securityVersion!=current.session().securityVersion())throw failure(ErrorCode.SESSION_REVOKED);
                if(admin) {
                    if(operator.mustChange)throw failure(ErrorCode.PASSWORD_CHANGE_REQUIRED);
                    String permission=op==Operation.RESET_PASSWORD?"identity:user:reset-password":"identity:user:revoke-sessions";
                    var scope=authority.grants().get(permission);
                    if(scope==null)throw failure(ErrorCode.PERMISSION_DENIED);
                    if(actor.equals(target))throw failure(ErrorCode.PERMISSION_DENIED);
                    requireTarget(authority,subject,target,scope,op,operator);
                    if(expectedVersion==null || expectedVersion!=subject.version)throw failure(ErrorCode.VERSION_CONFLICT);
                }
                if(!subject.active)throw failure(ErrorCode.BUSINESS_STATE_CONFLICT);
                if(!operator.matches(passwords,confirm))throw failure(ErrorCode.SECURITY_CONFIRMATION_FAILED);
                if(op==Operation.CHANGE_PASSWORD || op==Operation.RESET_PASSWORD) {
                    try{passwords.validate(next);}catch(IllegalArgumentException invalid){throw failure(ErrorCode.VALIDATION_FAILED);}
                    if(subject.matches(passwords,next))throw failure(ErrorCode.VALIDATION_FAILED);
                    store.change(target,passwords.hash(next),op==Operation.RESET_PASSWORD);
                }
                long version=store.revoke(target);
                store.event(actor,target,op.name(),"SUCCESS",TraceContext.currentId());
                // 读当前授权而非请求旧快照；未来授权写事务须持有员工锁并增授权版本。
                if(admin)requireTarget(authority,store.lockEmployee(target).orElseThrow(() -> failure(ErrorCode.RESOURCE_NOT_FOUND)),target,
                    authority.grants().get(op==Operation.RESET_PASSWORD?"identity:user:reset-password":"identity:user:revoke-sessions"),op,operator);
                var finalAuthority=authentication.loadForSession(tenant,actor).orElseThrow(() -> failure(ErrorCode.SESSION_REVOKED));
                long expectedSecurity=actor.equals(target)?version:authority.securityVersion();
                if(finalAuthority.securityVersion()!=expectedSecurity || finalAuthority.tenantSecurityVersion()!=authority.tenantSecurityVersion()
                    || finalAuthority.authorizationVersion()!=authority.authorizationVersion() || !finalAuthority.grants().equals(authority.grants())
                    || !finalAuthority.authorizedStoreIds().equals(authority.authorizedStoreIds()))throw failure(ErrorCode.PERMISSION_DENIED);
                if(!sessions.isActive(tenant,actor,current.session().sessionId()))throw failure(ErrorCode.SESSION_REVOKED);
                return version;
            });
        } catch(BusinessException failure) {
            // 业务事务已完整回滚；独立事务记录已通过频控的尝试，无提交值/异常正文。
            try{store.transaction(tenant,() -> {store.event(actor,target,op.name(),failure.error().code().name(),TraceContext.currentId());return null;});}
            catch(RuntimeException unavailable){LOG.warn("安全操作失败记录未完成，操作={}，traceId={}",op,TraceContext.currentId());}
            throw failure;
        } finally {if(confirm!=null)Arrays.fill(confirm,'\0');if(next!=null)Arrays.fill(next,'\0');}
        return cleanup(tenant,target);
    }
    private void requireTarget(StaffIdentity actor,StaffSecurityStore.Credential target,UUID id,DataScope scope,Operation op,StaffSecurityStore.Credential operator) {
        var assigned=store.employeeStores(id);
        // 员工多店管理必须覆盖其全部归属；可查看一部分门店不等于可接管账号。
        if(!scope.types().contains(DataScopeType.TENANT)
            && !(scope.types().contains(DataScopeType.STORES) && !assigned.isEmpty() && scope.storeIds().containsAll(assigned)))throw failure(ErrorCode.RESOURCE_NOT_FOUND);
        if(target.reserved || (target.tenantAdmin && (op==Operation.RESET_PASSWORD || !operator.tenantAdmin || !scope.types().contains(DataScopeType.TENANT))))throw failure(ErrorCode.PERMISSION_DENIED);
        var targetIdentity=authentication.loadForSession(actor.tenantId(),id);
        if(targetIdentity.isPresent())for(var grant:targetIdentity.orElseThrow().grants().entrySet()) {
            var own=actor.grants().get(grant.getKey());
            if(own==null || !covered(grant.getValue(),own))throw failure(ErrorCode.PERMISSION_DENIED);
        }
    }
    private static boolean covered(DataScope target,DataScope own) {
        if(own.types().contains(DataScopeType.TENANT))return true;
        // SELF是主体专属，不能将操作者SELF用于管理另一个员工。
        return !target.types().contains(DataScopeType.TENANT) && !target.types().contains(DataScopeType.SELF)
            && own.types().containsAll(target.types()) && own.storeIds().containsAll(target.storeIds());
    }
    public boolean cleanup(UUID tenant,UUID employee) {
        try {
            long cutoff=store.transaction(tenant,() -> store.pending(employee));
            if(cutoff==0)return true;
            sessions.revokeBefore(tenant,employee,cutoff);
            store.transaction(tenant,() -> {store.completed(employee,cutoff);return null;});
            return true;
        } catch(RuntimeException failure) {
            LOG.warn("员工会话逻辑失效已由数据库保证，物理清理待重试，员工={}，traceId={}",employee,TraceContext.currentId());
            return false;
        }
    }
    private static void requireSession(StaffHttpAuthentication.Authenticated session,StaffIdentity identity){
        if(session.session().securityVersion()!=identity.securityVersion() || session.session().tenantSecurityVersion()!=identity.tenantSecurityVersion())throw failure(ErrorCode.SESSION_REVOKED);
    }
    private static BusinessException failure(ErrorCode code){return new BusinessException(code);}
}
