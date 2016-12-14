import LogsController  from "./logs.controller";
import './logs.html'

export default angular.module('web.components', [])
    .controller('LogsController', LogsController)
    .name;