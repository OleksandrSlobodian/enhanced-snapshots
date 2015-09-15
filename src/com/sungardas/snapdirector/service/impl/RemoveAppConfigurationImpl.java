package com.sungardas.snapdirector.service.impl;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.util.EC2MetadataUtils;
import com.sungardas.snapdirector.aws.dynamodb.model.*;
import com.sungardas.snapdirector.aws.dynamodb.repository.*;
import com.sungardas.snapdirector.exception.OperationNotAllowedException;
import com.sungardas.snapdirector.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

@Service
public class RemoveAppConfigurationImpl implements RemoveAppConfiguration {
    @Value("${snapdirector.db.tables}")
    private String[] tables;

    @Autowired
    private AmazonSQS sqs;

    @Autowired
    private AmazonDynamoDB db;
    private DynamoDB dynamoDB;

    @Autowired
    private AmazonS3 s3;

    @Autowired
    private AmazonEC2 ec2;

    @Autowired
    private SnapshotService snapshotService;

    @Autowired
    private BackupService backupService;

    @Autowired
    private RetentionService retentionService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired WorkerConfigurationRepository configurationRepository;

    @Value("${sungardas.worker.configuration}")
    private String configurationId;

    @Autowired
    private AmazonDynamoDB amazonDynamoDB;

    WorkerConfiguration configuration;
    @PostConstruct
    private void init() {
        configuration = configurationRepository.findOne(configurationId);
        dynamoDB = new DynamoDB(db);
    }

    @Override
    public String getConfigurationId() {
        return configurationId;
    }

    @Override
    public void dropConfiguration(String currentUserEmail, String instanceId) {
        if (!userService.isAdmin(currentUserEmail)) {
            throw new OperationNotAllowedException("Only admin can delete service");
        }
        if (!instanceId.equals(EC2MetadataUtils.getInstanceId())) {
            throw new OperationNotAllowedException("Provided instance ID is incorrect");
        }

//        dropS3Bucket();
//        dropQueue();
//        dropDbData();
    }

    private void dropS3Bucket() {
        //s3.deleteBucket("");

    }

    private void terminateInstance(){
        ec2.terminateInstances(new TerminateInstancesRequest().withInstanceIds(configurationId));
    }

    private void dropQueue() {
        String queueURL= configuration.getTaskQueueURL();
        sqs.deleteQueue(queueURL);
        sqs.shutdown();
    }

    private void dropDbData() {
        userService.deleteAllUsers();
        taskService.deleteAllTasks();
        retentionService.deleteAllRetentions();
        backupService.deleteAllBackups();
        snapshotService.deleteAllSnapshots();

        boolean dropTables = userService.isTableEmpty()&&taskService.isTableEmpty()&&retentionService.isTableEmpty()&&
                backupService.isTableEmpty()&&snapshotService.isTableEmpty();

        if(dropTables) {
            for(String tableToDrop: tables) {
                dropTable(tableToDrop);
            }
        }

    }

    private void dropTable(String tableName) {
       Table tableToDelete =  dynamoDB.getTable(tableName);
        tableToDelete.delete();
        try {
            tableToDelete.waitForDelete();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
