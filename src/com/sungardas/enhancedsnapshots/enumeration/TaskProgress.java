package com.sungardas.enhancedsnapshots.enumeration;

public enum TaskProgress {
    NONE,
    STARTED,
    CREATING_SNAPSHOT,
    WAITING_SNAPSHOT,
    CREATING_TEMP_VOLUME,
    WAITING_TEMP_VOLUME,
    ATTACHING_VOLUME,
    COPYING,
    DETACHING_TEMP_VOLUME,
    DELETING_TEMP_VOLUME,
    DELETING_TEMP_SNAPSHOT,
    CLEANING_TEMP_RESOURCES,
    FAIL_CLEANING,
    INTERRUPTED_CLEANING,
    MOVE_TO_TARGET_ZONE,
    DONE
}
