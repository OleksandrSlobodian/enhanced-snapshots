import Auth from "./auth.service";
import Backups from "./backups.service";
import Configuration from "./configuration.service";
import Exception from "./exception.service";
import Interceptor from "./interceptor.service";
import Regions from "./regions.service";
import Storage from "./storage.service";
import System from "./system.service";
import Tasks from "./tasks.service";
import Users from "./users.service";
import Volumes from "./volumes.service";
import Zones from "./zones.service";

export default angular.module('web.services',
    [

    ])
    .factory('Interceptor', Interceptor)
    .service('Auth', Auth)
    .service('Backups', Backups)
    .service('Configuration', Configuration)
    .service('Exception', Exception)
    .service('Regions', Regions)
    .service('Storage', Storage)
    .service('System', System)
    .service('Tasks', Tasks)
    .service('Users', Users)
    .service('Volumes', Volumes)
    .service('Zones', Zones)
    .name;