package com.ctrip.framework.apollo.portal.service;

import com.ctrip.framework.apollo.portal.constant.PermissionType;
import com.ctrip.framework.apollo.portal.entity.po.Role;
import com.ctrip.framework.apollo.portal.entity.po.ServerConfig;
import com.ctrip.framework.apollo.portal.entity.po.UserRole;
import com.ctrip.framework.apollo.portal.repository.RoleRepository;
import com.ctrip.framework.apollo.portal.repository.ServerConfigRepository;
import com.ctrip.framework.apollo.portal.repository.UserRoleRepository;
import com.ctrip.framework.apollo.portal.spi.UserInfoHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SystemRoleManagerService {
  public static final Logger logger = LoggerFactory.getLogger(SystemRoleManagerService.class);

  private final UserInfoHolder userInfoHolder;

  private final RoleRepository roleRepository;

  private final UserRoleRepository userRoleRepository;

  private final ServerConfigRepository serverConfigRepository;

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
  public void initSystemRole() {
    // create application role
    createApplicationRole = roleRepository
            .findTopByRoleName(PermissionType.CREATE_APPLICATION);
    if (createApplicationRole == null) {
      createApplicationRole = new Role();
      createApplicationRole.setRoleName(PermissionType.CREATE_APPLICATION);
      createApplicationRole.setDataChangeCreatedBy("default");
      createApplicationRole.setDataChangeLastModifiedBy("");
      roleRepository.save(createApplicationRole);
    }

    loadSystemRoleServerConfig();
  }

  @Scheduled(cron="0 0/1 * * * ?")
  public void loadSystemRoleServerConfig() {
    // load createApplication role serverConfig
    ServerConfig serverConfigOfCreateApplicationRole = serverConfigRepository.findByKey("role.createApplication");
    if (serverConfigOfCreateApplicationRole != null) {
      try {
        isOpenCreateApplicationLimit = Byte.parseByte(serverConfigOfCreateApplicationRole.getValue());
        if (isOpenCreateApplicationLimit !=0 && isOpenCreateApplicationLimit !=1) {
          throw new IllegalArgumentException("apollo-portal serverConfig role.createApplication is illegalArgument");
        }
      }catch (Throwable e) {
        logger.error(e.getMessage());
        logger.error("apollo-portal serverConfig role.createApplication must be 0 or 1");
        isOpenCreateApplicationLimit = 0;
      }
    }
      // load allowAddAppMaster role serverConfig
      ServerConfig serverConfigOfAllowAddAppMasterRole = serverConfigRepository.findByKey("role.allowAddAppMaster");
      if (serverConfigOfAllowAddAppMasterRole != null) {
          try {
              isOpenAllowAddAppMasterLimit = Byte.parseByte(serverConfigOfAllowAddAppMasterRole.getValue());
              if (isOpenAllowAddAppMasterLimit !=0 && isOpenAllowAddAppMasterLimit !=1) {
                  throw new IllegalArgumentException("apollo-portal serverConfig role.allowAddAppMaster is illegalArgument");
              }
          }catch (Throwable e) {
              logger.error(e.getMessage());
              logger.error("apollo-portal serverConfig role.allowAddAppMaster must be 0 or 1");
              isOpenAllowAddAppMasterLimit = 0;
          }
      }
  }

  @Transactional
  public void addCreateApplicationRole(List<String> userIds) {
    String operator = userInfoHolder.getUser().getUserId();
    Set<String> hasPermittedIds = userRoleRepository
            .findByUserIdInAndRoleId(userIds, createApplicationRole.getId())
            .stream().map(UserRole::getUserId).collect(Collectors.toSet());

    userIds.stream()
            .filter(item -> !hasPermittedIds.contains(item))
            .map(userId -> {
              ArrayList<String> params = new ArrayList<>();
              params.add(userId);
              return params;
            }).forEach(item -> {
              UserRole userRole = new UserRole();
              userRole.setUserId(item.get(0));
              userRole.setRoleId(createApplicationRole.getId());
              userRole.setDataChangeCreatedBy(operator);
              userRole.setDataChangeLastModifiedBy("");
              userRoleRepository.save(userRole);
            });
  }

  @Transactional
  public void deleteCreateApplicationRole(List<String> userIds) {
    String operator = userInfoHolder.getUser().getUserId();
    List<UserRole> userRoles = userRoleRepository
            .findByUserIdInAndRoleId(userIds, createApplicationRole.getId());
    userRoles.forEach(item -> {
      item.setDeleted(true);
      item.setDataChangeLastModifiedBy(operator);
      userRoleRepository.save(item);
    });
  }

  public List<String>  getCreateApplicationRoleUsers() {
    return userRoleRepository.findByRoleId(createApplicationRole.getId())
            .stream().map(UserRole::getUserId).collect(Collectors.toList());
  }

  public boolean hasCreateApplicationRole() {
    if (isOpenCreateApplicationLimit == 0) {
      return true;
    }

    String operator = userInfoHolder.getUser().getUserId();
    Role createApplicationRole = roleRepository
            .findTopByRoleName(PermissionType.CREATE_APPLICATION);
    ArrayList<String> userIds = new ArrayList<>();
    userIds.add(operator);
    List<UserRole> userRoles = userRoleRepository
            .findByUserIdInAndRoleId(userIds, createApplicationRole.getId());
    return userRoles.size() > 0;
  }

    @Transactional
    public void addAllowAddAppMasterRole(String appId) {
        String operator = userInfoHolder.getUser().getUserId();
        String roleName = PermissionType.ALLOW_ADD_APP_MASTER + "+" + appId;
        Role addAppMasterRole = roleRepository.findTopByRoleName(roleName);
        if (addAppMasterRole == null) {
            addAppMasterRole = new Role();
            addAppMasterRole.setRoleName(roleName);
            addAppMasterRole.setDataChangeCreatedBy(operator);
            addAppMasterRole.setDataChangeLastModifiedBy("");
            roleRepository.save(addAppMasterRole);
        }
    }

    @Transactional
    public void removeAllowAddAppMasterRole(String appId) {
        String operator = userInfoHolder.getUser().getUserId();
        String roleName = PermissionType.ALLOW_ADD_APP_MASTER + "+" + appId;
        Role addAppMasterRole = roleRepository.findTopByRoleName(roleName);
        if (addAppMasterRole != null) {
            addAppMasterRole.setDeleted(true);
            addAppMasterRole.setDataChangeLastModifiedBy(operator);
            roleRepository.save(addAppMasterRole);
        }
    }

    public boolean hasAddAppMasterRole(String appId) {
        if (isOpenAllowAddAppMasterLimit == 0) {
            return true;
        }

        String roleName = PermissionType.ALLOW_ADD_APP_MASTER + "+" + appId;
        Role addAppMasterRole = roleRepository.findTopByRoleName(roleName);
        return addAppMasterRole != null;
    }
}
