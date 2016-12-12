module.exports = function () {
    require('./settings-update.modal.html');
    require('./settings.controller');
    require('./settings.html');
    require('./settings-update.modal.controller');
};

//import settingsCtrl from './settings.controller';
//export default app => {
//    app.component('settings', {
//        templateUrl: 'settings.html',
//        controller: settingsCtrl
//    });
//}