export default function ModalSystemUninstallCtrl ($scope, $modalInstance, System) {
    "ngInject";
    $scope.state = 'ask';

    $scope.deletionOptions = [{
        name: "Yes",
        value: true
    }, {
        name: "No",
        value: false
    }];

    $scope.delete = function () {
        var deletionData = {
            systemId: $scope.systemId,
            removeS3Bucket: $scope.removeS3Bucket.value
        };

        System.delete(deletionData).then(function () {
            $scope.state = "done";
        }, function(e){
            $scope.delError = e;
            $scope.state = "failed";
        });
    }
}