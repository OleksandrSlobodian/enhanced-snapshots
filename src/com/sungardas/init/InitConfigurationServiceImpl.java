package com.sungardas.init;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import javax.annotation.PostConstruct;
import javax.validation.constraints.NotNull;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.InstanceProfileCredentialsProvider;
import com.amazonaws.internal.StaticCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.Condition;
import com.amazonaws.services.dynamodbv2.model.ListTablesResult;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.util.EC2MetadataUtils;
import com.sun.management.UnixOperatingSystemMXBean;
import com.sungardas.enhancedsnapshots.aws.dynamodb.model.User;
import com.sungardas.enhancedsnapshots.dto.InitConfigurationDto;
import com.sungardas.enhancedsnapshots.exception.ConfigurationException;
import com.sungardas.enhancedsnapshots.exception.DataAccessException;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import static com.amazonaws.services.dynamodbv2.model.ComparisonOperator.EQ;

@Service
class InitConfigurationServiceImpl implements InitConfigurationService {

    private static final String NOT_ENOUGH_MEMORY_ERROR = "Current instance doesn't  provide enough memory to start SDFS. At least 3.75GB  of total memory expected.";
    private static final String CANT_GET_ACCESS_DYNAMODB = "Can't get access to DynamoDB. Check policy list used for AWS user";
    private static final String CANT_GET_ACCESS_S3 = "Can't get access to S3. Check policy list used for AWS user";
    private static final String AMAZON_S3_BUCKET = "amazon.s3.bucket";
    private static final String AMAZON_SDFS_SIZE = "amazon.sdfs.size";
    private static final String AMAZON_AWS_REGION = "amazon.aws.region";
    private static final String SUNGARGAS_WORKER_CONFIGURATION = "sungardas.worker.configuration";
    private static final Logger LOG = LogManager.getLogger(InitConfigurationServiceImpl.class);
    private static final long BYTES_IN_GB = 1_073_741_824;
    private static final String ENHANCED_SNAPSHOT_BUCKET_PREFIX = "com.sungardas.enhancedsnapshots.";
    private static final long SYSTEM_RESERVED_RAM_IN_BYTES = BYTES_IN_GB / 4;
    private static final long SDFS_RESERVED_RAM_IN_BYTES = BYTES_IN_GB;
    private static final int SDFS_VOLUME_SIZE_IN_GB_PER_GB_OF_RAM = 2000;
    private final String catalinaHomeEnvPropName = "catalina.home";
    private final String confFolderName = "conf";
    private final String propFileName = "amazon.properties";
    private final String DEFAULT_LOGIN = "admin@enhancedsnapshots";
    private AWSCredentialsProvider credentialsProvider;
    private String instanceId;
    private Region region;

    @Value("${enhancedsnapshots.sdfs.default.size}")
    private String defaultVolumeSize;

    @Value("${enhancedsnapshots.sdfs.min.size}")
    private String minVolumeSize;

    @Value("${enhancedsnapshots.db.tables}")
    private String[] tables;

    @Value("${amazon.s3.default.region}")
    private String defaultS3Region;

    private InitConfigurationDto initConfigurationDto = null;

    @PostConstruct
    private void init() {
        credentialsProvider = new InstanceProfileCredentialsProvider();
        instanceId = EC2MetadataUtils.getInstanceId();
        region = Regions.getCurrentRegion();
    }


    @Override
    public void setCredentialsIfValid(@NotNull CredentialsDto credentials) {
        validateCredentials(credentials.getAwsPublicKey(), credentials.getAwsSecretKey());
        credentialsProvider = new StaticCredentialsProvider(new BasicAWSCredentials(credentials.getAwsPublicKey(), credentials.getAwsSecretKey()));
    }

    @Override
    public void storeProperties() {
        validateCredentials(credentialsProvider.getCredentials().getAWSAccessKeyId(), credentialsProvider.getCredentials().getAWSSecretKey());
        Properties properties = new Properties();
        File file = Paths.get(System.getProperty(catalinaHomeEnvPropName), confFolderName, propFileName).toFile();
        try {
            properties.setProperty(AMAZON_AWS_REGION, region.getName());
            properties.setProperty(SUNGARGAS_WORKER_CONFIGURATION, instanceId);
            properties.setProperty(AMAZON_S3_BUCKET, initConfigurationDto.getS3().get(0).getBucketName());
            properties.setProperty(AMAZON_SDFS_SIZE, initConfigurationDto.getSdfs().getVolumeSize());
            properties.store(new FileOutputStream(file), "AWS Credentials");
        } catch (IOException ioException) {
            LOG.error("Can not create amazon.properties file", ioException);
            throw new ConfigurationException("Can not create amazon.properties file\n" +
                    "Check path or permission: " + file.getAbsolutePath(), ioException);
        }
    }

    @Override
    public void removeProperties() {
        File file = Paths.get(System.getProperty(catalinaHomeEnvPropName), confFolderName, propFileName).toFile();
        if (file.exists()) {
            file.delete();
        }
    }

    @Override
    public boolean areCredentialsValid() {
        AmazonEC2Client ec2Client = new AmazonEC2Client(credentialsProvider);
        ec2Client.setRegion(region);
        try {
            ec2Client.describeRegions();
            return true;
        } catch (AmazonClientException e) {
            LOG.warn("Provided AWS credentials are invalid.");
            return false;
        }
    }

    @Override
    public boolean credentialsAreProvided() {
        if (credentialsProvider.getCredentials() != null) {
            validateCredentials(credentialsProvider.getCredentials().getAWSAccessKeyId(), credentialsProvider.getCredentials().getAWSSecretKey());
            return true;
        } else {
            return false;
        }

    }

    @Override
    public boolean isAwsPropertyFileExists() {
        return getPropertyFile().exists();
    }

    @Override
    public boolean checkDefaultUser(String login, String password) {
        return DEFAULT_LOGIN.equals(login.toLowerCase()) && password.equals(instanceId);
    }

    private List<InitConfigurationDto.S3> getBucketsWithSdfsMetadata() {
        ArrayList<InitConfigurationDto.S3> result = new ArrayList<>();

        try {
            AmazonS3Client client = new AmazonS3Client(credentialsProvider);
            List<Bucket> allBuckets = client.listBuckets();
            String bucketName = ENHANCED_SNAPSHOT_BUCKET_PREFIX + instanceId;
            result.add(new InitConfigurationDto.S3(bucketName, false));

            String currentLocation = region.toString();
            if (currentLocation.equalsIgnoreCase(Regions.US_EAST_1.getName())) {
                currentLocation = "US";
            }
            for (Bucket bucket : allBuckets) {
                try {
                    if (bucket.getName().startsWith(ENHANCED_SNAPSHOT_BUCKET_PREFIX)) {
                        String location = client.getBucketLocation(bucket.getName());

                        // Because client.getBucketLocation(bucket.getName()) returns US if bucket is in us-east-1
                        if (!location.equalsIgnoreCase(currentLocation) && !location.equalsIgnoreCase("US")) {
                            continue;
                        }

                        ListObjectsRequest request = new ListObjectsRequest()
                                .withBucketName(bucket.getName()).withPrefix("sdfsstate");
                        if (client.listObjects(request).getObjectSummaries().size() > 0) {
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
        initConfigurationDto = new InitConfigurationDto();
        initConfigurationDto.setDb(new InitConfigurationDto.DB());
        boolean isDbValid = requiredTablesExist();
        initConfigurationDto.getDb().setValid(isDbValid);
        if (isDbValid) {
            initConfigurationDto.getDb().setAdminExist(adminExist());
        }

        String volumeName = "awspool";
        String mountPoint = "/mnt/awspool/";
        InitConfigurationDto.SDFS sdfs = new InitConfigurationDto.SDFS();
        sdfs.setMountPoint(mountPoint);
        sdfs.setVolumeName(volumeName);
        int maxVolumeSize = getMaxVolumeSize();
        sdfs.setMaxVolumeSize(String.valueOf(maxVolumeSize));
        sdfs.setVolumeSize(String.valueOf(Math.min(maxVolumeSize, Integer.parseInt(defaultVolumeSize))));
        sdfs.setMinVolumeSize(minVolumeSize);
        sdfs.setCreated(sdfsAlreadyExists(volumeName, mountPoint));

        initConfigurationDto.setS3(getBucketsWithSdfsMetadata());
        initConfigurationDto.setSdfs(sdfs);
        return initConfigurationDto;
    }

    private boolean requiredTablesExist() {
        AmazonDynamoDBClient amazonDynamoDB = new AmazonDynamoDBClient(credentialsProvider);
        amazonDynamoDB.setRegion(Regions.getCurrentRegion());
        try {
            ListTablesResult listResult = amazonDynamoDB.listTables();
            List<String> tableNames = listResult.getTableNames();
            LOG.info("List db structure: {}", tableNames.toArray());
            LOG.info("Check db structure is present: {}", tableNames.containsAll(Arrays.asList(tables)));
            return tableNames.containsAll(Arrays.asList(tables));
        } catch (AmazonServiceException e) {
            LOG.warn("Can't get a list of existed tables", e);
            throw new DataAccessException(CANT_GET_ACCESS_DYNAMODB, e);
        }
    }

    private boolean adminExist() {
        AmazonDynamoDBClient client = new AmazonDynamoDBClient(credentialsProvider);
        client.setRegion(Regions.getCurrentRegion());
        DynamoDBMapper mapper = new DynamoDBMapper(client);
        DynamoDBScanExpression expression = new DynamoDBScanExpression()
                .withFilterConditionEntry("role",
                        new Condition().withComparisonOperator(EQ.toString()).withAttributeValueList(new AttributeValue("admin")))
                .withFilterConditionEntry("instanceId",
                        new Condition().withComparisonOperator(EQ.toString()).withAttributeValueList(new AttributeValue(instanceId)));
        List<User> users = mapper.scan(User.class, expression);

        return !users.isEmpty();
    }


    private boolean sdfsAlreadyExists(String volumeName, String mountPoint) {
        LOG.info("sdfsAlreadyExists...");
        String volumeConfigPath = "/etc/sdfs/" + volumeName + "-volume-cfg.xml";
        File configf = new File(volumeConfigPath);
        File mountPointf = new File(mountPoint);
        return configf.exists() && mountPointf.exists();
    }

    private void validateCredentials(String accessKey, String secretKey) {
        if (accessKey == null || accessKey.isEmpty()) {
            throw new ConfigurationException("Empty AWS AccessKey");
        }
        if (secretKey == null || secretKey.isEmpty()) {
            throw new ConfigurationException("Empty AWS SecretKey");
        }
    }

    private File getPropertyFile() {
        return Paths.get(System.getProperty(catalinaHomeEnvPropName), confFolderName, propFileName).toFile();
    }

    @Override
    public String getInstanceId() {
        return instanceId;
    }

    @Override
    public void configureAWSLogAgent() {
        try {
            replaceInFile(new File("/etc/awslogs/awscli.conf"), "<region>", region.toString());
            replaceInFile(new File("/etc/awslogs/awslogs.conf"), "<instance-id>", instanceId);
        } catch (Exception e) {
            LOG.warn("Cant initialize AWS Log agent");
        }
    }

    @Override
    public void validateVolumeSize(final String volumeSize) {
        int size = Integer.parseInt(volumeSize);
        int min = Integer.parseInt(minVolumeSize);
        int max = getMaxVolumeSize();
        if (size < min || size > max) {
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

    public int getMaxVolumeSize() {
        UnixOperatingSystemMXBean osBean = (UnixOperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        //Total RAM - RAM available for Tomcat - reserved
        long totalRAM = osBean.getTotalPhysicalMemorySize() - Runtime.getRuntime().maxMemory() - SYSTEM_RESERVED_RAM_IN_BYTES - SDFS_RESERVED_RAM_IN_BYTES;
        int maxVolumeSize = (int) (totalRAM / BYTES_IN_GB) * SDFS_VOLUME_SIZE_IN_GB_PER_GB_OF_RAM;
        return maxVolumeSize;
    }
}
