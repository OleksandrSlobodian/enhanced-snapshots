import autoScroll from "./autoScroll.directive";
import CheckPassword from "./checkPassword.directive";
import ComplexPassword from "./complexPassword.directive";
import Emails from "./emails.directive";
import JqCron from "./jqCron.directive";
import StFilter from "./stFilter.directive";
import TagFilter from "./tagFilter.directive";
import UploadedFile from "./uploadedFile.directive";

export default angular.module('web.directives',
    [

    ])
    .directive('autoScroll', autoScroll)
    .directive('checkPassword', CheckPassword)
    .directive('complexPassword', ComplexPassword)
    .directive('emails', Emails)
    .directive('jqCron', JqCron)
    .directive('stFilter', StFilter)
    .directive('tagFilter', TagFilter)
    .directive('uploadedFile', UploadedFile)
    .name;