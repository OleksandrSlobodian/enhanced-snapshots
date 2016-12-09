module.exports = function (app) {
    require('./autoScroll')(app);
    require('./checkPassword')(app);
    require('./complexPassword')(app);
    require('./emails')(app);
    require('./jqCron')(app);
    require('./stFilter')(app);
    require('./tagFilter')(app);
    require('./uploadedFile')(app);
}