import TasksController from "./tasks.controller";
import './task-created.modal.html'
import './task-reject.modal.html'
import './tasks.html'

export default angular.module('web.components', [])
    .controller('TasksController', TasksController)
    .name;