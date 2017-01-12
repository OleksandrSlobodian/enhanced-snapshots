package com.sungardas.enhancedsnapshots.service;

import com.sungardas.enhancedsnapshots.aws.dynamodb.model.SnsRuleEntry;
import com.sungardas.enhancedsnapshots.aws.dynamodb.model.TaskEntry;
import com.sungardas.enhancedsnapshots.aws.dynamodb.model.TaskEntry.TaskEntryStatus;
import com.sungardas.enhancedsnapshots.dto.Dto;
import com.sungardas.enhancedsnapshots.dto.ExceptionDto;
import com.sungardas.enhancedsnapshots.dto.TaskProgressDto;

import java.util.List;

public interface NotificationService extends SimpleNotificationService{

    /**
     * Send notification to user about running task progress
     *
     * @param taskId   task ID
     * @param message  message to user
     * @param progress progress in range from 0 to 100
     */
    void notifyAboutRunningTaskProgress(String taskId, String message, double progress);

    /**
     * Send notification to user about task progress
     *
     * @param taskId   task ID
     * @param message  message to user
     * @param progress progress in range from 0 to 100
     * @param status   task status
     */
    void notifyAboutTaskProgress(String taskId, String message, double progress, TaskEntryStatus status);

    void notifyAboutTaskProgress(TaskProgressDto dto);

    void notifyAboutError(ExceptionDto exceptionDto);

    void notifyUser(String broker, Dto dto);

    /**
     * Get SNS ARN
     * @return SNS ARN
     */
    String getSnsTopic();

    /**
     * Set SNS ARN
     * @param snsTopic or null
     */
    void setSnsTopic(String snsTopic);

    //SNS rules CRUD
    void createRule(SnsRuleEntry ruleEntry);

    List<SnsRuleEntry> getRules();

    void updateRule(SnsRuleEntry ruleEntry);

    void deleteRule(String snsRuleId);

    void notifyViaSns(TaskEntry.TaskEntryType operation, TaskEntryStatus status, String volumeId);

    /**
     * reconnect to mail server
     *
     * @return true if connect succeeded
     */
    boolean reconnect();

    /**
     * disconnect from mail server
     */
    void disconnect();

    /**
     * Notify users about completed task
     * @param taskEntry task
     */
    void notifyAboutSuccess(TaskEntry taskEntry);


    /**
     * Notify about error
     * @param taskEntry task
     * @param e exception
     */
    void notifyAboutError(TaskEntry taskEntry, Exception e);


    /**
     * Notify about system status
     * @param message system message
     */
    void notifyAboutSystemStatus(String message);

}
