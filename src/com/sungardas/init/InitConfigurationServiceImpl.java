package com.sungardas.init;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.InstanceProfileCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.model.*;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.internal.BucketNameUtils;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.sungardas.enhancedsnapshots.aws.AmazonConfigProvider;
import com.sungardas.enhancedsnapshots.aws.dynamodb.model.*;
import com.sungardas.enhancedsnapshots.dto.InitConfigurationDto;
import com.sungardas.enhancedsnapshots.dto.MailConfigurationDto;
import com.sungardas.enhancedsnapshots.dto.UserDto;
import com.sungardas.enhancedsnapshots.dto.converter.BucketNameValidationDTO;
import com.sungardas.enhancedsnapshots.dto.converter.MailConfigurationDocumentConverter;
import com.sungardas.enhancedsnapshots.dto.converter.UserDtoConverter;
import com.sungardas.enhancedsnapshots.exception.ConfigurationException;
import com.sungardas.enhancedsnapshots.exception.DataAccessException;
import com.sungardas.enhancedsnapshots.service.CryptoService;
import com.sungardas.enhancedsnapshots.service.SDFSStateService;
import com.sungardas.enhancedsnapshots.util.EnhancedSnapshotSystemMetadataUtil;
import com.sungardas.enhancedsnapshots.util.SystemUtils;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.PropertiesConfigurationLayout;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ResourceLoader;
import org.springframework.scheduling.support.CronSequenceGenerator;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.annotation.PostConstruct;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.amazonaws.services.dynamodbv2.model.ComparisonOperator.EQ;

@Service
class InitConfigurationServiceImpl implements InitConfigurationService {

    private static final Logger LOG = LogManager.getLogger(InitConfigurationServiceImpl.class);

    private static final String catalinaHomeEnvPropName = "catalina.home";
    private static final String confFolderName = "conf";
    private static final String propFileName = "EnhancedSnapshots.properties";
    private static final String DEFAULT_LOGIN = "admin@enhancedsnapshots";

    private static final String CANT_GET_ACCESS_DYNAMODB = "Can't get access to DynamoDB. Check policy list used for AWS user";
    private static final String CANT_GET_ACCESS_S3 = "Can't get access to S3. Check policy list used for AWS user";

    // properties to store in config file
    private static final String WORKER_POLLING_RATE = "enhancedsnapshots.polling.rate";
    private static final String RETENTION_CRON_SCHEDULE = "enhancedsnapshots.retention.cron";
    private static final String WAIT_TIME_BEFORE_NEXT_CHECK_IN_SECONDS = "enhancedsnapshots.wait.time.before.new.sync";
    private static final String MAX_WAIT_TIME_VOLUME_TO_DETACH_IN_SECONDS = "enhancedsnapshots.max.wait.time.to.detach.volume";

    // properties from $catalina.home/conf/EnhancedSnapshots.properties config file
    @Value("${enhancedsnapshots.retention.cron: -1}")
    private String retentionCronExpression;
    @Value("${enhancedsnapshots.polling.rate: -1}")
    private String pollingRate;
    @Value("${enhancedsnapshots.wait.time.before.new.sync: -1}")
    private String waitTimeBeforeNewSync;
    @Value("${enhancedsnapshots.max.wait.time.to.detach.volume: -1}")
    private String maxWaitTimeToDetachVolume;

    @Value("${enhancedsnapshots.sdfs.min.size}")
    private String minVolumeSize;

    // default application properties
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
    @Value("${enhancedsnapshots.default.sdfs.size}")
    private int defaultVolumeSize;
    @Value("${enhancedsnapshots.db.tables}")
    private String[] tables;
    @Value("${enhancedsnapshots.default.sdfs.mount.point}")
    private String mountPoint;
    @Value("${enhancedsnapshots.default.sdfs.volume.name}")
    private String volumeName;

    @Value("${enhancedsnapshots.bucket.name.prefix.001}")
    private String enhancedSnapshotBucketPrefix001;
    @Value("${enhancedsnapshots.bucket.name.prefix.002}")
    private String enhancedSnapshotBucketPrefix002;

    @Value("${enhancedsnapshots.awscli.conf.path}")
    private String awscliConfPath;
    @Value("${enhancedsnapshots.awslogs.conf.path}")
    private String awslogsConfPath;

    @Value("${enhancedsnapshots.nginx.cert.path}")
    private String nginxCertPath;
    @Value("${enhancedsnapshots.nginx.key.path}")
    private String nginxKeyPath;
    @Value("${enhancedsnapshots.db.read.capacity}")
    private Long dbReadCapacity;
    @Value("${enhancedsnapshots.db.write.capacity}")
    private Long dbWriteCapacity;

    @Value("${enhancedsnapshots.system.reserved.memory}")
    private int systemReservedRam;
    @Value("${enhancedsnapshots.sdfs.volume.size.per.1gb.ram}")
    private int volumeSizePerGbOfRam;
    @Value("${enhancedsnapshots.sdfs.reserved.ram}")
    private int sdfsReservedRam;
    @Value("${enhancedsnapshots.system.reserved.storage}")
    private int systemReservedStorage;

    @Value("${enhancedsnapshots.saml.sp.cert.pem}")
    private String samlCertPem;
    @Value("${enhancedsnapshots.saml.sp.cert.jks}")
    private String samlCertJks;
    @Value("${enhancedsnapshots.saml.idp.metadata}")
    private String samlIdpMetadata;
    @Value("${enhancedsnapshots.cert.convert.script.path}")
    private String pemToJksScript;
    @Value("${enhancedsnapshots.saml.sp.cert.alias}")
    private String samlCertAlias;
    @Value("${enhancedsnapshots.system.task.history.tts}")
    private int taskHistoryTTS;
    @Value("${enhancedsnapshots.default.snapshot.store}")
    private boolean storeSnapshot;

    @Value("${CUSTOM_BUCKET_NAME:}")
    private String customBucketName;

    @Value("${enhancedsnapshots.logs.buffer.size}")
    private int bufferSize;
    @Value("${enhancedsnapshots.logs.file}")
    private String logFile;
    private boolean firstTimeInitialization;

    private static final String CUSTOM_BUCKET_NAME_DEFAULT_VALUE = "enhancedsnapshots";

    @Autowired
    private AmazonS3 amazonS3;
    @Autowired
    private ResourceLoader resourceLoader;
    @Autowired
    private ContextManager contextManager;
    @Autowired
    private SystemRestoreService systemRestoreService;

    @Autowired
    private CryptoService cryptoService;

    private AWSCredentialsProvider credentialsProvider;
    @Autowired
    protected AmazonDynamoDB amazonDynamoDB;
    protected DynamoDBMapper mapper;
    private Region region;

    private UserDto userDto;
    private String adminPassword;

    private List<String> tablesWithPrefix;
    private String dbPrefix;

    protected static final String UUID = java.util.UUID.randomUUID().toString();

    @Value("${enhancedsnapshots.s3.ia.enabled}")
    private boolean s3RulesEnabled;

    @PostConstruct
    protected void init() {
        configureAWSLogAgent();
        credentialsProvider = new InstanceProfileCredentialsProvider();
        region = Regions.getCurrentRegion();
        amazonDynamoDB.setRegion(region);
        dbPrefix = AmazonConfigProvider.getDynamoDbPrefix();
        DynamoDBMapperConfig config = new DynamoDBMapperConfig.Builder().withTableNameOverride(DynamoDBMapperConfig.TableNameOverride.
                withTableNamePrefix(dbPrefix)).build();
        mapper = new DynamoDBMapper(amazonDynamoDB, config);
        tablesWithPrefix = Arrays.stream(tables).map(s -> dbPrefix.concat(s)).collect(Collectors.toList());
        if (customBucketName != null) {
            customBucketName = customBucketName.toLowerCase();
        }
        if ((customBucketName.isEmpty() || CUSTOM_BUCKET_NAME_DEFAULT_VALUE.equals(customBucketName)) && !validateBucketName(customBucketName).isValid()) {
            customBucketName = null;
        }
    }

    private void setUser(User user) {
        if (user != null) {
            userDto = UserDtoConverter.convert(user);
            userDto.setRole("admin");
            adminPassword = user.getPassword();
        }
    }

    protected Configuration getConfiguration(){
        return mapper.load(Configuration.class, SystemUtils.getSystemId());
    }

    @Override
    public void configureSystem(ConfigDto config) {
        if (systemIsConfigured()){
            LOG.info("System is already configured");
            if (getPropertyFile().exists()) {
                syncSettingsInDbAndConfigFile();
            } else {
                storePropertiesEditableFromConfigFile();
            }
            Configuration conf = getConfiguration();
            refreshContext(conf.isSsoLoginMode(), conf.getEntityId());
            return;
        }
        if (EnhancedSnapshotSystemMetadataUtil.containsSdfsMetadata(config.getBucketName(), sdfsStateBackupFileName, amazonS3)) {
            LOG.info("Restoring system  from bucket {}", config.getBucketName());
            createDbStructure();
            systemRestoreService.restore(config.getBucketName());
            Configuration conf = mapper.load(Configuration.class, SystemUtils.getSystemId());
            storePropertiesEditableFromConfigFile();
            if (conf.isSsoLoginMode() && conf.isClusterMode()) {
                downloadFromS3(conf.getS3Bucket(), samlCertPem, System.getProperty(catalinaHomeEnvPropName));
                downloadFromS3(conf.getS3Bucket(), samlIdpMetadata, System.getProperty(catalinaHomeEnvPropName));
            }
            if (SystemUtils.clusterMode() && !conf.isClusterMode()) {
                conf.setClusterMode(true);
                conf.setMinNodeNumber(config.getCluster().getMinNodeNumber());
                conf.setMaxNodeNumber(config.getCluster().getMaxNodeNumber());
                mapper.save(conf);
            }

            refreshContext(conf.isSsoLoginMode(), conf.getEntityId());
            LOG.info("System is successfully restored.");
            return;
        }

        if (config.getMailConfiguration() != null) {
            checkMailConfiguration(config.getMailConfiguration());
        }

        LOG.info("Configuring system");
        if (!requiredTablesExist() && config.getUser() == null) {
            LOG.warn("No user info was provided");
            throw new ConfigurationException("Please create default user");
        }

        if (config.getUser() != null) {
            setUser(config.getUser());
        }
        validateVolumeSize(config.getVolumeSize());
        if (config.isSsoMode()) {
            configureSSO(config.getSpEntityId());
        }
        try {
            // we need to ensure before context refresh that provided bucket name is valid
            createBucket(config.getBucketName());
        } catch (IllegalArgumentException e) {
            LOG.warn("Failed to create bucket {}", config.getBucketName());
            throw new IllegalArgumentException("Failed to create bucket");
        }
        storePropertiesEditableFromConfigFile();
        createDBAndStoreSettings(config);
        if (config.isSsoMode()) {
            //upload certificate and metadata to bucket
            uploadToS3(config.getBucketName(), Paths.get(System.getProperty(catalinaHomeEnvPropName), samlIdpMetadata));
            uploadToS3(config.getBucketName(), Paths.get(System.getProperty(catalinaHomeEnvPropName), samlCertPem));
        }

        refreshContext(config.isSsoMode(), config.getSpEntityId());
        LOG.info("System is successfully configured");
    }

    @Override
    public void checkMailConfiguration(MailConfigurationDto configuration) {
        try {
            InetAddress.getByName(configuration.getMailSMTPHost()).isReachable(1000);
        } catch (Exception e) {
            LOG.error(e);
            throw new ConfigurationException("Mail server is not reachable");
        }
    }


    private void refreshContext(boolean ssoMode, String entityId){
        try {
            contextManager.refreshContext(ssoMode, entityId);
        } catch (Exception e) {
            LOG.warn("Failed to refresh context: {}", e);
            // we do not need remove property file in case it's not a first time configuration
            if (firstTimeInitialization) {
                removeProperties();
            }
            contextManager.refreshInitContext();
            throw e;
        }
    }

    /**
     *  Stores properties which can not be modified from UI to config file,
     *  changes in config file will be applied after system restart
     */
    public void storePropertiesEditableFromConfigFile() {
        storePropertiesEditableFromConfigFile(defaultRetentionCronExpression, defaultPollingRate, defaultWaitTimeBeforeNewSyncWithAWS, defaultMaxWaitTimeToDetachVolume);
    }

    protected void configureSSO(String spEntityID) {
        LOG.info("Configuring SSO");
        // check whether all necessary files exists
        if(!Files.exists(Paths.get(System.getProperty("catalina.home"), samlCertPem))){
            throw new ConfigurationException("Certificate for service provider is not uploaded");
        }
        if(!Files.exists(Paths.get(System.getProperty("catalina.home"), samlIdpMetadata))){
            throw new ConfigurationException("Metadata for identity provider is not uploaded");
        }
        try {
            // convert pem to jks
            File jksCert = Paths.get(System.getProperty(catalinaHomeEnvPropName), samlCertJks).toFile();
            // delete previous jks if exists
            if (jksCert.exists()) {
                jksCert.delete();
            }
            File convertScript = resourceLoader.getResource(pemToJksScript).getFile();
            convertScript.setExecutable(true);
            String[] parameters = new String[]{convertScript.getAbsolutePath(), samlCertAlias, spEntityID,
                    Paths.get(System.getProperty(catalinaHomeEnvPropName), samlCertPem).toString(),
                    Paths.get(System.getProperty(catalinaHomeEnvPropName), samlCertJks).toString()};
            ProcessBuilder builder = new ProcessBuilder(parameters);
            LOG.info("Executing convert script: {}", Arrays.toString(parameters));
            Process process = builder.start();
            process.waitFor();
            switch (process.exitValue()) {
                case 0:
                    LOG.info("Saml cert successfully converted to jks format");
                    break;
                default: {
                    LOG.error("Failed to convert saml cert to jks format");
                    try (BufferedReader input = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                        StringBuilder errorMessage = new StringBuilder();
                        for (String line; (line = input.readLine()) != null; ) {
                            errorMessage.append(line);
                        }
                        throw new ConfigurationException(errorMessage.toString());
                    }
                }
            }
        } catch (Exception e) {
            LOG.error("Failed to convert saml cert to jks format", e);
            throw new ConfigurationException(e.toString());
        }
    }

    /**
     *  Stores properties which can not be modified from UI to config file,
     *  changes in config file will be applied after system restart
     */
    private void storePropertiesEditableFromConfigFile(String retentionCronExpression, int pollingRate, int waitTimeBeforeNewSync,
                                                       int maxWaitTimeToDetachVolume) {
        File file = Paths.get(System.getProperty(catalinaHomeEnvPropName), confFolderName, propFileName).toFile();
        try {
            PropertiesConfiguration propertiesConfiguration = new PropertiesConfiguration();
            PropertiesConfigurationLayout layout = propertiesConfiguration.getLayout();
            layout.setLineSeparator("\n");
            String headerComment = "EnhancedSnapshots properties. Changes in this config file will be applied after application restart.";

            layout.setHeaderComment(headerComment);

            layout.setBlancLinesBefore(RETENTION_CRON_SCHEDULE, 1);
            layout.setComment(RETENTION_CRON_SCHEDULE, "Cron schedule for retention policy");
            propertiesConfiguration.setProperty(RETENTION_CRON_SCHEDULE, retentionCronExpression);

            layout.setBlancLinesBefore(WORKER_POLLING_RATE, 1);
            layout.setComment(WORKER_POLLING_RATE, "Polling rate to check whether there is some new task, in ms");
            propertiesConfiguration.setProperty(WORKER_POLLING_RATE, Integer.toString(pollingRate));

            layout.setBlancLinesBefore(WAIT_TIME_BEFORE_NEXT_CHECK_IN_SECONDS, 1);
            layout.setComment(WAIT_TIME_BEFORE_NEXT_CHECK_IN_SECONDS, "Wait time before new sync of Snapshot/Volume with AWS data, in seconds");
            propertiesConfiguration.setProperty(WAIT_TIME_BEFORE_NEXT_CHECK_IN_SECONDS, Integer.toString(waitTimeBeforeNewSync));

            layout.setBlancLinesBefore(MAX_WAIT_TIME_VOLUME_TO_DETACH_IN_SECONDS, 1);
            layout.setComment(MAX_WAIT_TIME_VOLUME_TO_DETACH_IN_SECONDS, "Max wait time for volume to be detached, in seconds");
            propertiesConfiguration.setProperty(MAX_WAIT_TIME_VOLUME_TO_DETACH_IN_SECONDS, Integer.toString(maxWaitTimeToDetachVolume));

            propertiesConfiguration.write(new FileWriter(file));
            LOG.debug("Config file {} stored successfully.", file.getPath());
        } catch (Exception ioException) {
            LOG.error("Can not create property file", ioException);
            throw new ConfigurationException("Can not create property file. Check path or permission: "
                    + file.getAbsolutePath(), ioException);
        }
    }

    /**
     * Create DB structure if required
     * Store admin user if required
     * Store configuration if required
     * @param config
     */
    protected void createDBAndStoreSettings(final ConfigDto config) {
        // create tables if they do not exist
        createDbStructure();
        storeAdminUserIfProvided();
        // store configuration if it does not exist
        storeConfiguration(config);
    }

    protected void createDbStructure() throws ConfigurationException {
        createTable(BackupEntry.class);
        createTable(Configuration.class);
        createTable(RetentionEntry.class);
        createTable(TaskEntry.class);
        createTable(SnapshotEntry.class);
        createTable(User.class);
        createTable(NodeEntry.class);
        createTable(EventEntry.class);
        createTable(NotificationConfigurationEntry.class);
        createTable(SnsRuleEntry.class);
    }

    private void createTable(Class tableClass) {
        CreateTableRequest createTableRequest = mapper.generateCreateTableRequest(tableClass);

        createTableRequest.setProvisionedThroughput(new ProvisionedThroughput(dbReadCapacity, dbWriteCapacity));
        if (tableExists(createTableRequest.getTableName())) {
            LOG.info("Table {} already exists", createTableRequest.getTableName());
            return;
        }
        try {
            DynamoDB dynamoDB = new DynamoDB(amazonDynamoDB);
            Table table = dynamoDB.createTable(createTableRequest);
            LOG.info("Creating table {} ... ", createTableRequest.getTableName());
            table.waitForActive();
            LOG.info("Table {} was created successfully.", createTableRequest.getTableName());
        } catch (Exception e) {
            LOG.error("Failed to create table {}. ", createTableRequest.getTableName());
            LOG.error(e);
            throw new ConfigurationException("Failed to create table" + createTableRequest.getTableName(), e);
        }
    }

    private void storeAdminUserIfProvided() {
        if (userDto != null) {
            userDto.setEmail(userDto.getEmail().toLowerCase());
            User userToCreate = UserDtoConverter.convert(userDto);
            userToCreate.setRole("admin");
            if (adminPassword != null) {
                userToCreate.setPassword(DigestUtils.sha512Hex(adminPassword));
            }
            mapper.save(userToCreate);
        }
    }

    private void storeConfiguration(final ConfigDto config) {
        Configuration configuration = new Configuration();
        configuration.setConfigurationId(SystemUtils.getSystemId());
        configuration.setEc2Region(Regions.getCurrentRegion().getName());
        configuration.setSdfsMountPoint(mountPoint);
        configuration.setSdfsVolumeName(volumeName);
        configuration.setS3Bucket(config.getBucketName());
        configuration.setSdfsSize(config.getVolumeSize());
        configuration.setSsoLoginMode(config.isSsoMode());
        configuration.setEntityId(config.getSpEntityId());
        configuration.setMailConfigurationDocument(MailConfigurationDocumentConverter.toMailConfigurationDocument(config.getMailConfiguration(), cryptoService, configuration.getConfigurationId(), null));
        configuration.setDomain(config.getDomain());
        // set default properties
        configuration.setRestoreVolumeIopsPerGb(restoreVolumeIopsPerGb);
        configuration.setRestoreVolumeType(restoreVolumeType);
        configuration.setTempVolumeIopsPerGb(tempVolumeIopsPerGb);
        configuration.setTempVolumeType(tempVolumeType);
        configuration.setAmazonRetryCount(amazonRetryCount);
        configuration.setAmazonRetrySleep(amazonRetrySleep);
        configuration.setMaxQueueSize(queueSize);
        configuration.setSdfsConfigPath(sdfsConfigPath);
        configuration.setSdfsLocalCacheSize(sdfsLocalCacheSize);
        configuration.setSdfsBackupFileName(sdfsStateBackupFileName);
        configuration.setRetentionCronExpression(defaultRetentionCronExpression);
        configuration.setWorkerDispatcherPollingRate(defaultPollingRate);
        configuration.setWaitTimeBeforeNewSyncWithAWS(defaultWaitTimeBeforeNewSyncWithAWS);
        configuration.setMaxWaitTimeToDetachVolume(defaultMaxWaitTimeToDetachVolume);
        configuration.setNginxCertPath(nginxCertPath);
        configuration.setNginxKeyPath(nginxKeyPath);
        configuration.setTaskHistoryTTS(taskHistoryTTS);
        configuration.setStoreSnapshot(storeSnapshot);
        configuration.setLogFile(logFile);
        configuration.setLogsBufferSize(bufferSize);
        configuration.setIaEnabled(s3RulesEnabled);
        if (SystemUtils.clusterMode()) {
            configuration.setClusterMode(SystemUtils.clusterMode());
            configuration.setMaxNodeNumber(config.getCluster().getMaxNodeNumber());
            configuration.setMinNodeNumber(config.getCluster().getMinNodeNumber());
            configuration.setChunkStoreEncryptionKey(SDFSStateService.generateChunkStoreEncryptionKey());
            configuration.setChunkStoreIV(SDFSStateService.generateChunkStoreIV());
            configuration.setSdfsCliPsw(SystemUtils.getSystemId());
        }
        if (System.getenv("UUID") != null && !System.getenv("UUID").isEmpty()) {
            configuration.setUUID(System.getenv("UUID"));
        } else {
            configuration.setUUID(UUID);
        }
        configuration.setSungardasSSO(config.isSungardasSSO());
        // saving configuration to DB
        mapper.save(configuration);
    }

    /**
     * Store properties changed in config file to DB
     */
    public void syncSettingsInDbAndConfigFile() {
        // sync properties in DB with conf file
        boolean configurationChanged = false;
        Configuration loadedConf = mapper.load(Configuration.class, SystemUtils.getSystemId());

        if (cronExpressionIsValid(retentionCronExpression) && !retentionCronExpression.equals(loadedConf.getRetentionCronExpression())) {
            LOG.debug("Applying new cron expression {} for Retention policy.", retentionCronExpression);
            loadedConf.setRetentionCronExpression(retentionCronExpression);
            configurationChanged = true;
        }
        if (correctPropertyProvided(pollingRate, WORKER_POLLING_RATE) && Integer.parseInt(pollingRate) != loadedConf.getWorkerDispatcherPollingRate()) {
            LOG.debug("Applying new polling rate to pick up new task {} ms.", pollingRate);
            loadedConf.setWorkerDispatcherPollingRate(Integer.parseInt(pollingRate));
            configurationChanged = true;
        }
        if (correctPropertyProvided(waitTimeBeforeNewSync, WAIT_TIME_BEFORE_NEXT_CHECK_IN_SECONDS)
                && Integer.parseInt(waitTimeBeforeNewSync) != loadedConf.getWaitTimeBeforeNewSyncWithAWS()) {
            LOG.debug("Applying new wait time before new sync of Snapshot/Volume with AWS data {} seconds.", waitTimeBeforeNewSync);
            loadedConf.setWaitTimeBeforeNewSyncWithAWS(Integer.parseInt(waitTimeBeforeNewSync));
            configurationChanged = true;
        }
        if (correctPropertyProvided(maxWaitTimeToDetachVolume, MAX_WAIT_TIME_VOLUME_TO_DETACH_IN_SECONDS)
                && Integer.parseInt(maxWaitTimeToDetachVolume) != loadedConf.getMaxWaitTimeToDetachVolume()) {
            LOG.debug("Applying new max wait time for volume to be detach {} seconds.", maxWaitTimeToDetachVolume);
            loadedConf.setMaxWaitTimeToDetachVolume(Integer.parseInt(maxWaitTimeToDetachVolume));
            configurationChanged = true;
        }
        if (configurationChanged) {
            LOG.debug("Storing updated settings from config file to DB.");
            mapper.save(loadedConf);
        }
        // this is required to ensure that properties with invalid values in config file will be replaced with correct values
        removeProperties();
        storePropertiesEditableFromConfigFile(loadedConf.getRetentionCronExpression(), loadedConf.getWorkerDispatcherPollingRate(),
                loadedConf.getWaitTimeBeforeNewSyncWithAWS(), loadedConf.getMaxWaitTimeToDetachVolume());
    }

    /**
     * Remove config file with properties
     */
    protected void removeProperties() {
        File file = Paths.get(System.getProperty(catalinaHomeEnvPropName), confFolderName, propFileName).toFile();
        if (file.exists()) {
            file.delete();
        }
    }

    @Override
    public boolean systemIsConfigured() {
        if (!SystemUtils.clusterMode()) {
            return getPropertyFile().exists();
        } else {
            return tableExists(mapper.generateCreateTableRequest(NodeEntry.class).getTableName());
        }
    }

    @Override
    public boolean checkDefaultUser(String login, String password) {
        return DEFAULT_LOGIN.equals(login.toLowerCase()) && password.equals(SystemUtils.getSystemId());
    }

    protected List<InitConfigurationDto.S3> getBucketsWithSdfsMetadata() {
        ArrayList<InitConfigurationDto.S3> result = new ArrayList<>();

        try {
            List<Bucket> allBuckets = amazonS3.listBuckets();
            if (customBucketName != null) {
                result.add(new InitConfigurationDto.S3(customBucketName, false));
            }
            String bucketName = enhancedSnapshotBucketPrefix002 + SystemUtils.getSystemId();
            result.add(new InitConfigurationDto.S3(bucketName, false));

            String currentLocation = region.toString();
            if (currentLocation.equalsIgnoreCase(Regions.US_EAST_1.getName())) {
                currentLocation = "US";
            }
            for (Bucket bucket : allBuckets) {
                try {
                    if (bucket.getName().startsWith(enhancedSnapshotBucketPrefix001) || bucket.getName().startsWith(enhancedSnapshotBucketPrefix002)) {
                        String location = amazonS3.getBucketLocation(bucket.getName());

                        // Because client.getBucketLocation(bucket.getName()) returns US if bucket is in us-east-1
                        if (!location.equalsIgnoreCase(currentLocation) && !location.equalsIgnoreCase("US")) {
                            continue;
                        }

                        ListObjectsRequest request = new ListObjectsRequest()
                                .withBucketName(bucket.getName()).withPrefix(FilenameUtils.removeExtension(sdfsStateBackupFileName));
                        if (amazonS3.listObjects(request).getObjectSummaries().size() > 0) {
                            if (bucketName.equals(bucket.getName())) {
                                result.get(0).setCreated(true);
                            } else {
                                result.add(new InitConfigurationDto.S3(bucket.getName(), true));
                            }
                        }
                    }
                } catch (Exception ignored) {
                    // If any exception appears during working with bucket,
                    // just skip this bucket and try to scan the next one
                }
            }
        } catch (AmazonS3Exception e) {
            LOG.warn("Can't get access to S3");
            throw new DataAccessException(CANT_GET_ACCESS_S3, e);
        }
	return result;
    }

    @Override
    public InitConfigurationDto getInitConfigurationDto() {
        InitConfigurationDto initConfigurationDto = new InitConfigurationDto();
        initConfigurationDto.setDb(new InitConfigurationDto.DB());
        boolean isDbValid = requiredTablesExist();
        initConfigurationDto.getDb().setValid(isDbValid);
        if (isDbValid) {
            initConfigurationDto.getDb().setAdminExist(adminExist());
        }
        InitConfigurationDto.SDFS sdfs = new InitConfigurationDto.SDFS();
        sdfs.setMountPoint(mountPoint);
        sdfs.setVolumeName(volumeName);
        int maxVolumeSize = SDFSStateService.getMaxVolumeSize(systemReservedRam, volumeSizePerGbOfRam,  sdfsReservedRam);
        sdfs.setMaxVolumeSize(String.valueOf(maxVolumeSize));
        sdfs.setVolumeSize(String.valueOf(Math.min(maxVolumeSize, defaultVolumeSize)));
        sdfs.setMinVolumeSize(minVolumeSize);
        sdfs.setCreated(sdfsAlreadyExists());
        sdfs.setSdfsLocalCacheSize(sdfsLocalCacheSize);
        sdfs.setMinSdfsLocalCacheSize(1);
        sdfs.setMaxSdfsLocalCacheSize(SDFSStateService.getFreeStorageSpace(systemReservedStorage));

        initConfigurationDto.setS3(getBucketsWithSdfsMetadata());
        initConfigurationDto.setSdfs(sdfs);
        initConfigurationDto.setImmutableBucketNamePrefix(enhancedSnapshotBucketPrefix002);
        initConfigurationDto.setClusterMode(SystemUtils.clusterMode());

        if (System.getenv("UUID") != null && !System.getenv("UUID").isEmpty()) {
            initConfigurationDto.setUUID(System.getenv("UUID"));
        } else {
            initConfigurationDto.setUUID(UUID);
        }
        return initConfigurationDto;
    }

    protected boolean requiredTablesExist() {
        AmazonDynamoDBClient amazonDynamoDB = new AmazonDynamoDBClient(credentialsProvider);
        amazonDynamoDB.setRegion(Regions.getCurrentRegion());
        try {
            ListTablesResult listResult = amazonDynamoDB.listTables();
            List<String> tableNames = listResult.getTableNames();
            boolean present = tableNames.containsAll(tablesWithPrefix);
            LOG.info("List db structure: {}", tableNames.toArray());
            LOG.info("Check db structure is present: {}", present);
            return present;
        } catch (AmazonServiceException e) {
            LOG.warn("Can't get a list of existed tables", e);
            throw new DataAccessException(CANT_GET_ACCESS_DYNAMODB, e);
        }
    }

    private boolean adminExist() {
        DynamoDBScanExpression expression = new DynamoDBScanExpression()
                .withFilterConditionEntry("role",
                        new Condition().withComparisonOperator(EQ.toString()).withAttributeValueList(new AttributeValue("admin")));
        List<User> users = mapper.scan(User.class, expression);
        return !users.isEmpty();
    }

    private boolean sdfsAlreadyExists() {
        LOG.info("sdfs already exists...");
        File configf = new File(sdfsConfigPath);
        File mountPointf = new File(mountPoint);
        return configf.exists() && mountPointf.exists();
    }


    private File getPropertyFile() {
        return Paths.get(System.getProperty(catalinaHomeEnvPropName), confFolderName, propFileName).toFile();
    }

    private void configureAWSLogAgent() {
        try {
            replaceInFile(new File(awscliConfPath), "<region>", Regions.getCurrentRegion().toString());
            replaceInFile(new File(awslogsConfPath), "<instance-id>", SystemUtils.getSystemId());
        } catch (Exception e) {
            LOG.warn("Cant initialize AWS Log agent", e);
        }
    }


    protected void validateVolumeSize(final int volumeSize) {
        int min = Integer.parseInt(minVolumeSize);
        int max = SDFSStateService.getMaxVolumeSize(systemReservedRam, volumeSizePerGbOfRam,  sdfsReservedRam);
        if (volumeSize < min || volumeSize > max) {
            LOG.warn("Invalid volume size: {}", volumeSize);
            throw new ConfigurationException("Invalid volume size");
        }
    }

    private void replaceInFile(File file, String marker, String value) throws IOException {
        String lines[] = FileUtils.readLines(file).toArray(new String[1]);
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].contains(marker)) {
                lines[i] = lines[i].replace(marker, value);
            }
        }
        FileUtils.writeLines(file, Arrays.asList(lines));
    }

    private boolean tableExists(String tableName) {
        ListTablesResult listResult = amazonDynamoDB.listTables();
        List<String> tableNames = listResult.getTableNames();
        return tableNames.contains(tableName);
    }

    // in case user removes some properties from file or provided incorrect int values this check will help to avoid unwanted exceptions
    private boolean correctPropertyProvided(String propertyValue, String propertyName) {
        try {
            int newProperty = Integer.parseInt(propertyValue);
            if (newProperty > 0) {
                return true;
            }
        } catch (Exception igrone) {
        }
        LOG.warn("Incorrect value {} for property {}. Changes will not bve applied.", propertyValue, propertyName);
        return false;
    }

    private boolean cronExpressionIsValid(String cronExpression) {
        try {
            // Unix cron does not contain seconds but spring cron does
            new CronSequenceGenerator("0 " + cronExpression);
            return true;
        } catch (Exception e) {
            LOG.warn("Provided cron expression {} is invalid. Changes will not be applied", cronExpression);
            return false;
        }
    }

    public BucketNameValidationDTO validateBucketName(String bucketName) {
        if (!bucketName.startsWith(enhancedSnapshotBucketPrefix002)) {
            return new BucketNameValidationDTO(false, "Bucket name should start with " + enhancedSnapshotBucketPrefix002);
        }
        if (amazonS3.doesBucketExist(bucketName)) {
            // check whether we own this bucket
            List<Bucket> buckets = amazonS3.listBuckets();
            for (Bucket bucket : buckets) {
                if (bucket.getName().equals(bucketName)) {
                    return new BucketNameValidationDTO(true, "");
                }
            }
            return new BucketNameValidationDTO(false, "The requested bucket name is not available.Please select a different name.");
        }
        try {
            BucketNameUtils.validateBucketName(bucketName);
            return new BucketNameValidationDTO(true, "");
        } catch (IllegalArgumentException e) {
            return new BucketNameValidationDTO(false, e.getMessage());
        }
    }

    @Override
    public void saveAndProcessSAMLFiles(MultipartFile spCertificate, MultipartFile idpMetadata) {
        try {
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document document = builder.parse(idpMetadata.getInputStream());
            document.normalizeDocument();
            saveFileOnServer(samlIdpMetadata, idpMetadata);
            LOG.info("IDP metadata successfully stored");
        } catch (ParserConfigurationException | SAXException | IOException e) {
            LOG.error("IDP metadata processing failed", e);
            throw new ConfigurationException(e);
        }
        try {
            saveFileOnServer(samlCertPem, spCertificate);
            LOG.info("SP certificate successfully stored");
        } catch (IOException e) {
            LOG.error("SP certificate processing failed", e);
            throw new ConfigurationException(e);
        }
    }

    @Override
    public void setFlagOfFirstTimeConfiguration(boolean firstTimeInitialization) {
        this.firstTimeInitialization = firstTimeInitialization;
    }

    protected void createBucket(String bucketName) {
        if (!bucketExists(bucketName)) {
            LOG.info("Creating bucket {} in {} region", bucketName, region);
            // AWS throws exception when trying create bucket in US_EAST_1
            if (region.getName().equalsIgnoreCase(Regions.US_EAST_1.getName())) {
                amazonS3.createBucket(bucketName);
                return;
            }
            amazonS3.createBucket(bucketName, region.getName());
        }
    }

    private void saveFileOnServer(String fileName, MultipartFile fileToSave) throws IOException {
        fileToSave.transferTo(Paths.get(System.getProperty(catalinaHomeEnvPropName), fileName).toFile());
    }

    protected void uploadToS3(String bucketName, Path filePath) {
        File file = filePath.toFile();
        amazonS3.putObject(bucketName, file.getName(), file);
    }

    private void downloadFromS3(String bucket, String fileName, String destinationPath) {
        S3Object object = amazonS3.getObject(bucket, fileName);
        try {
            FileUtils.copyInputStreamToFile(object.getObjectContent(), Paths.get(destinationPath, fileName).toFile());
        } catch (IOException e) {
            LOG.error("Download {} from {} failed", fileName, bucket, e);
            throw new ConfigurationException(e);
        }
    }

    public InitConfigurationDto.DB containsMetadata(final String bucketName) {
        final InitConfigurationDto.DB db = new InitConfigurationDto.DB();
        if (EnhancedSnapshotSystemMetadataUtil.isBucketExits(bucketName, amazonS3)) {
            String version = EnhancedSnapshotSystemMetadataUtil.getBackupVersion(bucketName, sdfsStateBackupFileName, amazonS3);
            if (version == null || "0.0.1".equals(version)) {
                return db;
            } else {
                db.setAdminExist(true);
            }
            return db;
        } else {
            return db;
        }
    }

    // there is bug at US_EAST_1 AWS S3 endpoint, we should not check buckets existence with it
    private boolean bucketExists(String bucketName) {
        boolean result;
        if (region.getName().equalsIgnoreCase(Regions.US_EAST_1.getName())) {
            amazonS3.setRegion(Region.getRegion(Regions.EU_CENTRAL_1));
            result = amazonS3.doesBucketExist(bucketName);
            amazonS3.setRegion(region);
        } else {
            result = amazonS3.doesBucketExist(bucketName);
        }
        return result;
    }
}
