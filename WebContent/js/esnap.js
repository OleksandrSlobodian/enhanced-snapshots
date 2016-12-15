var app = angular.module('web', ['ui.router', 'angularAwesomeSlider', 'ui.bootstrap', 'smart-table', 'ngTagsInput', 'ngStomp', 'toastr']);

app.constant('BASE_URL', './');

// Settings for table paging
app.constant('ITEMS_BY_PAGE', 25);
app.constant('DISPLAY_PAGES', 7);

app.config(['$stateProvider', '$urlRouterProvider', '$httpProvider', function ($stateProvider, $urlRouterProvider, $httpProvider) {
    $urlRouterProvider.otherwise("/loader");

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

    var currentUser = ['$q', 'Users', function ($q, Users) {
        var currentUser = Users.getCurrent();
        return Boolean(currentUser) ? $q.resolve(currentUser) : $q.reject();
    }];

    var doRefresh = ['Users', '$q', 'Storage', 'Auth', '$rootScope', 'System',
        function (Users, $q, Storage, Auth, $rootScope, System) {
            $rootScope.isLoading = true;
            var deferred = $q.defer();

            var promises = [System.get(), Users.refreshCurrent()];
            $q.all(promises).then(function (results) {

                if (results[0].ssoMode != undefined) {
                    //response for System.get
                    Storage.save("ssoMode", {"ssoMode": results[0].ssoMode});
                }

                if (results[1].status === 200) {
                    deferred.resolve(results[1].status)
                } else {
                    deferred.resolve(false)
                }
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
            controller: ['$scope', '$rootScope', 'Storage', 'toastr', function ($scope, $rootScope, Storage, toastr) {
                $rootScope.$on('$stateChangeSuccess',
                    function () {
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
            templateUrl: "partials/volumes.html",
            controller: 'VolumesController',
            resolve: {
                currentUser: currentUser
            }
        })
        .state('app.volume.schedule', {
            url: "/schedule/:volumeId",
            templateUrl: "partials/schedule.html",
            controller: 'ScheduleController',
            resolve: {
                currentUser: currentUser
            }
        })

        .state('app.volume.history', {
            url: "/history/:volumeId",
            templateUrl: "partials/history.html",
            controller: 'HistoryController',
            resolve: {
                currentUser: currentUser
            }
        })
        .state('app.volume.tasks', {
            url: "/tasks/:volumeId",
            templateUrl: "partials/tasks.html",
            controller: "TasksController",
            resolve: {
                currentUser: currentUser
            }
        })
        .state('app.tasks', {
            url: "/tasks",
            templateUrl: "partials/tasks.html",
            controller: "TasksController",
            resolve: {
                currentUser: currentUser
            }
        })
        .state('app.settings', {
            url: "/settings",
            templateUrl: "partials/settings.html",
            controller: "SettingsController",
            resolve: {
                currentUser: currentUser
            }
        })
        .state('app.users', {
            url: "/users",
            templateUrl: "partials/users.html",
            controller: "UserController",
            resolve: {
                ssoMode: ssoMode,
                currentUser: currentUser
            }
        })
        .state('app.logs', {
            url: "/logs",
            templateUrl: "partials/logs.html",
            controller: "LogsController",
            resolve: {
                currentUser: currentUser
            }
        })
        .state('config', {
            url: "/config",
            templateUrl: "partials/config.html",
            controller: "ConfigController",
            resolve: {
                isConfig: isConfig
            }
        })
        .state('login', {
            url: "/login?err",
            templateUrl: "partials/login.html",
            controller: "LoginController",
            resolve: {
                refreshUserResult: doRefresh
            }
        })
        .state('loader', {
            url: "/loader",
            template: '<div class="loading">' +
            '<div class="text-center spinner-container">' +
            '<span class="glyphicon glyphicon-refresh text-muted spin"></span>' +
            '</div> </div>',
            controller: "LoaderController"
        })
        .state('logout', {
            url: "/logout",
            template: '<div class="loading">' +
            '<div class="text-center spinner-container">' +
            '<span class="glyphicon glyphicon-refresh text-muted spin"></span>' +
            '</div> </div>',
            controller: "LogoutController"
        })
        .state('registration', {
            url: "/registration",
            templateUrl: "partials/registration.html",
            controller: "RegistrationController"
        });

    $httpProvider.defaults.headers.common["X-Requested-With"] = 'XMLHttpRequest';
    $httpProvider.interceptors.push('Interceptor');
}])
    .run(['$rootScope', '$state', '$modal', '$stomp', 'toastr', 'Storage', 'Users', 'System', '$q',
        function ($rootScope, $state, $modal, $stomp, toastr, Storage, Users, System, $q) {
            $rootScope.isLoading = true;

            var isUserSaved = Storage.get("currentUser");
            var isSsoSaved = Storage.get("ssoMode");

            if (!isUserSaved || !isSsoSaved) {
                var promises = [System.get(), Users.refreshCurrent()];
                $q.all(promises).then(function (results) {
                    if (results[0].ssoMode != undefined) {
                        //response for System.get
                        Storage.save("ssoMode", {"ssoMode": results[0].ssoMode});
                    }

                    //response for Users.refreshCurrent
                    if (results[1].data && results[1].data.email) {
                        $state.go('app.volume.list');
                    } else {
                        $state.go('login');
                    }

                    $rootScope.isLoading = false;
                }, function (err) {
                    console.log(err);
                    $rootScope.isLoading = false;
                });
            }

            $rootScope.getUserName = function () {
                return (Storage.get("currentUser") || {}).email;
            };

            $rootScope.isConfigState = function () {
                return (Storage.get("currentUser") || {}).role === 'configurator';
            };

            $rootScope.subscribeWS = function () {
                $stomp.setDebug(function (args) {
                    // console.log(args);
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
            if (angular.isDefined($rootScope.getUserName())) {
                $rootScope.subscribeWS();
            }
        }]);
'use strict';
angular.module('web')
    .controller('ConfigController', ['$scope', 'Volumes', 'Configuration', '$modal', '$state', 'Storage', function ($scope, Volumes, Configuration, $modal, $state, Storage) {
        var DELAYTIME = 600 * 1000;
        $scope.STRINGS = {
            s3: {
                empty: 'Bucket name field cannot be empty',
                new: 'New bucket will be created as',
                existing: 'Existing bucket will be used'
            },
            db: {
                isValid: {
                    true: 'Database exists',
                    false: 'No database found'
                },
                hasAdminUser: {
                    false: 'You will need to create a new user on the next step'
                }
            },
            sdfs: {
                name: {
                    new: 'New volume will be created as',
                    existing: 'Existing volume will be used'
                },
                point: 'At mounting point:',
                size: 'Would you like to update volume size?'
            }
        };

        $scope.iconClass = {
            true: 'ok',
            false: 'cog'
        };

        $scope.statusColorClass = {
            true: 'success',
            false: 'danger'
        };

        $scope.isCustomBucketName = false;
        $scope.isNameWrong = false;
        $scope.wrongNameMessage = '';
        $scope.isValidInstance = true;
        $scope.selectBucket = function (bucket) {
            $scope.selectedBucket = bucket;
            Configuration.get('bucket/' + encodeURIComponent(bucket.bucketName) + '/metadata').then(function (result) {
                //property settings.db.hasAdmin is a legacy code which should be changed. Currently this field is replaced
                // with value from result.data.hasAdmin of this function. Speak to Kostya for more details
                $scope.settings.db.hasAdmin = result.data.hasAdmin;
            }, function (err) {
                console.warn(err);
            });
        };

        if (angular.isUndefined($scope.isSSO)) {
            $scope.isSSO = false;
        }

        var wizardCreationProgress = function () {
            var modalInstance = $modal.open({
                animation: true,
                backdrop: false,
                templateUrl: './partials/modal.wizard-progress.html',
                scope: $scope
            });

            modalInstance.result.then(function () {
                $state.go('login')
            }, function () {
            });

            return modalInstance
        };

        var getCurrentConfig = function () {
            $scope.progressState = 'loading';
            var loader = wizardCreationProgress();

            Configuration.get('current').then(function (result, status) {
                $scope.settings = result.data;
                $scope.selectedBucket = (result.data.s3 || [])[0] || {};
                if (!$scope.settings.mailConfiguration) {
                    $scope.emails = [];
                    $scope.settings.mailConfiguration = {
                        events: {
                            "error": false,
                            "info": false,
                            "success": false
                        }
                    }
                } else {
                    $scope.emails = $scope.settings.mailConfiguration.recipients || [];
                }

                loader.dismiss();
            }, function (data, status) {
                $scope.isValidInstance = false;
                $scope.invalidMessage = data.data.localizedMessage;
                loader.dismiss();
            });
        };

        getCurrentConfig();

        $scope.emailNotifications = function () {
            $scope.connectionStatus = null;
            var emailNotificationsModalInstance = $modal.open({
                animation: true,
                templateUrl: './partials/modal.email-notifications.html',
                scope: $scope,
                backdrop: false
            });

            emailNotificationsModalInstance.result.then(function () {
                $scope.settings.mailConfiguration.recipients = $scope.emails;
            })
        };

        $scope.testConnection = function () {
            $scope.settings.mailConfiguration.recipients = $scope.emails;
            var testData = {
                testEmail: $scope.testEmail,
                domain: $scope.settings.domain,
                mailConfiguration: $scope.settings.mailConfiguration
            };

            Configuration.check(testData).then(function (response) {
                $scope.connectionStatus = response.status;
            }, function (error) {
                $scope.connectionStatus = error.status;
            });
        };

        $scope.sendSettings = function () {
            var volumeSize = $scope.isNewVolumeSize ? $scope.sdfsNewSize : $scope.settings.sdfs.volumeSize;

            var getMailConfig = function () {
                if (!$scope.settings.mailConfiguration.fromMailAddress) {
                    return null;
                } else {
                    return $scope.settings.mailConfiguration
                }
            };

            var getClusterNodes = function () {

                if ($scope.settings.clusterMode) {
                    var clusterNodes;

                    clusterNodes = {
                        minNodeNumber: $scope.settings.cluster.minNodeNumber,
                        maxNodeNumber: $scope.settings.cluster.maxNodeNumber
                    };

                    return clusterNodes
                }
                return null
            };

            var settings = {
                bucketName: $scope.selectedBucket.bucketName,
                volumeSize: volumeSize,
                cluster: getClusterNodes(),
                sungardasSSO: !!$scope.sungardasSSO,
                ssoMode: $scope.isSSO,
                domain: $scope.settings.domain,
                spEntityId: $scope.entityId || null,
                mailConfiguration: getMailConfig()
            };

            if (!$scope.settings.db.hasAdmin && !$scope.isSSO) {
                $scope.userToEdit = {
                    isNew: true,
                    admin: true
                };

                var userModalInstance = $modal.open({
                    animation: true,
                    templateUrl: './partials/modal.user-edit.html',
                    scope: $scope
                });

                userModalInstance.result.then(function () {
                    settings.user = $scope.userToEdit;
                    delete settings.user.isNew;

                    $scope.progressState = 'running';
                    Configuration.send('current', settings, DELAYTIME).then(function () {
                        $scope.progressState = 'success';
                    }, function () {
                        $scope.progressState = 'failed';
                    });

                    wizardCreationProgress();

                });
            } else {
                $scope.progressState = 'running';

                if (settings.ssoMode) {
                    settings.user = {email: $scope.adminEmail}
                }

                $scope.progressState = 'running';


                Configuration.send('current', settings, null, $scope.settings.sso).then(function () {
                    $scope.progressState = 'success';
                    Storage.save("ssoMode", {ssoMode: $scope.isSSO});
                }, function (data, status) {
                    $scope.progressState = 'failed';
                });

                wizardCreationProgress();
            }
        };

        $scope.validateName = function () {
            Configuration.get('bucket/' + encodeURIComponent($scope.selectedBucket.bucketName)).then(function (result) {
                $scope.isNameWrong = !result.data.valid;
                $scope.wrongNameMessage = result.data.message;
            }, function (data, status) {
            });
        };
    }]);
'use strict';

angular.module('web')
    .controller('HistoryController',
        ['$scope', '$rootScope', '$q', 'Storage', 'ITEMS_BY_PAGE', 'DISPLAY_PAGES', '$stateParams', '$state', '$modal', '$filter', 'Backups', 'Tasks', 'Zones',
            function ($scope, $rootScope, $q, Storage, ITEMS_BY_PAGE, DISPLAY_PAGES, $stateParams, $state, $modal, $filter, Backups, Tasks, Zones) {
                $scope.maxDeleteBackupDisplay = 5;
                $scope.itemsByPage = ITEMS_BY_PAGE;
                $scope.displayedPages = DISPLAY_PAGES;

                $scope.volumeId = $stateParams.volumeId;

                $scope.textClass = {
                    'false': 'Select',
                    'true': 'Unselect'
                };

                $scope.iconClass = {
                    'false': 'unchecked',
                    'true': 'check'
                };

                $scope.selectZone = function (zone) {
                    $scope.selectedZone = zone;
                };

                $scope.isAllSelected = false;
                $scope.selectedAmount = 0;

                $scope.checkSelection = function () {
                    $scope.selectedAmount = $scope.backups.filter(function (b) {
                        return b.isSelected;
                    }).length;
                    $scope.isAllSelected = $scope.selectedAmount == $scope.backups.length;
                };

                $scope.makeSelection = function () {
                    $scope.backups.forEach(function (backup) {
                        backup.isSelected = !$scope.isAllSelected;
                    });
                    $scope.checkSelection();
                };

                $scope.deleteSelection = function () {
                    $scope.selectedBackups = $scope.backups.filter(function (b) {
                        return b.isSelected;
                    });

                    var confirmInstance = $modal.open({
                        animation: true,
                        templateUrl: './partials/modal.backup-delete.html',
                        scope: $scope
                    });

                    confirmInstance.result.then(function () {
                        $rootScope.isLoading = true;
                        $scope.deleteErrors = [];

                        var fileNames = $scope.selectedBackups.map(function (b) {
                            return b.fileName
                        });
                        var remaining = fileNames.length;

                        var checkDeleteFinished = function () {
                            $rootScope.isLoading = remaining > 0;
                            if (!$rootScope.isLoading) {
                                if ($scope.deleteErrors.length) {
                                    console.log($scope.deleteErrors);
                                }
                                var finishedInstance = $modal.open({
                                    animation: true,
                                    templateUrl: './partials/modal.backup-delete-result.html',
                                    scope: $scope
                                });

                                finishedInstance.result.then(function () {
                                    $state.go('app.tasks');
                                }, function () {
                                    loadBackups();
                                });
                            }
                        };

                        for (var i = 0; i < fileNames.length; i++) {
                            Backups.delete(fileNames[i]).then(function () {
                                remaining--;
                                checkDeleteFinished();
                            }, function (e) {
                                $scope.deleteErrors.push(e);
                                remaining--;
                                checkDeleteFinished();
                            })
                        }
                    })
                };

                $rootScope.isLoading = false;
                $scope.backups = [];
                var loadBackups = function () {
                    $rootScope.isLoading = true;
                    Backups.getForVolume($scope.volumeId).then(function (data) {
                        data.forEach(function (backup) {
                            backup.isSelected = false;
                        });
                        $scope.backups = data;
                        $rootScope.isLoading = false;
                    }, function () {
                        $rootScope.isLoading = false;
                    })
                };
                loadBackups();

                $scope.restore = function (backup) {
                    $rootScope.isLoading = true;
                    $q.all([Zones.get(), Zones.getCurrent()])
                        .then(function (results) {
                            $scope.zones = results[0];
                            $scope.selectedZone = results[1]["zone-name"] || "";
                        })
                        .finally(function () {
                            $rootScope.isLoading = false;
                        });

                    $scope.objectToProcess = backup;
                    var confirmInstance = $modal.open({
                        animation: true,
                        templateUrl: './partials/modal.history-restore.html',
                        scope: $scope
                    });

                    confirmInstance.result.then(function () {
                        var newTask = {
                            id: "",
                            priority: "",
                            volumes: [$scope.objectToProcess.volumeId],
                            backupFileName: $scope.objectToProcess.fileName,
                            type: "restore",
                            zone: $scope.selectedZone,
                            status: "waiting",
                            schedulerManual: true,
                            schedulerName: Storage.get('currentUser').email,
                            schedulerTime: Date.now()
                        };
                        Tasks.insert(newTask).then(function () {
                            var successInstance = $modal.open({
                                animation: true,
                                templateUrl: './partials/modal.task-created.html',
                                scope: $scope
                            });

                            successInstance.result.then(function () {
                                $state.go('app.tasks');
                            });
                        });
                    });

                };

            }]);
'use strict';

angular.module('web')
    .controller('LoaderController', ['Users', '$state', '$q', 'System', 'Storage', 'Auth',
        function (Users, $state, $q, System, Storage, Auth) {

            var promises = [System.get(), Users.refreshCurrent()];
            $q.all(promises).then(function (results) {
                if (results[0].ssoMode != undefined) {
                    //response for System.get
                    Storage.save("ssoMode", {"ssoMode": results[0].ssoMode});
                }
                //response for Users.refreshCurrent
                if (typeof(results[1].data) != 'string' && results[1].status === 200) {
                    $state.go('app.volume.list');
                } else {
                    Auth.logOut();
                    $state.go('login');
                }
            }, function (err) {
            });
        }]);
'use strict';

angular.module('web')
    .controller('LoginController', ['$rootScope', '$scope', '$state', '$stateParams', '$stomp', 'Auth', 'System', 'Storage', 'toastr', '$window', 'refreshUserResult',
        function ($rootScope, $scope, $state, $stateParams, $stomp, Auth, System, Storage, toastr, $window, refreshUserResult) {

            $rootScope.isLoading = true;

            //LOGGING OUT ---------------------
            if ($stateParams.err && $stateParams.err == 'session') {
                toastr.warning('You were logged out. Please re-login', 'Session expired.');
            }

            var currentUser = Storage.get("currentUser");
            var ssoMode = Storage.get("ssoMode");

            if (currentUser && currentUser.length > 1) {
                if (ssoMode && ssoMode.ssoMode) {
                    $window.location.href = "/saml/logout";
                }
                Auth.logOut();
            }
            //------------------------------------

            // Show loader instead of login page if ssoMode is true ----------
            if (refreshUserResult === true) {
                $rootScope.isLoading = true;
                window.location = "/saml/login";
            } else {
                if (refreshUserResult === 200 && currentUser && ssoMode && ssoMode.ssoMode != undefined) {
                    $state.go('app.volume.list');
                } else {
                    $rootScope.isLoading = !!(ssoMode && ssoMode.ssoMode);
                }
            }

            //---------------------------------------------

            $scope.clearErr = function () {
                $scope.error = "";
            };

            $scope.login = function () {
                Auth.logIn($scope.email, $scope.password).then(function (data) {

                    if (data.role === 'configurator') {
                        $state.go('config');
                    } else {
                        System.get().then(function (data) {
                            if (data.currentVersion < data.latestVersion) {
                                Storage.save("notification", "Newer version is available! Please, create a new instance from the latest AMI.");
                            }
                            $scope.subscribeWS();
                        }).finally(function () {
                            $state.go('app.volume.list');
                        });
                    }
                }, function (res) {
                    $scope.error = res;
                    $scope.password = "";
                });
            };


        }]);
'use strict';

angular.module('web')
    .controller('LogoutController', ['$state', 'Auth', 'Storage', '$window',
        function ($state, Auth, Storage, $window) {

            var currentUser = Storage.get("currentUser");
            var ssoMode = Storage.get("ssoMode");

            if (ssoMode && ssoMode.ssoMode) {

                $window.location.href = "/saml/logout";
            } else {
                Auth.logOut();
                $state.go('login');
            }

        }]);
'use strict';

angular.module('web')
    .controller('LogsController', ['$location', '$anchorScroll', '$stomp', '$scope', '$rootScope', '$state', '$timeout', '$q', 'System',
        function ($location, $anchorScroll, $stomp, $scope, $rootScope, $state, $timeout, $q, System) {
            $scope.followLogs = false;
            $scope.logs = [];

            var maxLogs;
            $rootScope.isLoading = true;
            var collection = [];
            var subCollection = [];
            var initSubCollectionLength = 0;
            var logTypes = {
                warn: "warning",
                info: "info",
                error: "error",
                debug: ""
            };
            var counterStarted = false;

            System.get().then(function (settings) {
                // hack for handling 302 status
                if (typeof settings === 'string' && settings.indexOf('<html lang="en" ng-app="web"') > -1) {
                    $state.go('loader');
                }

                maxLogs = settings.systemProperties.logsBuffer;
                $stomp
                    .connect('/rest/ws')
                    .then(function (frame) {
                            $rootScope.isLoading = false;
                            $scope.logsListener = $stomp.subscribe('/logs', function (payload, headers, res) {
                                updateLogs(res);
                                if ($scope.followLogs) {
                                    var lastLogId = 'log-' + ($scope.logs.length ? $scope.logs.length - 1 : 0);
                                    $location.hash(lastLogId);
                                    $anchorScroll();
                                }
                            });

                        }, function (e) {
                            $rootScope.isLoading = false;
                            console.log(e);
                        }
                    );

                function updateLogs(msg) {
                    msg.body = JSON.parse(msg.body);
                    // get log type, which can be error, info, etc.
                    var getType = function (log) {
                        var logTypeRaw = (log.split(']')[0]).split('[').reverse()[0];
                        var logType = logTypeRaw.toLowerCase().trim();
                        return logTypes[logType]
                    };

                    var saveLogs = function (log) {
                        subCollection.push(log);
                        if (!counterStarted) {
                            counterStarted = true;
                            $timeout(function () {
                                var logsAdded = subCollection.length - initSubCollectionLength;
                                counterStarted = false;
                                updateLogsCollection(subCollection, logsAdded);
                            }, 500);
                        }
                    };

                    function updateLogsCollection(logsCollection, logsAdded) {
                        // yes, it's a magic number :) Logs are guaranteed to be displayed smoothly at
                        // this speed of 15 logs/half-sec
                        if (logsAdded < 15) {
                            sendToView(logsCollection);
                        } else {
                            // if speed of logs is more than 30 log/sec (15 logs/half-sec) => update view
                            // once per second 'till logs finished
                            if (logsCollection.length) {
                                $timeout(function () {
                                    sendToView(logsCollection);
                                }, 1000)
                            }

                        }

                        //reduces array length if total logs are more than user wants
                        function checkLength() {
                            if (collection.length > (maxLogs)) {
                                collection = collection.slice(-maxLogs);
                            }
                        }

                        function sendToView(logsCollection) {
                            collection = collection.concat(logsCollection);
                            checkLength();

                            subCollection = [];
                            initSubCollectionLength = 0;
                            $scope.$apply(function () {
                                $scope.logs = collection;
                            });
                        }
                    }

                    for (var i = 0; i < msg.body.length; i++) {
                        var logObject = {
                            type: getType(msg.body[i]),
                            message: msg.body[i]
                        };
                        saveLogs(logObject);
                    }
                }
            });

            $rootScope.$on('$stateChangeStart', function (event, toState, toParams, fromState, fromParams, options) {
                $rootScope.isLoading = false;
                //check is needed for cases when user comes to LOGS tab, which will also trigger this event
                if (fromState.name === 'app.logs' && $scope.logsListener) {
                    $scope.logsListener.unsubscribe();
                }
            })
        }]);
'use strict';

angular.module('web')
    .controller('RegistrationController', ['$scope', '$state', 'Users', '$modal', function ($scope, $state, Users, $modal) {
        $scope.passwordError = "";
        $scope.userExists = "";
        var userData = {};

        $scope.registerUser = function () {
            if ($scope.passwordReg === $scope.passwordConf) {
                // check if user already exists
                Users.getAll().then(function (data) {
                    var unique = true;

                    if (data) {
                        for (var i = 0; i < data.length; i++) {
                            if (data[i].email === userData.email) {
                                unique = false;
                                $scope.userExists = "User with such E-mail already exists";
                                break;
                            }
                        }
                    }

                    if (unique) {
                        userData = {
                            firstName: $scope.firstName,
                            lastName: $scope.lastName,
                            email: $scope.userEmail,
                            password: $scope.passwordReg
                        };

                        Users.insert(userData).then(function () {
                            var modalInstance = $modal.open({
                                animation: true,
                                templateUrl: './partials/modal.user-added.html'
                            });

                            modalInstance.result.then(function () {
                                $state.go('login');
                            });
                        });

                    }
                });
            }
            else {
                $scope.passwordError = "Password does not match"
            }
        };
    }]);


'use strict';

angular.module('web')
    .controller('ScheduleController', ['$scope', '$rootScope', '$stateParams', '$filter', 'Tasks', '$modal', function ($scope, $rootScope, $stateParams, $filter, Tasks, $modal) {

        $scope.volumeId = $stateParams.volumeId;
        $scope.schedules = [];

        var refreshList = function () {
            Tasks.getRegular($scope.volumeId).then(function (data) {
                $scope.schedules = data;
            });
        };
        refreshList();

        var scheduleToTask = function (schedule) {
            return {
                cron: schedule.cron,
                enabled: schedule.enabled,
                id: schedule.id,
                regular: "true",
                schedulerManual: "false",
                schedulerName: schedule.name,
                status: "waiting",
                type: "backup",
                volumes: [$scope.volumeId]
            }
        };

        var taskToSchedule = function (task) {
            return {
                isNew: false,
                id: task.id,
                name: task.schedulerName,
                enabled: task.enabled == 'true',
                cron: task.cron
            };
        };

        $scope.add = function () {
            $scope.scheduleToEdit = {
                isNew: true,
                id: null,
                name: '',
                enabled: true
            };

            var modalInstance = $modal.open({
                animation: true,
                templateUrl: './partials/modal.schedule-edit.html',
                scope: $scope
            });

            modalInstance.result.then(function () {
                $rootScope.isLoading = true;
                var newTask = scheduleToTask($scope.scheduleToEdit);
                Tasks.insert(newTask).then(function () {
                    refreshList();
                    $rootScope.isLoading = false;
                }, function () {
                    $rootScope.isLoading = false;
                });
            });
        };

        $scope.edit = function (task) {
            $scope.scheduleToEdit = taskToSchedule(task);

            var modalInstance = $modal.open({
                animation: true,
                templateUrl: './partials/modal.schedule-edit.html',
                scope: $scope
            });

            modalInstance.result.then(function () {
                $rootScope.isLoading = true;
                var newTask = scheduleToTask($scope.scheduleToEdit);
                Tasks.update(newTask).then(function () {
                    refreshList();
                    $rootScope.isLoading = false;
                }, function () {
                    $rootScope.isLoading = false;
                });
            });
        };

        $scope.remove = function (task) {
            $scope.scheduleToDelete = task;
            var confirmInstance = $modal.open({
                animation: true,
                templateUrl: './partials/modal.schedule-del.html',
                scope: $scope
            });

            confirmInstance.result.then(function () {
                Tasks.delete(task.id).then(function (data) {
                    refreshList();
                });
            });
        };
    }]);
'use strict';

angular.module('web')
    .controller('SettingsController', ['$rootScope', '$state', '$scope', 'System', 'currentUser', 'Users', '$modal', 'Configuration',
        function ($rootScope, $state, $scope, System, currentUser, Users, $modal, Configuration) {
            $rootScope.isLoading = false;
            var currentUser = Users.getCurrent();
            $scope.isAdmin = currentUser.role === "admin";

            $scope.STRINGS = {
                sdfs: {
                    sdfsLocalCacheSize: {
                        empty: 'Local Cache Size field cannot be empty.'
                    },
                    volumeSize: {
                        empty: 'Volume Size field cannot be empty.'
                    }
                },
                volumeType: {
                    empty: 'Volume size for io1 volume type cannot be empty.',
                    range: 'Volume size for io1 volume type must be in between 1 and 30.'
                },
                otherSettings: {
                    empty: 'All fields are required. Please fill in empty fields.'
                }
            };

            $rootScope.isLoading = true;
            System.get().then(function (data) {
                // hack for handling 302 status
                if (typeof data === 'string' && data.indexOf('<html lang="en" ng-app="web"') > -1) {
                    $state.go('loader');
                }
                data.ec2Instance.instanceIDs = data.ec2Instance.instanceIDs.join(", ");
                $scope.settings = data;
                if (!$scope.settings.mailConfiguration) {
                    $scope.emails = [];
                    $scope.settings.mailConfiguration = {
                        events: {
                            "error": false,
                            "info": false,
                            "success": false
                        }
                    }
                } else {
                    $scope.emails = $scope.settings.mailConfiguration.recipients || [];
                }

                $scope.initialSettings = angular.copy(data);
                $rootScope.isLoading = false;
            }, function (e) {
                console.log(e);
                $rootScope.isLoading = false;
            });

            $scope.backup = function () {
                var modalScope = $scope.$new(true);
                $modal.open({
                    animation: true,
                    templateUrl: './partials/modal.system-backup.html',
                    scope: modalScope,
                    controller: 'modalSystemBackupCtrl'
                });
            };

            $scope.uninstall = function () {
                $modal.open({
                    animation: true,
                    templateUrl: './partials/modal.system-uninstall.html',
                    controller: 'modalSystemUninstallCtrl'
                });

            };

            $scope.updateSettings = function () {

                var settingsUpdateModal = $modal.open({
                    animation: true,
                    scope: $scope,
                    templateUrl: './partials/modal.settings-update.html',
                    controller: 'modalSettingsUpdateCtrl'
                });

                settingsUpdateModal.result.then(function () {
                    $scope.initialSettings = angular.copy($scope.settings);
                }, function () {
                    $scope.initialSettings = angular.copy($scope.settings);
                });
            };

            $scope.emailNotifications = function () {
                $scope.connectionStatus = null;
                var emailNotificationsModalInstance = $modal.open({
                    animation: true,
                    templateUrl: './partials/modal.email-notifications.html',
                    scope: $scope
                });

                emailNotificationsModalInstance.result.then(function () {
                    $scope.settings.mailConfiguration.recipients = $scope.emails;
                }, function () {
                    $scope.settings = angular.copy($scope.initialSettings);
                })
            };

            $scope.testConnection = function () {
                var testData = {
                    testEmail: $scope.testEmail,
                    domain: $scope.settings.domain,
                    mailConfiguration: $scope.settings.mailConfiguration
                };

                Configuration.check(testData).then(function (response) {
                    $scope.connectionStatus = response.status;
                }, function (error) {
                    $scope.connectionStatus = error.status;
                });
            };

            $scope.isNewValues = function () {
                return JSON.stringify($scope.settings) !== JSON.stringify($scope.initialSettings);
            };

        }]);
'use strict';

angular.module('web')
    .controller('TasksController', ['$scope', '$rootScope', '$stateParams', '$stomp', 'Tasks', 'Storage', '$modal', '$timeout', '$state',
        function ($scope, $rootScope, $stateParams, $stomp, Tasks, Storage, $modal, $timeout, $state) {
            $scope.typeColorClass = {
                backup: "primary",
                restore: "success",
                delete: "danger",
                system_backup: "danger"

            };
            $scope.typeIconClass = {
                backup: "cloud-download",
                restore: "cloud-upload",
                delete: "remove",
                system_backup: "cog"
            };
            $scope.manualIconClass = {
                true: "user",
                false: "time"
            };

            $scope.statusPriority = function (task) {
                var priorities = {
                    canceled: 5,
                    running: 4,
                    queued: 3,
                    error: 2,
                    waiting: 1
                };
                return priorities[task.status] || 0;
            };

            $scope.typePriority = function (task) {
                return parseInt(task.priority) || 0;
            };

            $scope.volumeId = $stateParams.volumeId;

            $scope.tasks = [];
            $rootScope.isLoading = false;
            $scope.refresh = function () {
                $rootScope.isLoading = true;
                Tasks.get($scope.volumeId).then(function (data) {
                    // hack for handling 302 status
                    if (typeof data === 'string' && data.indexOf('<html lang="en" ng-app="web"') > -1) {
                        $state.go('logout');
                    }

                    $scope.tasks = data;
                    applyTaskStatuses();
                    $rootScope.isLoading = false;
                }, function () {
                    $rootScope.isLoading = false;
                });
            };
            $scope.refresh();

            $scope.$on("task-status-changed", function (e, d) {
                updateTaskStatus(d);
            });

            var applyTaskStatuses = function () {
                for (var i = 0; i < $scope.tasks.length; i++) {
                    var task = $scope.tasks[i];
                    var msg = Storage.get('lastTaskStatus_' + task.id) || {};
                    task.progress = msg.progress;
                    task.message = msg.message;
                }
            };

            var updateTaskStatus = function (msg) {
                var task = $scope.tasks.filter(function (t) {
                    return t.id == msg.taskId && msg.status != "complete";
                })[0];

                if (task) {
                    if (task.status == 'complete' || task.status == 'queued' || task.status == 'waiting') {
                        $scope.refresh();
                    } else {
                        $timeout(function () {
                            task.progress = msg.progress;
                            task.message = msg.message;
                            task.status = msg.status;
                        }, 0);

                        if (msg.progress == 100) {
                            Storage.remove('lastTaskStatus_' + task.id);
                            $scope.refresh();
                        }
                    }
                }
            };

            $scope.reject = function (task) {
                $scope.taskToReject = task;

                var rejectInstance = $modal.open({
                    animation: true,
                    templateUrl: './partials/modal.task-reject.html',
                    scope: $scope
                });

                rejectInstance.result.then(function () {
                    Tasks.delete(task.id).then(function () {
                        $scope.refresh();
                    });
                });
            };
        }]);
'use strict';

angular.module('web')
    .controller('UserController', ['$state', '$scope', '$rootScope', 'Users', 'ssoMode', 'Storage', 'toastr', '$modal', 'ITEMS_BY_PAGE', 'DISPLAY_PAGES',
        function ($state, $scope, $rootScope, Users, ssoMode, Storage, toastr, $modal, ITEMS_BY_PAGE, DISPLAY_PAGES) {
            $scope.itemsByPage = ITEMS_BY_PAGE;
            $scope.displayedPages = DISPLAY_PAGES;
            $scope.users = [];
            $scope.ssoMode = ssoMode.ssoMode;

            var currentUser = Users.getCurrent();
            $scope.isAdmin = currentUser.role === "admin";
            $scope.isCurrentUser = function (email) {
                return currentUser.email === email;
            };

            var updateCurrentUser = function () {
                if ($scope.isCurrentUser($scope.userToEdit.email)) {
                    var user = angular.copy($scope.userToEdit);
                    delete user.isNew;
                    delete user.password;
                    delete user.admin;
                    user.role = $scope.userToEdit.admin ? 'admin' : 'user';
                    Storage.save("currentUser", user);
                }
            };

            $scope.editUser = function (user) {
                $scope.userToEdit = angular.copy(user);
                $scope.userToEdit.isNew = false;
                var editUserModal = $modal.open({
                    animation: true,
                    templateUrl: './partials/modal.user-edit.html',
                    scope: $scope
                });

                editUserModal.result.then(function () {
                    $rootScope.isLoading = true;
                    $scope.userToEdit.password = $scope.userToEdit.password || "";

                    Users.update($scope.userToEdit).then(function () {
                        $scope.refreshUsers();
                        updateCurrentUser();
                        var confirmModal = $modal.open({
                            animation: true,
                            templateUrl: './partials/modal.user-added.html',
                            scope: $scope
                        });
                        $rootScope.isLoading = false;
                    }, function (e) {
                        $rootScope.isLoading = false;
                    });
                });
            };

            $scope.addUser = function () {
                $scope.userToEdit = {};
                $scope.userToEdit.isNew = true;
                $scope.userToEdit.admin = false;
                var modalInstance = $modal.open({
                    animation: true,
                    templateUrl: './partials/modal.user-edit.html',
                    scope: $scope
                });

                modalInstance.result.then(function () {
                    $rootScope.isLoading = true;

                    Users.insert($scope.userToEdit).then(function () {
                        var modalInstance = $modal.open({
                            animation: true,
                            templateUrl: './partials/modal.user-added.html',
                            scope: $scope
                        }, function (e) {
                            console.log(e);
                        });

                        modalInstance.result.then(function () {
                            $scope.refreshUsers();
                        });
                        $rootScope.isLoading = false;
                    }, function (e) {
                        $rootScope.isLoading = false;
                    });
                });
            };

            Users.getAll().then(function (data) {
                // hack for handling 302 status
                if (typeof data === 'string' && data.indexOf('<html lang="en" ng-app="web"') > -1) {
                    $state.go('loader');
                }

                $scope.users = data;
            });

            $scope.refreshUsers = function () {
                $rootScope.isLoading = true;
                $scope.users = [];
                Users.getAll().then(function (data) {
                    $scope.users = data;
                    $rootScope.isLoading = false;
                }, function () {
                    $rootScope.isLoading = false;
                })
            };

            $scope.deleteUser = function (user) {
                $scope.userToDelete = user;
                var modalInstance = $modal.open({
                    animation: true,
                    templateUrl: './partials/modal.user-delete.html',
                    scope: $scope
                });

                modalInstance.result.then(function () {
                    $rootScope.isLoading = true;
                    Users.delete(user.email).then(function () {
                        $scope.refreshUsers();
                        $rootScope.isLoading = false;
                    }, function () {
                        $rootScope.isLoading = false;
                    });
                })
            };
        }]);
'use strict';

angular.module('web')
    .controller('VolumesController', ['$scope', '$rootScope', '$state', '$q', 'Retention', '$filter', 'Storage', 'Regions', 'ITEMS_BY_PAGE', 'DISPLAY_PAGES', '$modal', 'Volumes', 'Tasks', 'Zones',
        function ($scope, $rootScope, $state, $q, Retention, $filter, Storage, Regions, ITEMS_BY_PAGE, DISPLAY_PAGES, $modal, Volumes, Tasks, Zones) {
            $scope.maxVolumeDisplay = 5;
            $scope.itemsByPage = ITEMS_BY_PAGE;
            $scope.displayedPages = DISPLAY_PAGES;

            $scope.stateColorClass = {
                "in-use": "success",
                "creating": "error",
                "available": "info",
                "deleting": "error",
                "deleted": "error",
                "error": "error",
                "removed": "danger"
            };

            $scope.textClass = {
                'false': 'Select',
                'true': 'Unselect'
            };

            $scope.iconClass = {
                'false': 'unchecked',
                'true': 'check'
            };

            var actions = {
                backup: {
                    type: 'backup',
                    bgClass: 'primary',
                    modalTitle: 'Backup Volume',
                    iconClass: 'cloud-download',
                    description: 'start backup task',
                    buttonText: 'Add backup task'
                },
                restore: {
                    type: 'restore',
                    bgClass: 'success',
                    modalTitle: 'Restore Backup',
                    iconClass: 'cloud-upload',
                    description: 'start restore task',
                    buttonText: 'Add restore task'

                },
                schedule: {
                    type: 'schedule',
                    bgClass: 'warning',
                    modalTitle: 'Add Schedule',
                    iconClass: 'time',
                    description: 'add schedule',
                    buttonText: 'Add schedule'
                }
            };

            $scope.isAllSelected = false;
            $scope.selectedAmount = 0;

            $scope.checkAllSelection = function () {
                var disabledAmount = $scope.volumes.filter(function (v) {
                    return $scope.isDisabled(v)
                }).length;
                $scope.selectedAmount = $scope.volumes.filter(function (v) {
                    return v.isSelected
                }).length;
                $scope.isAllSelected = ($scope.selectedAmount + disabledAmount == $scope.volumes.length);
            };

            $scope.selectAll = function () {
                $scope.volumes.forEach(function (volume) {
                    doSelection(volume, !$scope.isAllSelected);
                });
                $scope.checkAllSelection();
            };

            $scope.toggleSelection = function (volume) {
                doSelection(volume, !volume.isSelected);
                $scope.checkAllSelection();
            };

            var doSelection = function (volume, value) {
                if (volume.hasOwnProperty('isSelected')) {
                    volume.isSelected = value;
                }
            };

            $scope.isDisabled = function (volume) {
                return volume.state === 'removed'
            };

            // ---------filtering------------

            $scope.showFilter = function () {
                var filterInstance = $modal.open({
                    animation: true,
                    templateUrl: './partials/modal.volume-filter.html',
                    controller: 'modalVolumeFilterCtrl',
                    resolve: {
                        tags: function () {
                            return $scope.tags;
                        },
                        instances: function () {
                            return $scope.instances;
                        }
                    }
                });

                filterInstance.result.then(function (filter) {
                    $scope.stAdvancedFilter = filter;
                });
            };

            var processVolumes = function (data) {
                $scope.tags = {};
                $scope.instances = [""];
                for (var i = 0; i < data.length; i++) {
                    for (var j = 0; j < data[i].tags.length; j++) {
                        var tag = data[i].tags[j];
                        if (!$scope.tags.hasOwnProperty(tag.key)) {
                            $scope.tags[tag.key] = [tag.value];
                        } else {
                            if ($scope.tags[tag.key].indexOf(tag.value) == -1) {
                                $scope.tags[tag.key].push(tag.value);
                            }
                        }
                    }

                    var instance = data[i].instanceID;
                    if (instance && $scope.instances.indexOf(instance) == -1) {
                        $scope.instances.push(instance);
                    }
                    if (data[i].state !== 'removed') data[i].isSelected = false;
                }
                $scope.isAllSelected = false;
                return data;
            };

            //----------filtering-end-----------

            //-----------Volumes-get/refresh-------------

            $scope.changeRegion = function (region) {
                $scope.selectedRegion = region;
            };

            $scope.refresh = function () {
                $rootScope.isLoading = true;
                $scope.volumes = [];
                Volumes.get().then(function (data) {
                    // hack for handling 302 status
                    if (typeof data === 'string' && data.indexOf('<html lang="en" ng-app="web"') > -1) {
                        $state.go('loader');
                    }
                    $scope.volumes = processVolumes(data);
                    $rootScope.isLoading = false;
                }, function () {
                    $rootScope.isLoading = false;
                });
            };

            $scope.refresh();
            //-----------Volumes-get/refresh-end------------

            //-----------Volume-backup/restore/retention-------------
            $scope.selectZone = function (zone) {
                $scope.selectedZone = zone;
            };

            $scope.volumeAction = function (actionType) {
                $rootScope.isLoading = true;
                $q.all([Zones.get(), Zones.getCurrent()])
                    .then(function (results) {
                        $scope.zones = results[0];
                        $scope.selectedZone = results[1]["zone-name"] || "";
                    })
                    .finally(function () {
                        $rootScope.isLoading = false;
                    });


                $scope.selectedVolumes = $scope.volumes.filter(function (v) {
                    return v.isSelected;
                });
                $scope.actionType = actionType;
                $scope.action = actions[actionType];
                $scope.schedule = {name: '', cron: '', enabled: true};

                var confirmInstance = $modal.open({
                    animation: true,
                    templateUrl: './partials/modal.volumeAction.html',
                    scope: $scope
                });

                confirmInstance.result.then(function () {
                    $rootScope.isLoading = true;
                    var volList = $scope.selectedVolumes.map(function (v) {
                        return v.volumeId;
                    });

                    var getNewTask = function () {
                        var newTask = {
                            id: "",
                            priority: "",
                            volumes: volList,
                            status: "waiting"
                        };

                        switch (actionType) {
                            case 'restore':
                                newTask.backupFileName = "";
                                newTask.zone = $scope.selectedZone;
                            case 'backup':
                                newTask.type = actionType;
                                newTask.schedulerManual = true;
                                newTask.schedulerName = Storage.get('currentUser').email;
                                newTask.schedulerTime = Date.now();
                                break;
                            case 'schedule':
                                newTask.type = 'backup';
                                newTask.regular = true;
                                newTask.schedulerManual = false;
                                newTask.schedulerName = $scope.schedule.name;
                                newTask.cron = $scope.schedule.cron;
                                newTask.enabled = $scope.schedule.enabled;
                                break;
                        }

                        return newTask;
                    };

                    var t = getNewTask();
                    Tasks.insert(t).then(function () {
                        $rootScope.isLoading = false;
                        if (actionType != 'schedule') {
                            var successInstance = $modal.open({
                                animation: true,
                                templateUrl: './partials/modal.task-created.html',
                                scope: $scope
                            });

                            successInstance.result.then(function () {
                                $state.go('app.tasks');
                            });
                        }
                    }, function (e) {
                        $rootScope.isLoading = false;
                        console.log(e);
                    });

                });

            };

            var getShowRule = function (rule) {
                var showRules = {};
                angular.forEach($scope.rule, function (value, key) {
                    showRules[key] = value > 0;
                });
                Object.defineProperty(showRules, 'never', {
                    get: function () {
                        return !$scope.showRetentionRule.size && !$scope.showRetentionRule.count && !$scope.showRetentionRule.days;
                    },
                    set: function (value) {
                        if (value) {
                            $scope.showRetentionRule.size = false;
                            $scope.showRetentionRule.count = false;
                            $scope.showRetentionRule.days = false;
                        }
                    }
                });
                return showRules;
            };
            $scope.retentionRule = function (volume) {
                $rootScope.isLoading = true;
                Retention.get(volume.volumeId).then(function (data) {

                    $scope.rule = {
                        size: data.size,
                        count: data.count,
                        days: data.days
                    };
                    $scope.showRetentionRule = getShowRule($scope.rule);

                    $rootScope.isLoading = false;

                    var retentionModalInstance = $modal.open({
                        animation: true,
                        templateUrl: './partials/modal.retention-edit.html',
                        scope: $scope
                    });

                    retentionModalInstance.result.then(function () {
                        $rootScope.isLoading = true;
                        var rule = angular.copy($scope.rule);
                        angular.forEach(rule, function (value, key) {
                            rule[key] = $scope.showRetentionRule[key] ? rule[key] : 0
                        });
                        rule.volumeId = data.volumeId;

                        Retention.update(rule).then(function () {
                            $rootScope.isLoading = false;
                        }, function () {
                            $rootScope.isLoading = false;
                        })
                    });

                }, function () {
                    $rootScope.isLoading = false;
                });

            }
        }]);
/**
 * Created by Administrator on 21.07.2015.
 */
'use strict';

angular.module('web')
    .controller('modalScheduleCtrl', ['$scope', '$modalInstance', '$filter', 'schedule', 'Schedules', function ($scope, $modalInstance, $filter, schedule, Schedules) {

        $scope.Schedules = Schedules;
        $scope.schedule = schedule;

        $scope.isNew = schedule.id == 0;
        $scope.schedule = angular.copy(schedule);
        $scope.isEndless = typeof $scope.schedule.end == 'undefined' || !$scope.schedule.end;

        $scope.weekdays = {
            Monday: false,
            Tuesday: false,
            Wednesday: false,
            Thursday: false,
            Friday: false,
            Saturday: false,
            Sunday: false
        };

        $scope.shortdays = {
            Monday: "Mo",
            Tuesday: "Tu",
            Wednesday: "We",
            Thursday: "Th",
            Friday: "Fr",
            Saturday: "Sa",
            Sunday: "Su"
        };

        //-----------DATE FORMATING---------


        for (var day in $scope.weekdays) {
            $scope.weekdays[day] = $scope.schedule.week.indexOf(day) >= 0;
        }
        ;

        $scope.doEndless = function () {
            if ($scope.isEndless) {
                $scope.schedule.end = "";
            }
            else {
                $scope.schedule.end = $scope.schedule.start;
                $scope.schedule.end.setDate($scope.schedule.start.getDate() + 1);
            }
        };

        // ------------REPEAT-EVERY-----------------------
        $scope.periodicityNum = [1, 2, 3, 4, 5, 10, 15];
        $scope.periodicityWord = ["day", "week", "month", "year"];


        // ---------------- calendar pop-up ----
        $scope.opened = {
            start: false,
            end: false
        };

        $scope.today = function () {
            $scope.dt = new Date();
        };


        $scope.clear = function () {
            $scope.dt = null;
        };

        $scope.calendarOpen = function ($event, which) {
            $event.preventDefault();
            $event.stopPropagation();
            $scope.opened[which] = true;
        };


        // ---------------BUTTONS-------

        $scope.ok = function () {

            $scope.schedule.week = Object.keys($scope.weekdays)
                .filter(function (d) {
                    return $scope.weekdays[d];
                });
            $scope.schedule.start = $filter('date')($scope.schedule.start, 'yyyy-MM-dd hh:mm:ss');
            $scope.schedule.end = (function () {
                if ($scope.schedule.end != null && $scope.schedule.end) {
                    return $filter('date')($scope.schedule.end, 'yyyy-MM-dd hh:mm:ss')
                } else {
                    return ""
                }
            })();

            if ($scope.isNew) {
                Schedules.insert($scope.schedule).then(function () {
                    $modalInstance.close();
                });
            }
            else {
                Schedules.update($scope.schedule).then(function () {
                    $modalInstance.close();
                });
            }

        };

        $scope.cancel = function () {
            $modalInstance.dismiss();
        };

    }]);
'use strict';

angular.module('web')
    .controller('modalSettingsUpdateCtrl', ['$scope', '$modalInstance', 'System', 'Tasks', '$rootScope', function ($scope, $modalInstance, System, Tasks, $rootScope) {
        $scope.state = 'ask';

        var newSettings = angular.copy($scope.settings);
        if (!newSettings.mailConfiguration.fromMailAddress) newSettings.mailConfiguration = null;
        //deletion of Arrays from model per request of backend
        delete newSettings.systemProperties.volumeTypeOptions;
        delete newSettings.ec2Instance.instanceIDs;

        $scope.updateSettings = function () {
            $rootScope.isLoading = true;
            System.send(newSettings).then(function () {
                $scope.state = "done";
                $rootScope.isLoading = false;
            }, function (e) {
                $scope.state = "failed";
                $rootScope.isLoading = false;
            });
        };
    }]);
'use strict';

angular.module('web')
    .controller('modalSystemBackupCtrl', ['$scope', '$modalInstance', 'Tasks', 'Storage', function ($scope, $modalInstance, Tasks, Storage) {
        $scope.state = 'ask';

        $scope.sendTask = function () {
            var newTask = {
                type: "system_backup",
                status: "waiting",
                regular: "false",
                schedulerManual: true,
                schedulerName: Storage.get('currentUser').email,
                schedulerTime: Date.now()
            };
            Tasks.insert(newTask).then(function () {
                $scope.state = "done";
            }, function () {
                $scope.state = "failed";
            });

        }
    }]);
'use strict';

angular.module('web')
    .controller('modalSystemUninstallCtrl', ['$scope', '$modalInstance', 'System', function ($scope, $modalInstance, System) {
        $scope.state = 'ask';

        $scope.deletionOptions = [{
            name: "Yes",
            value: true
        }, {
            name: "No",
            value: false
        }];

        $scope.delete = function () {
            var deletionData = {
                systemId: $scope.systemId,
                removeS3Bucket: $scope.removeS3Bucket.value
            };

            System.delete(deletionData).then(function () {
                $scope.state = "done";
            }, function (e) {
                $scope.delError = e;
                $scope.state = "failed";
            });
        }
    }]);
'use strict';

angular.module('web')
    .controller('modalVolumeFilterCtrl', ['$scope', '$modalInstance', 'Regions', 'Storage', 'tags', 'instances', function ($scope, $modalInstance, Regions, Storage, tags, instances) {
        $scope.tags = tags;
        $scope.instances = instances;
        $scope.globalRegion = {
            location: "",
            name: "GLOBAL",
            id: ""
        };
        $scope.sliderOptions = {
            from: 0,
            to: 16384,
            step: 4,
            dimension: " GiB",
            skin: "plastic"
        };

        Regions.get().then(function (regions) {
            $scope.regions = regions
        });

        //$scope.selectedRegion = $scope.globalRegion;

        $scope.clear = function () {
            var defaultFilter = {
                volumeId: "",
                name: "",
                size: "0;16384",
                instanceID: "",
                region: $scope.globalRegion,
                tags: []
            };
            $scope.filter = angular.copy(defaultFilter);
        };

        if (Storage.get('VolumeFilter')) {
            $scope.filter = Storage.get('VolumeFilter');
        } else {
            $scope.clear();
        }

        $scope.ok = function () {
            var f = $scope.filter;
            var stAdvancedFilter = {
                "volumeId": {
                    "type": "str",
                    "value": f.volumeId
                },
                "volumeName": {
                    "type": "str",
                    "value": f.name
                },
                "size": {
                    "type": "int-range",
                    "value": {
                        "lower": parseInt(f.size.split(";")[0], 10),
                        "higher": parseInt(f.size.split(";")[1], 10)
                    }
                },
                "instanceID": {
                    "type": "str-strict",
                    "value": f.instanceID
                },
                "availabilityZone": {
                    "type": "str",
                    "value": f.region.id
                },
                "tags": {
                    "type": "array-inc",
                    "value": f.tags
                }
            };

            Storage.save('VolumeFilter', f);
            $modalInstance.close(stAdvancedFilter);
        }


    }]);
app.directive('autoScroll', function () {
    return {
        scope: {
            autoScroll: "="
        },
        link: function (scope, element, attr) {

            scope.$watchCollection('autoScroll', function (newValue) {
                if (newValue && JSON.parse(attr.enableScroll)) {
                    $(element).scrollTop($(element)[0].scrollHeight + $(element)[0].clientHeight);
                }
            });
        }
    }
});
app.directive('checkPassword', [function () {
    return {
        require: 'ngModel',
        link: function (scope, elem, attrs, ctrl) {
            var firstPassword = '#' + attrs.checkPassword;
            elem.bind('keyup', function () {
                scope.$apply(function () {
                    var firstPass = angular.element(document.querySelector(firstPassword)).val()
                    var v = elem.val() === firstPass;
                    ctrl.$setValidity('passwordmatch', v);
                });
            });
        }
    }
}]);
app.directive('complexPassword', function () {
    return {
        require: 'ngModel',
        link: function (scope, elm, attrs, ctrl) {
            ctrl.$parsers.unshift(function (password) {
                var hasUpperCase = /[A-Z]/.test(password);
                var hasLowerCase = /[a-z]/.test(password);
                var hasNumbers = /\d/.test(password);
                var hasNonalphas = /\W/.test(password);
                var characterGroupCount = hasUpperCase + hasLowerCase + hasNumbers + hasNonalphas;

                if ((password.length >= 8) && (characterGroupCount >= 3)) {
                    ctrl.$setValidity('complexity', true);
                    return password;
                }
                else {
                    ctrl.$setValidity('complexity', false);
                    return undefined;
                }

            });
        }
    }
});
"use strict";

angular.module('web')
    .directive('emails', function () {
        return {
            restrict: 'E',
            scope: {emails: '='},
            template: '<div class="input-group" style="clear: both;">' +
            '<input type="email" class="form-control" ng-model="newEmail" placeholder="email"/>' +
            '<span class="input-group-btn" style="width:0px;"></span>' +
            '<span class="input-group-btn"><button class="btn btn-primary" ng-click="add()" ng-disabled="!newEmail"><span class="glyphicon glyphicon-plus"></span></button></span>' +
            '</div>' +
            '<div class="tags" style="margin-top: 5px">' +
            '<div ng-repeat="mail in emails track by $index" class="tag label label-success" ng-click="remove($index)">' +
            '<span class="glyphicon glyphicon-remove"></span>' +
            '<div class="tag-value">{{mail}}</div>' +
            '</div>' +
            '</div>',
            link: function ($scope, $element) {
                $scope.newEmail = "";
                var inputs = angular.element($element[0].querySelectorAll('input'));

                // This adds the new tag to the tags array
                $scope.add = function () {
                    if ($scope.newEmail) {
                        $scope.emails.push($scope.newEmail);
                        $scope.newEmail = "";
                    }
                    event.preventDefault();
                };

                // This is the ng-click handler to remove an item
                $scope.remove = function (idx) {
                    $scope.emails.splice(idx, 1);
                };

                // Capture all keypresses
                inputs.bind('keypress', function (event) {
                    // But we only care when Enter was pressed
                    if ($scope.newEmail && ( event.keyCode == 13 )) {
                        event.preventDefault();
                        $scope.$apply($scope.add);
                    }
                });
            }
        };
    });
'use strict';

angular.module('web')
    .directive('jqCron', function () {
        return {
            restrict: 'E',
            require: 'ngModel',
            scope: {
                ngModel: '='
            },
            link: function (scope, ele, attr, ctrl) {
                var options = {
                    initial: scope.ngModel || "* * * * *",
                    onChange: function () {
                        var value = $(this).cron("value");
                        scope.ngModel = value;
                        if (ctrl.$viewValue != value) {
                            ctrl.$setViewValue(value);
                        }
                    }
                };
                $(ele).cron(options);
            }
        };

    });
'use strict';

angular.module('web')
    .directive('stFilter', function () {
        return {
            require: '^stTable',
            scope: {
                stFilter: '='
            },
            link: function (scope, ele, attr, ctrl) {
                var table = ctrl;

                scope.$watch('stFilter', function (val) {
                    ctrl.search(val, 'availabilityZone');
                });

            }
        };
    });
"use strict";

angular.module('web')
    .directive('tagFilter', function () {
        return {
            restrict: 'E',
            scope: {tags: '=', src: '=', keyph: '@', valueph: '@'},
            template: '<div class="input-group tag-input" style="clear: both;">' +
            '<input type="text" class="form-control" ng-model="newTag.key" placeholder="{{keyph}}" typeahead="key for key in srcKeys | filter:$viewValue" typeahead-editable="false"/>' +
            '<span class="input-group-btn" style="width:0px;"></span>' +
            '<input type="text" class="form-control" ng-model="newTag.value" placeholder="{{valueph}}" style="border-left: 0" typeahead="val for val in src[newTag.key] | filter:$viewValue" typeahead-editable="false" />' +
            '<span class="input-group-btn"><button class="btn btn-primary" ng-click="add()"><span class="glyphicon glyphicon-plus"></span></button></span>' +
            '</div>' +
            '<div class="tags">' +
            '<div ng-repeat="tag in tags track by $index" class="tag label label-success" ng-click="remove($index)">' +
            '<span class="glyphicon glyphicon-remove"></span>' +
            '<div class="tag-value">{{tag.key}} : {{tag.value}}</div>' +
            '</div>' +
            '</div>',
            link: function ($scope, $element, $attrs) {
                $scope.newTag = {};
                $scope.srcKeys = [];
                var inputs = angular.element($element[0].querySelectorAll('input'));

                $scope.$watch("src", function (v) {
                    $scope.srcKeys = Object.keys(v) || [];
                });

                // This adds the new tag to the tags array
                $scope.add = function () {
                    if ($scope.newTag.hasOwnProperty('key') && $scope.newTag.hasOwnProperty('value')) {
                        $scope.tags.push($scope.newTag);
                        $scope.newTag = {};
                    }
                    event.preventDefault();
                };

                // This is the ng-click handler to remove an item
                $scope.remove = function (idx) {
                    $scope.tags.splice(idx, 1);
                };

                // Capture all keypresses
                inputs.bind('keypress', function (event) {
                    // But we only care when Enter was pressed
                    if ($scope.newTag.key != "" && $scope.newTag.value != "" && ( event.keyCode == 13 )) {
                        event.preventDefault();
                        $scope.$apply($scope.add);
                    }
                });
            }
        };
    });
app.directive('uploadedFile', function () {
    return {
        scope: {
            'uploadedFile': '='
        },
        link: function (scope, el, attrs) {
            el.bind('change', function (event) {
                var file = event.target.files[0];
                scope.uploadedFile = file ? file : undefined;
                scope.$apply();
            });
        }
    };
});
angular.module('web')
    .filter('sizeConvertion', function () {
        return function (data) {
            var gb = data / 1024 / 1024 / 1024;

            if (data) {
                if (gb < 1) {
                    return parseInt(data / 1024 / 1024) + " MB"
                } else {
                    return parseInt(data / 1024 / 1024 / 1024) + " GB"
                }
            }
        }
    });
'use strict';

angular.module('web')
    .filter('stAdvancedFilter', function () {

        var filterMatch = function (base, filter, type) {
            var typeOptions = {
                "str": function () {
                    return filter.length == 0 || (base ? base.toLowerCase().indexOf(filter.toLowerCase()) > -1 : false)
                },
                "str-strict": function () {
                    return filter.length == 0 || (base ? base === filter : false)
                },
                "int-range": function () {
                    return (filter.lower <= base && filter.higher >= base)
                },
                "array-inc": function () {
                    return filter.length == 0 || (base.length > 0 && (function () {
                            for (var i = 0; i < filter.length; i++) {
                                var tagToFilter = filter[i];
                                for (var j = 0; j < base.length; j++) {
                                    var tagFromBase = base[j];
                                    if (tagFromBase.key === tagToFilter.key && tagFromBase.value === tagToFilter.value) {
                                        return true;
                                    }
                                }
                            }
                            return false;
                        })());
                }
            };
            return typeOptions[type]();
        };

        var volumeMatch = function (item, filterObj) {
            var filterKeys = Object.keys(filterObj);
            for (var i = 0; i < filterKeys.length; i++) {
                var key = filterKeys[i];
                if (!filterMatch(item[key], filterObj[key].value, filterObj[key].type)) {
                    return false;
                }
            }

            return true;
        };

        return function stAdvancedFilter(array, filterObj) {
            if (!angular.isUndefined(array)
                && !angular.isUndefined(filterObj)
                && array.length > 0) {
                var result = [];
                array.forEach(function (item) {
                    if (volumeMatch(item, filterObj)) {
                        result.push(item);
                    }
                });

                return result;
            } else {
                return array;
            }
        };
    });
'use strict';

angular.module('web')
    .service('Auth', ['Storage', '$q', '$http', 'BASE_URL', function (Storage, $q, $http, BASE_URL) {
        var sessionUrl = BASE_URL + "login";
        var logoutUrl = BASE_URL + "logout";
        var statuses = {
            404: "Service is unavailable",
            401: "Your authentication information was incorrect. Please try again"
        };


        var _login = function (email, pass) {
            var deferred = $q.defer();

            $http({
                method: 'POST',
                url: sessionUrl,
                headers: {'Content-Type': 'application/x-www-form-urlencoded'},
                transformRequest: function (obj) {
                    var str = [];
                    for (var p in obj)
                        str.push(encodeURIComponent(p) + "=" + encodeURIComponent(obj[p]));
                    return str.join("&");
                },
                data: {email: email, password: pass}
            }).then(function (response) {
                Storage.save("currentUser", response.data);
                deferred.resolve(response.data);
            }, function (err, status) {
                deferred.reject(statuses[status]);
            });

            return deferred.promise;
        };

        var _logout = function () {
            Storage.remove("ssoMode");
            Storage.remove("currentUser");

            return $http.get(logoutUrl);
        };

        return {
            logIn: function (email, pass) {
                return _login(email, pass);
            },

            logOut: function () {
                return _logout();
            }
        };
    }]);
'use strict';

angular.module('web')
    .service('Backups', ['$q', '$http', 'BASE_URL', function ($q, $http, BASE_URL) {
        var url = BASE_URL + 'rest/backup';

        var _getForVolume = function (volume) {
            var deferred = $q.defer();
            $http.get(url + '/' + volume).success(function (data) {
                deferred.resolve(data);
            }).error(function (msg) {
                // TODO: handle 401 here
                deferred.reject(msg);
            });
            return deferred.promise;
        };

        var _delete = function (fileName) {
            return $http.delete(url + '/' + fileName)
                .success(function () {
                    // backup deleted
                })
                .error(function (msg) {
                    // TODO: handle 406
                });
        };

        return {
            getForVolume: function (volume) {
                return _getForVolume(volume);
            },
            delete: function (fileName) {
                return _delete(fileName);
            }
        }
    }]);
'use strict';

angular.module('web')
    .service('Configuration', ['$q', '$http', 'BASE_URL', function ($q, $http, BASE_URL) {
        var url = BASE_URL + 'rest/configuration';

        var _get = function (type) {
            var deferred = $q.defer();
            $http({
                url: url + "/" + type,
                method: 'GET'
            }).then(function (data, status) {
                deferred.resolve(data, status);
            }, function (data, status) {
                deferred.reject(data, status)
            });
            return deferred.promise;
        };

        var _send = function (type, item, timeout, files) {
            var deferred = $q.defer();

            if (files) {
                _sendFiles(item, files).then(function (result) {
                    console.info("Files uploaded successfully");
                }, function () {
                    return deferred.reject()
                })
            }

            var request = {
                url: url + "/" + type,
                method: "POST",
                data: item || {}
            };
            if (timeout) {
                request.timeout = timeout;
            }

            $http(request).then(function () {
                deferred.resolve()
            }, function (data, status) {
                deferred.reject(data, status)
            });
            return deferred.promise;
        };

        var _sendFiles = function (item, files) {
            var deferred = $q.defer();
            var formData = new FormData();
            var namesArray = ["idp_metadata.xml", "saml_sp_cert.pem"];

            for (var key in files) {
                formData.append('file', files[key]);
            }

            formData.append('name', namesArray);

            $http({
                url: url + "/uploadFiles",
                method: "POST",
                data: formData,
                transformRequest: angular.identity,
                transformResponse: angular.identity,
                headers: {'Content-Type': undefined}
            }).then(function () {
                deferred.resolve()
            }, function (error) {
                console.warn(error.data);
                deferred.reject();
            });

            //files are sent separately from other setting. That's why
            //they should be removed from settings collection before the later is sent
            delete item.sso;

            return deferred.promise;
        };

        var _check = function (emailConfig) {
            var deferred = $q.defer();

            var request = {
                url: "/rest/system/mail/configuration/test",
                method: "POST",
                data: emailConfig
            };

            $http(request).then(function (response) {
                deferred.resolve(response)
            }, function (data) {
                deferred.reject(data)
            });
            return deferred.promise;
        };

        return {
            get: function (type) {
                return _get(type);
            },
            send: function (type, item, timeout, files) {
                return _send(type, item, timeout, files);
            },
            check: function (emailConfig) {
                return _check(emailConfig)
            }
        }
    }]);
'use strict';

angular.module('web')
    .service('Exception', ['toastr', function (toastr) {
        return {
            handle: function (error) {
                toastr.error((error.data || {}).localizedMessage || "Error occurred!");
                console.log(error);
            }
        };
    }]);
/**
 * Created by avas on 31.07.2015.
 */

angular.module('web')
    .factory('Interceptor', ['$q', 'Exception', function ($q, Exception) {

        return {
            responseError: function (rejection) {
                if (rejection.status === 500 && rejection.data.localizedMessage) {
                    Exception.handle(rejection);
                } else if (rejection.status === 401) {
                    var localLoginPage = "#/login?err=session";
                    var ssoPage = "/saml/logout";
                    var isSso = rejection.data &&
                        rejection.data.loginMode &&
                        rejection.data.loginMode === "SSO";

                    window.location = isSso ? ssoPage : localLoginPage;
                }
                return $q.reject(rejection);
            }
        }
    }]);
'use strict';

angular.module('web')
    .service('Regions', ['$q', '$http', 'BASE_URL', function ($q, $http, BASE_URL) {
        var url = BASE_URL + 'rest/regions';

        return {
            get: function () {
                var deferred = $q.defer();
                $http.get(url).success(function (data) {
                    deferred.resolve(data);
                });
                return deferred.promise;

            }
        }
    }]);
'use strict';

angular.module('web')
    .service('Retention', ['$q', '$http', 'BASE_URL', function ($q, $http, BASE_URL) {
        var url = BASE_URL + 'rest/retention';

        var _get = function (id) {
            var deferred = $q.defer();
            $http({
                url: url + "/" + id,
                method: 'GET'
            }).success(function (data) {
                deferred.resolve(data);
            });
            return deferred.promise;
        };


        var _update = function (item) {
            return $http({
                url: url,
                method: "POST",
                data: item
            });
        };

        return {
            get: function (id) {
                return _get(id);
            },
            update: function (item) {
                return _update(item);
            }

        }
    }]);
'use strict';

angular.module('web')
    .service('Storage', [function () {

        return {

            get: function (key) {
                return JSON.parse(sessionStorage.getItem(key));
            },

            save: function (key, data) {
                sessionStorage.setItem(key, JSON.stringify(data));
            },

            remove: function (key) {
                sessionStorage.removeItem(key);
            },

            clearAll: function () {
                sessionStorage.clear();
            }
        };
    }]);
'use strict';

angular.module('web')
    .service('System', ['$q', '$http', 'BASE_URL', function ($q, $http, BASE_URL) {
        var url = BASE_URL + 'rest/system';

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
    }]);
'use strict';

angular.module('web')
    .service('Tasks', ['$q', '$http', 'Storage', 'BASE_URL', function ($q, $http, Storage, BASE_URL) {
        var url = BASE_URL + 'rest/task';

        var getAll = function (volumeId) {
            var deferred = $q.defer();
            $http.get(url + (volumeId ? "/" + volumeId : "")).then(function (result) {
                deferred.resolve(result.data);
            }, function (e) {
                deferred.reject(e);
            });
            return deferred.promise;
        };

        var _getRegular = function (vol) {
            var deferred = $q.defer();
            $http.get(url + '/regular/' + vol).then(function (result) {
                deferred.resolve(result.data);
            }, function (e) {
                deferred.reject(e);
            });
            return deferred.promise;
        };

        var save = function (item) {
            return $http({
                url: url,
                method: 'PUT',
                data: item
            })
        };

        var remove = function (id) {
            return $http({
                url: url + "/" + id,
                method: "DELETE"
            })
        };

        var add = function (item) {
            return $http({
                url: url,
                method: "POST",
                data: item
            });
        };

        return {
            get: function (volumeId) {
                return getAll(volumeId);
            },
            getRegular: function (vol) {
                return _getRegular(vol);
            },
            update: function (item) {
                return save(item);
            },
            insert: function (item) {
                return add(item);
            },
            delete: function (id) {
                return remove(id);
            }
        }
    }]);
'use strict';

angular.module('web')
    .service('Users', ['$q', '$http', 'Storage', 'BASE_URL', function ($q, $http, Storage, BASE_URL) {
        var url = BASE_URL + "rest/user";
        var storageKey = '_users';

        var getUsers = function () {
            var deferred = $q.defer();
            $http({
                url: url,
                method: 'GET'
            }).then(function (result) {
                deferred.resolve(result.data);
            }, function (e) {
                deferred.reject(e);
            });
            return deferred.promise;
        };

        var add = function (user) {
            var deferred = $q.defer();
            $http({
                url: url,
                method: 'POST',
                data: user
            }).then(function (result) {
                deferred.resolve(result.data);
            }, function (e) {
                deferred.reject(e);
            });
            return deferred.promise;
        };

        var updateUser = function (user) {
            return $http({
                url: url,
                method: 'PUT',
                data: user
            })
        };

        var getCurrentUser = function () {
            return Storage.get('currentUser')
        };

        var refreshCurrentUser = function () {
            var deferred = $q.defer();
            $http({
                url: url + "/currentUser",
                method: 'GET'
            }).then(function (result) {
                if (result.data.email) {
                    Storage.save('currentUser', result.data);
                }
                deferred.resolve(result);
            }, function (e) {
                deferred.reject(e);
            });
            return deferred.promise;
        };

        var remove = function (email) {
            return $http({
                url: url + "/" + email,
                method: 'DELETE'
            })
        };

        return {
            insert: function (user) {
                return add(user);
            },

            delete: function (email) {
                return remove(email);
            },

            update: function (user) {
                return updateUser(user);
            },

            getCurrent: function () {
                return getCurrentUser();
            },

            refreshCurrent: function () {
                return refreshCurrentUser();
            },

            getAll: function () {
                return getUsers().then(function (data) {
                    return data;
                })
            }
        }
    }]);
'use strict';

angular.module('web')
    .service('Volumes', ['$q', '$http', 'Storage', 'BASE_URL', function ($q, $http, Storage, BASE_URL) {
        var url = BASE_URL + 'rest/volume';

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
    }]);
'use strict';

angular.module('web')
    .service('Zones', ['$q', '$http', 'BASE_URL', function ($q, $http, BASE_URL) {
        var url = BASE_URL + 'rest/zones';

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
    }]);