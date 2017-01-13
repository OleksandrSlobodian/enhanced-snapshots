package com.sungardas.enhancedsnapshots.service;

import com.sungardas.enhancedsnapshots.aws.dynamodb.model.BackupEntry;

import java.util.List;

import java.util.Collection;

public interface BackupService {
    void deleteBackup(String backupName);

    List<BackupEntry> getBackupList(String volumeId);

    void deleteBackup(Collection<BackupEntry> backupEntries);

    boolean consistentBackupSupported (String volumeId);
}
