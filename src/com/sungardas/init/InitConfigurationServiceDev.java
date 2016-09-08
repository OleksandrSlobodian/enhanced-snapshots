package com.sungardas.init;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.internal.StaticCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig;
import com.amazonaws.services.s3.AmazonS3Client;
import com.sungardas.enhancedsnapshots.aws.AmazonConfigProviderDEV;
import com.sungardas.enhancedsnapshots.aws.dynamodb.model.Configuration;
import com.sungardas.enhancedsnapshots.aws.dynamodb.model.User;
import com.sungardas.enhancedsnapshots.dto.InitConfigurationDto;
import com.sungardas.enhancedsnapshots.dto.converter.BucketNameValidationDTO;
import com.sungardas.enhancedsnapshots.exception.ConfigurationException;
import com.sungardas.enhancedsnapshots.service.impl.CryptoServiceImpl;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

class InitConfigurationServiceDev extends InitConfigurationServiceImpl {

    @Value("${enhancedsnapshots.bucket.name.prefix.002}")
    private String enhancedSnapshotBucketPrefix;
    @Value("${amazon.aws.accesskey}")
    private String amazonAWSAccessKey;
    @Value("${amazon.aws.secretkey}")
    private String amazonAWSSecretKey;
    @Value("${sungardas.worker.configuration}")
    private String instanceId;
    @Value("${amazon.aws.region}")
    private String region;
    @Value("${enhancedsnapshots.db.tables}")
    private String[] tables;
    @Value("${enhancedsnapshots.default.tempVolumeType}")
    private String tempVolumeType;
    @Value("${enhancedsnapshots.default.tempVolumeIopsPerGb}")
    private int tempVolumeIopsPerGb;
    @Value("${enhancedsnapshots.default.restoreVolumeType}")
    private String restoreVolumeType;
    @Value("${enhancedsnapshots.default.restoreVolumeIopsPerGb}")
    private int restoreVolumeIopsPerGb;
    @Value("${enhancedsnapshots.default.amazon.retry.count}")
    private int amazonRetryCount;
    @Value("${enhancedsnapshots.default.amazon.retry.sleep}")
    private int amazonRetrySleep;
    @Value("${enhancedsnapshots.default.queue.size}")
    private int queueSize;
    @Value("${enhancedsnapshots.default.sdfs.volume.config.path}")
    private String sdfsConfigPath;
    @Value("${enhancedsnapshots.default.sdfs.backup.file.name}")
    private String sdfsStateBackupFileName;
    @Value("${enhancedsnapshots.default.retention.cron}")
    private String defaultRetentionCronExpression;
    @Value("${enhancedsnapshots.default.polling.rate}")
    private int defaultPollingRate;
    @Value("${enhancedsnapshots.default.sdfs.local.cache.size}")
    private int sdfsLocalCacheSize;
    @Value("${enhancedsnapshots.default.wait.time.before.new.sync}")
    private int defaultWaitTimeBeforeNewSyncWithAWS;
    @Value("${enhancedsnapshots.default.max.wait.time.to.detach.volume}")
    private int defaultMaxWaitTimeToDetachVolume;

    @Value("${enhancedsnapshots.dev.isSystemConfigured:false}")
    private boolean isSystemConfigured;

    private AWSCredentialsProvider credentialsProvider;
    private AmazonS3Client amazonS3;

    public void removeProperties() {
    }

    protected List<InitConfigurationDto.S3> getBucketsWithSdfsMetadata() {
        ArrayList<InitConfigurationDto.S3> result = new ArrayList<>();
        return result;
    }

    @PostConstruct
    protected void init() {
        String accessKey = new CryptoServiceImpl().decrypt(instanceId, amazonAWSAccessKey);
        String secretKey = new CryptoServiceImpl().decrypt(instanceId, amazonAWSSecretKey);
        credentialsProvider = new StaticCredentialsProvider(new BasicAWSCredentials(accessKey, secretKey));
        amazonS3 = new AmazonS3Client(credentialsProvider);
        DynamoDBMapperConfig config = new DynamoDBMapperConfig.Builder().withTableNameOverride(DynamoDBMapperConfig.TableNameOverride.
                withTableNamePrefix(AmazonConfigProviderDEV.getDynamoDbPrefix("DEV"))).build();
        mapper = new DynamoDBMapper(amazonDynamoDB, config);
    }

    @Override
    public InitConfigurationDto getInitConfigurationDto() {
        InitConfigurationDto config = new InitConfigurationDto();
        List<InitConfigurationDto.S3> names = new ArrayList<>();
        names.add(new InitConfigurationDto.S3(enhancedSnapshotBucketPrefix + "s0", false));
        names.add(new InitConfigurationDto.S3(enhancedSnapshotBucketPrefix + "s1", true));
        names.add(new InitConfigurationDto.S3(enhancedSnapshotBucketPrefix + "s2", true));

        InitConfigurationDto.SDFS sdfs = new InitConfigurationDto.SDFS();
        sdfs.setCreated(true);
        sdfs.setMountPoint("/mnt/awspool");
        sdfs.setVolumeName("awspool");
        sdfs.setVolumeSize("40");
        sdfs.setMinVolumeSize("10");
        sdfs.setMaxVolumeSize("2000");
        sdfs.setSdfsLocalCacheSize(1);
        sdfs.setMinSdfsLocalCacheSize(0);
        sdfs.setMaxSdfsLocalCacheSize(3);

        InitConfigurationDto.DB db = new InitConfigurationDto.DB();
        db.setValid(true);
        db.setAdminExist(true);

        config.setS3(names);
        config.setSdfs(sdfs);
        config.setDb(db);
        config.setImmutableBucketNamePrefix(enhancedSnapshotBucketPrefix);

        return config;
    }

    @Override
    public boolean systemIsConfigured() {
        return isSystemConfigured;
    }

    protected Configuration getConfiguration(){
        return mapper.load(Configuration.class, "DEV");
    }

    @Override
    public boolean checkDefaultUser(String login, String password) {
        return true;
    }

    protected boolean requiredTablesExist(){
        return true;
    }

    protected void validateVolumeSize(final int volumeSize) {
        int min = 10;
        int max = 2000;
        if (volumeSize < min || volumeSize > max) {
            throw new ConfigurationException("Invalid volume size");
        }
    }

    @Override
    public void storePropertiesEditableFromConfigFile() {
    }

    @Override
    public void configureSSO(String spEntityID) {

    }

    @Override
    public void createDBAndStoreSettings(final ConfigDto config) {
        createDbStructure();
        storeSettings();
    }

    private void storeSettings() {
        Configuration configuration = getDevConf();
        mapper.save(configuration);

        User user = new User("admin@admin", DigestUtils.sha512Hex("admin"), "admin", "dev", "dev");
        user.setId("DEV");
        mapper.save(user);
    }

    @Override
    protected void createBucket(String bucketName) {
    }

    private Configuration getDevConf() {
        Configuration configuration = new Configuration();
        configuration.setConfigurationId("DEV");
        configuration.setEc2Region(Regions.EU_WEST_1.getName());
        configuration.setSdfsMountPoint("");
        configuration.setSdfsVolumeName("");
        configuration.setRestoreVolumeIopsPerGb(restoreVolumeIopsPerGb);
        configuration.setRestoreVolumeType(restoreVolumeType);
        configuration.setTempVolumeIopsPerGb(tempVolumeIopsPerGb);
        configuration.setTempVolumeType(tempVolumeType);
        configuration.setSdfsLocalCacheSize(sdfsLocalCacheSize);
        configuration.setAmazonRetryCount(amazonRetryCount);
        configuration.setAmazonRetrySleep(amazonRetrySleep);
        configuration.setMaxQueueSize(queueSize);
        configuration.setSdfsConfigPath(sdfsConfigPath);
        configuration.setSdfsBackupFileName(sdfsStateBackupFileName);
        configuration.setRetentionCronExpression(defaultRetentionCronExpression);
        configuration.setWorkerDispatcherPollingRate(defaultPollingRate);
        configuration.setWaitTimeBeforeNewSyncWithAWS(defaultWaitTimeBeforeNewSyncWithAWS);
        configuration.setMaxWaitTimeToDetachVolume(defaultMaxWaitTimeToDetachVolume);
        configuration.setS3Bucket("com.sungardas.enhancedsnapshots.dev");
        configuration.setSdfsSize(500);
        configuration.setSdfsVolumeName("awspool");
        configuration.setSdfsMountPoint("/mnt/awspool");
        configuration.setSsoLoginMode(true);

        return configuration;
    }



    public void syncSettingsInDbAndConfigFile() {
    }

    public BucketNameValidationDTO validateBucketName(String bucketName) {
        return new BucketNameValidationDTO(true, "");
    }

    public void saveAndProcessSAMLFiles(MultipartFile spCertificate, MultipartFile idpMetadata) {

    }

    @Override
    public InitConfigurationDto.DB containsMetadata(final String bucketName) {
        InitConfigurationDto.DB db = new InitConfigurationDto.DB();
        db.setValid(true);
        db.setAdminExist(true);
        return db;
    }
}
