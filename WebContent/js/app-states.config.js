export default function RunConfig ($stateProvider, $urlRouterProvider) {
    "ngInject";
    $urlRouterProvider.otherwise("/app/volumes");

    var authenticated = ['$rootScope', function ($rootScope) {
        if (angular.isUndefined($rootScope.getUserName())) throw "User not authorized!";
        return true;
    }];

    var isConfig = ['$rootScope', function ($rootScope) {
        if (!$rootScope.isConfigState())  throw "System is not in configuration state!";
        return true;
    }];

    var ssoMode = ['System', '$q', '$rootScope', function (System, $q, $rootScope) {
        $rootScope.isLoading = true;
        var deferred = $q.defer();

        System.get().then(function (data) {
            $rootScope.isLoading = false;
            deferred.resolve(data);
        }, function () {
            $rootScope.isLoading = false;
            deferred.reject(false);
        });

        return deferred.promise;
    }];

    var doRefresh = ['Users', '$q', 'Storage', function (Users, $q, Storage) {
        var deferred = $q.defer();

        Users.refreshCurrent().then(function (data) {
            if (data.status === 302) {
                Storage.save("ssoMode", {"ssoMode": false});
            }
            deferred.resolve(false)
        }, function (rejection) {
            if (rejection.status === 401) {
                var isSso = rejection.data &&
                    rejection.data.loginMode &&
                    rejection.data.loginMode === "SSO";

                deferred.resolve(isSso);
            }
            deferred.resolve(false)
        });

        return deferred.promise;
    }];

    $stateProvider
        .state('app', {
            abstract: true,
            url: "/app",
            templateUrl: "partials/app.html",
            resolve: {
                authenticated: authenticated
            },
            controller:['$scope', '$rootScope', 'Storage', 'toastr', function ($scope, $rootScope, Storage, toastr) {
                "ngInject";
                $rootScope.$on('$stateChangeSuccess',
                    function(){
                        var notification = Storage.get("notification");
                        if (notification) {
                            toastr.info(notification, undefined, {
                                closeButton: true,
                                timeOut: 20000
                            });
                            Storage.remove("notification");
                        }
                    });
                $rootScope.isAdmin = (Storage.get("currentUser") || {}).role === 'admin';
            }]
        })
        .state('app.volume', {
            abstract: true,
            template: "<ui-view></ui-view>",
            url: ""
        })
        .state('app.volume.list', {
            url: "/volumes",
            templateUrl: "js/components/volumes/volume-list/volumes.html",
            controller: 'VolumesController'
        })
        .state('app.volume.schedule', {
            url: "/volume-schedule/:volumeId",
            templateUrl: "js/components/volumes/volume-schedule/schedule.html",
            controller: 'ScheduleController'
        })

        .state('app.volume.history', {
            url: "/volume-history/:volumeId",
            templateUrl: "js/components/volumes/volume-history/history.html",
            controller: 'HistoryController'
        })
        .state('app.volume.tasks', {
            url: "/tasks/:volumeId",
            templateUrl: "js/components/tasks/tasks.html",
            controller: "TasksController"
        })
        .state('app.tasks', {
            url: "/tasks",
            templateUrl: "js/components/tasks/tasks.html",
            controller: "TasksController"
        })
        .state('app.settings', {
            url: "/settings",
            templateUrl: "js/components/settings/settings.html",
            controller: "SettingsController"
        })
        .state('app.users', {
            url: "/users",
            templateUrl: "js/components/users/users.html",
            controller: "UserController",
            resolve: {
                ssoMode: ssoMode
            }
        })
        .state('app.logs', {
            url: "/logs",
            templateUrl: "js/components/logs/logs.html",
            controller: "LogsController"
        })
        //TODO: move to future feature folder
        .state('config', {
            url: "/config",
            templateUrl: "partials/config.html",
            controller: "ConfigController",
            resolve: {
                isConfig: isConfig
            }
        })
        //TODO: move to future feature folder
        .state('login', {
            url: "/login?err",
            templateUrl: "./rest/login.html",
            controller: "LoginController",
            resolve: {
                refreshUserResult: doRefresh
            }
        })
        //TODO: move to future feature folder
        .state('registration', {
            url: "/registration",
            templateUrl: "partials/registration.html",
            controller: "RegistrationController"
        });
}