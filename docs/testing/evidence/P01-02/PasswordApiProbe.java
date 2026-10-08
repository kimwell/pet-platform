import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
public class PasswordApiProbe {
 public static void main(String[] args) throws Exception {
  char[] fixture="仅技术夹具，不是真实账号密码".toCharArray(); byte[] salt=new byte[32];
  PBEKeySpec spec=new PBEKeySpec(fixture,salt,600000,256);
  byte[] first=SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
  byte[] second=SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
  if(first.length!=32 || !MessageDigest.isEqual(first,second)) throw new AssertionError("算法或Unicode不一致");
  spec.clearPassword();java.util.Arrays.fill(fixture,'\0');
  System.out.println("JDK PBKDF2算法、参数和Unicode技术断言通过；固定盐仅技术夹具，不是生产凭据");
 }
}
