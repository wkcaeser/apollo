package com.ctrip.framework.apollo.portal.constant;

public interface PermissionType {

  /**
   * system level permission
   */
  String CREATE_APPLICATION = "CreateApplication";

  String ALLOW_ADD_APP_MASTER = "AllowAddAppMaster";

  /**
   * APP level permission
   */

  String CREATE_NAMESPACE = "CreateNamespace";

  String CREATE_CLUSTER = "CreateCluster";

  /**
   * 分配用户权限的权限
   */
  String ASSIGN_ROLE = "AssignRole";

  /**
   * namespace level permission
   */

  String MODIFY_NAMESPACE = "ModifyNamespace";

  String RELEASE_NAMESPACE = "ReleaseNamespace";


}
