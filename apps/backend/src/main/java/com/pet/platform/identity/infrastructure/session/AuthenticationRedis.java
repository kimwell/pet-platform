package com.pet.platform.identity.infrastructure.session;

import cn.dev33.satoken.dao.SaTokenDaoForRedisTemplate;
import cn.dev33.satoken.session.*;
import com.pet.platform.shared.api.ErrorCode;
import com.pet.platform.shared.exception.BusinessException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.function.Supplier;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import tools.jackson.databind.json.JsonMapper;

/** 认证专属DAO：复用冻结官方Redis实现，固定JSON线模型；不开放业务缓存或任意脚本。 */
public final class AuthenticationRedis extends SaTokenDaoForRedisTemplate {
    private final String prefix;
    private final JsonMapper json=JsonMapper.builder().build();
    private record Terminal(int index,String token,String device,String deviceId,long created) { }
    private record SessionWire(int version,String id,String type,String domain,String loginId,String token,
            int history,long created,Map<String,String> data,List<Terminal> terminals) { }
    public AuthenticationRedis(RedisConnectionFactory factory,String environment) { prefix="pet:"+environment+":";init(factory); }
    @Override public String wrapKey(String key) {
        if (key==null || key.length()>512 || !key.startsWith(prefix)
                || !(key.startsWith(prefix+"staff:staff:") || key.startsWith(prefix+"platform:platform:")
                || key.startsWith(prefix+"customer:customer:"))) throw unavailable();
        return key;
    }
    private <T> T safe(Supplier<T> call) { try { return call.get(); } catch(BusinessException e) { throw e; } catch(RuntimeException e) { throw unavailable(); } }
    private static BusinessException unavailable() { return new BusinessException(ErrorCode.DEPENDENCY_UNAVAILABLE); }
    private static void value(String value) { if(value==null || value.getBytes(StandardCharsets.UTF_8).length>131072) throw unavailable(); }
    private static void ttl(long ttl) { if(ttl<1 || ttl>604800) throw unavailable(); }
    @Override public String get(String key) { return safe(() -> {String v=super.get(key);if(v!=null)value(v);return v;}); }
    @Override public void set(String key,String value,long timeout) { value(value);ttl(timeout);safe(() -> {super.set(key,value,timeout);return true;}); }
    @Override public void update(String key,String value) { value(value);safe(() -> {super.update(key,value);return true;}); }
    @Override public void delete(String key) { safe(() -> {super.delete(key);return true;}); }
    @Override public long getTimeout(String key) { return safe(() -> super.getTimeout(key)); }
    @Override public void updateTimeout(String key,long timeout) { ttl(timeout);safe(() -> {super.updateTimeout(key,timeout);return true;}); }
    @Override public List<String> searchData(String p,String k,int s,int n,boolean sort) { throw unavailable(); }
    @Override public Object getObject(String key) { return getObject(key,SaSession.class); }
    @Override public <T> T getObject(String key,Class<T> type) {
        if(type!=SaSession.class)throw unavailable();
        return safe(() -> {
            String raw=get(key);if(raw==null)return null;
            SessionWire w=json.readValue(raw,SessionWire.class);
            if(w.version()!=1 || !key.equals(w.id()) || w.data()==null || w.terminals()==null || w.terminals().size()>5)throw unavailable();
            SaSession session=new SaSession();session.setId(w.id()).setType(w.type()).setLoginType(w.domain())
                .setLoginId(w.loginId()).setToken(w.token()).setCreateTime(w.created());
            session.setHistoryTerminalCount(w.history());
            session.setDataMap(new java.util.concurrent.ConcurrentHashMap<>(w.data()));
            session.setTerminalList(new Vector<>(w.terminals().stream().map(t -> new SaTerminalInfo().setIndex(t.index())
                .setTokenValue(t.token()).setDeviceType(t.device()).setDeviceId(t.deviceId()).setCreateTime(t.created())).toList()));
            return type.cast(session);
        });
    }
    private String encode(Object obj) {
        if(!(obj instanceof SaSession s) || s.getTerminalList().size()>5 || (s.getLoginId()!=null && !(s.getLoginId() instanceof String)))throw unavailable();
        Map<String,String> data=new TreeMap<>();s.getDataMap().forEach((k,v) -> {
            if(!Set.of("tenant","employee","principal","session","security","tenantSecurity","channel","expires","idle","csrf").contains(k) || !(v instanceof String))throw unavailable();
            data.put(k,(String)v);
        });
        var terminals=s.getTerminalList().stream().map(t -> {
            if(t.getExtraData()!=null && !t.getExtraData().isEmpty())throw unavailable();
            return new Terminal(t.getIndex(),t.getTokenValue(),t.getDeviceType(),t.getDeviceId(),t.getCreateTime());
        }).toList();
        return safe(() -> json.writeValueAsString(new SessionWire(1,s.getId(),s.getType(),s.getLoginType(),(String)s.getLoginId(),s.getToken(),s.getHistoryTerminalCount(),s.getCreateTime(),data,terminals)));
    }
    @Override public void setObject(String key,Object obj,long timeout) { set(key,encode(obj),timeout); }
    @Override public void updateObject(String key,Object obj) { update(key,encode(obj)); }
    // 以下方法仅接收服务端安全摘要，认证前不制造TenantContext。
    private String auxiliary(String domain,String suffix) { if(!Set.of("staff","platform").contains(domain))throw unavailable(); if(!suffix.matches("[a-z]+:[a-f0-9]{64}"))throw unavailable();return prefix+"auth:"+domain+":"+suffix; }
    public void reserveAttempt(String domain,String ipDigest,String accountDigest) {
        var script=new DefaultRedisScript<List>("local a=redis.call('INCR',KEYS[1]); if a==1 then redis.call('EXPIRE',KEYS[1],300) end; local b=redis.call('INCR',KEYS[2]); if b==1 then redis.call('EXPIRE',KEYS[2],900) end; return {a,b,redis.call('TTL',KEYS[1]),redis.call('TTL',KEYS[2])}",List.class);
        List<?> r=safe(() -> stringRedisTemplate.execute(script,List.of(auxiliary(domain,"ip:"+ipDigest),auxiliary(domain,"account:"+accountDigest))));
        if(r==null || r.size()!=4)throw unavailable();
        if(((Number)r.get(0)).longValue()>60 || ((Number)r.get(1)).longValue()>10)
            throw BusinessException.rateLimited(Math.max(1,((Number)r.get(((Number)r.get(0)).longValue()>60?2:3)).longValue()));
    }
    public void reserveSensitive(String domain,String ip,String actor,String target) {
        var script=new DefaultRedisScript<List>("local result={}; for i=1,3 do local n=redis.call('INCR',KEYS[i]); if n==1 then redis.call('EXPIRE',KEYS[i],300) end; result[i]=n; result[i+3]=redis.call('TTL',KEYS[i]); end; return result",List.class);
        List<?> counts=safe(() -> stringRedisTemplate.execute(script,List.of(auxiliary(domain,"sip:"+ip),auxiliary(domain,"actor:"+actor),auxiliary(domain,"target:"+target))));
        if(counts==null || counts.size()!=6)throw unavailable();
        int[] maximum={60,8,8};
        for(int i=0;i<3;i++)if(((Number)counts.get(i)).longValue()>maximum[i])throw BusinessException.rateLimited(Math.max(1,((Number)counts.get(i+3)).longValue()));
    }
    public String preSession(String domain,String digest) { return safe(() -> stringRedisTemplate.opsForValue().get(auxiliary(domain,"pre:"+digest))); }
    public void createPreSession(String domain,String digest,String csrf) { safe(() -> {stringRedisTemplate.opsForValue().set(auxiliary(domain,"pre:"+digest),csrf,Duration.ofMinutes(10));return true;}); }
    public long preTtl(String domain,String digest) { return safe(() -> stringRedisTemplate.getExpire(auxiliary(domain,"pre:"+digest))); }
    public void deletePreSession(String domain,String digest) { safe(() -> stringRedisTemplate.delete(auxiliary(domain,"pre:"+digest))); }
    public <T> T accountLock(String domain,String digest,Supplier<T> action) {
        String key=auxiliary(domain,"lock:"+digest),owner=UUID.randomUUID().toString();
        boolean acquired=false;
        try {
            if(!Boolean.TRUE.equals(safe(() -> stringRedisTemplate.opsForValue().setIfAbsent(key,owner,Duration.ofSeconds(30)))))throw BusinessException.rateLimited(1);
            acquired=true;
            return action.get();
        } finally {
            // SET NX超时可能已经执行；仍按随机owner尝试释放，绝不删除其他持有者。
            Long result=safe(() -> stringRedisTemplate.execute(new DefaultRedisScript<>("if redis.call('GET',KEYS[1])==ARGV[1] then return redis.call('DEL',KEYS[1]) else return 0 end",Long.class),List.of(key),owner));
            if(result==null || (acquired && result!=1))throw unavailable();
        }
    }
}
