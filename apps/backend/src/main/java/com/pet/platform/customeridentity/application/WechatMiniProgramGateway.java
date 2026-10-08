package com.pet.platform.customeridentity.application;
/** 仅外部微信交换边界；业务不依赖SDK类型，不能输入OpenID调用登录。 */
public interface WechatMiniProgramGateway {
 record Identity(String appId,String openId,String unionId) {
   @Override public String toString(){return "Identity[受限微信认证结果]";}
 }
 Identity exchange(WechatConfigResolver.Resolved config,String code);
}
