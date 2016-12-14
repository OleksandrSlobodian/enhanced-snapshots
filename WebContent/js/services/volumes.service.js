export default function Volumes ($q, $http, Storage, BASE_URL) {
    "ngInject";
    var url = BASE_URL + './rest/volume';

    return {
        get: function () {
            var deferred = $q.defer();
            $http.get(url).then(function (result) {
                var data = result.data;
                deferred.resolve(data);
            }, function (data, status) {
                deferred.reject(data, status)
            });
            return deferred.promise;
        }
    }
}