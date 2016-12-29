'use strict';

angular.module('web')
    .controller('modalSystemBackupCtrl', ['$scope', '$modalInstance', 'System', function ($scope, $modalInstance, System) {
        $scope.state = 'ask';

        $scope.sendTask = function () {
            System.backup().then(function () {
                $scope.state = "done";
            }, function () {
                $scope.state = "failed";
            });

        }
    }]);