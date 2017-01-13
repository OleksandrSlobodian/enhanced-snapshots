package com.sungardas.enhancedsnapshots.rest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sungardas.enhancedsnapshots.aws.dynamodb.model.BackupEntry;
import com.sungardas.enhancedsnapshots.exception.DataAccessException;
import com.sungardas.enhancedsnapshots.exception.DataException;
import com.sungardas.enhancedsnapshots.service.BackupService;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.security.RolesAllowed;

@RestController
@RequestMapping("/backup")
public class BackupController {

    private static final Logger LOG = LogManager.getLogger(BackupController.class);

    @Autowired
    private BackupService backupService;


    @ExceptionHandler(DataException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ResponseBody
    private DataException dataException(DataException e) {
        return e;
    }

    @RolesAllowed({"ROLE_ADMIN", "ROLE_USER"})
    @RequestMapping(value = "/{volumeId}", method = RequestMethod.GET)
    public ResponseEntity<String> get(@PathVariable(value = "volumeId") String volumeId) {
        List<BackupEntry> items = backupService.getBackupList(volumeId);
        LOG.debug("Available backups for volume {}: [{}] .", volumeId, jsonArrayRepresentation(items).toString());
        return new ResponseEntity<>(jsonArrayRepresentation(items).toString(), HttpStatus.OK);
    }

    private JSONArray jsonArrayRepresentation(List<BackupEntry> backupEntries) {
        JSONArray backupsJSONArray = new JSONArray();
        for (BackupEntry entry : backupEntries) {
            JSONObject backupItem = new JSONObject();
            backupItem.put("fileName", entry.getFileName());
            backupItem.put("volumeId", entry.getVolumeId());
            backupItem.put("timeCreated", entry.getTimeCreated());
            backupItem.put("size", entry.getSize());
            backupsJSONArray.put(backupItem);
        }
        return backupsJSONArray;
    }

    @RolesAllowed({"ROLE_ADMIN", "ROLE_USER"})
    @RequestMapping(value = "/consistent", method = RequestMethod.POST)
    public ResponseEntity checkConsistentBackupSupport (@RequestBody String[] volumeIds) {
        Map<String, Boolean> result = new HashMap<>();
        for(String volumeId: volumeIds) {
            result.put(volumeId, backupService.consistentBackupSupported(volumeId));
        }
        return new ResponseEntity<>(result, HttpStatus.OK);
    }


    @RolesAllowed({"ROLE_ADMIN", "ROLE_USER"})
    @RequestMapping(value = "/{backupName}", method = RequestMethod.DELETE)
    public ResponseEntity<String> deleteBackup(@PathVariable String backupName) {
        LOG.debug("Removing backup [{}]", backupName);
        try {
            backupService.deleteBackup(backupName);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (DataAccessException e) {
            return new ResponseEntity<>("Failed to remove backup.", HttpStatus.NOT_ACCEPTABLE);
        }
    }
}
