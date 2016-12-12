'use strict';

angular.module('web')
    .controller('ScheduleController', ScheduleController);

function ScheduleController ($scope, $rootScope, $stateParams, $filter, Tasks, $modal) {
    "ngInject";

    $scope.volumeId = $stateParams.volumeId;
    $scope.schedules = [];

    var refreshList = function () {
        Tasks.getRegular($scope.volumeId).then(function (data) {
            $scope.schedules = data;
        });
    };
    refreshList();

    var scheduleToTask = function (schedule) {
        return {
            cron: schedule.cron,
            enabled: schedule.enabled,
            id: schedule.id,
            regular: "true",
            schedulerManual: "false",
            schedulerName: schedule.name,
            status: "waiting",
            type: "backup",
            volumes: [$scope.volumeId]
        }
    };

    var taskToSchedule = function (task) {
        return {
            isNew: false,
            id: task.id,
            name: task.schedulerName,
            enabled: task.enabled == 'true',
            cron: task.cron
        };
    };

    $scope.add = function () {
        $scope.scheduleToEdit = {
            isNew: true,
            id: null,
            name: '',
            enabled: true
        };

        var modalInstance = $modal.open({
            animation: true,
            templateUrl: './partials/volume-schedule-edit.modal.html',
            scope: $scope
        });

        modalInstance.result.then(function () {
            $rootScope.isLoading = true;
            var newTask = scheduleToTask($scope.scheduleToEdit);
            Tasks.insert(newTask).then(function () {
                refreshList();
                $rootScope.isLoading = false;
            }, function () {
                $rootScope.isLoading = false;
            });
        });
    };

    $scope.edit = function (task) {
        $scope.scheduleToEdit = taskToSchedule(task);

        var modalInstance = $modal.open({
            animation: true,
            templateUrl: './partials/volume-schedule-edit.modal.html',
            scope: $scope
        });

        modalInstance.result.then(function () {
            $rootScope.isLoading = true;
            var newTask = scheduleToTask($scope.scheduleToEdit);
            Tasks.update(newTask).then(function () {
                refreshList();
                $rootScope.isLoading = false;
            }, function () {
                $rootScope.isLoading = false;
            });
        });
    };

    $scope.remove = function (task) {
        $scope.scheduleToDelete = task;
        var confirmInstance = $modal.open({
            animation: true,
            templateUrl: './partials/volume-schedule-delete.modal.html',
            scope: $scope
        });

        confirmInstance.result.then(function () {
            Tasks.delete(task.id).then(function (data) {
                refreshList();
            });
        });
    };
}