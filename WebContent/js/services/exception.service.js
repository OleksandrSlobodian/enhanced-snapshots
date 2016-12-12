'use strict';

angular.module('web')
    .service('Exception', exception);

function exception (toastr) {
    "ngInject";
    return {
        handle: function (error){
            toastr.error((error.data || {}).localizedMessage || "Error occurred!");
            console.log(error);
        }
    };
}