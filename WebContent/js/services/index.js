module.exports = function (app) {
    require('./auth.service.js')(app);
    require('./backups.service.js')(app);
    require('./configuration.service.js')(app);
    require('./exception.service')(app);
    //require('./interceptor.service')(app);
    require('./regions.service')(app);
    require('./retention.service')(app);
    require('./storage.service')(app);
    require('./system.service')(app);
    require('./tasks.service')(app);
    require('./users.service')(app);
    require('./volumes.service')(app);
    require('./zones.service')(app);
}