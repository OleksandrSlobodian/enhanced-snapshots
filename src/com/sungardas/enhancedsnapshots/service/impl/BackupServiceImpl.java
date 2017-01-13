package com.sungardas.enhancedsnapshots.service.impl;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import com.amazonaws.SdkClientException;
import com.amazonaws.services.simplesystemsmanagement.model.InstanceInformation;
import com.sungardas.enhancedsnapshots.aws.dynamodb.model.BackupEntry;
import com.sungardas.enhancedsnapshots.aws.dynamodb.repository.BackupRepository;
import com.sungardas.enhancedsnapshots.service.AWSCommunicationService;
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
    private static final String[] SUPPORTED_PLATFORMS_FOR_CONSIATENT_BACKUP = {"Windows", "Linux"};
    private static final String SSM_DOCUMENT_TO_DETERMINE_FS = "";


    @Autowired
    private BackupRepository backupRepository;
    @Autowired
    private StorageService storageService;
    @Autowired
    private SnapshotService snapshotService;
    @Autowired
    private AWSCommunicationService awsCommunicationService;

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

    @Override
    public boolean consistentBackupSupported(String volumeId) {
        String instanceId = awsCommunicationService.getInstanceVolumeBelongsTo(volumeId);
        if (instanceId == null) {
            LOG.info("Volume {} is not attached. Backup is consistent by default.", volumeId);
            return true;
        }
        if (awsCommunicationService.isRootVolume(volumeId)) {
            LOG.info("Consistent backup are not supported for volume {}, since it's a root volume.", volumeId);
            return false;
        }
        InstanceInformation instanceInformation;
        try {
            instanceInformation = awsCommunicationService.getSSMInstanceInformation(instanceId);
            if (instanceInformation == null) {
                LOG.info("Volume {} is attached to the instance {} which is not accessible for AWS EC2 System Management Service. Consistent backup is not supported.",
                        volumeId, instanceId);
                return false;
            }
            // we will receive SdkClientException in case AWS SSM service is not accessible for current region
        } catch (SdkClientException e) {
            LOG.info("AWS SMS service is not supported in current region. Consistent backup for volume {} is not supported.", volumeId);
            return false;
        }
        if (!platformIsSupportedForConsistentBackup(instanceInformation.getPlatformType())) {
            LOG.info("Consistent backup for volume {} is not supported. Instance {} has unsupported OS type: {}.", volumeId, instanceId, instanceInformation.getPlatformType());
            return false;
        }
        //TODO: find out SF type
        if (fsIsSupportedForConsistentBackup()) {
            LOG.info("Consistent backup for volume {} is not supported. Instance {} has unsupported OS type: {}.", volumeId, instanceId, instanceInformation.getPlatformType());
            return false;
        }

        LOG.info("Consistent backup for volume {} is supported.", volumeId);
        return true;
    }

    private boolean platformIsSupportedForConsistentBackup(String platform) {
        return Arrays.asList(SUPPORTED_PLATFORMS_FOR_CONSIATENT_BACKUP).contains(platform);
    }

    private boolean fsIsSupportedForConsistentBackup()
    {

        return true;
    }




}
