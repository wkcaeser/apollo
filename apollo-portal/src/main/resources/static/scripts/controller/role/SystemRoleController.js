angular.module('systemRole', ['app.service', 'apollo.directive', 'app.util', 'toastr', 'angular-loading-bar'])
    .controller('SystemRoleController',
    ['$scope', '$location', '$window', 'toastr', 'AppService', 'UserService', 'AppUtil', 'EnvService',
        'PermissionService', 'SystemRoleService', function SystemRoleController($scope, $location, $window, toastr, AppService, UserService, AppUtil, EnvService,
          PermissionService, SystemRoleService) {

    $scope.addCreateApplicationBtnDisabled = false;
    $scope.deleteCreateApplicationBtnDisabled = false;

    $scope.modifySystemRoleWidgetId = 'modifySystemRoleWidgetId';

    $scope.hasCreateApplicationPermissionUserList = [];

    initPermission();

    $scope.addCreateApplicationRoleToUSer = function() {
        var user = $('.' + $scope.modifySystemRoleWidgetId).select2('data')[0];
        SystemRoleService.add_create_application_role(user.id)
            .then(
                function (value) {
                    toastr.info("添加成功");
                    getCreateApplicationRoleUsers();
                },
                function (reason) {
                    toastr.warn(AppUtil.errorMsg(reason), "添加失败");
                }
            );
    };

    $scope.deleteCreateApplicationRoleFromUser = function(userId) {
        SystemRoleService.delete_create_application_role(userId)
            .then(
                function (value) {
                    toastr.info("删除成功");
                    getCreateApplicationRoleUsers();
                },
                function (reason) {
                    toastr.warn(AppUtil.errorMsg(reason), "删除失败");
                }
            );
    };


    function getCreateApplicationRoleUsers() {
        SystemRoleService.get_create_application_role_users()
            .then(
                function (result) {
                    $scope.hasCreateApplicationPermissionUserList = result;
                },
                function (reason) {
                    toastr.warn(AppUtil.errorMsg(reason), "获取拥有创建项目的用户列表出错");
                }
            )
    }

    function initPermission() {
        PermissionService.has_root_permission()
            .then(function (result) {
                $scope.isRootUser = result.hasPermission;
            });
        getCreateApplicationRoleUsers();
    }
}]);
