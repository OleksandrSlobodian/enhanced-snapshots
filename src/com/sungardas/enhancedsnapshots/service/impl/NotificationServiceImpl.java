package com.sungardas.enhancedsnapshots.service.impl;

import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.MetricDatum;
import com.amazonaws.services.cloudwatch.model.PutMetricDataRequest;
import com.amazonaws.services.cloudwatch.model.StandardUnit;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.model.AmazonSNSException;
import com.amazonaws.services.sns.model.GetTopicAttributesRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sungardas.enhancedsnapshots.aws.dynamodb.model.NotificationConfigurationEntry;
import com.sungardas.enhancedsnapshots.aws.dynamodb.model.SnsRuleEntry;
import com.sungardas.enhancedsnapshots.aws.dynamodb.model.TaskEntry;
import com.sungardas.enhancedsnapshots.aws.dynamodb.repository.NotificationConfigurationRepository;
import com.sungardas.enhancedsnapshots.aws.dynamodb.repository.SnsRuleRepository;
import com.sungardas.enhancedsnapshots.aws.dynamodb.repository.TaskRepository;
import com.sungardas.enhancedsnapshots.dto.Dto;
import com.sungardas.enhancedsnapshots.dto.ExceptionDto;
import com.sungardas.enhancedsnapshots.dto.TaskProgressDto;
import com.sungardas.enhancedsnapshots.exception.SnsNotificationException;
import com.sungardas.enhancedsnapshots.service.MasterInitialization;
import com.sungardas.enhancedsnapshots.service.NotificationService;
import com.sungardas.enhancedsnapshots.service.SchedulerService;
import com.sungardas.enhancedsnapshots.service.Task;
import com.sungardas.enhancedsnapshots.util.SystemUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.sungardas.enhancedsnapshots.aws.dynamodb.model.TaskEntry.TaskEntryStatus.RUNNING;

@Service
public class NotificationServiceImpl implements NotificationService, MasterInitialization {

    private static final Logger LOG = LogManager.getLogger(NotificationServiceImpl.class);

    public static final String TASK_PROGRESS_DESTINATION = "/task";
    public static final String ERROR_DESTINATION = "/error";

    @Autowired
    private SimpMessagingTemplate template;

    @Autowired
    private NotificationConfigurationRepository notificationConfigurationRepository;

    @Autowired
    private AmazonSNS amazonSNS;

    @Autowired
    private SnsRuleRepository snsRuleRepository;

    @Autowired
    private SchedulerService schedulerService;

    @Autowired
    private AmazonCloudWatch cloudWatch;

    @Autowired
    private TaskRepository taskRepository;

    @Value("${enhancedsnapshots.sns.subject}")
    private String snsSubject;

    @Value("${enhancedsnapshots.default.backup.threadPool.size}")
    private int backupThreadPoolSize;

    @Value("${enhancedsnapshots.default.restore.threadPool.size}")
    private int restoreThreadPoolSize;

    private ExecutorService eventsExecutorService = Executors.newSingleThreadExecutor();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    private void postConstruct() {
        if (notificationConfigurationRepository.count() == 0) {
            notificationConfigurationRepository.save(new NotificationConfigurationEntry());
        }
    }

    @Override
    public void init() {
        schedulerService.addTask(new Task() {
            @Override
            public String getId() {
                return NotificationServiceImpl.class.getName();
            }

            @Override
            public void run() {
                String nameSpace = "ESS_INFO";
                pushMetricData(nameSpace, "BACKUP_QUEUE_SIZE", getQueueSize(TaskEntry.TaskEntryType.BACKUP));
                pushMetricData(nameSpace, "RESTORE_QUEUE_SIZE", getQueueSize(TaskEntry.TaskEntryType.RESTORE));
                pushMetricData(nameSpace, "BACKUP_WORKERS_AVAILABLE", backupThreadPoolSize - taskRepository.countByRegularAndTypeAndStatus(Boolean.FALSE.toString(), TaskEntry.TaskEntryType.BACKUP.getType(), TaskEntry.TaskEntryStatus.RUNNING.getStatus()));
                pushMetricData(nameSpace, "RESTORE_WORKERS_AVAILABLE", restoreThreadPoolSize - taskRepository.countByRegularAndTypeAndStatus(Boolean.FALSE.toString(), TaskEntry.TaskEntryType.RESTORE.getType(), TaskEntry.TaskEntryStatus.RUNNING.getStatus()));
            }
        }, "*/1 * * * *");
    }

    @Override
    public void notifyAboutRunningTaskProgress(String taskId, String message, double progress) {
        notifyAboutTaskProgress(new TaskProgressDto(taskId, message, progress, RUNNING));
    }

    @Override
    public void notifyAboutTaskProgress(String taskId, String message, double progress, TaskEntry.TaskEntryStatus status) {
        notifyAboutTaskProgress(new TaskProgressDto(taskId, message, progress, status));
    }

    @Override
    public void notifyAboutTaskProgress(TaskProgressDto dto) {
        notifyUser(TASK_PROGRESS_DESTINATION, dto);
    }

    @Override
    public void notifyAboutError(ExceptionDto exceptionDto) {
        notifyUser(ERROR_DESTINATION, exceptionDto);
    }

    @Override
    public void notifyUser(String destination, Dto dto) {
        try {
            template.convertAndSend(destination, dto);
        } catch (Throwable t) {

        }
    }

    @Override
    public String getSnsTopic() {
        NotificationConfigurationEntry entry = getNotificationConfiguration();
        return entry.getSnsTopic();
    }

    @Override
    public void setSnsTopic(String snsTopic) {
        if(snsTopic != null && !snsTopic.isEmpty()) {
            checkSnsTopic(snsTopic);
        }
        NotificationConfigurationEntry entry = getNotificationConfiguration();
        entry.setSnsTopic(snsTopic);
        notificationConfigurationRepository.save(entry);
    }

    @Override
    public void createRule(SnsRuleEntry ruleEntry) {
        validateRule(ruleEntry);
        snsRuleRepository.save(ruleEntry);
    }

    @Override
    public List<SnsRuleEntry> getRules() {
        return snsRuleRepository.findAll();
    }

    @Override
    public void updateRule(SnsRuleEntry ruleEntry) {
        validateRule(ruleEntry);
        snsRuleRepository.save(ruleEntry);
    }

    @Override
    public void deleteRule(String snsRuleId) {
        snsRuleRepository.delete(snsRuleId);
    }

    @Override
    public void notifyViaSns(TaskEntry.TaskEntryType operation, TaskEntry.TaskEntryStatus status, String volumeId) {
        eventsExecutorService.execute(() -> sendSnsEvent(operation, status, volumeId));
    }


    private void sendSnsEvent(TaskEntry.TaskEntryType operation, TaskEntry.TaskEntryStatus status, String volumeId) {
        String snsTopic = getNotificationConfiguration().getSnsTopic();
        if(snsTopic != null && !snsTopic.isEmpty()) {
            List<SnsRuleEntry> rules = snsRuleRepository.findByOperationAndStatusAndVolumeId(operation.name(),
                    status.name(),
                    volumeId);

            if (rules.isEmpty()) {
                //no rule for volumeId
                //check for generic rule
                rules = snsRuleRepository.findByOperationAndStatusAndVolumeIdIsNull(operation.name(), status.name());
                if (rules.isEmpty()) {
                    //rules not found
                    return;
                }
            }

            Map<String, String> messageObject = new HashMap<>();
            messageObject.put("type", operation.toString());
            messageObject.put("status", status.toString());
            messageObject.put("volumeId", volumeId);

            try {
                amazonSNS.publish(snsTopic, objectMapper.writeValueAsString(messageObject), snsSubject);
            } catch (JsonProcessingException e) {
                LOG.error(e);
            }
        }
    }

    private void validateRule(SnsRuleEntry ruleEntry) {
        try {
            TaskEntry.TaskEntryType.valueOf(ruleEntry.getOperation());
        } catch (Exception e) {
            throw new SnsNotificationException("Operation field is invalid");
        }
        try {
            TaskEntry.TaskEntryStatus.valueOf(ruleEntry.getStatus());
        } catch (Exception e) {
            throw new SnsNotificationException("Status field is invalid");
        }
    }

    private NotificationConfigurationEntry getNotificationConfiguration() {
        return notificationConfigurationRepository.findOne(NotificationConfigurationEntry.id);
    }

    /**
     * Check is sns topic valid
     * @param snsTopic topicArn
     * @throws com.sungardas.enhancedsnapshots.exception.SnsNotificationException if topicArn invalid
     */
    private void checkSnsTopic(String snsTopic) {
        try {
            amazonSNS.getTopicAttributes(new GetTopicAttributesRequest(snsTopic));
        } catch (AmazonSNSException e) {
            throw new SnsNotificationException(e);
        }
    }

    private void pushMetricData(String nameSpace, String metricName, double value) {
        MetricDatum metricDatum = new MetricDatum();
        metricDatum.setValue(value);
        metricDatum.setUnit(StandardUnit.Count);
        metricDatum.setTimestamp(new Date());
        metricDatum.setMetricName(metricName);
        Dimension dimension = new Dimension().withName("System").withValue(SystemUtils.getSystemId());
        metricDatum.setDimensions(Arrays.asList(dimension));
        cloudWatch.putMetricData(new PutMetricDataRequest()
                .withNamespace(nameSpace).withMetricData(metricDatum));
    }

    private long getQueueSize(TaskEntry.TaskEntryType type) {
        return taskRepository.countByRegularAndTypeAndStatus(Boolean.FALSE.toString(), type.getType(), TaskEntry.TaskEntryStatus.QUEUED.getStatus()) +
                taskRepository.countByRegularAndTypeAndStatus(Boolean.FALSE.toString(), type.getType(), TaskEntry.TaskEntryStatus.WAITING.getStatus());
    }
}
