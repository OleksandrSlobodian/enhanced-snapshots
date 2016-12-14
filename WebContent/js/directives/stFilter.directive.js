class StFilter {
    constructor () {
        this.scope = {
            stFilter: '='
        };
        this.require = '^stTable';
    }
    link (scope, ele, attr, ctrl) {
        var table = ctrl;

        scope.$watch('stFilter', function (val) {
            ctrl.search(val, 'availabilityZone');
        });
    }
}

export default () => new StFilter();