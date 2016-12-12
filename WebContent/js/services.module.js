import Interceptor from './services/interceptor.service';

export default angular.module('app.services',
    [

    ])
    .factory('Interceptor', Interceptor)
    .config(($httpProvider) => {
        "ngInject";
        $httpProvider.defaults.headers.common["X-Requested-With"] = 'XMLHttpRequest';
        $httpProvider.interceptors.push('Interceptor');
    })
    .name