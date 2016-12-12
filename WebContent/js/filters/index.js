module.exports = function (app) {
    require('./sizeConvertion.filter')(app);
    require('./stAdvanced.filter')(app);
}