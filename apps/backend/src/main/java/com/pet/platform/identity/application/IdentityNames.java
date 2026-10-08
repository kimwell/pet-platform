package com.pet.platform.identity.application;

import java.util.Locale;

/** 唯一规范化入口：先strip首尾空白，再转ASCII小写；不接受内部空白或Unicode账号。 */
public final class IdentityNames {
    private IdentityNames() { }
    public static String tenantCode(String value) { return canonical(value,"[a-z0-9][a-z0-9-]{0,31}","租户编码须为1至32位字母、数字或连字符"); }
    public static String loginName(String value) { return canonical(value,"[a-z0-9][a-z0-9._-]{0,63}","登录名须为1至64位字母、数字、点、下划线或连字符"); }
    public static String name(String value) {
        if (value == null) throw new IllegalArgumentException("名称不能为空");
        var name = value.strip();
        if (name.isEmpty() || name.codePointCount(0,name.length()) > 100 || name.codePoints().anyMatch(Character::isISOControl)) throw new IllegalArgumentException("名称须为1至100个有效字符");
        return name;
    }
    private static String canonical(String value,String pattern,String message) {
        if (value == null || !value.strip().matches("[A-Za-z0-9._-]+")) throw new IllegalArgumentException(message);
        var normalized = value.strip().toLowerCase(Locale.ROOT);
        if (!normalized.matches(pattern)) throw new IllegalArgumentException(message);
        return normalized;
    }
}
