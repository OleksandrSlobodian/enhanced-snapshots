module.exports = function (app) {
    require('./ConfigController')(app);
    require('./HistoryController')(app);
    require('./LoginController')(app);
    require('./LogsController')(app);
    require('./modalSchedule')(app);
    require('./modalSettingsUpdateCtrl')(app);
    require('./modalSystemBackupController')(app);
    require('./modalSystemUninstallController')(app);
    require('./modalVolumeFilterController')(app);
    require('./RegistrationController')(app);
    require('./ScheduleController')(app);
    require('./SettingsController')(app);
    require('./TasksController')(app);
    require('./UserController')(app);
    require('./VolumesController')(app);
}