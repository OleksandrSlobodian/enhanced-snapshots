'use strict';

angular.module('web')
    .service('Retention', retention);

function retention ($q, $http, BASE_URL) {
    "ngInject";
    var url = BASE_URL + 'rest/retention';

    var _get = function (id) {
        var deferred = $q.defer();
        $http({
            url: url + "/" + id,
            method: 'GET'
        }).success(function (data) {
            deferred.resolve(data);
        });
        return deferred.promise;
    };


    var _update = function (item) {
        return $http({
            url: url,
            method: "POST",
            data: item
        });
    };

    return {
        get: function (id) {
            return _get(id);
        },
        update: function (item) {
            return _update(item);
        }

    }
}