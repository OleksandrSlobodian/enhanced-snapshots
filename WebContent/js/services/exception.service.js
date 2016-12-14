export default function Exception (toastr) {
    "ngInject";
    return {
        handle: function (error){
            toastr.error((error.data || {}).localizedMessage || "Error occurred!");
            console.log(error);
        }
    };
}