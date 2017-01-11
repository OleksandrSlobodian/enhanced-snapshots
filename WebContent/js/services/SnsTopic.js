angular.module('web')
    .service('SnsTopic', ['$q', '$http', 'BASE_URL', function ($q, $http, BASE_URL) {
        var url = BASE_URL + 'rest/notification/sns/settings';

        var _get = function () {
            var deferred = $q.defer();
            $http.get(url).success(function (data) {
                deferred.resolve(data);
            }, function (e) {
                deferred.reject(e);
            });
            return deferred.promise;

        };

        return {
            get: function () {
                return _get();
            }
        }
    }]);