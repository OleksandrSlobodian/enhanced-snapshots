package com.sungardas.enhancedsnapshots.aws.dynamodb.model;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;

@DynamoDBTable(tableName = "NotificationConfiguration")
public class NotificationConfigurationEntry {

    @DynamoDBHashKey
    public static final String id = "NOTIFICATION_CONFIG";

    @DynamoDBAttribute
    private String snsTopic;

    public String getSnsTopic() {
        return snsTopic;
    }

    public void setSnsTopic(String snsTopic) {
        this.snsTopic = snsTopic;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {}
}
