package com.sungardas.enhancedsnapshots.service.impl;

import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.model.AmazonSNSException;
import com.amazonaws.services.sns.model.GetTopicAttributesRequest;
import com.sungardas.enhancedsnapshots.aws.dynamodb.model.NotificationConfigurationEntry;
import com.sungardas.enhancedsnapshots.aws.dynamodb.model.TaskEntry;
import com.sungardas.enhancedsnapshots.aws.dynamodb.repository.NotificationConfigurationRepository;
import com.sungardas.enhancedsnapshots.dto.Dto;
import com.sungardas.enhancedsnapshots.dto.ExceptionDto;
import com.sungardas.enhancedsnapshots.dto.TaskProgressDto;
import com.sungardas.enhancedsnapshots.exception.SnsNotificationException;
import com.sungardas.enhancedsnapshots.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

import static com.sungardas.enhancedsnapshots.aws.dynamodb.model.TaskEntry.TaskEntryStatus.RUNNING;

@Service
public class NotificationServiceImpl implements NotificationService {

    public static final String TASK_PROGRESS_DESTINATION = "/task";
    public static final String ERROR_DESTINATION = "/error";

    @Autowired
    private SimpMessagingTemplate template;

    @Autowired
    private NotificationConfigurationRepository notificationConfigurationRepository;

    @Autowired
    private AmazonSNS amazonSNS;

    @PostConstruct
    private void init() {
        if (notificationConfigurationRepository.count() == 0) {
            notificationConfigurationRepository.save(new NotificationConfigurationEntry());
        }
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
}
