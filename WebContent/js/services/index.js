module.exports = function (app) {
    require('./Auth')(app);
    require('./Backups')(app);
    require('./Configuration')(app);
    require('./Exception')(app);
    require('./Interceptor')(app);
    require('./Regions')(app);
    require('./Retention')(app);
    require('./Storage')(app);
    require('./System')(app);
    require('./Tasks')(app);
    require('./Users')(app);
    require('./Volumes')(app);
    require('./Zones')(app);
}