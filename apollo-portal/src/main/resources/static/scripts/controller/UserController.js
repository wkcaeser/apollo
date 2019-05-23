user_module.controller('UserController',
                      ['$scope', '$window', 'toastr', 'AppUtil', 'UserService',
                       UserController]);

function UserController($scope, $window, toastr, AppUtil, UserService) {

    $scope.user = {};
    
    $scope.createOrUpdateUser = function () {
        UserService.createOrUpdateUser($scope.user).then(function (result) {
            toastr.success("操作成功");
        }, function (result) {
            AppUtil.showErrorMsg(result, "操作失败");
        })

    }
}
