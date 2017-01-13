'use strict';

angular.module('web')
    .controller('SettingsSnsNotificationController', ['$rootScope', '$state', '$scope', 'System', 'currentUser', 'Users', '$modal',
        'Configuration', 'SnsTopic', 'SnsOperation', 'SnsStatus', 'Volumes', 'SnsRule', 'toastr',
        function ($rootScope, $state, $scope, System, currentUser, Users, $modal, Configuration, SnsTopic,
                  SnsOperation, SnsStatus, Volumes, SnsRule, toastr) {

        $scope.STRINGS = {
            otherSettings: {
                empty: 'All fields are required. Please fill in empty fields.'
            }
        };
        $rootScope.isLoading = true;

        Volumes.get().then(function (results) {
            $scope.Volumes = results;
            var allVolumesIds = $scope.Volumes.map(function(volume) {
                if (volume.state === 'in-use') {
                    return volume.volumeId;
                }
            });
            allVolumesIds.splice(0, 0, "All");
            var firstUndefinedVolume = allVolumesIds.sort().indexOf(undefined);
            $scope.volumesIds = allVolumesIds.splice(0, firstUndefinedVolume);
        })
        .finally(function () {
            $rootScope.isLoading = false;
        });

        SnsTopic.get().then(function (results) {
            $scope.SnsTopic = results;
        })
        .finally(function () {
            $rootScope.isLoading = false;
        });

        SnsOperation.get().then(function (results) {
            $scope.SnsOperations = results;
        })
        .finally(function () {
            $rootScope.isLoading = false;
        });

        SnsStatus.get().then(function (results) {
            $scope.SnsStatus = results;
        })
        .finally(function () {
            $rootScope.isLoading = false;
        });

        SnsRule.get().then(function (results) {
            $scope.SnsRule = results;
        })
        .finally(function () {
            $rootScope.isLoading = false;
        });

        $scope.applyTopic = function () {
            $rootScope.isLoading = true;
            var newSnsTopic = angular.copy($scope.SnsTopic);
            SnsTopic.send(newSnsTopic).then(function () {
                $scope.state = "done";
                $rootScope.isLoading = false;
            }, function (e) {
                $scope.state = "failed";
                $rootScope.isLoading = false;
                toastr.error(({}).localizedMessage || "Invalid SNS topic name or you haven`t permission");
            });
        };

        $scope.applyRule = function () {
            $rootScope.isLoading = true;
            var newSnsRule = {
                "operation": $scope.SnsRule.operation,
                "status": $scope.SnsRule.status,
                "volumeId": $scope.SnsRule.volumeId
            };

            SnsRule.send(newSnsRule).then(function () {
                $scope.state = "done";
                $rootScope.isLoading = false;
                $scope.SnsRule.operation = '';
                $scope.SnsRule.status = '';
                $scope.SnsRule.volumeId = '';
                SnsRule.get().then(function (results) {
                    $scope.SnsRule = results;
                })
            }, function (e) {
                $scope.state = "failed";
                $rootScope.isLoading = false;
                $scope.SnsRule.operation = '';
                $scope.SnsRule.status = '';
                $scope.SnsRule.volumeId = '';
            });
        };

        $scope.removeRule = function (index) {
            $rootScope.isLoading = true;
            var deletionData = $scope.SnsRule[index].id;

            SnsRule.remove(deletionData).then(function () {
                $scope.state = "done";
                $rootScope.isLoading = false;
                SnsRule.get().then(function (results) {
                    $scope.SnsRule = results;
                })
            }, function(e){
                $scope.state = "failed";
                $rootScope.isLoading = false;
            });
        };
}]);