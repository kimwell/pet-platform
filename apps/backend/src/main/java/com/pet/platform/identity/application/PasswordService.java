package com.pet.platform.identity.application;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import org.springframework.stereotype.Service;

/** 冻结JDK实现，非自写哈希算法；输入不trim、不归一化、不截断。 */
@Service
public final class PasswordService {
    public static final int ITERATIONS = 600_000;
    private final SecureRandom random = new SecureRandom();
    private record Encoding(int iterations, byte[] salt, byte[] hash) { }
    public String hash(char[] password) {
        validate(password);
        byte[] salt = new byte[32]; random.nextBytes(salt);
        byte[] result = derive(password,salt,ITERATIONS);
        try { return "$pbkdf2-sha256$v1$"+ITERATIONS+"$"+Base64.getEncoder().encodeToString(salt)+"$"+Base64.getEncoder().encodeToString(result); }
        finally { Arrays.fill(result,(byte)0); }
    }
    public boolean matches(char[] password,String encoded) {
        try { validate(password); } catch (IllegalArgumentException failure) { return false; }
        var parsed = parse(encoded);
        if (parsed == null) return false;
        byte[] actual = derive(password,parsed.salt(),parsed.iterations());
        try { return MessageDigest.isEqual(parsed.hash(),actual); }
        finally { Arrays.fill(actual,(byte)0); Arrays.fill(parsed.hash(),(byte)0); }
    }
    public boolean needsRehash(String encoded) {
        var parsed = parse(encoded);
        return parsed == null || parsed.iterations() < ITERATIONS;
    }
    public void validate(char[] password) {
        if (password == null || password.length > 256) throw new IllegalArgumentException("密码须为12至128个有效字符");
        int count=0;
        for (int i=0;i<password.length;i++,count++) {
            if (Character.isHighSurrogate(password[i])) {
                if (++i >= password.length || !Character.isLowSurrogate(password[i])) throw new IllegalArgumentException("密码包含非法字符编码");
            } else if (Character.isLowSurrogate(password[i])) throw new IllegalArgumentException("密码包含非法字符编码");
        }
        if (count < 12 || count > 128) throw new IllegalArgumentException("密码须为12至128个有效字符");
    }
    private Encoding parse(String encoded) {
        if (encoded == null || encoded.length() > 256) return null;
        String[] parts=encoded.split("\\$",-1);
        if (parts.length!=6 || !parts[0].isEmpty() || !parts[1].equals("pbkdf2-sha256") || !parts[2].equals("v1") || !parts[3].matches("[0-9]{5,7}")) return null;
        try {
            int iterations=Integer.parseInt(parts[3]);
            // 支持将来迁移旧参数，但签发永不降低冻结值；上限限制损坏编码的计算成本。
            if (iterations < 10_000 || iterations > 2_000_000) return null;
            byte[] salt=Base64.getDecoder().decode(parts[4]), hash=Base64.getDecoder().decode(parts[5]);
            if (salt.length!=32 || hash.length!=32 || !Base64.getEncoder().encodeToString(salt).equals(parts[4]) || !Base64.getEncoder().encodeToString(hash).equals(parts[5])) return null;
            return new Encoding(iterations,salt,hash);
        } catch (IllegalArgumentException failure) { return null; }
    }
    private byte[] derive(char[] password,byte[] salt,int iterations) {
        var spec=new PBEKeySpec(password,salt,iterations,256);
        try { return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded(); }
        catch (GeneralSecurityException failure) { throw new IllegalStateException("密码算法不可用"); }
        finally { spec.clearPassword(); }
    }
}
