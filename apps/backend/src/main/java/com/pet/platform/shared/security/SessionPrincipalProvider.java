package com.pet.platform.shared.security;

/** 标记真实会话Provider：旧异步快照尚无撤销重验，禁止传播此来源的用户授权。 */
public interface SessionPrincipalProvider extends CurrentPrincipalProvider { }
