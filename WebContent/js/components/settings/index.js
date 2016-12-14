import SettingsController  from "./settings.controller";
import ModalSettingsUpdateCtrl  from "./settings-update.modal.controller";
import './settings-update.modal.html'
import './settings.html'

export default angular.module('web.components', [])
    .controller('SettingsController', SettingsController)
    .controller('ModalSettingsUpdateCtrl', ModalSettingsUpdateCtrl)
    .name;