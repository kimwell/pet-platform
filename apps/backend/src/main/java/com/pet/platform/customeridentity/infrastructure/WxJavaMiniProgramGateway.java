package com.pet.platform.customeridentity.infrastructure;
import cn.binarywang.wx.miniapp.api.impl.WxMaServiceHttpClientImpl;
import cn.binarywang.wx.miniapp.config.impl.WxMaDefaultConfigImpl;
import cn.binarywang.wx.miniapp.util.WxMaConfigHolder;
import com.pet.platform.customeridentity.application.*;
import com.pet.platform.shared.api.ErrorCode;
import com.pet.platform.shared.exception.BusinessException;
import me.chanjar.weixin.common.enums.WxType;
import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.common.util.http.SimpleGetRequestExecutor;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.*;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;
import java.util.concurrent.ConcurrentHashMap;
/** 冻结4.8.0登录适配；SDK普通get会取access_token/重试/记录原响应，登录专用get只执行一次。 */
@Component
public final class WxJavaMiniProgramGateway implements WechatMiniProgramGateway {
 private record Key(String id,long version,String appId,String secretDigest,int timeout) {}
 private final ConcurrentHashMap<Key,LoginClient> clients=new ConcurrentHashMap<>();
 public Identity exchange(WechatConfigResolver.Resolved config,String code){
  Key key=new Key(config.configId(),config.version(),config.appId(),digest(config.secret()),config.timeoutMillis());
  var client=clients.computeIfAbsent(key,k->new LoginClient(config));
  try {
   var response=client.getUserService().getSessionInfo(code);
   if(response==null || response.getOpenid()==null || !response.getOpenid().matches("[A-Za-z0-9_-]{1,128}"))throw error(ErrorCode.WECHAT_RESPONSE_INVALID);
   // UnionID只在短暂返回结果内，不保存、不参与匹配；session_key从不流出此适配器。
   return new Identity(config.appId(),response.getOpenid(),response.getUnionid());
  }catch(WxErrorException e){int n=e.getError()==null?0:e.getError().getErrorCode();
   if(n==40029 || n==40163)throw error(ErrorCode.WECHAT_CODE_INVALID);
   throw error(ErrorCode.WECHAT_UPSTREAM_ERROR);
  }catch(BusinessException e){throw e;}catch(RuntimeException e){throw error(ErrorCode.WECHAT_RESPONSE_INVALID);}
 }
 private static String digest(String value){try{return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)));}catch(java.security.NoSuchAlgorithmException e){throw new IllegalStateException("摘要算法不可用");}}
 /** 单应用不可变客户端；不使用switchover或全局可变配置。 */
 static final class LoginClient extends WxMaServiceHttpClientImpl {
  private CloseableHttpClient http;
  private final int timeout;
  LoginClient(WechatConfigResolver.Resolved c){timeout=c.timeoutMillis();var cfg=new WxMaDefaultConfigImpl();cfg.setAppid(c.appId());cfg.setSecret(c.secret());setMaxRetryTimes(0);try{setWxMaConfig(cfg);}finally{WxMaConfigHolder.remove();}}
  @Override public void initHttp(){http=HttpClients.custom().disableAutomaticRetries().disableRedirectHandling().setMaxConnTotal(10).setMaxConnPerRoute(10)
   .setDefaultRequestConfig(RequestConfig.custom().setConnectTimeout(timeout).setSocketTimeout(timeout).setConnectionRequestTimeout(timeout).build()).build();}
  @Override public CloseableHttpClient getRequestHttpClient(){return http;}
  @Override public String get(String url,String query)throws WxErrorException {
   if(!"https://api.weixin.qq.com/sns/jscode2session".equals(url))throw error(ErrorCode.WECHAT_UPSTREAM_ERROR);
   try{return SimpleGetRequestExecutor.create(this).execute(url,query,WxType.MiniApp);}
   catch(java.net.SocketTimeoutException | org.apache.http.conn.ConnectTimeoutException e){throw error(ErrorCode.WECHAT_RESULT_UNCERTAIN);}
   catch(java.io.IOException e){throw error(ErrorCode.WECHAT_RESULT_UNCERTAIN);}
  }
  void close(){try{http.close();}catch(java.io.IOException ignored){/* 关闭不记录上游信息。 */}}
 }
 @PreDestroy public void close(){clients.values().forEach(LoginClient::close);clients.clear();}
 private static BusinessException error(ErrorCode code){return new BusinessException(code);}
}
