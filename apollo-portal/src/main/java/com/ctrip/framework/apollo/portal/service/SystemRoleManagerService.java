package com.ctrip.framework.apollo.portal.service;

import com.ctrip.framework.apollo.portal.constant.PermissionType;
import com.ctrip.framework.apollo.portal.entity.po.Permission;
import com.ctrip.framework.apollo.portal.entity.po.Role;
import com.ctrip.framework.apollo.portal.entity.po.ServerConfig;
import com.ctrip.framework.apollo.portal.entity.po.UserRole;
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
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SystemRoleManagerService {
  public static final Logger logger = LoggerFactory.getLogger(SystemRoleManagerService.class);

  public static final String SYSTEM_PERMISSION_TARGET_ID = "SystemRole";

  public static final String CREATE_APPLICATION_ROLE_NAME = RoleUtils.buildCreateApplicationRoleName(PermissionType.CREATE_APPLICATION, SYSTEM_PERMISSION_TARGET_ID);

  private final UserInfoHolder userInfoHolder;

  private final RoleRepository roleRepository;

  private final UserRoleRepository userRoleRepository;

  private final ServerConfigRepository serverConfigRepository;

  @Autowired
  private RolePermissionService rolePermissionService;
  @Autowired
  private PermissionRepository  permissionRepository;

  private Role createApplicationRole;

  private volatile byte isOpenCreateApplicationLimit;

    public byte getIsOpenAllowAddAppMasterLimit() {
        return isOpenAllowAddAppMasterLimit;
    }

    private volatile byte isOpenAllowAddAppMasterLimit;

  @Autowired
  public SystemRoleManagerService(RoleRepository roleRepository,
                                  UserRoleRepository userRoleRepository, UserInfoHolder userInfoHolder, ServerConfigRepository serverConfigRepository) {
    this.roleRepository = roleRepository;
    this.userRoleRepository = userRoleRepository;
    this.userInfoHolder = userInfoHolder;
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
      // load allowAddAppMaster role serverConfig
      ServerConfig serverConfigOfAllowAddAppMasterRole = serverConfigRepository.findByKey("role.manage-app-master.enabled");
      if (serverConfigOfAllowAddAppMasterRole != null) {
          try {
              isOpenAllowAddAppMasterLimit = Byte.parseByte(serverConfigOfAllowAddAppMasterRole.getValue());
              if (isOpenAllowAddAppMasterLimit !=0 && isOpenAllowAddAppMasterLimit !=1) {
                  throw new IllegalArgumentException("apollo-portal serverConfig role.manage-app-master.enabled is illegalArgument");
              }
          }catch (Throwable e) {
              logger.error(e.getMessage());
              logger.error("apollo-portal serverConfig role.manage-app-master.enabled must be 0 or 1");
              isOpenAllowAddAppMasterLimit = 0;
          }
      }
  }

  public boolean hasCreateApplicationRole(String userId) {
    if (isOpenCreateApplicationLimit == 0) {
      return true;
    }

    return rolePermissionService.userHasPermission(userId, PermissionType.CREATE_APPLICATION, SYSTEM_PERMISSION_TARGET_ID);
  }

  public boolean hasAddAppMasterRole(String userId, String appId) {
    if (isOpenAllowAddAppMasterLimit == 0) {
      return true;
    }

    return rolePermissionService.userHasPermission(userId, PermissionType.MANAGE_APP_MASTER, appId);
  }
}
