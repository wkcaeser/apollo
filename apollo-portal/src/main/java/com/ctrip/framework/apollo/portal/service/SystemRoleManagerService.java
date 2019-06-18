package com.ctrip.framework.apollo.portal.service;

import com.ctrip.framework.apollo.portal.component.config.PortalConfig;
import com.ctrip.framework.apollo.portal.constant.PermissionType;
import com.ctrip.framework.apollo.portal.entity.po.Permission;
import com.ctrip.framework.apollo.portal.entity.po.Role;
import com.ctrip.framework.apollo.portal.repository.PermissionRepository;
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

  public static final String CREATE_APPLICATION_LIMIT_SWITCH_KEY = "role.create-application.enabled";
  public static final String MANAGE_APP_MASTER_LIMIT_SWITCH_KEY = "role.manage-app-master.enabled";

  private final RolePermissionService rolePermissionService;
  private final PermissionRepository  permissionRepository;

  private final PortalConfig portalConfig;


  @Autowired
  public SystemRoleManagerService(RolePermissionService rolePermissionService,
                                  PermissionRepository permissionRepository, PortalConfig portalConfig) {
    this.rolePermissionService = rolePermissionService;
    this.permissionRepository = permissionRepository;
    this.portalConfig = portalConfig;
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

  public boolean isCreateApplicationPermissionEnabled() {
    return portalConfig.isCreateApplicationPermissionEnabled();
  }

  public boolean isManageAppMasterPermissionEnabled() {
    return portalConfig.isManageAppMasterPermissionEnabled();
  }

  public boolean hasCreateApplicationPermission(String userId) {
    if (!isCreateApplicationPermissionEnabled()) {
      return true;
    }

    return rolePermissionService.userHasPermission(userId, PermissionType.CREATE_APPLICATION, SYSTEM_PERMISSION_TARGET_ID);
  }

  public boolean hasManageAppMasterPermission(String userId, String appId) {
    if (!isManageAppMasterPermissionEnabled()) {
      return true;
    }

    return rolePermissionService.userHasPermission(userId, PermissionType.MANAGE_APP_MASTER, appId);
  }
}
