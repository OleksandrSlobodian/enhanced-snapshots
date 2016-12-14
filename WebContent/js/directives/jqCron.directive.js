class JqCron {
    constructor () {
        this.restrict = 'E';
        this.require = 'ngModel';
        this.scope = {
            ngModel: '='
        };
    }
    link (scope, ele, attr, ctrl) {
        var options = {
            initial: scope.ngModel || "* * * * *",
            onChange: ()  =>{
                var value = $(this).cron("value");
                scope.ngModel = value;
                if(ctrl.$viewValue != value){
                    ctrl.$setViewValue(value);
                }
            }
        };
        $(ele).cron(options);
    }
}
export default () => new JqCron();