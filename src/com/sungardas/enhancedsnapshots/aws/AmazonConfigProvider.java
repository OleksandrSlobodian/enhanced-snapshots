package com.sungardas.enhancedsnapshots.aws;

import com.amazonaws.auth.InstanceProfileCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.autoscaling.AmazonAutoScaling;
import com.amazonaws.services.autoscaling.AmazonAutoScalingClient;
import com.amazonaws.services.cloudformation.AmazonCloudFormation;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancing;
import com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancingClient;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.sungardas.enhancedsnapshots.aws.dynamodb.DynamoDBOperationsBridge;
import com.sungardas.enhancedsnapshots.components.RetryInterceptor;
import com.sungardas.enhancedsnapshots.util.SystemUtils;
import org.socialsignin.spring.data.dynamodb.core.DynamoDBOperations;
import org.socialsignin.spring.data.dynamodb.repository.config.EnableDynamoDBRepositories;
import org.springframework.aop.framework.ProxyFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("prod")
@EnableDynamoDBRepositories(
        basePackages = "com.sungardas.enhancedsnapshots.aws.dynamodb.repository",
        dynamoDBOperationsRef = "dynamoDBOperations"
)
public class AmazonConfigProvider {
    private InstanceProfileCredentialsProvider credentialsProvider;

    @Bean(name = "retryInterceptor")
    public RetryInterceptor retryInterceptor() {
        return new RetryInterceptor();
    }

    @Bean
    public InstanceProfileCredentialsProvider amazonCredentialsProvider() {
        if (credentialsProvider == null) {
            credentialsProvider = new InstanceProfileCredentialsProvider(true);
        }
        return credentialsProvider;
    }

    @Bean(name = "amazonDynamoDB")
    public ProxyFactoryBean amazonDynamoDbProxy() {
        ProxyFactoryBean proxyFactoryBean = new ProxyFactoryBean();

        proxyFactoryBean.setTarget(amazonDynamoDB());
        proxyFactoryBean.setInterceptorNames("retryInterceptor");
        return proxyFactoryBean;
    }

    @Bean
    public ProxyFactoryBean amazonEC2Proxy() {
        ProxyFactoryBean proxyFactoryBean = new ProxyFactoryBean();

        proxyFactoryBean.setTarget(amazonEC2());
        proxyFactoryBean.setInterceptorNames("retryInterceptor");

        return proxyFactoryBean;
    }

    @Bean
    public ProxyFactoryBean amazonS3Proxy() {
        ProxyFactoryBean proxyFactoryBean = new ProxyFactoryBean();

        proxyFactoryBean.setTarget(amazonS3());
        proxyFactoryBean.setInterceptorNames("retryInterceptor");

        return proxyFactoryBean;
    }

    @Bean
    public DynamoDBMapperConfig dynamoDBMapperConfig() {
        DynamoDBMapperConfig.Builder builder = new DynamoDBMapperConfig.Builder();
        builder.withTableNameOverride(DynamoDBMapperConfig.TableNameOverride.
                withTableNamePrefix(getDynamoDbPrefix()));
        return builder.build();
    }

    @Bean (name = "amazonDynamoDbMapper")
    public ProxyFactoryBean amazonDynamoDbMapperProxy() {
        ProxyFactoryBean proxyFactoryBean = new ProxyFactoryBean();

        proxyFactoryBean.setTarget(dynamoDBMapper());
        proxyFactoryBean.setInterceptorNames("retryInterceptor");

        return proxyFactoryBean;
    }

    @Bean(name = "dynamoDB")
    public AmazonDynamoDB amazonDynamoDB() {
        AmazonDynamoDB amazonDynamoDB = new AmazonDynamoDBClient(amazonCredentialsProvider());
        amazonDynamoDB.setRegion(getRegion());
        return amazonDynamoDB;
    }

    @Bean(name = "dbMapperWithoutProxy")
    public DynamoDBMapper dynamoDBMapper() {
        return new DynamoDBMapper(amazonDynamoDB(), dynamoDBMapperConfig());
    }

    @Bean
    public ProxyFactoryBean amazonAutoScalingProxy() {
        ProxyFactoryBean proxyFactoryBean = new ProxyFactoryBean();
        proxyFactoryBean.setTarget(autoScalingClient());
        proxyFactoryBean.setInterceptorNames("retryInterceptor");
        return proxyFactoryBean;
    }

    @Bean
    public ProxyFactoryBean amazonELBProxy() {
        ProxyFactoryBean proxyFactoryBean = new ProxyFactoryBean();
        proxyFactoryBean.setTarget(elasticLoadBalancingClient());
        proxyFactoryBean.setInterceptorNames("retryInterceptor");
        return proxyFactoryBean;
    }

    @Bean
    public ProxyFactoryBean amazonCloudWatch() {
        ProxyFactoryBean proxyFactoryBean = new ProxyFactoryBean();
        proxyFactoryBean.setTarget(cloudWatchClient());
        proxyFactoryBean.setInterceptorNames("retryInterceptor");
        return proxyFactoryBean;
    }

    @Bean
    public ProxyFactoryBean amazonSNS() {
        ProxyFactoryBean proxyFactoryBean = new ProxyFactoryBean();
        proxyFactoryBean.setTarget(amazonSNSClient());
        proxyFactoryBean.setInterceptorNames("retryInterceptor");
        return proxyFactoryBean;
    }

    @Bean
    public ProxyFactoryBean amazonSQS() {
        ProxyFactoryBean proxyFactoryBean = new ProxyFactoryBean();
        proxyFactoryBean.setTarget(amazonSQSClient());
        proxyFactoryBean.setInterceptorNames("retryInterceptor");
        return proxyFactoryBean;
    }

    @Bean
    public ProxyFactoryBean amazonCloudFormation() {
        ProxyFactoryBean proxyFactoryBean = new ProxyFactoryBean();
        proxyFactoryBean.setTarget(amazonCloudFormationClient());
        proxyFactoryBean.setInterceptorNames("retryInterceptor");
        return proxyFactoryBean;
    }

    protected AmazonEC2 amazonEC2() {
        AmazonEC2 amazonEC2 = new AmazonEC2Client(amazonCredentialsProvider());
        amazonEC2.setRegion(getRegion());
        return amazonEC2;
    }

    protected AmazonS3 amazonS3() {
        AmazonS3 amazonS3 = new AmazonS3Client(amazonCredentialsProvider());
        Region current = getRegion();
        if (!current.equals(Region.getRegion(Regions.US_EAST_1))) {
            amazonS3.setRegion(current);
        }
        return amazonS3;
    }

    protected AmazonSNS amazonSNSClient() {
        AmazonSNSClient snsClient = new AmazonSNSClient(amazonCredentialsProvider());
        snsClient.setRegion(getRegion());
        return snsClient;
    }

    protected AmazonSQS amazonSQSClient() {
        AmazonSQSClient sqsClient = new AmazonSQSClient(amazonCredentialsProvider());
        sqsClient.setRegion(getRegion());
        return sqsClient;
    }

    protected AmazonAutoScaling autoScalingClient() {
        AmazonAutoScalingClient autoScalingClient = new AmazonAutoScalingClient(amazonCredentialsProvider());
        autoScalingClient.setRegion(getRegion());
        return autoScalingClient;
    }

    protected AmazonElasticLoadBalancing elasticLoadBalancingClient() {
        AmazonElasticLoadBalancingClient elasticLoadBalancingClient = new AmazonElasticLoadBalancingClient(amazonCredentialsProvider());
        elasticLoadBalancingClient.setRegion(getRegion());
        return elasticLoadBalancingClient;
    }

    protected AmazonCloudFormation amazonCloudFormationClient() {
        AmazonCloudFormation amazonCloudFormation = new AmazonCloudFormationClient(amazonCredentialsProvider());
        amazonCloudFormation.setRegion(getRegion());
        return amazonCloudFormation;
    }

    protected AmazonCloudWatch cloudWatchClient() {
        AmazonCloudWatchClient cloudWatchClient = new AmazonCloudWatchClient(amazonCredentialsProvider());
        cloudWatchClient.setRegion(getRegion());
        return cloudWatchClient;
    }

    @Bean(name = "dynamoDBOperations")
    public DynamoDBOperations dynamoDBOperations() {
        return new DynamoDBOperationsBridge(amazonDynamoDB(), dynamoDBMapperConfig());
    }

    public static String getDynamoDbPrefix() {
        return getDynamoDbPrefix(SystemUtils.getSystemId());
    }

    public static String getDynamoDbPrefix(String systemId) {
        return "ENHANCEDSNAPSHOTS_" + systemId + "_";
    }

    protected Region getRegion (){
        return Regions.getCurrentRegion();
    }
}