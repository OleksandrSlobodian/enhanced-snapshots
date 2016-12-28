'use strict';

angular.module('web')
    .controller('modalSystemBackupCtrl', ['$scope', '$modalInstance', 'System', 'Tasks', 'Storage', function ($scope, $modalInstance, System, Tasks, Storage) {
        $scope.state = 'ask';

        $scope.sendTask = function () {
            //var newTask = {
            //    type: "system_backup",
            //    status: "waiting",
            //    regular: "false",
            //    schedulerManual: true,
            //    schedulerName: Storage.get('currentUser').email,
            //    schedulerTime: Date.now()
            //};
            System.backup().then(function () {
                $scope.state = "done";
            }, function () {
                $scope.state = "failed";
            });

        }
    }]);