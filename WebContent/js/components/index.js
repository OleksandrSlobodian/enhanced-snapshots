module.exports = function (app) {
    require('./logs/index.js')(app);
    require('./settings/index.js')(app);
    //require('./logs/logs.component')(app);
    //require('./settings/settings.component')(app);
    //require('./settings/settings-update.component')(app);

    require('./tasks/index')(app);
    require('./users/index')(app);
    require('./volumes/volume-filter/index')(app);
    require('./volumes/volume-history/index')(app);
    require('./volumes/volume-list/index')(app);
    require('./volumes/volume-schedule/index')(app);
};