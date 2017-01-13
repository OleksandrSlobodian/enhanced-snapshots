package com.sungardas.enhancedsnapshots.service;

import com.amazonaws.SdkClientException;
import com.amazonaws.services.ec2.model.*;
import com.amazonaws.services.simplesystemsmanagement.model.InstanceInformation;

import java.util.List;

public interface AWSCommunicationService {

    Snapshot createSnapshot(Volume volume);

    void deleteSnapshot(String snapshotId);

    void cleanupSnapshots(String volumeId, String snapshotIdToLeave);

    Snapshot waitForCompleteState(Snapshot snapshot);

    Snapshot syncSnapshot(String snapshotId);

    Volume waitForVolumeState(Volume volume, VolumeState expectedState);

    Volume syncVolume(Volume volume);

    /**
     * Checks whether volume is root volume
     * @param volumeId
     * @return
     */
    boolean isRootVolume(String volumeId);

    /**
     * Returns InstanceInformation in case AWS SSM service is supported in region and enabled on instance
     * Throws SdkClientException in case operation is not supported for current region
     * @param instanceId
     * @return
     */
    InstanceInformation getSSMInstanceInformation(String instanceId) throws SdkClientException;

    /**
     * Returns instance id of instance volume belongs to
     * @param volumeId
     * @return instance id of instance volume belongs to or null in case volume is not atttached
     */
    String getInstanceVolumeBelongsTo(String volumeId);

    /**
     * iopsPerGb paramenter is only required for io1 volume type, for other volume types it will be skipped
     */
    Volume createVolumeFromSnapshot(String snapshotId, String availabilityZoneName, VolumeType type, int iopsPerGb);

    void deleteVolume(Volume volume);

    void attachVolume(Instance instance, Volume volume);

	Volume getVolume(String volumeId);

    List<AvailabilityZone> describeAvailabilityZonesForCurrentRegion();

    String getCurrentAvailabilityZone();

    void createTemporaryTag(String resourceId, String description);

    void deleteTemporaryTag(String resourceId);

    Volume createVolume(int size, VolumeType type);

    Volume createIO1Volume(int size, int iopsPerGb);

    Instance getInstance(String instanceId);

    InstanceStatus getInstanceStatus(String instanceId);

    /**
     * Returns list of instances in stopped or running state
     * @return
     */
    List<Instance> getInstances();

    void detachVolume(Volume volume);

    void setResourceName(String resourceid, String value);

    void addTag(String resourceId, String name, String value);

    void addTag(String resourceId, List<Tag> tags);

    boolean snapshotExists(String snapshotId);

    boolean volumeExists(String volumeId);

    Snapshot getSnapshot(String snapshotId);

    void dropS3Bucket(String bucketName);

    //TODO: awslog restarted after each sdfs remount
    //TODO: need to clarify whether we still need restart it
    //TODO: in case we need, it should be triggered by SystemService while restarting sdfs, not by SDFSStateService
    void restartAWSLogService();

    String getDNSName(String instanceId);
}