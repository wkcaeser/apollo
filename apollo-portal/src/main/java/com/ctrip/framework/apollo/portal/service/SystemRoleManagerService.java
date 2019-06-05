package com.ctrip.framework.apollo.portal.service;

import com.ctrip.framework.apollo.portal.constant.PermissionType;
import com.ctrip.framework.apollo.portal.entity.po.Permission;
import com.ctrip.framework.apollo.portal.entity.po.Role;
import com.ctrip.framework.apollo.portal.entity.po.ServerConfig;
import com.ctrip.framework.apollo.portal.repository.PermissionRepository;
import com.ctrip.framework.apollo.portal.repository.RoleRepository;
import com.ctrip.framework.apollo.portal.repository.ServerConfigRepository;
import com.ctrip.framework.apollo.portal.repository.UserRoleRepository;
import com.ctrip.framework.apollo.portal.spi.UserInfoHolder;
import com.ctrip.framework.apollo.portal.util.RoleUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.HashSet;
import java.util.Set;

@Service
public class SystemRoleManagerService {
  public static final Logger logger = LoggerFactory.getLogger(SystemRoleManagerService.class);

  public static final String SYSTEM_PERMISSION_TARGET_ID = "SystemRole";

  public static final String CREATE_APPLICATION_ROLE_NAME = RoleUtils.buildCreateApplicationRoleName(PermissionType.CREATE_APPLICATION, SYSTEM_PERMISSION_TARGET_ID);

  private final ServerConfigRepository serverConfigRepository;

  @Autowired
  private RolePermissionService rolePermissionService;
  @Autowired
  private PermissionRepository  permissionRepository;

  private volatile byte isOpenCreateApplicationLimit;

  public byte getIsOpenManageAppMasterLimit() {
        return isOpenManageAppMasterLimit;
    }

  private volatile byte isOpenManageAppMasterLimit;

  @Autowired
  public SystemRoleManagerService(ServerConfigRepository serverConfigRepository) {
      this.serverConfigRepository = serverConfigRepository;
  }

  @PostConstruct
  public synchronized void initSystemRole() {

    if (permissionRepository.findTopByPermissionTypeAndTargetId(PermissionType.CREATE_APPLICATION, SYSTEM_PERMISSION_TARGET_ID) != null) {
      return;
    }

    // create application permission init
    Permission createAppPermission = new Permission();
    createAppPermission.setPermissionType(PermissionType.CREATE_APPLICATION);
    createAppPermission.setTargetId(SYSTEM_PERMISSION_TARGET_ID);
    createAppPermission.setDataChangeCreatedBy("");
    createAppPermission.setDataChangeLastModifiedBy("");
    rolePermissionService.createPermission(createAppPermission);

    //  create application role init
    Role createAppRole  = new Role();
    createAppRole.setRoleName(CREATE_APPLICATION_ROLE_NAME);
    createAppRole.setDataChangeCreatedBy("");
    createAppRole.setDataChangeLastModifiedBy("");
    Set<Long>  createAppPermissionSet = new HashSet<>();
    createAppPermissionSet.add(createAppPermission.getId());
    rolePermissionService.createRoleWithPermissions(createAppRole,  createAppPermissionSet);

    loadSystemRoleServerConfig();
  }

  @Scheduled(cron="0 0/1 * * * ?")
  public void loadSystemRoleServerConfig() {
    // load createApplication role serverConfig
    ServerConfig serverConfigOfCreateApplicationRole = serverConfigRepository.findByKey("role.create-application.enabled");
    if (serverConfigOfCreateApplicationRole != null) {
      try {
        isOpenCreateApplicationLimit = Byte.parseByte(serverConfigOfCreateApplicationRole.getValue());
        if (isOpenCreateApplicationLimit !=0 && isOpenCreateApplicationLimit !=1) {
          throw new IllegalArgumentException("apollo-portal serverConfig role.create-application.enabled is illegalArgument");
        }
      }catch (Throwable e) {
        logger.error(e.getMessage());
        logger.error("apollo-portal serverConfig role.create-application.enabled must be 0 or 1");
        isOpenCreateApplicationLimit = 0;
      }
    }
      // load manageAppMaster role serverConfig
      ServerConfig serverConfigOfManageAppMasterRole = serverConfigRepository.findByKey("role.manage-app-master.enabled");
      if (serverConfigOfManageAppMasterRole != null) {
          try {
              isOpenManageAppMasterLimit = Byte.parseByte(serverConfigOfManageAppMasterRole.getValue());
              if (isOpenManageAppMasterLimit !=0 && isOpenManageAppMasterLimit !=1) {
                  throw new IllegalArgumentException("apollo-portal serverConfig role.manage-app-master.enabled is illegalArgument");
              }
          }catch (Throwable e) {
              logger.error(e.getMessage());
              logger.error("apollo-portal serverConfig role.manage-app-master.enabled must be 0 or 1");
              isOpenManageAppMasterLimit = 0;
          }
      }
  }

  public boolean hasCreateApplicationRole(String userId) {
    if (isOpenCreateApplicationLimit == 0) {
      return true;
    }

    return rolePermissionService.userHasPermission(userId, PermissionType.CREATE_APPLICATION, SYSTEM_PERMISSION_TARGET_ID);
  }

  public boolean hasManageAppMasterRole(String userId, String appId) {
    if (isOpenManageAppMasterLimit == 0) {
      return true;
    }

    return rolePermissionService.userHasPermission(userId, PermissionType.MANAGE_APP_MASTER, appId);
  }
}
