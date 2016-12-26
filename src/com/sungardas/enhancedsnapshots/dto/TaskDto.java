package com.sungardas.enhancedsnapshots.dto;


import java.util.Collections;
import java.util.List;

public class TaskDto {

    private String id;
    private String status;
    private String type;
    private List<VolumeInfo> volumes = Collections.emptyList();
    private String schedulerManual;
    private String schedulerName;
    private String schedulerTime;
    private String backupFileName;
    private String cron;
    private String zone;
    private String regular = Boolean.FALSE.toString();
    private String enabled;
    private String instanceToAttach;


    public TaskDto() {
    }

    public String getBackupFileName() {
        return backupFileName;
    }

    public void setBackupFileName(String backupFileName) {
        this.backupFileName = backupFileName;
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

    public List<VolumeInfo> getVolumes() {
        return volumes;
    }

    public void setVolumes(List<VolumeInfo> volumes) {
        this.volumes = volumes;
    }

    public String getSchedulerManual() {
        return schedulerManual;
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

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public String getEnabled() {
        return enabled;
    }

    public void setEnabled(String enabled) {
        this.enabled = enabled;
    }
    public String getZone() {
        return zone;
    }

    public void setZone(String zone) {
        this.zone = zone;
    }


    public String getInstanceToAttach() {
        return instanceToAttach;
    }

    public void setInstanceToAttach(String instanceToAttach) {
        this.instanceToAttach = instanceToAttach;
    }

    public static class VolumeInfo {
        public String volumeId;
        public String zone;
        public String instanceId;

        public VolumeInfo(String volumeId, String zone, String instanceId) {
            this.volumeId = volumeId;
            this.zone = zone;
            this.instanceId = instanceId;
        }
    }
}
