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
public final class SaStaffSessions implements StaffSessionPort {
    private final StpLogic logic;
    private final AuthenticationRedis redis;
    private final StaffAuthentication authentication;
    private final int webAbsolute,webIdle,miniAbsolute,miniIdle;
    public SaStaffSessions(AuthenticationRedis redis,StaffAuthentication authentication,String environment,int webAbsolute,int webIdle,int miniAbsolute,int miniIdle) {
        this.redis=redis;this.authentication=authentication;this.webAbsolute=webAbsolute;this.webIdle=webIdle;this.miniAbsolute=miniAbsolute;this.miniIdle=miniIdle;
        logic=new StpLogic("staff").setConfig(config(environment,"staff"));
        // 预留空间无登录入口；任何STAFF Token均不能在这些逻辑中获得主体。
        new StpLogic("customer").setConfig(config(environment,"customer"));new StpLogic("platform").setConfig(config(environment,"platform"));
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
    @Override public Issued create(StaffIdentity identity,Channel channel) {
        String login=identity.employeeId().toString();
        return redis.accountLock(digest(login),() -> {
            var current=authentication.loadForSession(identity.tenantId(),identity.employeeId()).orElseThrow(() -> new BusinessException(ErrorCode.LOGIN_FAILED));
            if(current.securityVersion()!=identity.securityVersion() || current.tenantSecurityVersion()!=identity.tenantSecurityVersion())throw new BusinessException(ErrorCode.LOGIN_FAILED);
            var account=logic.getSessionByLoginId(login,false);
            if(account!=null) {
                for(var terminal:new ArrayList<>(account.getTerminalList())) {
                    try {logic.checkActiveTimeout(terminal.getTokenValue());if(logic.getLoginIdByToken(terminal.getTokenValue())==null)account.removeTerminal(terminal.getTokenValue());}
                    catch(NotLoginException expired){logic.logoutByTokenValue(terminal.getTokenValue(),logic.createSaLogoutParameter().setIsKeepFreezeOps(true));}
                }
                account=logic.getSessionByLoginId(login,false);
                if(account!=null && account.getTerminalList().size()>=5)throw new BusinessException(ErrorCode.SESSION_LIMIT_REACHED);
            }
            int absolute=channel==Channel.WEB?webAbsolute:miniAbsolute,idle=channel==Channel.WEB?webIdle:miniIdle;
            String token=logic.createLoginSession(login,logic.createSaLoginParameter().setDeviceType(channel.name())
                .setTimeout(absolute).setActiveTimeout(idle).setIsShare(false).setMaxLoginCount(-1).setRightNowCreateTokenSession(true));
            var session=new SessionFact(UUID.randomUUID(),identity.tenantId(),identity.employeeId(),identity.securityVersion(),identity.tenantSecurityVersion(),channel,Instant.ofEpochMilli(System.currentTimeMillis()+absolute*1000L),idle,random());
            try {
                var data=new HashMap<String,Object>();data.put("tenant",session.tenantId().toString());data.put("employee",login);data.put("session",session.sessionId().toString());
                data.put("security",Long.toString(session.securityVersion()));data.put("tenantSecurity",Long.toString(session.tenantSecurityVersion()));
                data.put("channel",channel.name());data.put("expires",session.expiresAt().toString());data.put("idle",Integer.toString(idle));data.put("csrf",session.csrfToken());
                logic.getTokenSessionByToken(token,false).setDataMap(new java.util.concurrent.ConcurrentHashMap<>(data)).update();
                return new Issued(token,session,current);
            } catch(RuntimeException e){logic.logoutByTokenValue(token);throw e;}
        });
    }
    @Override public SessionFact verify(String token,Channel channel) {
        try {
            logic.setTokenValueToStorage(token);logic.checkLogin();
            var stored=logic.getTokenSessionByToken(token,false);if(stored==null)throw new BusinessException(ErrorCode.SESSION_EXPIRED);
            var d=stored.getDataMap();
            var fact=new SessionFact(UUID.fromString((String)d.get("session")),UUID.fromString((String)d.get("tenant")),UUID.fromString((String)d.get("employee")),
                Long.parseLong((String)d.get("security")),Long.parseLong((String)d.get("tenantSecurity")),Channel.valueOf((String)d.get("channel")),Instant.parse((String)d.get("expires")),Integer.parseInt((String)d.get("idle")),(String)d.get("csrf"));
            if(!logic.getLoginId().equals(fact.employeeId().toString()) || fact.channel()!=channel)throw new BusinessException(ErrorCode.AUTH_DOMAIN_MISMATCH);
            if(!fact.expiresAt().isAfter(Instant.now()))throw new BusinessException(ErrorCode.SESSION_EXPIRED);
            return fact;
        } catch(NotLoginException e){throw new BusinessException(ErrorCode.SESSION_EXPIRED);}
        catch(IllegalArgumentException | NullPointerException e){throw new BusinessException(ErrorCode.DEPENDENCY_UNAVAILABLE);}
    }
    @Override public void touch(String token) { logic.updateLastActiveToNow(token); }
    @Override public void logout(String token) {
        Object login=logic.getLoginIdByToken(token);if(login==null)return;
        redis.accountLock(digest(login.toString()),() -> {logic.logoutByTokenValue(token);return true;});
    }
    @Override public void limit(String ip,String account) {redis.reserveAttempt(digest(ip),digest(account));}
    @Override public String createPre(String csrf) {String pre=random();redis.createPreSession(digest(pre),csrf);return pre;}
    @Override public String readPre(String pre){return redis.preSession(digest(pre));}
    @Override public long preTtl(String pre){return redis.preTtl(digest(pre));}
    @Override public void deletePre(String pre){redis.deletePreSession(digest(pre));}
}
