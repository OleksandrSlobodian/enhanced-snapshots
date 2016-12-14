import ScheduleController  from "./schedule.controller";
import ModalScheduleCtrl  from "./schedule.modal.controller";
import './schedule-delete.modal.html'
import './schedule-edit.modal.html'
import './schedule.html'

export default angular.module('web.components', [])
    .controller('ScheduleController', ScheduleController)
    .controller('ModalScheduleCtrl', ModalScheduleCtrl)
    .name;