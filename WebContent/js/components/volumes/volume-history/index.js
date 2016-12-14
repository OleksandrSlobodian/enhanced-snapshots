import HistoryController  from "./history.controller";
import './history.html'
import './history-restore.modal.html'

export default angular.module('web.components', [])
    .controller('HistoryController', HistoryController)
    .name;