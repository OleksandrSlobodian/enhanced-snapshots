package com.sungardas.enhancedsnapshots.aws.dynamodb.model;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTypeConverted;
import com.amazonaws.services.ec2.model.Tag;
import com.sungardas.enhancedsnapshots.aws.dynamodb.Marshaller.ListTagDynamoDBTypeConverter;

import java.util.List;

@DynamoDBTable(tableName = "BackupList")
final public class BackupEntry {

    private String fileName;
    private String timeCreated;
    private String size;
    private String state;
    private String snapshotId;
    private String volumeType;
    private String iops;
    private String sizeGiB;
    private String volumeId;
    private String volumeName;

    @DynamoDBTypeConverted(converter = ListTagDynamoDBTypeConverter.class)
    private List<Tag> tags;


    public BackupEntry() {
    }

	public BackupEntry(String volumeId, String volumeName, String fileName, String timeCreated, String backupSize, BackupState state,
                       String snapshotId, String volumeType, String iops, String sizeGiB) {
        setVolumeId(volumeId);
		setFileName(fileName);
		setTimeCreated(timeCreated);
		setSize(backupSize);
		setState(state.getState());
		setSnapshotId(snapshotId);
		setVolumeType(volumeType);
		setIops(iops);
		setSizeGiB(sizeGiB);
        setVolumeName(volumeName);
	}

    @DynamoDBHashKey()
    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getVolumeId() {
        return volumeId;
    }

    public void setVolumeId(String volumeId) {
        this.volumeId = volumeId;
    }

    public String getTimeCreated() {
        return timeCreated;
    }


    public void setTimeCreated(final String timeCreated) {
        this.timeCreated = timeCreated;
    }

    public String getSize() {
        return size;
    }

    public void setSize(final String size) {
        this.size = size;
    }

    public String getState() {
        return state;
    }

    public void setState(final String state) {
        this.state = state;
    }

    public String getSnapshotId() {
        return snapshotId;
    }

    public void setSnapshotId(final String snapshotId) {
        this.snapshotId = snapshotId;
    }

    public String getVolumeType() {
        return volumeType;
    }

    public void setVolumeType(final String volumeType) {
        this.volumeType = volumeType;
    }

    public String getIops() {
        return iops;
    }

    public void setIops(final String iops) {
        this.iops = iops;
    }

    public String getSizeGiB() {
        return sizeGiB;
    }

    public void setSizeGiB(final String sizeGiB) {
        this.sizeGiB = sizeGiB;
    }

    public String getVolumeName() {
        return volumeName;
    }

    public void setVolumeName(String volumeName) {
        this.volumeName = volumeName;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BackupEntry that = (BackupEntry) o;

        return fileName != null ? fileName.equals(that.fileName) : that.fileName == null;

    }

    @Override
    public int hashCode() {
        return fileName != null ? fileName.hashCode() : 0;
    }

    public void setTags(List<Tag> tags) {
        this.tags = tags;
    }

    public List<Tag> getTags() {
        return tags;
    }
}
