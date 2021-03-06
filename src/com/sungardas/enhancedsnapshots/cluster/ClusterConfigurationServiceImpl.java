package com.sungardas.enhancedsnapshots.cluster;

import com.amazonaws.services.autoscaling.AmazonAutoScaling;
import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.amazonaws.services.autoscaling.model.DeletePolicyRequest;
import com.amazonaws.services.autoscaling.model.PutScalingPolicyRequest;
import com.amazonaws.services.autoscaling.model.UpdateAutoScalingGroupRequest;
import com.amazonaws.services.cloudformation.AmazonCloudFormation;
import com.amazonaws.services.cloudformation.model.DeleteStackRequest;
import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.model.*;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.model.Topic;
import com.amazonaws.services.sns.util.Topics;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.CreateQueueRequest;
import com.amazonaws.services.sqs.model.DeleteQueueRequest;
import com.sungardas.enhancedsnapshots.aws.dynamodb.model.NodeEntry;
import com.sungardas.enhancedsnapshots.aws.dynamodb.repository.NodeRepository;
import com.sungardas.enhancedsnapshots.components.ConfigurationMediator;
import com.sungardas.enhancedsnapshots.exception.ConfigurationException;
import com.sungardas.enhancedsnapshots.service.SDFSStateService;
import com.sungardas.enhancedsnapshots.util.SystemUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service("ClusterConfigurationService")
@DependsOn({"ConfigurationMediator", "CreateAppConfiguration"})
public class ClusterConfigurationServiceImpl implements ClusterConfigurationService {

    private static final Logger LOG = LogManager.getLogger(ClusterConfigurationServiceImpl.class);
    private static final String SCALE_UP_POLICY = "ESS-ScaleUpPolicy-" + SystemUtils.getSystemId();
    private static final String SCALE_DOWN_POLICY = "ESS-ScaleDownPolicy-" + SystemUtils.getSystemId();
    private static final String METRIC_DATA_NAME = "ESS-Load-Metric-" + SystemUtils.getSystemId();
    private static final String ESS_OVERLOAD_ALARM = "ESS-Overload-Alarm-" + SystemUtils.getSystemId();
    private static final String ESS_IDLE_ALARM = "ESS-Idle-Alarm-" + SystemUtils.getSystemId();
    private static final String ESS_TOPIC_NAME = "ESS-" + SystemUtils.getSystemId() + "-topic";
    private static final String ESS_QUEUE_NAME = "ESS-" + SystemUtils.getSystemId() + "-queue";
    private static final String ESS_LB_NAME = SystemUtils.getSystemId() + "-lb";

    @Autowired
    private AmazonSNS amazonSNS;
    @Autowired
    private AmazonSQS amazonSQS;
    @Autowired
    private AmazonCloudWatch cloudWatch;
    @Autowired
    private AmazonAutoScaling autoScaling;
    @Autowired
    private NodeRepository nodeRepository;
    @Autowired
    private ConfigurationMediator configurationMediator;
    @Autowired(required = false)
    private ClusterEventPublisher clusterEventPublisher;
    @Autowired(required = false)
    private SDFSStateService sdfsStateService;
    @Autowired
    private AmazonCloudFormation cloudFormation;

    @Value("${enhancedsnapshots.default.backup.threadPool.size}")
    private int backupThreadPoolSize;
    @Value("${enhancedsnapshots.default.restore.threadPool.size}")
    private int restoreThreadPoolSize;
    private AutoScalingGroup autoScalingGroup;


    @PostConstruct
    private void init() {
        if (configurationMediator.isClusterMode()) {
            if (!clusterIsConfigured()) {
                configureClusterInfrastructure();
                nodeRepository.save(getMasterNodeInfo());
            } else if (nodeRepository.findOne(SystemUtils.getInstanceId()) == null) {
                joinCluster();
            } else if (nodeRepository.findByMaster(true).isEmpty()) {
                NodeEntry node = nodeRepository.findOne(SystemUtils.getInstanceId());
                if (node == null) {
                    node = getMasterNodeInfo();
                }
                nodeRepository.save(node);
            }
        }
    }

    private void joinCluster() {
        String instanceId = SystemUtils.getInstanceId();
        if (nodeRepository.exists(instanceId)) {
            LOG.warn("Instance {} already present in cluster", instanceId);
        } else {
            LOG.info("Joining cluster {}", instanceId);
            NodeEntry newNode = new NodeEntry(instanceId, false,
                    restoreThreadPoolSize, backupThreadPoolSize, sdfsStateService.getSDFSVolumeId());
            nodeRepository.save(newNode);
            clusterEventPublisher.nodeLaunched(newNode.getNodeId(), sdfsStateService.getSDFSVolumeId(), null);
        }
    }

    protected NodeEntry getMasterNodeInfo() {
        return new NodeEntry(SystemUtils.getInstanceId(), true,
                restoreThreadPoolSize, backupThreadPoolSize, sdfsStateService.getSDFSVolumeId());
    }

    public void configureClusterInfrastructure() {
        LOG.info("Configuration of cluster infrastructure started");

        // update AutoScalingGroup with min and max node number
        autoScaling.updateAutoScalingGroup(new UpdateAutoScalingGroupRequest()
                .withAutoScalingGroupName(getAutoScalingGroup().getAutoScalingGroupName())
                .withMaxSize(configurationMediator.getMaxNodeNumberInCluster())
                .withMinSize(configurationMediator.getMinNodeNumberInCluster())
                .withDesiredCapacity(configurationMediator.getMinNodeNumberInCluster()));
        LOG.info("AutoScalingGroup {} updated: {}", autoScalingGroup.getAutoScalingGroupName(), autoScalingGroup.toString());

        // we create this infrustructure from JAVA since currently we can not get arn when we create policy from CFT
        //create AutoScaling Policies
        String scaleUpPolicyARN = autoScaling.putScalingPolicy(new PutScalingPolicyRequest().withAutoScalingGroupName(getAutoScalingGroup().getAutoScalingGroupName())
                .withPolicyName(SCALE_UP_POLICY)
                .withAutoScalingGroupName(getAutoScalingGroup().getAutoScalingGroupName())
                //Increase or decrease the current capacity of the group by the specified number of instances.
                .withAdjustmentType("ChangeInCapacity")
                .withPolicyType("SimpleScaling")
                .withScalingAdjustment(1)).getPolicyARN();
        LOG.info("Scale up policy created: {}", SCALE_UP_POLICY);

        String scaleDownPolicyARN = autoScaling.putScalingPolicy(new PutScalingPolicyRequest().withAutoScalingGroupName(getAutoScalingGroup().getAutoScalingGroupName())
                .withPolicyName(SCALE_DOWN_POLICY)
                .withAutoScalingGroupName(getAutoScalingGroup().getAutoScalingGroupName())
                //Increase or decrease the current capacity of the group by the specified number of instances.
                .withAdjustmentType("ChangeInCapacity")
                .withPolicyType("SimpleScaling")
                .withScalingAdjustment(-1)).getPolicyARN();
        LOG.info("Scale down policy created: {}", SCALE_DOWN_POLICY);

        // create custom metric
        MetricDatum metricDatum = new MetricDatum();
        metricDatum.setValue(0.0);
        metricDatum.setUnit(StandardUnit.Count);
        metricDatum.setTimestamp(new Date());
        metricDatum.setMetricName(METRIC_DATA_NAME);
        cloudWatch.putMetricData(new PutMetricDataRequest()
                .withNamespace("ESS/Tasks").withMetricData(metricDatum));
        LOG.info("Custom metric added: {}", metricDatum.toString());

        // create custom alarm
        cloudWatch.putMetricAlarm(new PutMetricAlarmRequest()
                .withAlarmName(ESS_OVERLOAD_ALARM)
                .withMetricName(METRIC_DATA_NAME)
                .withComparisonOperator(ComparisonOperator.GreaterThanOrEqualToThreshold)
                .withThreshold(80.0)
                .withPeriod(300)
                .withEvaluationPeriods(2)
                .withStatistic(Statistic.Average)
                .withNamespace("ESS/Tasks")
                .withAlarmActions(scaleUpPolicyARN));
        LOG.info("Load alarm added: ", cloudWatch.describeAlarms().getMetricAlarms()
                .stream().filter(alarm -> alarm.getAlarmName().equals(ESS_OVERLOAD_ALARM)).findFirst().get().toString());

        // create custom alarm
        cloudWatch.putMetricAlarm(new PutMetricAlarmRequest()
                .withAlarmName(ESS_IDLE_ALARM)
                .withMetricName(METRIC_DATA_NAME)
                .withComparisonOperator(ComparisonOperator.LessThanThreshold)
                .withThreshold(40.0)
                .withPeriod(300)
                .withEvaluationPeriods(2)
                .withStatistic(Statistic.Average)
                .withNamespace("ESS/Tasks")
                .withAlarmActions(scaleDownPolicyARN));
        LOG.info("Alarm for idle resources added: ", cloudWatch.describeAlarms().getMetricAlarms()
                .stream().filter(alarm -> alarm.getAlarmName().equals(ESS_IDLE_ALARM)).findFirst().get().toString());

        // subscribe to topic
        amazonSQS.createQueue(new CreateQueueRequest().withQueueName(ESS_QUEUE_NAME));
        Topics.subscribeQueue(amazonSNS, amazonSQS, getEssTopicArn(), amazonSQS.getQueueUrl(ESS_QUEUE_NAME).getQueueUrl());

        LOG.info("Cluster infrastructure successfully configured.");
    }

    @Override
    public void updateCloudWatchMetric() {
        MetricDatum metricDatum = new MetricDatum();
        metricDatum.setValue(getSystemLoadLevel() * 100);
        metricDatum.setUnit(StandardUnit.Count);
        metricDatum.setTimestamp(new Date());
        metricDatum.setMetricName(METRIC_DATA_NAME);
        cloudWatch.putMetricData(new PutMetricDataRequest()
                .withNamespace("ESS/Tasks").withMetricData(metricDatum));
        LOG.info("Custom metric added: {}", metricDatum.toString());
    }

    @Override
    public void updateAutoScalingSettings(int minNodeNumber, int maxNodeNumber) {
        autoScaling.updateAutoScalingGroup(new UpdateAutoScalingGroupRequest()
                .withAutoScalingGroupName(getAutoScalingGroup().getAutoScalingGroupName())
                .withMaxSize(maxNodeNumber)
                .withMinSize(minNodeNumber)
                .withDesiredCapacity(minNodeNumber));

    }

    /**
     * Returns ARN of topic where AutoScaling publishes system events
     *
     * @return
     */
    private String getEssTopicArn() {
        Topic ess_topic = amazonSNS.listTopics().getTopics()
                .stream().filter(topic -> topic.getTopicArn().endsWith(ESS_TOPIC_NAME)).findFirst()
                .orElseThrow(() -> new ConfigurationException("Topic " + ESS_TOPIC_NAME + " does not exist."));
        return ess_topic.getTopicArn();
    }

    /**
     * Returns % of system load level
     *
     * @return
     */
    private double getSystemLoadLevel() {
        List<NodeEntry> nodes = nodeRepository.findAll();
        int freeBackupWorkers = 0;
        int freeRestoreWorkers = 0;
        for (NodeEntry n : nodes) {
            freeBackupWorkers += n.getFreeBackupWorkers();
            freeRestoreWorkers += n.getFreeRestoreWorkers();
        }
        int backupWorkers = backupThreadPoolSize * nodes.size();
        int restoreWorkers = restoreThreadPoolSize * nodes.size();
        double backupLoadLevel = (double) (backupWorkers - freeBackupWorkers) / backupWorkers;
        double restoreLoadLevel = (double) (restoreWorkers - freeRestoreWorkers) / restoreWorkers;

        return Math.max(backupLoadLevel, restoreLoadLevel);
    }

    private AutoScalingGroup getAutoScalingGroup() {
        //there is no possibility to set custom name for AutoScalingGroup from CFT
        //that's why we have to determine created group name in code base on stack name
        //CloudFormation service uses next schema for AutoScalingGroup name
        //$CUSTOM_STACK_NAME-AutoScalingGroup-<some random string>

        Optional<AutoScalingGroup> asg = null;
        if (autoScalingGroup == null) {
            asg = autoScaling.describeAutoScalingGroups().getAutoScalingGroups().stream()
                    .filter(autoScalingGroup -> autoScalingGroup.getAutoScalingGroupName()
                            .startsWith(SystemUtils.getCloudFormationStackName() + "-AutoScalingGroup-")).findFirst();
            autoScalingGroup = asg.orElseThrow(() -> new ConfigurationException("No appropriate AutoScalingGroup was found"));
        }
        return autoScalingGroup;
    }

    // we assume that system is configured if configuration exists
    private boolean clusterIsConfigured() {
        return (nodeRepository.count() != 0) ? true : false;
    }

    @Override
    public void removeClusterInfrastructure() {
        autoScaling.deletePolicy(new DeletePolicyRequest().withAutoScalingGroupName(getAutoScalingGroup().getAutoScalingGroupName()).withPolicyName(SCALE_UP_POLICY));
        autoScaling.deletePolicy(new DeletePolicyRequest().withAutoScalingGroupName(getAutoScalingGroup().getAutoScalingGroupName()).withPolicyName(SCALE_DOWN_POLICY));
        cloudWatch.deleteAlarms(new DeleteAlarmsRequest().withAlarmNames(ESS_OVERLOAD_ALARM, ESS_IDLE_ALARM));
        // CloudWatch metrics are stored for two weeks. Old data will be removed automatically.

        amazonSQS.deleteQueue(new DeleteQueueRequest().withQueueUrl(ESS_QUEUE_NAME));
        cloudFormation.deleteStack(new DeleteStackRequest().withStackName(SystemUtils.getCloudFormationStackName()));
    }
}
