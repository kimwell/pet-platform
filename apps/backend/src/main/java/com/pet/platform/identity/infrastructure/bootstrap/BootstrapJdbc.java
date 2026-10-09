package com.pet.platform.identity.infrastructure.bootstrap;

import com.pet.platform.identity.application.bootstrap.*;
import java.sql.*;
import java.util.Map;
import java.util.UUID;

/** 专用连接仅SET ROLE到初始化能力角色；没有表DML或重置入口。 */
public final class BootstrapJdbc implements BootstrapWriter {
    private final Map<String,String> environment;
    public BootstrapJdbc(Map<String,String> environment) { this.environment=Map.copyOf(environment); }
    /** 只补充指定租户保留角色的详情读取，不接受权限/角色/范围参数。 */
    public int upgradeEmployeeRead(UUID tenantId) {
        try(var c=CommandDatabase.connect(environment,"PET_BOOTSTRAP","pet_bootstrap")) {
            c.setAutoCommit(false);
            try {
                try(var s=c.createStatement()) { s.execute("SET LOCAL ROLE pet_bootstrap"); }
                int changed;
                try(var q=c.prepareStatement("select pet_identity.upgrade_employee_read(?)")) {
                    q.setObject(1,tenantId);
                    try(var r=q.executeQuery()) { r.next();changed=r.getInt(1); }
                }
                c.commit();return changed;
            } catch(SQLException | RuntimeException failure) { c.rollback();throw failure; }
        } catch(SQLException failure) { throw new IllegalStateException("员工读取权限补充未确认成功；请核查独立初始化角色、迁移及保留管理员角色"); }
    }
    public int upgradeIdentityManagement(UUID tenantId) {
        try(var c=CommandDatabase.connect(environment,"PET_BOOTSTRAP","pet_bootstrap")) {
            c.setAutoCommit(false);
            try {
                try(var s=c.createStatement()){s.execute("SET LOCAL ROLE pet_bootstrap");}
                int changed;
                try(var q=c.prepareStatement("select pet_identity.upgrade_identity_management(?)")) {
                    q.setObject(1,tenantId);try(var r=q.executeQuery()){r.next();changed=r.getInt(1);}
                }
                c.commit();return changed;
            }catch(SQLException|RuntimeException e){c.rollback();throw e;}
        }catch(SQLException e){throw new IllegalStateException("管理权限升级未确认成功；请核查迁移及保留角色");}
    }
    public int upgradeOrganizations(UUID tenantId){
        try(var c=CommandDatabase.connect(environment,"PET_BOOTSTRAP","pet_bootstrap")){c.setAutoCommit(false);try{try(var s=c.createStatement()){s.execute("SET LOCAL ROLE pet_bootstrap");}int changed;try(var q=c.prepareStatement("select pet_identity.upgrade_organizations(?)")){q.setObject(1,tenantId);try(var result=q.executeQuery()){result.next();changed=result.getInt(1);}}c.commit();return changed;}catch(SQLException|RuntimeException e){c.rollback();throw e;}}
        catch(SQLException e){throw new IllegalStateException("组织权限升级未确认成功；请核查独立角色及保留管理员");}
    }
    @Override public BootstrapResult initialize(String code,String name,String login,String hash,String storeCode,String storeName) {
        try(var c=CommandDatabase.connect(environment,"PET_BOOTSTRAP","pet_bootstrap")) {
            c.setAutoCommit(false);
            try {
                try(var s=c.createStatement()) { s.execute("SET LOCAL ROLE pet_bootstrap"); }
                BootstrapResult result;
                try(var q=c.prepareStatement("select * from pet_identity.bootstrap_tenant(?,?,?,?,?,?)")) {
                    q.setString(1,code);q.setString(2,name);q.setString(3,login);q.setString(4,hash);q.setString(5,storeCode);q.setString(6,storeName);
                    try(var r=q.executeQuery()) { if(!r.next()) throw new IllegalStateException("初始化未返回结果");result=new BootstrapResult(r.getObject(1,UUID.class),r.getObject(2,UUID.class),r.getObject(3,UUID.class)); }
                }
                c.commit();return result;
            } catch (SQLException | RuntimeException failure) { c.rollback();throw failure; }
        } catch (SQLException failure) {
            if("P0001".equals(failure.getSQLState())) throw new IllegalStateException("租户已存在或初始化冲突；未修改密码或权限");
            throw new IllegalStateException("初始化未确认成功；请核查连接、角色、正式迁移及目标数据状态");
        }
    }
}
