package com.ctrip.framework.apollo.portal.service;

import com.ctrip.framework.apollo.portal.constant.PermissionType;
import com.ctrip.framework.apollo.portal.entity.po.Role;
import com.ctrip.framework.apollo.portal.entity.po.UserRole;
import com.ctrip.framework.apollo.portal.repository.RoleRepository;
import com.ctrip.framework.apollo.portal.repository.UserRoleRepository;
import com.ctrip.framework.apollo.portal.spi.UserInfoHolder;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SystemRoleManagerService {
private final UserInfoHolder userInfoHolder;

  private final RoleRepository roleRepository;

  private final UserRoleRepository userRoleRepository;

  private Role createApplicationRole;

  @Autowired
  public SystemRoleManagerService(RoleRepository roleRepository,
      UserRoleRepository userRoleRepository, UserInfoHolder userInfoHolder) {
    this.roleRepository = roleRepository;
    this.userRoleRepository = userRoleRepository;
    this.userInfoHolder = userInfoHolder;
  }

  @PostConstruct
  public void initCreateApplicationRole() {
    createApplicationRole = roleRepository
        .findTopByRoleName(PermissionType.CREATE_APPLICATION);
    if (createApplicationRole == null) {
      createApplicationRole = new Role();
      createApplicationRole.setRoleName(PermissionType.CREATE_APPLICATION);
      createApplicationRole.setDataChangeCreatedBy("default");
      createApplicationRole.setDataChangeLastModifiedBy("");
      roleRepository.save(createApplicationRole);
    }

  }

  public void addOrUpdateCreateApplicationRole(String userId, boolean canCreateApplication) {
    String operator = userInfoHolder.getUser().getUserId();
    ArrayList<String> userIds = new ArrayList<>();
    userIds.add(userId);
    List<UserRole> userRoles = userRoleRepository
        .findByUserIdInAndRoleId(userIds, createApplicationRole.getId());
    UserRole userRole;
    if (userRoles.size() > 0) {
      userRole = userRoles.get(0);
      userRole.setDeleted(!canCreateApplication);
      userRole.setDataChangeLastModifiedBy(operator);
    } else {
      userRole = new UserRole();
      userRole.setUserId(userId);
      userRole.setRoleId(createApplicationRole.getId());
      userRole.setDeleted(!canCreateApplication);
      userRole.setDataChangeCreatedBy(operator);
      userRole.setDataChangeLastModifiedBy("");
    }
    userRoleRepository.save(userRole);
  }


  public boolean hasCreateApplicationRole() {
    String operator = userInfoHolder.getUser().getUserId();
    Role createApplicationRole = roleRepository
        .findTopByRoleName(PermissionType.CREATE_APPLICATION);
    ArrayList<String> userIds = new ArrayList<>();
    userIds.add(operator);
    List<UserRole> userRoles = userRoleRepository
        .findByUserIdInAndRoleId(userIds, createApplicationRole.getId());
    return userRoles.size() > 0;
  }
}
