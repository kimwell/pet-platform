package com.pet.platform.identity.infrastructure.session;

import cn.dev33.satoken.config.SaTokenConfig;
import cn.dev33.satoken.stp.StpLogic;
import cn.dev33.satoken.exception.NotLoginException;
import com.pet.platform.identity.application.authentication.*;
import com.pet.platform.shared.api.ErrorCode;
import com.pet.platform.shared.exception.BusinessException;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.Instant;
import java.util.*;

/** 唯一STAFF逻辑适配器；显式载体注入，使用官方期限检查而非仅Token索引。 */
public class SaIdentitySessions<T> implements IdentitySessionPort<T> {
    private final StpLogic logic;
    private final AuthenticationRedis redis;
    private final java.util.function.Function<State,T> reload;
    private final java.util.function.Function<T,State> state;
    private final String domain;
    private final int webAbsolute,webIdle,miniAbsolute,miniIdle;
    public SaIdentitySessions(AuthenticationRedis redis,String environment,String domain,java.util.function.Function<State,T> reload,java.util.function.Function<T,State> state,int webAbsolute,int webIdle,int miniAbsolute,int miniIdle) {
        if(!Set.of("staff","platform").contains(domain))throw new IllegalArgumentException("认证空间未登记");
        this.redis=redis;this.domain=domain;this.reload=reload;this.state=state;this.webAbsolute=webAbsolute;this.webIdle=webIdle;this.miniAbsolute=miniAbsolute;this.miniIdle=miniIdle;
        logic=new StpLogic(domain).setConfig(config(environment,domain));
    }
    static SaTokenConfig config(String env,String domain) { return new SaTokenConfig().setTokenName("pet:"+env+":"+domain)
        .setIsReadBody(false).setIsReadHeader(false).setIsReadCookie(false).setIsWriteHeader(false)
        .setIsConcurrent(true).setIsShare(false).setMaxLoginCount(-1).setTokenStyle("random-64")
        .setTimeout(28800).setActiveTimeout(1800).setDynamicActiveTimeout(true).setAutoRenew(false).setIsLog(false); }
    public static String digest(String value) {
        try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}
        catch(NoSuchAlgorithmException e){throw new IllegalStateException("安全摘要算法不可用");}
    }
    public static String random() { byte[] b=new byte[32];new SecureRandom().nextBytes(b);return Base64.getUrlEncoder().withoutPadding().encodeToString(b); }
    @Override public Issued<T> create(T identity,StaffSessionPort.Channel channel) {
        if(domain.equals("platform") && channel!=StaffSessionPort.Channel.WEB)throw new BusinessException(ErrorCode.AUTH_DOMAIN_MISMATCH);
        State original=state.apply(identity);String login=original.principalId().toString();
        return redis.accountLock(domain,digest(login),() -> {
            T currentIdentity=reload.apply(original);State current=state.apply(currentIdentity);
            if(!current.equals(original))throw new BusinessException(ErrorCode.LOGIN_FAILED);
            var account=logic.getSessionByLoginId(login,false);
            if(account!=null) {
                for(var terminal:new ArrayList<>(account.getTerminalList())) {
                    try {
                        var recorded=logic.getTokenSessionByToken(terminal.getTokenValue(),false);
                        if(recorded!=null && security(recorded.getDataMap())<current.securityVersion()){logic.logoutByTokenValue(terminal.getTokenValue());continue;}
                        logic.checkActiveTimeout(terminal.getTokenValue());if(logic.getLoginIdByToken(terminal.getTokenValue())==null)account.removeTerminal(terminal.getTokenValue());}
                    catch(NotLoginException expired){logic.logoutByTokenValue(terminal.getTokenValue(),logic.createSaLogoutParameter().setIsKeepFreezeOps(true));}
                }
                account=logic.getSessionByLoginId(login,false);
                if(account!=null && account.getTerminalList().size()>=5)throw new BusinessException(ErrorCode.SESSION_LIMIT_REACHED);
            }
            int absolute=channel==StaffSessionPort.Channel.WEB?webAbsolute:miniAbsolute,idle=channel==StaffSessionPort.Channel.WEB?webIdle:miniIdle;
            String token=logic.createLoginSession(login,logic.createSaLoginParameter().setDeviceType(channel.name())
                .setTimeout(absolute).setActiveTimeout(idle).setIsShare(false).setMaxLoginCount(-1).setRightNowCreateTokenSession(true));
            var session=new Fact(UUID.randomUUID(),original.tenantId(),original.principalId(),original.securityVersion(),original.tenantSecurityVersion(),channel,Instant.ofEpochMilli(System.currentTimeMillis()+absolute*1000L),idle,random());
            try {
                var data=new HashMap<String,Object>();if(session.tenantId()!=null)data.put("tenant",session.tenantId().toString());data.put(domain.equals("staff")?"employee":"principal",login);data.put("session",session.sessionId().toString());
                data.put("security",Long.toString(session.securityVersion()));if(session.tenantId()!=null)data.put("tenantSecurity",Long.toString(session.tenantSecurityVersion()));
                data.put("channel",channel.name());data.put("expires",session.expiresAt().toString());data.put("idle",Integer.toString(idle));data.put("csrf",session.csrfToken());
                logic.getTokenSessionByToken(token,false).setDataMap(new java.util.concurrent.ConcurrentHashMap<>(data)).update();
                T after=reload.apply(original);
                if(!state.apply(after).equals(original))throw new BusinessException(ErrorCode.LOGIN_FAILED);
                return new Issued<>(token,session,after);
            } catch(RuntimeException e){logic.logoutByTokenValue(token);throw e;}
        });
    }
    @Override public Fact verify(String token,StaffSessionPort.Channel channel) {
        try {
            logic.setTokenValueToStorage(token);logic.checkLogin();
            var stored=logic.getTokenSessionByToken(token,false);if(stored==null)throw new BusinessException(ErrorCode.SESSION_EXPIRED);
            var d=stored.getDataMap();
            var fact=new Fact(UUID.fromString((String)d.get("session")),d.containsKey("tenant")?UUID.fromString((String)d.get("tenant")):null,UUID.fromString((String)d.getOrDefault("principal",d.get("employee"))),
                Long.parseLong((String)d.get("security")),Long.parseLong((String)d.getOrDefault("tenantSecurity","0")),StaffSessionPort.Channel.valueOf((String)d.get("channel")),Instant.parse((String)d.get("expires")),Integer.parseInt((String)d.get("idle")),(String)d.get("csrf"));
            if(!logic.getLoginId().equals(fact.principalId().toString()) || fact.channel()!=channel)throw new BusinessException(ErrorCode.AUTH_DOMAIN_MISMATCH);
            if(!fact.expiresAt().isAfter(Instant.now()))throw new BusinessException(ErrorCode.SESSION_EXPIRED);
            return fact;
        } catch(NotLoginException e){throw new BusinessException(ErrorCode.SESSION_EXPIRED);}
        catch(IllegalArgumentException | NullPointerException e){throw new BusinessException(ErrorCode.DEPENDENCY_UNAVAILABLE);}
    }
    @Override public void touch(String token) { logic.updateLastActiveToNow(token); }
    @Override public void logout(String token) {
        Object login=logic.getLoginIdByToken(token);if(login==null)return;
        redis.accountLock(domain,digest(login.toString()),() -> {logic.logoutByTokenValue(token);return true;});
    }
    @Override public void limit(String ip,String account) {redis.reserveAttempt(domain,digest(ip),digest(account));}
    @Override public void limitSensitive(String ip,UUID tenant,UUID actor,UUID target) {
        redis.reserveSensitive(domain,digest(ip),digest(tenant+"/"+actor),digest(tenant+"/"+target));
    }
    @Override public void revokeBefore(UUID tenant,UUID employee,long cutoff) {
        redis.accountLock(domain,digest(employee.toString()),() -> {
            var account=logic.getSessionByLoginId(employee.toString(),false);
            if(account==null)return true;
            for(var terminal:new ArrayList<>(account.getTerminalList())) {
                String token=terminal.getTokenValue();var stored=logic.getTokenSessionByToken(token,false);
                if(stored==null) {removeTerminal(token,employee);continue;}
                var data=stored.getDataMap();
                if(!Objects.equals(tenant==null?null:tenant.toString(),data.get("tenant")) || !employee.toString().equals(data.getOrDefault("principal",data.get("employee"))))throw new BusinessException(ErrorCode.DEPENDENCY_UNAVAILABLE);
                if(security(data)<cutoff)removeTerminal(token,employee);
            }
            return true;
        });
    }
    private static long security(Map<String,Object> data){try{return Long.parseLong((String)data.get("security"));}catch(IllegalArgumentException | NullPointerException e){throw new BusinessException(ErrorCode.DEPENDENCY_UNAVAILABLE);}}
    private void removeTerminal(String token,UUID employee) {
        logic.logoutByTokenValue(token,logic.createSaLogoutParameter().setIsKeepFreezeOps(true));
        // 官方logout遇到已丢失Token索引会提前返回；补偿明确删除设备记录和悬挂终端。
        logic.deleteTokenSession(token);redis.delete(logic.splicingKeyLastActiveTime(token));
        var account=logic.getSessionByLoginId(employee.toString(),false);
        if(account!=null){account.removeTerminal(token);account.logoutByTerminalCountToZero();}
    }
    @Override public boolean isActive(UUID tenant,UUID employee,UUID sessionId) {
        try {
        var account=logic.getSessionByLoginId(employee.toString(),false);
        if(account==null)return false;
        for(var terminal:new ArrayList<>(account.getTerminalList())) {
            String token=terminal.getTokenValue();var stored=logic.getTokenSessionByToken(token,false);
            if(stored==null || !sessionId.toString().equals(stored.getDataMap().get("session")))continue;
            try{logic.checkActiveTimeout(token);}catch(NotLoginException e){return false;}
            var data=stored.getDataMap();
            return Objects.equals(tenant==null?null:tenant.toString(),data.get("tenant")) && employee.toString().equals(data.getOrDefault("principal",data.get("employee")))
                && employee.toString().equals(logic.getLoginIdByToken(token)) && Instant.parse((String)data.get("expires")).isAfter(Instant.now());
        }
        return false;
        }catch(IllegalArgumentException | NullPointerException failure){throw new BusinessException(ErrorCode.DEPENDENCY_UNAVAILABLE);}
    }
    @Override public String createPre(String csrf) {String pre=random();redis.createPreSession(domain,digest(pre),csrf);return pre;}
    @Override public String readPre(String pre){return redis.preSession(domain,digest(pre));}
    @Override public long preTtl(String pre){return redis.preTtl(domain,digest(pre));}
    @Override public void deletePre(String pre){redis.deletePreSession(domain,digest(pre));}
}
