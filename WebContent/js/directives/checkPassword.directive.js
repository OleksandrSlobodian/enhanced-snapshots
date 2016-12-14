class CheckPassword {
    constructor () {
        this.require = 'ngModel';
    }
    link (scope, elem, attrs, ctrl) {
        var firstPassword = '#' + attrs.checkPassword;
        elem.bind('keyup', () => {
            scope.$apply(() => {
                var firstPass = angular.element(document.querySelector(firstPassword)).val();
                var v = elem.val() === firstPass;
                ctrl.$setValidity('passwordmatch', v);
            });
        });
    }
}
export default () => new CheckPassword();