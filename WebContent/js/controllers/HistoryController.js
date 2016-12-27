'use strict';

angular.module('web')
    .controller('HistoryController',
        ['$scope', '$rootScope', '$q', 'Storage', 'ITEMS_BY_PAGE', 'DISPLAY_PAGES', '$stateParams', '$state', '$modal', '$filter', 'Backups', 'Tasks', 'Zones', 'Instances',
            function ($scope, $rootScope, $q, Storage, ITEMS_BY_PAGE, DISPLAY_PAGES, $stateParams, $state, $modal, $filter, Backups, Tasks, Zones, Instances) {

        $scope.maxDeleteBackupDisplay = 5;
        $scope.itemsByPage = ITEMS_BY_PAGE;
        $scope.displayedPages = DISPLAY_PAGES;
        $scope.isRestoreAction = true;

        $scope.volumeId = $stateParams.volumeId;

        $scope.restoreActions = ["Restore in AZ", "Attach to instance"];
        $scope.restoreAction = "Restore in AZ";

        $scope.textClass = {
            'false': 'Select',
            'true': 'Unselect'
        };

        $scope.iconClass = {
            'false': 'unchecked',
            'true': 'check'
        };

        $scope.selectZone = function (zone) {
            $scope.selectedZone = zone;
        };

        $scope.selectAction = function (action) {
            $scope.restoreAction = action;
            if ($scope.restoreAction == "Restore in AZ") {
                $scope.isRestoreAction = true;
            }
            $scope.isRestoreAction = false;
        };

        $scope.selectInstance = function (instance) {
            $scope.instance = instance;
        };

        $scope.isAllSelected = false;
        $scope.selectedAmount = 0;

        $scope.checkSelection = function () {
            $scope.selectedAmount = $scope.backups.filter(function (b) { return b.isSelected; }).length;
            $scope.isAllSelected = $scope.selectedAmount == $scope.backups.length;
        };

        $scope.makeSelection = function () {
            $scope.backups.forEach(function (backup) {
                backup.isSelected = !$scope.isAllSelected;
            });
            $scope.checkSelection();
        };

        $scope.deleteSelection = function () {
            $scope.selectedBackups = $scope.backups.filter(function (b) { return b.isSelected; });

            var confirmInstance = $modal.open({
                animation: true,
                templateUrl: './partials/modal.backup-delete.html',
                scope: $scope
            });

            confirmInstance.result.then(function () {
                $rootScope.isLoading = true;
                $scope.deleteErrors = [];

                var fileNames = $scope.selectedBackups.map(function (b) { return b.fileName });
                var remaining = fileNames.length;

                var checkDeleteFinished = function () {
                    $rootScope.isLoading = remaining > 0;
                    if (!$rootScope.isLoading){
                        if ($scope.deleteErrors.length) { console.log($scope.deleteErrors); }
                        var finishedInstance = $modal.open({
                            animation: true,
                            templateUrl: './partials/modal.backup-delete-result.html',
                            scope: $scope
                        });

                        finishedInstance.result.then(function () {
                            $state.go('app.tasks');
                        }, function () {
                            loadBackups();
                        });
                    }
                };

                for (var i = 0; i < fileNames.length; i++) {
                    Backups.delete(fileNames[i]).then(function () {
                        remaining--;
                        checkDeleteFinished();
                    }, function (e) {
                        $scope.deleteErrors.push(e);
                        remaining--;
                        checkDeleteFinished();
                    })
                }
            })
        };

        $rootScope.isLoading = false;
        $scope.backups = [];
        var loadBackups = function () {
            $rootScope.isLoading = true;
            Backups.getForVolume($scope.volumeId).then(function (data) {
                data.forEach(function (backup) {
                    backup.isSelected = false;
                });
                $scope.backups = data;
                $rootScope.isLoading = false;
            }, function () {
                $rootScope.isLoading = false;
            })
        };
        loadBackups();

        $scope.restore = function (backup) {
            $rootScope.isLoading = true;
            $q.all([Zones.get(), Zones.getCurrent(), Instances.get()])
                .then(function (results) {
                    $scope.zones = results[0];
                    $scope.selectedZone = results[1]["zone-name"] || "";
                    $scope.instances = results[2];
                    $scope.instance = $scope.instances[0];
                })
                .finally(function () {
                    $rootScope.isLoading = false;
                });

            $scope.objectToProcess = backup;
            var confirmInstance = $modal.open({
                animation: true,
                templateUrl: './partials/modal.history-restore.html',
                scope: $scope
            });

            confirmInstance.result.then(function () {
                var newTask = {
                    id: "",
                    priority: "",
                    //volumes: [$scope.objectToProcess.volumeId],
                    volumes: {
                        volumeId: $scope.objectToProcess.volumeId
                    },
                    backupFileName: $scope.objectToProcess.fileName,
                    type: "restore",
                    instance: $scope.instance,
                    status: "waiting",
                    schedulerManual: true,
                    schedulerName: Storage.get('currentUser').email,
                    schedulerTime: Date.now()
                };
                if ($scope.isRestoreAction) {
                    newTask.volumes.zone = $scope.selectedZone;
                } else if (!$scope.isRestoreAction) {
                    newTask.volumes.instance = $scope.instance;
                }
                Tasks.insert(newTask).then(function () {
                    var successInstance = $modal.open({
                        animation: true,
                        templateUrl: './partials/modal.task-created.html',
                        scope: $scope
                    });

                    successInstance.result.then(function () {
                        $state.go('app.tasks');
                    });
                });
            });

        };

    }]);