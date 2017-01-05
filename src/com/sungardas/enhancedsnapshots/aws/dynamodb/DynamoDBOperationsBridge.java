package com.sungardas.enhancedsnapshots.aws.dynamodb;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig;
import org.socialsignin.spring.data.dynamodb.core.DynamoDBTemplate;
import org.socialsignin.spring.data.dynamodb.mapping.event.AfterDeleteEvent;
import org.socialsignin.spring.data.dynamodb.mapping.event.BeforeDeleteEvent;
import org.socialsignin.spring.data.dynamodb.mapping.event.BeforeSaveEvent;

import java.util.Collections;
import java.util.Iterator;

public class DynamoDBOperationsBridge extends DynamoDBTemplate {

    private DynamoDBMapperConfig config;

    public DynamoDBOperationsBridge(AmazonDynamoDB amazonDynamoDB, DynamoDBMapperConfig dynamoDBMapperConfig) {
        super(amazonDynamoDB, dynamoDBMapperConfig);
        config = dynamoDBMapperConfig;
    }

    public DynamoDBOperationsBridge(AmazonDynamoDB amazonDynamoDB) {
        super(amazonDynamoDB);
    }

    @Override
    public void batchSave(Iterable<?> entities) {
        Iterator<?> iteratorBefore = entities.iterator();
        while( iteratorBefore.hasNext() ){
            maybeEmitEvent(new BeforeSaveEvent<Object>(iteratorBefore.next()));
        }
        dynamoDBMapper.batchWrite(entities, Collections.EMPTY_LIST, config);
        Iterator<?> iteratorAfter = entities.iterator();
        while( iteratorAfter.hasNext() ){
            maybeEmitEvent(new BeforeSaveEvent<Object>(iteratorAfter.next()));
        }
    }

    @Override
    public void batchDelete(Iterable<?> entities) {
        Iterator<?> iteratorBefore = entities.iterator();
        while( iteratorBefore.hasNext() ){
            maybeEmitEvent(new BeforeDeleteEvent<Object>(iteratorBefore.next()));
        }
        dynamoDBMapper.batchWrite(Collections.EMPTY_LIST, entities, config);
        Iterator<?> iteratorAfter = entities.iterator();
        while( iteratorAfter.hasNext() ){
            maybeEmitEvent(new AfterDeleteEvent<Object>(iteratorAfter.next()));
        }
    }
}
