import ModalVolumeFilterCtrl  from "./volumeFilter.modal.controller";
import './volume-filter.modal.html'

export default angular.module('web.components', [])
    .controller('ModalVolumeFilterCtrl', ModalVolumeFilterCtrl)
    .name;