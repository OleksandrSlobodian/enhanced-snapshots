import VolumesController  from "./volumes.controller";
import './backup-delete-result.modal.html'
import './backup-delete.modal.html'
import './retention-edit.modal.html'
import './volumeAction.modal.html'
import './volumes.html'

export default angular.module('web.components', [])
    .controller('VolumesController', VolumesController)
    .name;