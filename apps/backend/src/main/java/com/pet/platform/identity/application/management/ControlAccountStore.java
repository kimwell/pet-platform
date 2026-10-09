package com.pet.platform.identity.application.management;
import com.pet.platform.identity.application.authentication.PlatformHttpAuthentication;
import com.pet.platform.identity.application.management.ControlAccountModels.*;
import com.pet.platform.platform.application.*;
import com.pet.platform.platform.application.ControlModels.ControlMutation;
import com.pet.platform.shared.api.PageResponse;
import java.util.*;
import java.util.function.Supplier;
public interface ControlAccountStore {
 <T>T transaction(PlatformHttpAuthentication.Authenticated current,Supplier<T> work);
 PageResponse<ControlAccountView> accounts(ControlQuery q);ControlAccountView account(UUID id);
 ControlMutation write(String action,UUID id,long version,String login,String name,String hash,List<String> permissions,String status);
}
