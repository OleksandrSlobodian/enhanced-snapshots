package com.sungardas.snapdirector.aws.dynamodb.model;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;

@DynamoDBTable(tableName = "Snapshots")
public class Snapshot {

    @DynamoDBHashKey(attributeName = "snapshotId")
    private String snapshotId;

    @DynamoDBAttribute(attributeName = "volumeId")
    private String volumeId;

    public Snapshot() {
    }

    public Snapshot(String snapshotId, String volumeId) {
        this.snapshotId = snapshotId;
        this.volumeId = volumeId;
    }

    public String getSnapshotId() {
        return snapshotId;
    }

    public void setSnapshotId(String snapshotId) {
        this.snapshotId = snapshotId;
    }

    public String getVolumeId() {
        return volumeId;
    }

    public void setVolumeId(String volumeId) {
        this.volumeId = volumeId;
    }
}
