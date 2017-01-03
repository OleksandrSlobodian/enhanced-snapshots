package com.sungardas.enhancedsnapshots.components;

import com.amazonaws.AmazonClientException;
import com.sungardas.enhancedsnapshots.aws.dynamodb.model.NodeEntry;
import com.sungardas.enhancedsnapshots.aws.dynamodb.model.TaskEntry;
import com.sungardas.enhancedsnapshots.aws.dynamodb.repository.NodeRepository;
import com.sungardas.enhancedsnapshots.aws.dynamodb.repository.TaskRepository;
import com.sungardas.enhancedsnapshots.exception.EnhancedSnapshotsInterruptedException;
import com.sungardas.enhancedsnapshots.exception.EnhancedSnapshotsTaskInterruptedException;
import com.sungardas.enhancedsnapshots.service.TaskService;
import com.sungardas.enhancedsnapshots.tasks.executors.TaskExecutor;
import com.sungardas.enhancedsnapshots.util.SystemUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static com.sungardas.enhancedsnapshots.aws.dynamodb.model.TaskEntry.TaskEntryStatus.*;


@Service
@DependsOn({"ConfigurationMediator", "MasterService"})
public class WorkersDispatcher {

    @Autowired
    private ConfigurationMediator configurationMediator;
    @Autowired
    @Qualifier("awsBackupVolumeTaskExecutor")
    private TaskExecutor awsBackupVolumeTaskExecutor;
    @Autowired
    @Qualifier("awsRestoreVolumeTaskExecutor")
    private TaskExecutor awsRestoreVolumeTaskExecutor;
    @Autowired
    private TaskService taskService;
    @Autowired
    private TaskRepository taskRepository;
    @Autowired
    private NodeRepository nodeRepository;


    private ExecutorService executor;

    private ThreadPoolExecutor backupExecutor;
    private ThreadPoolExecutor restoreExecutor;

    @Value("${enhancedsnapshots.default.backup.threadPool.size}")
    private int backupThreadPoolSize;

    @Value("${enhancedsnapshots.default.restore.threadPool.size}")
    private int restoreThreadPoolSize;

    private String instanceId;

    @PostConstruct
    private void init() {
        instanceId = SystemUtils.getInstanceId();
        backupExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(backupThreadPoolSize);
        restoreExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(restoreThreadPoolSize);

        executor = Executors.newSingleThreadExecutor();
        executor.execute(new TaskWorker());

    }

    @PreDestroy
    public void destroy() {
        executor.shutdownNow();
        backupExecutor.shutdownNow();
        restoreExecutor.shutdownNow();
    }

    private Set<TaskEntry> sortByTimeAndPriority(List<TaskEntry> list) {
        Set<TaskEntry> result = new TreeSet<>((TaskEntry o1, TaskEntry o2) -> o1.getSchedulerTime().compareTo(o2.getSchedulerTime()));
        result.addAll(list);
        return result;
    }

    private class TaskWorker implements Runnable {
        private final Logger LOGtw = LogManager.getLogger(TaskWorker.class);

        @Override
        public void run() {
            LOGtw.info("Starting worker dispatcher");
            while (true) {
                if (Thread.interrupted()) {
                    throw new EnhancedSnapshotsInterruptedException("Task interrupted");
                }
                if (configurationMediator.isClusterMode()) {
                    NodeEntry nodeEntry = nodeRepository.findOne(instanceId);
                    nodeEntry.setFreeBackupWorkers(backupThreadPoolSize - backupExecutor.getActiveCount());
                    nodeEntry.setFreeRestoreWorkers(restoreThreadPoolSize - restoreExecutor.getActiveCount());
                    nodeRepository.save(nodeEntry);
                }
                TaskEntry entry = null;
                try {
                    Set<TaskEntry> taskEntrySet = getAssignedTasks();
                    while (!taskEntrySet.isEmpty()) {
                        entry = taskEntrySet.iterator().next();

                        try {
                            if (taskService.exists(entry.getId())) {
                                switch (TaskEntry.TaskEntryType.getType(entry.getType())) {
                                    case BACKUP:
                                        LOGtw.info("Task was identified as backup");
                                        entry.setStatus(WAITING.getStatus());
                                        taskRepository.save(entry);
                                        TaskEntry backupTask = entry;
                                        backupExecutor.submit(() -> awsBackupVolumeTaskExecutor.execute(backupTask));
                                        break;
                                    case RESTORE:
                                        LOGtw.info("Task was identified as restore");
                                        entry.setStatus(WAITING.getStatus());
                                        taskRepository.save(entry);
                                        TaskEntry restoreTask = entry;
                                        restoreExecutor.submit(() -> awsRestoreVolumeTaskExecutor.execute(restoreTask));
                                        break;
                                    case UNKNOWN: {
                                        LOGtw.warn("Executor for type {} is not implemented. Task {} is going to be removed.", entry.getType(), entry.getId());
                                        taskService.removeTask(entry.getId());
                                    }
                                }
                            } else {
                                LOGtw.debug("Task canceled: {}", entry);
                            }
                        } catch (EnhancedSnapshotsTaskInterruptedException e) {
                            //skip if task canceled
                            LOGtw.debug("Task canceled: {}", entry);
                        }
                        taskEntrySet = getAssignedTasks();
                    }
                    sleep();
                } catch (AmazonClientException e) {
                    LOGtw.error(e);
                } catch (EnhancedSnapshotsInterruptedException e) {
                    return;
                    // this is required to close thread when uninstalling system
                } catch (IllegalStateException e) {
                    LOGtw.warn("Stopping worker dispatcher ...");
                    return;
                } catch (Exception e) {
                    LOGtw.error(e);
                    if (entry != null) {
                        entry.setStatus(ERROR.getStatus());
                        taskRepository.save(entry);
                    }
                }
            }
        }

        private Set<TaskEntry> getAssignedTasks() {
            List<TaskEntry> taskEntries = new ArrayList<>();
            taskEntries.addAll(taskRepository.findByStatusAndRegularAndWorker(TaskEntry.TaskEntryStatus.QUEUED.getStatus(), Boolean.FALSE.toString(), instanceId));
            taskEntries.addAll(taskRepository.findByStatusAndRegularAndWorker(TaskEntry.TaskEntryStatus.PARTIALLY_FINISHED.getStatus(), Boolean.FALSE.toString(), instanceId));
            return sortByTimeAndPriority(taskEntries);
        }

        private void sleep() {
            try {
                TimeUnit.MILLISECONDS.sleep(configurationMediator.getWorkerDispatcherPollingRate());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
