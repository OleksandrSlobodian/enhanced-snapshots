'use strict';

angular.module('web')
    .controller('SettingsController', ['$rootScope', '$state', '$scope', 'System', 'currentUser', 'Users', '$modal',
                'Configuration', 'SnsTopic', 'SnsOperation', 'SnsStatus', 'Volumes', 'SnsRule', 'toastr',
        function ($rootScope, $state, $scope, System, currentUser, Users, $modal, Configuration, SnsTopic,
                  SnsOperation, SnsStatus, Volumes, SnsRule, toastr) {
        if($state.current.name == 'app.settings') {
            $state.go('app.settings.systemInfo');
        }
        $rootScope.isLoading = false;
        var currentUser = Users.getCurrent();
        $scope.isAdmin = currentUser.role === "admin";

        $scope.STRINGS = {
            sdfs: {
                sdfsLocalCacheSize: {
                  empty: 'Local Cache Size field cannot be empty.'
                } ,
                volumeSize: {
                 empty: 'Volume Size field cannot be empty.'
                }
            },
            volumeType: {
               empty: 'Volume size for io1 volume type cannot be empty.',
               range: 'Volume size for io1 volume type must be in between 1 and 50.'
            },
            otherSettings: {
                empty: 'All fields are required. Please fill in empty fields.'
            }
        };

        $rootScope.isLoading = true;
        System.get().then(function (data) {
            // hack for handling 302 status
            if (typeof data === 'string' && data.indexOf('<html lang="en" ng-app="web"')>-1) {
                $state.go('loader');
            }
            data.ec2Instance.instanceIDs = data.ec2Instance.instanceIDs.join(", ");
            $scope.settings = data;
            if (!$scope.settings.mailConfiguration) {
                $scope.emails = [];
                $scope.settings.mailConfiguration = {
                    events: {
                        "error": false,
                        "info": false,
                        "success": false
                    }
                }
            } else {
                $scope.emails = $scope.settings.mailConfiguration.recipients || [];
            }

            $scope.initialSettings = angular.copy(data);
            $rootScope.isLoading = false;
        }, function (e) {
            console.log(e);
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
            console.log($scope.volumesIds);
        })
        .finally(function () {
            $rootScope.isLoading = false;
        });


        $scope.backup = function () {
            var modalScope = $scope.$new(true);
            $modal.open({
                animation: true,
                templateUrl: './partials/modal.system-backup.html',
                scope: modalScope,
                controller: 'modalSystemBackupCtrl'
            });
        };

        $scope.uninstall = function () {
            $modal.open({
                animation: true,
                templateUrl: './partials/modal.system-uninstall.html',
                controller: 'modalSystemUninstallCtrl'
            });

        };

        $scope.updateSettings = function () {
            var settingsUpdateModal = $modal.open({
                animation: true,
                scope: $scope,
                templateUrl: './partials/modal.settings-update.html',
                controller: 'modalSettingsUpdateCtrl'
            });

            settingsUpdateModal.result.then(function () {
                $scope.initialSettings = angular.copy($scope.settings);
            }, function () {
                $scope.initialSettings = angular.copy($scope.settings);
            });
        };

        $scope.applyTopic = function () {
            $rootScope.isLoading = true;
            var newSnsTopic = angular.copy($scope.SnsTopic);
            SnsTopic.send(newSnsTopic).then(function () {
                $scope.state = "done";
                $rootScope.isLoading = false;
            }, function (e) {
                $scope.state = "failed";
                $rootScope.isLoading = false;
                toastr.error(({}).localizedMessage || "Invalid SNS topic ARN or you haven`t permission");
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
                $scope.SnsRule.push(newSnsRule);
                $scope.SnsRule.operation = '';
                $scope.SnsRule.status = '';
                $scope.SnsRule.volumeId = '';
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
                $scope.SnsRule.splice(index, 1);
            }, function(e){
                $scope.state = "failed";
                $rootScope.isLoading = false;
            });
            $scope.SnsRule.splice(index, 1);
        };

        $scope.emailNotifications = function () {
            $scope.connectionStatus = null;
            var emailNotificationsModalInstance = $modal.open({
                animation: true,
                templateUrl: './partials/modal.email-notifications.html',
                scope: $scope
            });

            emailNotificationsModalInstance.result.then(function () {
                $scope.settings.mailConfiguration.recipients = $scope.emails;
            }, function () {
                $scope.settings = angular.copy($scope.initialSettings);
            })
        };

        $scope.testConnection = function () {
            var testData = {
                testEmail: $scope.testEmail,
                domain: $scope.settings.domain,
                mailConfiguration: $scope.settings.mailConfiguration
            };

            Configuration.check(testData).then(function (response) {
                $scope.connectionStatus = response.status;
            }, function (error) {
                $scope.connectionStatus = error.status;
            });
        };

        $scope.isNewValues = function () {
           return JSON.stringify($scope.settings) !== JSON.stringify($scope.initialSettings);
        };

    }]);