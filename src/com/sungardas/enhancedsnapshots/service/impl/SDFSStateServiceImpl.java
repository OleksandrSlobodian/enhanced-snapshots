package com.sungardas.enhancedsnapshots.service.impl;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.BucketLifecycleConfiguration;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.services.s3.model.StorageClass;
import com.amazonaws.services.s3.model.lifecycle.LifecycleFilter;
import com.amazonaws.services.s3.model.lifecycle.LifecyclePrefixPredicate;
import com.sungardas.enhancedsnapshots.aws.dynamodb.model.BackupEntry;
import com.sungardas.enhancedsnapshots.aws.dynamodb.model.BackupState;
import com.sungardas.enhancedsnapshots.aws.dynamodb.model.EventEntry;
import com.sungardas.enhancedsnapshots.cluster.ClusterEventListener;
import com.sungardas.enhancedsnapshots.components.ConfigurationMediator;
import com.sungardas.enhancedsnapshots.exception.ConfigurationException;
import com.sungardas.enhancedsnapshots.exception.EnhancedSnapshotsException;
import com.sungardas.enhancedsnapshots.exception.SDFSException;
import com.sungardas.enhancedsnapshots.service.SDFSStateService;
import org.apache.commons.lang.ArrayUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;


@Service
@Profile("prod")
public class SDFSStateServiceImpl implements SDFSStateService, ClusterEventListener {

    private static final Logger LOG = LogManager.getLogger(SDFSStateServiceImpl.class);

    private boolean reconfigurationInProgressFlag = false;

    private static final String MOUNT_CMD = "--mount";
    private static final String UNMOUNT_CMD = "--unmount";
    private static final String GET_SATE_CMD = "--state";
    private static final String CONFIGURE_CMD = "--configure";
    private static final String CONFIGURE_CLUSTER_CMD = "--configurenode";
    private static final String EXPAND_VOLUME_CMD = "--expandvolume";
    private static final String CLOUD_SYNC_CMD = "--cloudsync";
    private static final String SHOW_VOLUME_ID_CMD = "--showvolumes";
    private static final String SYNC_CLUSTER_VOLUMES = "--syncvolumes";
    private static final String SET_LOCAL_CACHE_SIZE = "--setlocalcache";
    private static final String DELETE_CLUSTER_VOLUME = "--deletevolume";


    @Value("${enhancedsnapshots.default.sdfs.mount.time}")
    private int sdfsMountTime;
    @Value("${enhancedsnapshots.sdfs.script.path}")
    private String sdfsScript;
    private String bucketLocation;

    private static final int VOLUME_ID_INDEX = 0;
    private static final int TIME_INDEX = 1;
    private static final int TYPE_INDEX = 2;
    private static final int IOPS_INDEX = 3;
    private static final long BYTES_IN_GIB = 1073741824l;

    @Autowired
    private ResourceLoader resourceLoader;

    @Autowired
    private AmazonS3 amazonS3;

    @Autowired
    private ConfigurationMediator configurationMediator;

    @Value("${enhancedsnapshots.s3.rule.ia.names}")
    private String[] s3RuleNames;

    @Value("${enhancedsnapshots.s3.rule.ia.prefixes}")
    private String[] s3RulePrefixes;

    @Value("${enhancedsnapshots.s3.rule.ia.days}")
    private int s3MoveToIaAfterDays;

    @Value("${enhancedsnapshots.s3.ia.enabled}")
    private boolean s3RulesEnabled;

    @Override
    public void restoreSDFS() {
        File file = null;
        try {
            stopSDFS();
            startSDFS(true);
            //SDFS mount time
            TimeUnit.SECONDS.sleep(sdfsMountTime);
            LOG.info("SDFS state restored.");
        } catch (Exception e) {
            if (file != null && file.exists()) {
                file.delete();
            }
            throw new SDFSException("Can't restore sdfs state", e);
        }
    }

    @Override
    public Long getBackupTime() {
        ListObjectsRequest request = new ListObjectsRequest()
                .withBucketName(configurationMediator.getS3Bucket()).withPrefix(configurationMediator.getSdfsBackupFileName());
        List<S3ObjectSummary> list = amazonS3.listObjects(request).getObjectSummaries();
        if (list.size() > 0) {
            return list.get(0).getLastModified().getTime();
        } else {
            return null;
        }
    }

    private void startSDFS(Boolean restore) {
        try {
            if (sdfsIsRunnig()) {
                LOG.info("SDFS is already running");
                return;
            }
            boolean applyIaRules = false;
            if (!new File(configurationMediator.getSdfsConfigPath()).exists()) {
                configureSDFS();
                applyIaRules = true;

            }
            String[] parameters = {getSdfsScriptFile(sdfsScript).getAbsolutePath(), MOUNT_CMD, restore.toString()};

            Process p = executeScript(parameters);
            switch (p.exitValue()) {
                case 0:
                    LOG.info("SDFS is running");
                    break;
                default:
                    throw new ConfigurationException("Failed to start SDFS");
            }
            if (configurationMediator.isClusterMode()) {

                p = executeScript(new String[]{getSdfsScriptFile(sdfsScript).getAbsolutePath(), SYNC_CLUSTER_VOLUMES, configurationMediator.getSdfsCliPsw()});
                switch (p.exitValue()) {
                    case 0:
                        LOG.info("SDFS is synchronized");
                        break;
                    default:
                        throw new ConfigurationException("Failed to synchronize SDFS");
                }
            }
            if (applyIaRules) {
                LOG.info("Applying S3 lifecycle rules");
                createS3LifeCycleRules(configurationMediator.getS3Bucket());
                LOG.info("S3 lifecycle rules has been applied");
            }
        } catch (Exception e) {
            LOG.error(e);
            throw new ConfigurationException("Failed to start SDFS");
        }
    }

    /**
     * Create S3 life cycle rules if they does not exists.
     */
    protected void createS3LifeCycleRules(String bucketName) {
        List<BucketLifecycleConfiguration.Rule> iaRules = new ArrayList<>();
        for(int i = 0; i < s3RuleNames.length; i++) {
            iaRules.add(getRule(s3RuleNames[i], s3RulesEnabled, s3RulePrefixes[i], s3MoveToIaAfterDays));
        }
        BucketLifecycleConfiguration bucketLifecycleConfiguration = amazonS3.getBucketLifecycleConfiguration(bucketName);
        if (bucketLifecycleConfiguration != null) {
            List<BucketLifecycleConfiguration.Rule> currentRules = bucketLifecycleConfiguration.getRules();

            for (BucketLifecycleConfiguration.Rule rule : iaRules) {
                if (!contains(currentRules, rule.getId())) {
                    currentRules.add(rule);
                }
            }

            amazonS3.setBucketLifecycleConfiguration(bucketName, new BucketLifecycleConfiguration(currentRules));
        } else {
            amazonS3.setBucketLifecycleConfiguration(bucketName, new BucketLifecycleConfiguration(iaRules));
        }
    }

    private boolean contains(List<BucketLifecycleConfiguration.Rule> rules, String id) {
        for(BucketLifecycleConfiguration.Rule rule: rules) {
            if(id.equals(rule.getId())) {
                return true;
            }
        }
        return false;
    }

    private BucketLifecycleConfiguration.Rule getRule(String name, boolean enabled, String prefix, int days){
        BucketLifecycleConfiguration.Rule rule = new BucketLifecycleConfiguration.Rule();
        rule.setId(name);
        if(enabled) {
            rule.setStatus(SDFSStateService.IA_ENABLED);
        }else {
            rule.setStatus(SDFSStateService.IA_DISABLED);
        }
        rule.setFilter(new LifecycleFilter(new LifecyclePrefixPredicate(prefix)));
        rule.setTransitions(Arrays.asList(new BucketLifecycleConfiguration.Transition()
                .withStorageClass(StorageClass.StandardInfrequentAccess).withDays(days)));
        return rule;
    }

    @Override
    public void startSDFS() {
        startSDFS(false);
    }


    private void configureSDFS() throws IOException, InterruptedException {
        String[] parameters;
        if (configurationMediator.isClusterMode()) {
            LOG.info("Configuring SDFS in cluster mode...");
            parameters = new String[]{getSdfsScriptFile(sdfsScript).getAbsolutePath(), CONFIGURE_CLUSTER_CMD, configurationMediator.getSdfsVolumeSize(), configurationMediator.getS3Bucket(),
                    getBucketLocation(configurationMediator.getS3Bucket()), configurationMediator.getSdfsLocalCacheSize(), configurationMediator.getChunkStoreEncryptionKey(),
                    configurationMediator.getChunkStoreIV(), configurationMediator.getSdfsCliPsw()};
        } else {
            LOG.info("Configuring SDFS in standalone mode...");
            parameters = new String[]{getSdfsScriptFile(sdfsScript).getAbsolutePath(), CONFIGURE_CMD, configurationMediator.getSdfsVolumeSize(), configurationMediator.getS3Bucket(),
                    getBucketLocation(configurationMediator.getS3Bucket()), configurationMediator.getSdfsLocalCacheSize()};
        }
        Process p = executeScript(parameters);
        switch (p.exitValue()) {
            case 0:
                LOG.info("SDFS is configured");
                break;
            default:
                throw new ConfigurationException("Failed to configure SDFS");
        }
    }

    @Override
    public void stopSDFS() {
        try {
            if (!sdfsIsRunnig()) {
                LOG.info("SDFS is already stopped");
                return;
            }
            String[] parameters = {getSdfsScriptFile(sdfsScript).getAbsolutePath(), UNMOUNT_CMD};
            Process p = executeScript(parameters);
            switch (p.exitValue()) {
                case 0:
                    LOG.info("SDFS is currently stopped");
                    break;
                default:
                    throw new ConfigurationException("Failed to stop SDFS");
            }
        } catch (Exception e) {
            LOG.error(e);
            throw new ConfigurationException("Failed to stop SDFS");
        }
    }

    private boolean sdfsIsRunnig() {
        try {
            String[] parameters = {getSdfsScriptFile(sdfsScript).getAbsolutePath(), GET_SATE_CMD};
            Process p = executeScript(parameters);
            switch (p.exitValue()) {
                case 0:
                    LOG.debug("SDFS is currently running");
                    return true;
                case 1:
                    LOG.debug("SDFS is currently stopped");
                    return false;
                default:
                    throw new ConfigurationException("Failed to stop SDFS");
            }
        } catch (Exception e) {
            LOG.error(e);
            throw new ConfigurationException("Failed to determine SDFS state");
        }
    }

    public boolean sdfsIsAvailable() {
        try {
            if (reconfigurationInProgressFlag) {
                LOG.debug("SDFS is unavailable. Reconfiguration is in progress ... ");
                return false;
            }
            return sdfsIsRunnig();
        } catch (Exception e) {
            LOG.error(e);
            throw new ConfigurationException("Failed to determine SDFS state");
        }
    }

    @Override
    public void expandSdfsVolume(int newVolumeSize) {
        String[] parameters;
                    try {
                        parameters = new String[]{getSdfsScriptFile(sdfsScript).getAbsolutePath(), EXPAND_VOLUME_CMD, configurationMediator.getSdfsMountPoint(), newVolumeSize + VOLUME_SIZE_UNIT};
                        Process p = executeScript(parameters);
                        switch (p.exitValue()) {
                            case 0:
                    LOG.debug("SDFS volume was expanded successfully");
                    break;
                default:
                    throw new ConfigurationException("Failed to expand SDFS volume");
            }
        } catch (Exception e) {
            LOG.error(e);
            throw new ConfigurationException("Failed to expand SDFS volume");
        }
    }

    @Override
    public void cloudSync() {
        String[] parameters;
        try {
            parameters = new String[]{getSdfsScriptFile(sdfsScript).getAbsolutePath(), CLOUD_SYNC_CMD};
            Process p = executeScript(parameters);
            switch (p.exitValue()) {
                case 0:
                    LOG.debug("SDFS metadata sync successfully");
                    break;
                default:
                    throw new ConfigurationException("Failed to sync SDFS metadata");
            }
        } catch (Exception e) {
            LOG.error(e);
            throw new ConfigurationException("Failed to sync SDFS metadata");
        }
    }

    private void removeSdfsConfFile() {
        File sdfsConf = new File(configurationMediator.getSdfsConfigPath());
        if (sdfsConf.exists()) {
            sdfsConf.delete();
            LOG.info("SDFS conf file was successfully removed.");
        }
    }

    private Process executeScript(String[] parameters) throws IOException, InterruptedException {
        LOG.info("Executing script: {}", Arrays.toString(parameters));
        Process p = Runtime.getRuntime().exec(parameters);
        p.waitFor();
        print(p);
        return p;
    }

    private String getBucketLocation(String bucket) {
        if (bucketLocation != null) {
            return bucketLocation;
        }
        if (amazonS3.doesBucketExist(bucket)) {
            bucketLocation = amazonS3.getBucketLocation(bucket);
        } else {
            bucketLocation = Regions.getCurrentRegion().getName();
        }
        return bucketLocation;
    }

    private void print(Process p) throws IOException {
        String line;
        BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
        while ((line = input.readLine()) != null) {
            System.out.println(line);
        }
        input = new BufferedReader(new InputStreamReader(p.getErrorStream()));
        while ((line = input.readLine()) != null) {
            System.out.println(line);
        }
    }


    private File getSdfsScriptFile(String scriptName) throws IOException {
        File sdfsScript = resourceLoader.getResource(scriptName).getFile();
        sdfsScript.setExecutable(true);
        return sdfsScript;
    }

    public List<BackupEntry> getBackupsFromSDFSMountPoint() {
        File[] files = new File(configurationMediator.getSdfsMountPoint()).listFiles();
        LOG.info("Found {} files in system backup", files.length);
        List<BackupEntry> backupEntryList = new ArrayList<>();
        for (File file : files) {
            BackupEntry entry = getBackupFromFile(file);
            if (entry != null) {
                backupEntryList.add(entry);
            }
        }
        LOG.info("All backups restored.");
        return backupEntryList;
    }

    @Override
    public long getSDFSVolumeId() {
        try {
            String[] parameters = new String[]{getSdfsScriptFile(sdfsScript).getAbsolutePath(), SHOW_VOLUME_ID_CMD, configurationMediator.getSdfsCliPsw()};
            LOG.info("Executing script: {}", Arrays.toString(parameters));
            Process p = Runtime.getRuntime().exec(parameters);
            p.waitFor();
            String line = new BufferedReader(new InputStreamReader(p.getInputStream())).lines().skip(3).findFirst().get();

            return Long.parseLong(line);
        } catch (Exception e) {
            LOG.error(e);
            throw new EnhancedSnapshotsException("Unable to get SDFS volumeId", e);
        }
    }

    @Override

    public void setLocalCacheSize(int localCacheSize) {
        String[] parameters;
        try {
            parameters = new String[]{getSdfsScriptFile(sdfsScript).getAbsolutePath(), SET_LOCAL_CACHE_SIZE, localCacheSize + LOCAL_CACHE_SIZE_UNIT};
            Process p = executeScript(parameters);
            switch (p.exitValue()) {
                case 0:
                    LOG.debug("SDFS local cache size updated successfully");
                    break;
                default:
                    throw new ConfigurationException("Failed to update SDFS local cache size");
            }
        } catch (Exception e) {
            LOG.error(e);
            throw new ConfigurationException("Failed to update SDFS local cache size");
        }

    public void enableS3IA() {
        List<BucketLifecycleConfiguration.Rule> rules = amazonS3.getBucketLifecycleConfiguration(configurationMediator.getS3Bucket()).getRules();
        for (BucketLifecycleConfiguration.Rule rule: rules) {
            if(ArrayUtils.contains(s3RuleNames, rule.getId())) {
                rule.setStatus(IA_ENABLED);
            }
        }
        amazonS3.setBucketLifecycleConfiguration(configurationMediator.getS3Bucket(),
                new BucketLifecycleConfiguration(rules));
    }

    @Override
    public void disableS3IA() {
        List<BucketLifecycleConfiguration.Rule> rules = amazonS3.getBucketLifecycleConfiguration(configurationMediator.getS3Bucket()).getRules();
        for (BucketLifecycleConfiguration.Rule rule: rules) {
            if(ArrayUtils.contains(s3RuleNames, rule.getId())) {
                rule.setStatus(IA_DISABLED);
            }
        }
        amazonS3.setBucketLifecycleConfiguration(configurationMediator.getS3Bucket(),
                new BucketLifecycleConfiguration(rules));

    }

    private BackupEntry getBackupFromFile(File file) {
        String fileName = file.getName();
        String[] props = fileName.split("\\.");
        if (props.length != 5) {
            return null;
        } else {
            BackupEntry backupEntry = new BackupEntry();

            backupEntry.setFileName(fileName);
            backupEntry.setIops(props[IOPS_INDEX]);
            backupEntry.setSizeGiB(String.valueOf((int) (file.length() / BYTES_IN_GIB)));
            backupEntry.setTimeCreated(props[TIME_INDEX]);
            backupEntry.setVolumeType(props[TYPE_INDEX]);
            backupEntry.setState(BackupState.COMPLETED.getState());
            backupEntry.setVolumeId(props[VOLUME_ID_INDEX]);
            backupEntry.setSize(String.valueOf(file.length()));

            return backupEntry;
        }
    }

    @Override
    public void launched(EventEntry eventEntry) {
    }

    @Override
    public void terminated(EventEntry eventEntry) {
        try {
            String[] parameters = {getSdfsScriptFile(sdfsScript).getAbsolutePath(), DELETE_CLUSTER_VOLUME, configurationMediator.getSdfsCliPsw(), String.valueOf(eventEntry.getVolumeId())};
            executeScript(parameters);
        } catch (Exception e) {
            LOG.error("Failed to remove sdfs volume", e);
        }
    }
}