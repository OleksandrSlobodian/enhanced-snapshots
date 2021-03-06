package com.sungardas.enhancedsnapshots.components.logwatcher;

import com.sungardas.enhancedsnapshots.components.ConfigurationMediator;
import org.apache.commons.io.input.ReversedLinesFileReader;
import org.apache.commons.io.input.Tailer;
import org.apache.commons.io.input.TailerListener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.DependsOn;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.util.Collections.reverse;


@Service
@DependsOn({"ConfigurationMediator", "MasterService"})
public class LogsWatcherService implements TailerListener {

    private static final Logger LOG = LogManager.getLogger(LogsWatcherService.class);
    private static final String LOGS_DESTINATION = "/logs";
    private File logFile;
    private Tailer tailer;

    @Autowired
    private ConfigurationMediator configurationMediator;
    @Autowired
    private SimpMessagingTemplate template;
    @Value("${catalina.home}")
    private String catalinaHome;

    @PreDestroy
    public void destroy() {
        stop();
    }

    public void stop() {
        if (tailer != null) {
            tailer.stop();
            LOG.info("Logs watcher stopped.");
        }
    }

    public void start() {
        if (tailer == null) {
            tailer = Tailer.create(getLogsFile(), this, 500L, true);
            LOG.info("Logs watcher started. File {} will be tracked for changes.", configurationMediator.getLogFileName());
        }
    }

    private File getLogsFile() {
        if (logFile == null) {
            logFile = Paths.get(catalinaHome, configurationMediator.getLogFileName()).toFile();
        }
        return logFile;
    }

    @Override
    public void init(Tailer tailer) {}

    @Override
    public void fileNotFound() {
        LOG.warn("Log file {} was not found", configurationMediator.getLogFileName());
    }

    @Override
    public void fileRotated() {}

    @Override
    public synchronized void handle(String line) {
        template.convertAndSend(LOGS_DESTINATION, Arrays.asList(line));
    }

    @Override
    public void handle(Exception ex) {
        LOG.warn("Failed to read log file {}", configurationMediator.getLogFileName(), ex);
    }

    public synchronized void sendLatestLogs() {
        List<String> list = new ArrayList<>();
        try {
            ReversedLinesFileReader reader = new ReversedLinesFileReader(getLogsFile());
            while (list.size() < configurationMediator.getLogsBufferSize()) {
                list.add(reader.readLine());
            }
            reverse(list);
            template.convertAndSend(LOGS_DESTINATION, list);
        } catch (IOException e) {
            LOG.warn("Failed to read logs {}", e);
            reverse(list);
            template.convertAndSend(LOGS_DESTINATION, list);
        }
    }
}
