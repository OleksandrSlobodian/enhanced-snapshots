'use strict';

angular.module('web')
    .directive('jqCron', jqCron);

function jqCron (){
    return {
        restrict: 'E',
        require: 'ngModel',
        scope: {
            ngModel: '='
        },
        link:function(scope, ele, attr, ctrl){
            var options = {
                initial: scope.ngModel || "* * * * *",
                onChange: function () {
                    var value = $(this).cron("value");
                    scope.ngModel = value;
                    if(ctrl.$viewValue != value){
                        ctrl.$setViewValue(value);
                    }
                }
            };
            $(ele).cron(options);
        }
    };

}