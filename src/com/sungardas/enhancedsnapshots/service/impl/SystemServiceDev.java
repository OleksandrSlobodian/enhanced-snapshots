package com.sungardas.enhancedsnapshots.service.impl;

import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service("SystemService")
@DependsOn("CreateAppConfiguration")
@Profile("dev")
public class SystemServiceDev extends SystemServiceImpl {

    protected void backupSDFS(final Path tempDirectory) throws IOException {
    }

    protected void storeFiles(Path tempDirectory) {
    }

    @Override
    protected String getSystemId() {
        return "DEV";
    }
}
