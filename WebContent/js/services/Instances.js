'use strict';

angular.module('web')
    .service('Instances', ['$q', '$http', 'BASE_URL', function ($q, $http, BASE_URL) {
        var url = BASE_URL + '/rest/instances';

        var _get = function () {
            var deferred = $q.defer();
            $http.get(url).success(function (data) {
                deferred.resolve(data);
            });
            return deferred.promise;

        };

        return {
            get: function () {
                return _get();
            }
        }
    }]);