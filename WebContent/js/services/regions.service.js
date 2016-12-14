export default function Regions ($q, $http, BASE_URL) {
    "ngInject";
    var url = BASE_URL + './rest/regions';

    return {
        get: function () {
            var deferred = $q.defer();
            $http.get(url).success(function (data) {
                deferred.resolve(data);
            });
            return deferred.promise;

        }
    }
}