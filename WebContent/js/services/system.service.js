'use strict';

angular.module('web')
    .service('System', system);

function system ($q, $http, BASE_URL) {
    "ngInject";
    var url = BASE_URL + './rest/system';

    var _get = function () {
        var deferred = $q.defer();
        $http.get(url).then(function (result) {
            deferred.resolve(result.data);
        }, function (e) {
            deferred.reject(e);
        });
        return deferred.promise;
    };

    var _send = function (volumeSettings) {
        var deferred = $q.defer();
        $http({
            url: url,
            method: "POST",
            data: volumeSettings
        }).then(function (result) {
            deferred.resolve(result.data);
        }, function (e) {
            deferred.reject(e);
        });
        return deferred.promise;
    };

    var _delete = function (deletionData) {
        var deferred = $q.defer();
        $http.post(url + '/delete', deletionData).then(function (result) {
            deferred.resolve(result.data);
        }, function (e) {
            deferred.reject(e);
        });
        return deferred.promise;
    };

    return {
        get: function () {
            return _get();
        },
        send: function (volumeSettings) {
            return _send(volumeSettings)
        },
        delete: function (deletionData) {
            return _delete(deletionData);
        }
    }
}