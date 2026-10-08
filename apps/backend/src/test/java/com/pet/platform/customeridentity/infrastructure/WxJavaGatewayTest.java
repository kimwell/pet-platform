package com.pet.platform.customeridentity.infrastructure;
import com.pet.platform.customeridentity.application.*;
import com.pet.platform.shared.api.ErrorCode;
import com.pet.platform.shared.exception.BusinessException;
import java.util.*;import java.lang.reflect.*;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.ResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
/** 真实冻结SDK与生产Gateway，HTTP响应为明确技术替身；不声称微信网络认证。 */
class WxJavaGatewayTest {
 final WxJavaMiniProgramGateway gateway=new WxJavaMiniProgramGateway();
 final WechatConfigResolver.Resolved config=new WechatConfigResolver.Resolved(UUID.randomUUID(),"main",1,"wx0000000000000001","OnlyTestSecretInput1234567890",500,20);
 CloseableHttpClient install(WechatConfigResolver.Resolved c)throws Exception{
  var client=new WxJavaMiniProgramGateway.LoginClient(c);var field=client.getClass().getDeclaredField("http");field.setAccessible(true);((CloseableHttpClient)field.get(client)).close();var http=mock(CloseableHttpClient.class);field.set(client,http);
  var keyType=Class.forName(WxJavaMiniProgramGateway.class.getName()+"$Key");var ctor=keyType.getDeclaredConstructors()[0];ctor.setAccessible(true);
  String digest=HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(c.secret().getBytes(java.nio.charset.StandardCharsets.UTF_8)));
  Object key=ctor.newInstance(c.configId(),c.version(),c.appId(),digest,c.timeoutMillis());var f=gateway.getClass().getDeclaredField("clients");f.setAccessible(true);((Map)f.get(gateway)).put(key,client);return http;
 }
 void response(CloseableHttpClient http,String body)throws Exception{when(http.execute(any(HttpUriRequest.class),any(ResponseHandler.class))).thenReturn(body);}
 void error(ErrorCode code){assertEquals(code,assertThrows(BusinessException.class,()->gateway.exchange(config,"OnlyTestCode")).error().code());}
 @AfterEach void close(){gateway.close();}
 @Test void sdkCodeExchangeHasFixedEndpointOneRequestAndNoAccessTokenOrSessionKeyOutput()throws Exception{var h=install(config);response(h,"{\"openid\":\"OnlyTestOpenId\",\"session_key\":\"OnlyTestSessionKey\"}");var r=gateway.exchange(config,"OnlyTestCode");assertEquals("OnlyTestOpenId",r.openId());assertNull(r.unionId());assertFalse(r.toString().contains("OnlyTestOpenId"));var request=org.mockito.ArgumentCaptor.forClass(HttpUriRequest.class);verify(h,times(1)).execute(request.capture(),any(ResponseHandler.class));String u=request.getValue().getURI().toString();assertTrue(u.startsWith("https://api.weixin.qq.com/sns/jscode2session?"));assertFalse(u.contains("access_token"));assertTrue(u.contains("appid="+config.appId()));assertTrue(u.contains("js_code=OnlyTestCode"));}
 @Test void invalidCodeMappedWithoutRetryOrRawResponse()throws Exception{var h=install(config);response(h,"{\"errcode\":40029,\"errmsg\":\"OnlyTestSecretInput should not escape\"}");error(ErrorCode.WECHAT_CODE_INVALID);verify(h,times(1)).execute(any(HttpUriRequest.class),any(ResponseHandler.class));}
 @Test void alreadyConsumedCodeMapped()throws Exception{var h=install(config);response(h,"{\"errcode\":40163,\"errmsg\":\"used\"}");error(ErrorCode.WECHAT_CODE_INVALID);}
 @Test void busyResponseNeverRetriesCode()throws Exception{var h=install(config);response(h,"{\"errcode\":-1,\"errmsg\":\"busy\"}");error(ErrorCode.WECHAT_UPSTREAM_ERROR);verify(h,times(1)).execute(any(HttpUriRequest.class),any(ResponseHandler.class));}
 @Test void networkAndTimeoutAreUncertainInsteadOfSuccess()throws Exception{var h=install(config);when(h.execute(any(HttpUriRequest.class),any(ResponseHandler.class))).thenThrow(new java.net.SocketTimeoutException("OnlyTestSecretInput"));error(ErrorCode.WECHAT_RESULT_UNCERTAIN);verify(h,times(1)).execute(any(HttpUriRequest.class),any(ResponseHandler.class));}
 @Test void ioFailureHasSameUncertainSemantics()throws Exception{var h=install(config);when(h.execute(any(HttpUriRequest.class),any(ResponseHandler.class))).thenThrow(new java.io.IOException("OnlyTestCode"));error(ErrorCode.WECHAT_RESULT_UNCERTAIN);}
 @Test void malformedOrMissingOpenIdIsSafeFailure()throws Exception{var h=install(config);response(h,"{\"session_key\":\"OnlyTestSessionKey\"}");error(ErrorCode.WECHAT_RESPONSE_INVALID);response(h,"not json");error(ErrorCode.WECHAT_RESPONSE_INVALID);}
 @Test void clientsAreIsolatedByConfigurationVersionAndSecret()throws Exception{var c2=new WechatConfigResolver.Resolved(config.tenantId(),"main",2,config.appId(),"OtherTestSecretInput1234567890",500,20);var h1=install(config);var h2=install(c2);response(h1,"{\"openid\":\"version-one\"}");response(h2,"{\"openid\":\"version-two\"}");assertEquals("version-one",gateway.exchange(config,"c1").openId());assertEquals("version-two",gateway.exchange(c2,"c2").openId());assertEquals("version-one",gateway.exchange(config,"c3").openId());verify(h1,times(2)).execute(any(HttpUriRequest.class),any(ResponseHandler.class));verify(h2,times(1)).execute(any(HttpUriRequest.class),any(ResponseHandler.class));assertEquals("default",cn.binarywang.wx.miniapp.util.WxMaConfigHolder.get());}
}
