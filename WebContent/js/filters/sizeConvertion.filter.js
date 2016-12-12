angular.module('web')
    .filter('sizeConvertion', sizeConvertion);

function sizeConvertion () {
    return function (data) {
        var gb = data / 1024 / 1024 / 1024;

        if (data) {
            if (gb < 1) {
                return parseInt(data / 1024 / 1024) + " MB"
            } else {
                return parseInt(data / 1024 / 1024 / 1024) + " GB"
            }
        }
    }
}