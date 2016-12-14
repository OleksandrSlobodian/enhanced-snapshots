export default function ModalSystemBackupCtrl($scope, $modalInstance, Tasks, Storage) {
    "ngInject";
    $scope.state = 'ask';

    $scope.sendTask = function () {
        var newTask = {
            type: "system_backup",
            status: "waiting",
            regular: "false",
            schedulerManual: true,
            schedulerName: Storage.get('currentUser').email,
            schedulerTime: Date.now()
        };
        Tasks.insert(newTask).then(function () {
            $scope.state = "done";
        }, function () {
            $scope.state = "failed";
        });

    }
}