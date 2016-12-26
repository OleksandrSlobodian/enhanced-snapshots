module.exports = function(grunt) {

    grunt.initConfig({
        pkg: grunt.file.readJSON('package.json'),

        concat: {
            dist: {
                src: [
                    'js/app.js',
                    'js/**/*.js',
                    '!js/esnap.js',
                    '!js/esnap.min.js'
                ],
                dest: 'js/esnap.js'
            },
            dev: {
                src: [
                    'js/app.js',
                    'js/**/*.js',
                    '!js/esnap.js',
                    '!js/esnap.min.js'
                ],
                dest: 'js/esnap.min.js' //code here is actually not minified!
            }
        },

        watch: {
            scripts: {
                files: 'js/**/*.js',
                tasks: ['default'],
                options: {
                    spawn:false,
                    event:['all']
                }
            },
            scripts: {
                files: 'partials/**/*.html',
                tasks: ['default'],
                options: {
                    spawn:false,
                    event:['all']
                }
            }
            //scripts: {
            //    files: 'css/**/*.css',
            //    tasks: ['default'],
            //    options: {
            //        spawn:false,
            //        event:['all']
            //    }
            //}
        },

        copy: {
            main: {
                files: [
                    // includes files within path
                    {expand: true, cwd: '../WebContent/', src: ['**'], dest: 'C:/Program Files/apache-tomcat-8.5.8/webapps/ROOT/'}
                ]
            }
        },

        uglify: {
            build: {
                src: 'js/esnap.js',
                dest: 'js/esnap.min.js'
            }
        },

        bower_concat: {
            options: {
                separator : ';'
            },
            all: {
                dest: {
                    js: 'lib/vendor.min.js',
                    css: 'lib/vendor.css'
                },
                bowerOptions: {
                    relative: false
                },
                mainFiles: {
                    'jquery': 'dist/jquery.min.js',
                    'angular': 'angular.min.js',
                    'angular-ui-router': 'release/angular-ui-router.min.js',
                    'bootstrap': [
                        'dist/css/bootstrap.css',
                        'dist/js/bootstrap.min.js'
                    ],
                    'angular-bootstrap': 'ui-bootstrap-tpls.min.js',
                    'angular-smart-table': 'dist/smart-table.min.js',
                    'angular-toastr': [
                        'dist/angular-toastr.min.css',
                        'dist/angular-toastr.tpls.min.js'
                    ],
                    'jquery-cron': [
                        'cron/jquery-cron.css',
                        'cron/jquery-cron-min.js'
                    ],
                    'ng-tags-input': [
                        'ng-tags-input.min.css',
                        'ng-tags-input.bootstrap.min.css',
                        'ng-tags-input.min.js'
                    ],
                    'angular-awesome-slider': [
                        'dist/css/angular-awesome-slider.min.css',
                        'dist/angular-awesome-slider.min.js'
                    ],
                    'ng-stomp': 'ng-stomp.min.js',
                    'sockjs': 'sockjs.min.js',
                    'stomp-websocket': 'lib/stomp.min.js'
                }

            },
            dev: {
                dest: {
                    js: 'lib/vendor.min.js',
                    css: 'lib/vendor.css'
                },
                bowerOptions: {
                    relative: false
                },
                mainFiles: {
                    'jquery': 'dist/jquery.js',
                    'angular': 'angular.js',
                    'angular-ui-router': 'release/angular-ui-router.js',
                    'bootstrap': [
                        'dist/css/bootstrap.css',
                        'dist/js/bootstrap.js'
                    ],
                    'angular-bootstrap': 'ui-bootstrap-tpls.js',
                    'angular-smart-table': 'dist/smart-table.js',
                    'angular-toastr': [
                        'dist/angular-toastr.min.css',
                        'dist/angular-toastr.tpls.js'
                    ],
                    'jquery-cron': [
                        'cron/jquery-cron.css',
                        'cron/jquery-cron-min.js'
                    ],
                    'ng-tags-input': [
                        'ng-tags-input.min.css',
                        'ng-tags-input.bootstrap.min.css',
                        'ng-tags-input.js'
                    ],
                    'angular-awesome-slider': [
                        'dist/css/angular-awesome-slider.min.css',
                        'dist/angular-awesome-slider.js'
                    ],
                    'ng-stomp': 'ng-stomp.js',
                    'sockjs': 'sockjs.js',
                    'stomp-websocket': 'lib/stomp.js'
                }

            }
        }
    });

    grunt.loadNpmTasks('grunt-contrib-concat');
    grunt.loadNpmTasks('grunt-contrib-uglify');
    grunt.loadNpmTasks('grunt-bower-concat');
    grunt.loadNpmTasks('grunt-contrib-watch');
    grunt.loadNpmTasks('grunt-contrib-copy');

    grunt.registerTask( 'default', [ 'dev', 'copy' ] );
    grunt.registerTask('prod', ['concat', 'uglify', 'bower_concat:all']);
    grunt.registerTask('dev', ['concat:dev', 'bower_concat:dev']);
};