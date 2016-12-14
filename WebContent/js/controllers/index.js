import ConfigController from "./config.controller";
import LoginController from "./login.controller";
import RegistrationController from "./registration.controller";
import ModalSystemBackupCtrl from "./systemBackup.modal.controller";
import ModalSystemUninstallCtrl from "./systemUninstall.modal.controller";

export default angular.module('web.controllers',
    [

    ])
    .controller('ConfigController', ConfigController)
    .controller('LoginController', LoginController)
    .controller('RegistrationController', RegistrationController)
    .controller('ModalSystemBackupCtrl', ModalSystemBackupCtrl)
    .controller('ModalSystemUninstallCtrl', ModalSystemUninstallCtrl)
    .name;