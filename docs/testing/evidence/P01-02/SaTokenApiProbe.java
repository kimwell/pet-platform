import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.dao.SaTokenDaoDefaultImpl;
import cn.dev33.satoken.config.SaTokenConfig;
import cn.dev33.satoken.stp.StpLogic;
public class SaTokenApiProbe {
  static void require(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
  static StpLogic logic(String type, String name) {
    return new StpLogic(type).setConfig(new SaTokenConfig().setTokenName(name)
      .setIsReadBody(false).setIsReadHeader(false).setIsReadCookie(false)
      .setIsWriteHeader(false).setIsConcurrent(true).setIsShare(false)
      .setTimeout(28800).setActiveTimeout(1800));
  }
  public static void main(String[] args) {
    SaManager.setSaTokenDao(new SaTokenDaoDefaultImpl());
    StpLogic p=logic("platform","pet:probe:platform"), s=logic("staff","pet:probe:staff"), c=logic("customer","pet:probe:customer");
    String a=s.createLoginSession("technical-fixture",s.createSaLoginParameter().setDeviceType("WEB").setTimeout(28800).setActiveTimeout(1800).setIsShare(false));
    String b=s.createLoginSession("technical-fixture",s.createSaLoginParameter().setDeviceType("MINIPROGRAM").setTimeout(28800).setActiveTimeout(1800).setIsShare(false));
    require(!a.equals(b),"设备令牌必须独立");
    require(s.getLoginIdByToken(a).equals("technical-fixture"),"员工会话可定位");
    require(p.getLoginIdByToken(a)==null && c.getLoginIdByToken(a)==null,"认证空间隔离");
    require(s.splicingKeySession("fixture").equals("pet:probe:staff:staff:session:fixture"),"键前缀以精确版本为准");
    require(s.getTokenTimeout(a)>0 && s.getTokenTimeout(a)<=28800,"绝对有效期");
    s.logoutByTokenValue(a);
    require(s.getLoginIdByToken(a)==null && s.getLoginIdByToken(b)!=null,"退出当前设备不撤销其他设备");
    s.kickout("technical-fixture");
    Object kicked=s.getLoginIdByToken(b);
    require(kicked==null || !kicked.equals("technical-fixture"),"账号撤销使全部会话失效");
    System.out.println("Sa-Token精确版本API与内存技术断言通过；不证明Redis、HTTP、Cookie或CSRF运行能力");
    System.exit(0);
  }
}
