export default function RunConfig ($stateProvider, $urlRouterProvider) {
    "ngInject";
    //$urlRouterProvider.otherwise("/app/volumes");
    $urlRouterProvider.otherwise("/loader");//andr changes

    const authenticated = ($rootScope) => {
        "ngInject";
        if (angular.isUndefined($rootScope.getUserName())) throw "User not authorized!";
        return true;
    };

    const isConfig = ($rootScope) => {
        "ngInject";
        if (!$rootScope.isConfigState())  throw "System is not in configuration state!";
        return true;
    };

    const ssoMode = (System, $q, $rootScope) => {
        "ngInject";
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
    };

    //const doRefresh = (Users, $q, Storage, Auth, $rootScope, System) => {
    //    "ngInject";
    //    $rootScope.isLoading = true;
    //    var deferred = $q.defer();
    //
    //    var promises = [System.get(), Users.refreshCurrent()];
    //    $q.all(promises).then(function (results) {
    //        if (results[0].ssoMode != undefined) {
    //            //response for System.get
    //            Storage.save("ssoMode", {"ssoMode": results[0].ssoMode});
    //        }
    //        if (results[1].status === 200) {
    //            deferred.resolve(results[1].status)
    //            } else {
    //            deferred.resolve(false)
    //        }
    //    });
    //
    //    return deferred.promise;
    //};
    const doRefresh = (Users, $q, Storage, $rootScope) => {
        "ngInject";
        $rootScope.isLoading = true;
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
    };

    $stateProvider
        .state('app', {
            abstract: true,
            url: "/app",
            templateUrl: "partials/app.html",
            resolve: {
                authenticated: authenticated
            },
            controller: ($scope, $rootScope, Storage, toastr) => {
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
            }
        })
        .state('app.volume', {
            abstract: true,
            template: "<ui-view></ui-view>",
            url: ""
        })
        .state('app.volume.list', {
            url: "/volumes",
            templateUrl: "volumes.html",
            controller: 'VolumesController'
        })
        .state('app.volume.schedule', {
            url: "/volume-schedule/:volumeId",
            templateUrl: "schedule.html",
            controller: 'ScheduleController'
        })

        .state('app.volume.history', {
            url: "/volume-history/:volumeId",
            templateUrl: "history.html",
            controller: 'HistoryController'
        })
        .state('app.volume.tasks', {
            url: "/tasks/:volumeId",
            templateUrl: "tasks.html",
            controller: "TasksController"
        })
        .state('app.tasks', {
            url: "/tasks",
            templateUrl: "tasks.html",
            controller: "TasksController"
        })
        .state('app.settings', {
            url: "/settings",
            templateUrl: "settings.html",
            controller: "SettingsController"
        })
        .state('app.users', {
            url: "/users",
            templateUrl: "users.html",
            controller: "UserController",
            resolve: {
                ssoMode: ssoMode
            }
        })
        .state('app.logs', {
            url: "/logs",
            templateUrl: "logs.html",
            controller: "LogsController"
        })
        //TODO: move to future feature folder
        .state('config', {
            url: "/config",
            templateUrl: "config.html",
            controller: "ConfigController",
            resolve: {
                isConfig: isConfig
            }
        })
        //TODO: move to future feature folder
        .state('login', {
            url: "/auth?err",
            templateUrl: "login.html",
            controller: "LoginController",
            resolve: {
                refreshUserResult: doRefresh
            }
        })
        //TODO: andrey changes
        .state('loader', {
            url: "/loader",
            template: '<div class="loading">'+
                '<div class="text-center spinner-container">' +
                '<span class="glyphicon glyphicon-refresh text-muted spin"></span>'+
                '</div> </div>',
            controller: "LoaderController"
        })
        .state('logout', {
            url: "/logout",
            template: '<div class="loading">'+
                '<div class="text-center spinner-container">' +
                '<span class="glyphicon glyphicon-refresh text-muted spin"></span>'+
                '</div> </div>',
            controller: "LogoutController"
        })
        //TODO: move to future feature folder
        .state('registration', {
            url: "/registration",
            templateUrl: "registration.html",
            controller: "RegistrationController"
        });
}