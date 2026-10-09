package com.pet.platform.platform.application;
import com.pet.platform.shared.api.*;
import com.pet.platform.shared.exception.BusinessException;
import java.util.*;
/** 原始参数白名单及稳定排序，不允许内部属性或任意表达式。 */
public record ControlQuery(String keyword,String status,PageQuery page,String sortBy,String sortOrder) {
 public static ControlQuery from(Map<String,String[]> parameters,boolean account){
  if(!Set.of("keyword","status","page","pageSize","sortBy","sortOrder").containsAll(parameters.keySet()))throw new BusinessException(ErrorCode.BAD_REQUEST);
  String keyword=scalar(parameters,"keyword"),status=scalar(parameters,"status"),sort=scalar(parameters,"sortBy"),order=scalar(parameters,"sortOrder");
  if(keyword!=null&&(keyword.isBlank()||keyword.codePointCount(0,keyword.length())>100))throw new BusinessException(ErrorCode.VALIDATION_FAILED);
  if(status!=null&&!Set.of("ACTIVE","DISABLED").contains(status))throw new BusinessException(ErrorCode.BAD_REQUEST);
  var allowed=account?Set.of("loginName","displayName","status","createdAt"):Set.of("code","name","status","createdAt");
  if(sort==null&&order!=null||sort!=null&&!allowed.contains(sort)||order!=null&&!Set.of("asc","desc").contains(order))throw new BusinessException(ErrorCode.SORT_INVALID);
  return new ControlQuery(keyword,status,PageQuery.from(parameters),sort==null?"createdAt":sort,order==null?"desc":order);
 }
 private static String scalar(Map<String,String[]> p,String key){if(!p.containsKey(key))return null;var values=p.get(key);if(values==null||values.length!=1||values[0]==null)throw new BusinessException(ErrorCode.BAD_REQUEST);return values[0];}
}
