'use strict';

angular.module('web')
    .service('Zones', zones);

function zones ($q, $http, BASE_URL) {
    "ngInject";
    var url = BASE_URL + './rest/zones';

    var _get = function (extension) {
        var deferred = $q.defer();
        $http.get(url + (extension || "")).success(function (data) {
            deferred.resolve(data);
        }).error(function (err) {
            deferred.reject(err);
        });
        return deferred.promise;
    };

    return {
        get: function () {
            return _get();
        },
        getCurrent: function () {
            return _get("/current");
        }
    }
}