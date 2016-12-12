module.exports = function (app) {
    require('./autoScroll.directive')(app);
    require('./checkPassword.directive')(app);
    require('./complexPassword.directive')(app);
    require('./emails.directive')(app);
    require('./jqCron.directive')(app);
    require('./stFilter.directive')(app);
    require('./tagFilter.directive')(app);
    require('./uploadedFile.directive')(app);
}