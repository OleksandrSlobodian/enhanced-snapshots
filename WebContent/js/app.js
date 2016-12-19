import './components/auth/login.html';
import 'bootstrap/dist/css/bootstrap.min.css';

import WebServices from './services';
import WebFilters from './filters';
import WebDirectives from './directives';
import WebComponents from './components';
import WebControllers from './controllers';

import StatesConfig from './app-states.config';

export default angular.module('web',
    [
        'ui.router',
        'angularAwesomeSlider',
        'ui.bootstrap',
        'smart-table',
        'ngTagsInput',
        'ngStomp',
        'toastr',
        WebServices,
        WebFilters,
        WebDirectives,
        WebComponents,
        WebControllers
    ])
    .constant('BASE_URL', './')
    .constant('ITEMS_BY_PAGE', 25)
    .constant('DISPLAY_PAGES', 7)
    .config(StatesConfig)
    .config(($httpProvider) => {
        "ngInject";
        $httpProvider.defaults.headers.common["X-Requested-With"] = 'XMLHttpRequest';
        $httpProvider.interceptors.push('Interceptor');
    })
    .run(function ($rootScope, $state, $modal, $stomp, toastr, Storage, Users, System) {
        "ngInject";
        $rootScope.isLoading = true;

        System.get().then(function (results) {
            //response for System.get
            if (results.ssoMode != undefined) {
                Storage.save("ssoMode", {"ssoMode": results.ssoMode});
            }

            Users.refreshCurrent().then(function (data) {
                if (data.data && data.data.email) {
                    $state.go('app.volume.list');
                }
            });
            $rootScope.isLoading = false;
        }, function (err) {
            console.log(err);
            $rootScope.isLoading = false;
        });

        $rootScope.getUserName = function () {
            return (Storage.get("currentUser") || {}).email;
        };

        $rootScope.isConfigState = function () {
            return (Storage.get("currentUser") || {}).role === 'configurator';
        };

        $rootScope.subscribeWS = function () {
            $stomp.setDebug(function (args) {
                console.log(args);
            });

            $stomp
                .connect('/rest/ws')
                .then(function (frame) {
                    $rootScope.errorListener = $stomp.subscribe('/error', function (err) {
                        toastr.error(err.message, err.title);
                    });
                    $rootScope.taskListener = $stomp.subscribe('/task', function (msg) {
                        Storage.save('lastTaskStatus_' + msg.taskId, msg);
                        $rootScope.$broadcast("task-status-changed", msg);
                    });
                }, function (e) {
                    console.log(e);
                });
        };


        $rootScope.$on('$stateChangeError', function (e) {
            e.preventDefault();
            if (Storage.get("ssoMode")) {
                $rootScope.isLoading = true;
            } else {
                $state.go('login');
            }
        });

        $rootScope.errorListener = {};
        $rootScope.taskListener = {};
        if (angular.isDefined($rootScope.getUserName())) { $rootScope.subscribeWS(); }
    });