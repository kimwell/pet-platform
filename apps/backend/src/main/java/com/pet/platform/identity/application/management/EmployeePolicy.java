package com.pet.platform.identity.application.management;
import java.util.UUID;
public interface EmployeePolicy { void require(String permission,UUID id); }
