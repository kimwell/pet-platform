package com.pet.platform.identity.application.bootstrap;
import java.util.UUID;
/** 仅独立初始化命令调用，固定首次创建能力。 */
public interface PlatformBootstrapWriter { UUID initialize(String login,String name,String hash); }
