import SizeConvertion from "./sizeConvertion.filter";
import StAdvancedFilter from "./stAdvanced.filter";

export default angular.module('web.filters',
    [

    ])
    .filter('SizeConvertion', SizeConvertion)
    .filter('StAdvancedFilter', StAdvancedFilter)
    .name;