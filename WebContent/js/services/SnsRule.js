angular.module('web')
    .service('SnsRule', ['$q', '$http', 'BASE_URL', function ($q, $http, BASE_URL) {
        var url = BASE_URL + 'rest/notification/sns/rule';

        var _get = function () {
            var deferred = $q.defer();
            $http.get(url).success(function (data) {
                deferred.resolve(data);
            }, function (e) {
                deferred.reject(e);
            });
            return deferred.promise;

        };

        var _send = function (rule) {
            var deferred = $q.defer();
            $http({
                url: url,
                method: "POST",
                data: rule
            }).then(function (result) {
                deferred.resolve(result.data);
            }, function (e) {
                deferred.reject(e);
            });
            return deferred.promise;
        };

        //var _remove = function (deletionData) {
        //    var deferred = $q.defer();
        //    $http({
        //        url: url,
        //        method: "DELETE",
        //        data: deletionData
        //    }).then(function (deletionData) {
        //    //$http.post(url + '/delete', deletionData).then(function (result) {
        //        deferred.resolve(deletionData.data);
        //    }, function (e) {
        //        deferred.reject(e);
        //    });
        //    return deferred.promise;
        //};
        var _remove = function (deletionData) {
            return $http.delete(url + '/' + deletionData)
                .success(function () {
                    // backup deleted
                })
                .error(function (msg) {
                    // TODO: handle 406
                });
        };

        return {
            get: function () {
                return _get();
            },
            send: function (rule) {
                return _send(rule)
            },
            remove: function (deletionData) {
                return _remove(deletionData);
            }
        }
    }]);