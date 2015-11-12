package com.sungardas.enhancedsnapshots.service.impl;

import com.sungardas.enhancedsnapshots.aws.dynamodb.model.TaskEntry;
import com.sungardas.enhancedsnapshots.aws.dynamodb.repository.BackupRepository;
import com.sungardas.enhancedsnapshots.aws.dynamodb.repository.TaskRepository;
import com.sungardas.enhancedsnapshots.dto.TaskDto;
import com.sungardas.enhancedsnapshots.dto.converter.TaskDtoConverter;
import com.sungardas.enhancedsnapshots.exception.DataAccessException;
import com.sungardas.enhancedsnapshots.exception.EnhancedSnapshotsException;
import com.sungardas.enhancedsnapshots.service.ConfigurationService;
import com.sungardas.enhancedsnapshots.service.SchedulerService;
import com.sungardas.enhancedsnapshots.service.TaskService;
import com.sungardas.enhancedsnapshots.tasks.AWSRestoreVolumeTask;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class TaskServiceImpl implements TaskService {

    private static final Logger LOG = LogManager.getLogger(TaskServiceImpl.class);

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private BackupRepository backupRepository;

    @Value("${sungardas.worker.configuration}")
    private String configurationId;

    @Autowired
    private ConfigurationService configuration;

    @Autowired
    private SchedulerService schedulerService;

    @Override
    public List<String> createTask(TaskDto taskDto) {
        List<TaskEntry> newTasks = TaskDtoConverter.convert(taskDto);
        List<String> messages = new ArrayList<>();
        String configurationId = configuration.getWorkerConfiguration().getConfigurationId();
        for (TaskEntry taskEntry : newTasks) {
            taskEntry.setWorker(configurationId);
            taskEntry.setInstanceId(configurationId);
            taskEntry.setStatus(TaskEntry.TaskEntryStatus.QUEUED.getStatus());
            if (Boolean.valueOf(taskEntry.getRegular())) {
                try {
                    schedulerService.addTask(taskEntry);
                } catch (EnhancedSnapshotsException e) {
                    taskRepository.delete(taskEntry);
                    LOG.error(e);
                    throw e;
                }
            }
            messages.add(getMessage(taskEntry));
        }
        taskRepository.save(newTasks);
        return messages;
    }

    private String getMessage(TaskEntry taskEntry) {
        switch (taskEntry.getType()) {
            case "restore":
                String sourceFile = taskEntry.getOptions();
                if (sourceFile == null || sourceFile.isEmpty()) {
                    return AWSRestoreVolumeTask.RESTORED_NAME_PREFIX + backupRepository.getLast(taskEntry.getVolume(), configurationId).getFileName();
                } else {
                    return AWSRestoreVolumeTask.RESTORED_NAME_PREFIX + backupRepository.getByBackupFileName(sourceFile).getFileName();
                }
        }
        return StringUtils.EMPTY;
    }

    @Override
    public List<TaskDto> getAllTasks() {
        try {
            return TaskDtoConverter.convert(taskRepository.findByRegularAndInstanceId(Boolean.FALSE.toString(),
                    configuration.getWorkerConfiguration().getConfigurationId()));
        } catch (RuntimeException e) {
            LOG.error("Failed to get tasks.", e);
            throw new DataAccessException("Failed to get tasks.", e);
        }
    }

    @Override
    public List<TaskDto> getAllTasks(String volumeId) {
        try {
            return TaskDtoConverter.convert(taskRepository.findByRegularAndVolumeAndInstanceId(Boolean.FALSE.toString(),
                    volumeId, configuration.getWorkerConfiguration().getConfigurationId()));
        } catch (RuntimeException e) {
            LOG.error("Failed to get tasks.", e);
            throw new DataAccessException("Failed to get tasks.", e);
        }
    }

    @Override
    public List<TaskDto> getAllRegularTasks(String volumeId) {
        try {
            return TaskDtoConverter.convert(taskRepository.findByRegularAndVolumeAndInstanceId(Boolean.TRUE.toString(),
                    volumeId, configuration.getWorkerConfiguration().getConfigurationId()));
        } catch (RuntimeException e) {
            LOG.error("Failed to get tasks.", e);
            throw new DataAccessException("Failed to get tasks.", e);
        }
    }

    @Override
    public void removeTask(String id) {
        if (taskRepository.exists(id)) {
            TaskEntry taskEntry = taskRepository.findOne(id);
            if (TaskEntry.TaskEntryStatus.RUNNING.getStatus().equals(taskEntry.getStatus())) {
                throw new EnhancedSnapshotsException("Can`t remove task " + id + ", task in status: " + taskEntry.getStatus());
            }
            taskRepository.delete(id);
            if (Boolean.valueOf(taskEntry.getRegular())) {
                schedulerService.removeTask(taskEntry.getId());
            }
            LOG.info("TaskEntry {} was removed successfully.", id);
        } else {
            LOG.info("TaskEntry {} can not be removed since it does not exist.", id);
        }
    }

    @Override
    public boolean isCanceled(String id) {
        return !taskRepository.exists(id);
    }

    @Override
    public void updateTask(TaskDto taskInfo) {
        removeTask(taskInfo.getId());
        createTask(taskInfo);
    }
}
