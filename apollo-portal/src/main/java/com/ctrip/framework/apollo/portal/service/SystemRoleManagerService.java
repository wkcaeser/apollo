package com.ctrip.framework.apollo.portal.service;

import com.ctrip.framework.apollo.portal.constant.PermissionType;
import com.ctrip.framework.apollo.portal.entity.po.Permission;
import com.ctrip.framework.apollo.portal.entity.po.Role;
import com.ctrip.framework.apollo.portal.repository.PermissionRepository;
import com.ctrip.framework.apollo.portal.repository.ServerConfigRepository;
import com.ctrip.framework.apollo.portal.util.RoleUtils;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SystemRoleManagerService {
  public static final Logger logger = LoggerFactory.getLogger(SystemRoleManagerService.class);

  private static final String SYSTEM_PERMISSION_TARGET_ID = "SystemRole";

  public static final String CREATE_APPLICATION_ROLE_NAME = RoleUtils.buildCreateApplicationRoleName(PermissionType.CREATE_APPLICATION, SYSTEM_PERMISSION_TARGET_ID);

  private static final String CREATE_APPLICATION_LIMIT_SWITCH_KEY = "role.create-application.enabled";
  private static final String MANAGE_APP_MASTER_LIMIT_SWITCH_KEY = "role.manage-app-master.enabled";

  private final ServerConfigRepository serverConfigRepository;

  private final RolePermissionService rolePermissionService;
  private final PermissionRepository  permissionRepository;

  @Autowired
  private PortalDBPropertySource portalDBPropertySource;


  @Autowired
  public SystemRoleManagerService(ServerConfigRepository serverConfigRepository, RolePermissionService rolePermissionService, PermissionRepository permissionRepository) {
      this.serverConfigRepository = serverConfigRepository;
      this.rolePermissionService = rolePermissionService;
      this.permissionRepository = permissionRepository;
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
  }

  public byte getCreateApplicationRoleSwitchValue() {
    if (!portalDBPropertySource.containsProperty(CREATE_APPLICATION_LIMIT_SWITCH_KEY)) {
        return 0;
    }
    Object value = portalDBPropertySource.getProperty(CREATE_APPLICATION_LIMIT_SWITCH_KEY);

    byte isOpenCreateApplicationLimit;
    try {
        isOpenCreateApplicationLimit = Byte.parseByte(String.valueOf(value));
        if (isOpenCreateApplicationLimit !=0 && isOpenCreateApplicationLimit !=1) {
            throw new IllegalArgumentException("apollo-portal serverConfig role.create-application.enabled is illegalArgument");
        }
    }catch (Throwable e) {
        logger.error(e.getMessage());
        logger.error("apollo-portal serverConfig role.create-application.enabled must be 0 or 1");
        isOpenCreateApplicationLimit = 0;
    }
    return isOpenCreateApplicationLimit;
  }

  public byte getManageAppMasterRoleSwitchValue() {
    if (!portalDBPropertySource.containsProperty(MANAGE_APP_MASTER_LIMIT_SWITCH_KEY)) {
        return 0;
    }
    Object value = portalDBPropertySource.getProperty(MANAGE_APP_MASTER_LIMIT_SWITCH_KEY);
    byte isOpenManageAppMasterLimit;
    try {
      isOpenManageAppMasterLimit = Byte.parseByte(String.valueOf(value));
      if (isOpenManageAppMasterLimit !=0 && isOpenManageAppMasterLimit !=1) {
        throw new IllegalArgumentException("apollo-portal serverConfig role.manage-app-master.enabled is illegalArgument");
      }
    }catch (Throwable e) {
      logger.error(e.getMessage());
      logger.error("apollo-portal serverConfig role.manage-app-master.enabled must be 0 or 1");
      isOpenManageAppMasterLimit = 0;
    }
    return isOpenManageAppMasterLimit;
  }

  public boolean hasCreateApplicationRole(String userId) {
    if (getCreateApplicationRoleSwitchValue() == 0) {
      return true;
    }

    return rolePermissionService.userHasPermission(userId, PermissionType.CREATE_APPLICATION, SYSTEM_PERMISSION_TARGET_ID);
  }

  public boolean hasManageAppMasterRole(String userId, String appId) {
    if (getManageAppMasterRoleSwitchValue() == 0) {
      return true;
    }

    return rolePermissionService.userHasPermission(userId, PermissionType.MANAGE_APP_MASTER, appId);
  }
}
