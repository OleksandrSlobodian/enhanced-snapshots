package com.sungardas.enhancedsnapshots.aws.dynamodb.model;

import com.amazonaws.services.dynamodbv2.datamodeling.*;
import com.amazonaws.services.ec2.model.Tag;
import com.amazonaws.util.json.Jackson;
import com.sungardas.enhancedsnapshots.aws.dynamodb.Marshaller.ListTagDynamoDBTypeConverter;
import com.sungardas.enhancedsnapshots.enumeration.TaskProgress;

import java.util.List;


@DynamoDBTable(tableName = "Tasks")
public class TaskEntry {

    @DynamoDBHashKey
    private String id;
    private String worker;
    private String status;
    private String type;
    private String volume;
    private String schedulerManual;
    private String schedulerName;
    private String schedulerTime;
    private String backupFileName;
    private long completeTime;
    private long startTime;
    private String tempVolumeType;
    private int tempVolumeIopsPerGb;
    private String progress = TaskProgress.NONE.name();
    private String tempVolumeId;
    private String tempSnapshotId;
    private String availabilityZone;

    // for backup tasks only
    private String cron;
    private String regular = Boolean.FALSE.toString();
    private String enabled;
    @DynamoDBTypeConverted(converter = ListTagDynamoDBTypeConverter.class)
    private List<Tag> tags;
    private boolean consistentBackup;

    // for restore tasks only
    private String restoreVolumeType;
    private int restoreVolumeIopsPerGb;
    private String instanceToAttach;


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getInstanceToAttach() {
        return instanceToAttach;
    }

    public void setInstanceToAttach(String instanceToAttach) {
        this.instanceToAttach = instanceToAttach;
    }

    public String getWorker() {
        return worker;
    }

    public void setWorker(String worker) {
        this.worker = worker;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getVolume() {
        return volume;
    }

    public void setVolume(String volume) {
        this.volume = volume;
    }

    public String getSchedulerManual() {
        return String.valueOf(schedulerManual);
    }

    public void setSchedulerManual(boolean schedulerManual) {
        this.schedulerManual = String.valueOf(schedulerManual);
    }

    public void setSchedulerManual(String schedulerManual) {
        this.schedulerManual = schedulerManual;
    }

    public String getSchedulerName() {
        return schedulerName;
    }

    public void setSchedulerName(String schedulerName) {
        this.schedulerName = schedulerName;
    }

    public String getSchedulerTime() {
        return schedulerTime;
    }

    public void setSchedulerTime(String schedulerTime) {
        this.schedulerTime = schedulerTime;
    }

    public void setBackupFileName(String backupFileName) {
        this.backupFileName = backupFileName;
    }

    public String getCron() {
        return cron;
    }

    public void setCron(String cron) {
        this.cron = cron;
    }

    public String getRegular() {
        return regular;
    }

    public void setRegular(String regular) {
        this.regular = regular;
    }

    public void setRegular(boolean regular) {
        this.regular = String.valueOf(regular);
    }

    public String getEnabled() {
        return enabled;
    }

    public void setEnabled(String enabled) {
        this.enabled = enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = String.valueOf(enabled);
    }

    public long getCompleteTime() {
        return completeTime;
    }

    public void setCompleteTime(long completeTime) {
        this.completeTime = completeTime;
    }

    public String getBackupFileName() {
        return backupFileName;
    }

    public int getTempVolumeIopsPerGb() {
        return tempVolumeIopsPerGb;
    }

    public void setTempVolumeIopsPerGb(int tempVolumeIopsPerGb) {
        this.tempVolumeIopsPerGb = tempVolumeIopsPerGb;
    }

    public String getTempVolumeType() {
        return tempVolumeType;
    }

    public void setTempVolumeType(String tempVolumeType) {
        this.tempVolumeType = tempVolumeType;
    }

    public String getAvailabilityZone() {
        return availabilityZone;
    }

    public void setAvailabilityZone(String availabilityZone) {
        this.availabilityZone = availabilityZone;
    }

    @Deprecated
    @Override
    public String toString() {
        return Jackson.toJsonString(this);
    }

    public String getRestoreVolumeType() {
        return restoreVolumeType;
    }

    public boolean isConsistentBackup() {
        return consistentBackup;
    }

    public void setConsistentBackup(boolean consistentBackup) {
        this.consistentBackup = consistentBackup;
    }

    public void setRestoreVolumeType(String restoreVolumeType) {
        this.restoreVolumeType = restoreVolumeType;
    }

    public int getRestoreVolumeIopsPerGb() {
        return restoreVolumeIopsPerGb;
    }

    public void setRestoreVolumeIopsPerGb(int restoreVolumeIopsPerGb) {
        this.restoreVolumeIopsPerGb = restoreVolumeIopsPerGb;
    }

    public void setProgress(String progress) {
        this.progress = progress;
    }

    public String getProgress() {
        return progress;
    }

    public String getTempVolumeId() {
        return tempVolumeId;
    }


    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(final long startTime) {
        this.startTime = startTime;
    }

    public void setTempVolumeId(final String tempVolumeId) {
        this.tempVolumeId = tempVolumeId;
    }

    public String getTempSnapshotId() {
        return tempSnapshotId;
    }

    public void setTempSnapshotId(final String tempSnapshotId) {
        this.tempSnapshotId = tempSnapshotId;
    }

    public TaskProgress progress() {
        return TaskProgress.valueOf(progress);
    }

    public void setTags(List<Tag> tags) {
        this.tags = tags;
    }

    public List<Tag> getTags() {
        return tags;
    }

    public enum TaskEntryType {
        BACKUP("backup"),
        RESTORE("restore"),
        UNKNOWN("unknown");

        private String type;

        TaskEntryType(String type) {
            this.type = type;
        }

        public static TaskEntryType getType(String type) {
            try {
                return valueOf(type.toUpperCase());
            } catch (IllegalArgumentException e) {
                return UNKNOWN;
            }
        }

        public String getType() {
            return type;
        }

    }

    public enum TaskEntryStatus {
        WAITING("waiting"),
        RUNNING("running"),
        QUEUED("queued"),
        COMPLETE("complete"),
        CANCELED("canceled"),
        PARTIALLY_FINISHED("partially_finished"),
        ERROR("error");

        private String status;

        TaskEntryStatus(String status) {
            this.status = status;
        }

        @Override
        public String toString() {
            return status;
        }
        public String getStatus() {
            return status;
        }
    }
}
