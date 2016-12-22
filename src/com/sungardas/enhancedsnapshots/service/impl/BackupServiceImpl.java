package com.sungardas.enhancedsnapshots.service.impl;

import java.util.Collection;
import java.util.List;

import com.sungardas.enhancedsnapshots.aws.dynamodb.model.BackupEntry;
import com.sungardas.enhancedsnapshots.aws.dynamodb.repository.BackupRepository;
import com.sungardas.enhancedsnapshots.service.BackupService;

import com.sungardas.enhancedsnapshots.service.SnapshotService;
import com.sungardas.enhancedsnapshots.service.StorageService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
public class BackupServiceImpl implements BackupService {

    private static final Logger LOG = LogManager.getLogger(BackupServiceImpl.class);

    private static final String BACKUP_FILE_EXT = ".backup";

    @Autowired
    private BackupRepository backupRepository;
    @Autowired
    private StorageService storageService;
    @Autowired
    private SnapshotService snapshotService;

    private void deleteBackup(BackupEntry backupEntry) {
        try {
            snapshotService.deleteSnapshot(backupEntry.getSnapshotId());
            storageService.deleteFile(backupEntry.getFileName());
            backupRepository.delete(backupEntry);
            LOG.info("Backup {} successfully removed", backupEntry);
        } catch (Exception e) {
            LOG.error("Failed to delete backup {}");
            LOG.error(e);
        }
    }

    @Override
    public List<BackupEntry> getBackupList(String volumeId) {
        return backupRepository.findByVolumeId(volumeId);
    }

    @Override
    public void deleteBackup(String backupName) {
        deleteBackup(backupRepository.findOne(backupName + BACKUP_FILE_EXT));
    }

    @Override
    public void deleteBackup(Collection<BackupEntry> backupEntries) {
        LOG.debug("Removing backups: {}", backupEntries);
        for(BackupEntry backupEntry: backupEntries){
            deleteBackup(backupEntry);
        }
    }
}
