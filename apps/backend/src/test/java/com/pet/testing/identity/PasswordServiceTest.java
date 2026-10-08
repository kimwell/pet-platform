package com.pet.testing.identity;

import com.pet.platform.identity.application.PasswordService;
import com.pet.platform.identity.application.IdentityNames;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** 所有密码仅为临时技术向量；不进入正式配置、种子或JAR。 */
class PasswordServiceTest {
    private final PasswordService passwords=new PasswordService();
    @Test void randomSaltAndCorrectIncorrectPasswords() {
        char[] input="临时技术密码-Unicode😀".toCharArray();
        var first=passwords.hash(input);var second=passwords.hash(input);
        assertFalse(first.equals(second));assertFalse(first.contains(new String(input)));
        assertTrue(passwords.matches(input,first));assertTrue(passwords.matches(input,second));
        assertFalse(passwords.matches("错误临时技术密码123".toCharArray(),first));assertFalse(passwords.needsRehash(first));
    }
    @Test void unicodeMatchesIndependentUtf8ReferenceVector() {
        // 独立Python hashlib标准PBKDF2 UTF-8向量，包含空格、中文和补充平面字符。
        assertTrue(passwords.matches("技术密码 空格中文😀Ab".toCharArray(),"$pbkdf2-sha256$v1$600000$AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8=$hAWmHMDcpGuI+JtKMjPEZXucqhgJDxncCfdSX62HGVk="));
    }
    @Test void doesNotTrimCaseFoldOrNormalizePasswords() {
        var input=" 临时技术密码-AbCé ".toCharArray();var encoded=passwords.hash(input);
        assertFalse(passwords.matches(new String(input).strip().toCharArray(),encoded));
        assertFalse(passwords.matches(" 临时技术密码-abcé ".toCharArray(),encoded));
        assertFalse(passwords.matches(" 临时技术密码-AbCe\u0301 ".toCharArray(),encoded));
    }
    @Test void rejectsTooShortLongAndBrokenUnicodeWithoutTruncation() {
        for(char[] input:new char[][]{null,new char[0],"12345678901".toCharArray(),"a".repeat(129).toCharArray(),"12345678901\ud800".toCharArray()}) {
            assertThrows(IllegalArgumentException.class,() -> passwords.hash(input));assertFalse(passwords.matches(input,"invalid"));
        }
        assertDoesNotThrow(() -> passwords.hash("😀".repeat(128).toCharArray()));
        assertThrows(IllegalArgumentException.class,() -> passwords.hash("😀".repeat(129).toCharArray()));
    }
    @Test void malformedUnknownAndExcessiveCostEncodingsFailSafely() {
        var good=passwords.hash("临时技术密码-encoded".toCharArray());
        for(String encoded:new String[]{null,"", "md5:invalid",good.replace("$v1$","$v2$"),good.replace("600000","9999999"),good.replace("600000","1"),good.substring(0,good.length()-2),"x".repeat(300)}) {
            assertFalse(passwords.matches("临时技术密码-encoded".toCharArray(),encoded));assertTrue(passwords.needsRehash(encoded));
        }
        assertTrue(passwords.needsRehash(good.replace("600000","10000")));
    }
    @Test void identityNamesNormalizeOnlyApprovedAsciiNames() {
        assertEquals("tenant-a",IdentityNames.tenantCode(" Tenant-A "));
        assertEquals("user.name_a",IdentityNames.loginName(" USER.Name_A "));
        assertEquals("中文名称",IdentityNames.name(" 中文名称 "));
        for(String input:new String[]{"tenant a","租户","a/b","a".repeat(33),"_tenant","İ"}) assertThrows(IllegalArgumentException.class,() -> IdentityNames.tenantCode(input));
        for(String input:new String[]{"user name","员工","a".repeat(65),".user"}) assertThrows(IllegalArgumentException.class,() -> IdentityNames.loginName(input));
    }
}
