package com.sungardas.enhancedsnapshots.util;

import com.amazonaws.util.EC2MetadataUtils;


public class SystemUtils {

    private static String CLUSTER_ID = System.getenv("CLUSTER_ID");
    private static String STACK_NAME = System.getenv("STACK_NAME");
    private static String DEV_SYSTEM_ID = "DEV";

    public static String getSystemId() {
        if (CLUSTER_ID != null) {
            return CLUSTER_ID;
        }
        return getInstanceId();
    }

    public static boolean clusterMode() {
        if (CLUSTER_ID != null) {
            return true;
        }
        return false;
    }

    public static SystemMode getSystemMode() {
        if (CLUSTER_ID != null) {
            return SystemMode.CLUSTER;
        }
        return SystemMode.STANDALONE;
    }


    public enum SystemMode {
        STANDALONE, CLUSTER
    }

    public static String getCloudFormationStackName() {
        return STACK_NAME;
    }

    private static String instanceId;

    public static String getInstanceId() {
        if (instanceId == null) {
            instanceId = EC2MetadataUtils.getInstanceId();
            if (instanceId != null) {
                return instanceId;
            } else {
                instanceId = DEV_SYSTEM_ID;
            }
        }
        return instanceId;
    }
}
