import LogsController  from "./logs/logs.controller";
import './logs/logs.html';

import SettingsController  from "./settings/settings.controller";
import ModalSettingsUpdateCtrl  from "./settings/settings-update.modal.controller";
import './settings/settings-update.modal.html';
import './settings/settings.html';

import TasksController from "./tasks/tasks.controller";
import './tasks/task-created.modal.html';
import './tasks/task-reject.modal.html';
import './tasks/tasks.html';

import UserController from "./users/user.controller";
import './users/user-added.modal.html';
import './users/user-delete.modal.html';
import './users/user-edit.modal.html';
import './users/users.html';


import ModalVolumeFilterCtrl  from "./volumes/volume-filter/volumeFilter.modal.controller";
import './volumes/volume-filter/volume-filter.modal.html';

import HistoryController  from "./volumes/volume-history/history.controller";
import './volumes/volume-history/history.html';
import './volumes/volume-history/history-restore.modal.html';

import VolumesController  from "./volumes/volume-list/volumes.controller";
import './volumes/volume-list/backup-delete-result.modal.html';
import './volumes/volume-list/backup-delete.modal.html';
import './volumes/volume-list/retention-edit.modal.html';
import './volumes/volume-list/volumeAction.modal.html';
import './volumes/volume-list/volumes.html';

import ScheduleController  from "./volumes/volume-schedule/schedule.controller";
import ModalScheduleCtrl  from "./volumes/volume-schedule/schedule.modal.controller";
import './volumes/volume-schedule/schedule-delete.modal.html';
import './volumes/volume-schedule/schedule-edit.modal.html';
import './volumes/volume-schedule/schedule.html';

export default angular.module('web.components', [])
    .controller('LogsController', LogsController)
    .controller('SettingsController', SettingsController)
    .controller('ModalSettingsUpdateCtrl', ModalSettingsUpdateCtrl)
    .controller('TasksController', TasksController)
    .controller('UserController', UserController)
    .controller('ModalVolumeFilterCtrl', ModalVolumeFilterCtrl)
    .controller('HistoryController', HistoryController)
    .controller('VolumesController', VolumesController)
    .controller('ScheduleController', ScheduleController)
    .controller('ModalScheduleCtrl', ModalScheduleCtrl)
    .name;

