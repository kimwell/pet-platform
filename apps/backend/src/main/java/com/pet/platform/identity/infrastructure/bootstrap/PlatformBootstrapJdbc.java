package com.pet.platform.identity.infrastructure.bootstrap;
import com.pet.platform.identity.application.bootstrap.PlatformBootstrapWriter;
import java.sql.*;
import java.util.*;
/** 专用登录身份只执行控制面首次创建函数。 */
public final class PlatformBootstrapJdbc implements PlatformBootstrapWriter {
    private final Map<String,String> environment;
    public PlatformBootstrapJdbc(Map<String,String> environment){this.environment=Map.copyOf(environment);}
    @Override public UUID initialize(String login,String name,String hash){
        try(var c=CommandDatabase.connect(environment,"PET_PLATFORM_BOOTSTRAP","pet_platform_bootstrap")){
            c.setAutoCommit(false);
            try {
                try(var s=c.createStatement()){s.execute("SET LOCAL ROLE pet_platform_bootstrap");}
                UUID id=UUID.randomUUID();
                try(var q=c.prepareStatement("select pet_control.bootstrap_platform(?,?,?,?)")){
                    q.setObject(1,id);q.setString(2,login);q.setString(3,name);q.setString(4,hash);
                    try(var r=q.executeQuery()){if(!r.next() || !id.equals(r.getObject(1,UUID.class)))throw new IllegalStateException("平台初始化未返回结果");}
                }
                c.commit();return id;
            }catch(SQLException | RuntimeException failure){c.rollback();throw failure;}
        }catch(SQLException failure){
            if("P0001".equals(failure.getSQLState()))throw new IllegalStateException("平台初始化已存在或冲突；未修改密码或权限");
            throw new IllegalStateException("平台初始化未确认成功；请核查独立角色、正式迁移及目标状态");
        }
    }
}
