import UserController from "./user.controller";
import './user-added.modal.html'
import './user-delete.modal.html'
import './user-edit.modal.html'
import './users.html'

export default angular.module('web.components', [])
    .controller('UserController', UserController)
    .name;