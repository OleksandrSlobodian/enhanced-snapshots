package com.sungardas.enhancedsnapshots.rest;

import com.sungardas.enhancedsnapshots.components.ConfigurationMediator;
import com.sungardas.enhancedsnapshots.dto.MailConfigurationTestDto;
import com.sungardas.enhancedsnapshots.dto.SystemConfiguration;
import com.sungardas.enhancedsnapshots.service.MailService;
import com.sungardas.enhancedsnapshots.service.SDFSStateService;
import com.sungardas.enhancedsnapshots.service.SystemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.security.RolesAllowed;


@RestController
@RequestMapping("/system")
public class SystemController {

    @Autowired
    private SDFSStateService sdfsStateService;
    @Autowired
    private SystemService systemService;
    @Autowired
    private MailService mailService;
    @Autowired
    private ConfigurationMediator configurationMediator;
    @Value("${enhancedsnapshots.default.maxIopsPerGb}")
    private int maxIopsPerGb;

    @RolesAllowed("ROLE_ADMIN")
    @RequestMapping(value = "/delete", method = RequestMethod.POST)
    public ResponseEntity<String> deleteService(@RequestBody RemoveAppDTO removeAppDTO) {
        if (!configurationMediator.getConfigurationId().equals(removeAppDTO.systemId)) {
            return new ResponseEntity<>("{\"msg\":\"Provided system ID is incorrect\"}", HttpStatus.FORBIDDEN);
        }
        systemService.systemUninstall(removeAppDTO.removeS3Bucket);
        return new ResponseEntity<>("", HttpStatus.OK);
    }

    @RolesAllowed({"ROLE_ADMIN", "ROLE_USER"})
    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<SystemConfiguration> getSystem() {
        return new ResponseEntity<>(systemService.getSystemConfiguration(), HttpStatus.OK);
    }

    @RolesAllowed({"ROLE_ADMIN", "ROLE_USER"})
    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<String> updateSystemProperties(@RequestBody SystemConfiguration newConfiguration) {
        SystemConfiguration currentConfiguration = systemService.getSystemConfiguration();
        if (!checkIopsAreValid(newConfiguration.getSystemProperties())) {
            return new ResponseEntity<>("iops per GB can not be less than 1 and more than 50", HttpStatus.BAD_REQUEST);
        }
        if (newConfiguration.getSdfs().getVolumeSize() > currentConfiguration.getSdfs().getMaxVolumeSize()) {
            return new ResponseEntity<>("Volume size can not be more than " + currentConfiguration.getSdfs().getMaxVolumeSize(), HttpStatus.BAD_REQUEST);
        }
        if (newConfiguration.getSdfs().getSdfsLocalCacheSize() > currentConfiguration.getSdfs().getMaxSdfsLocalCacheSize()) {
            return new ResponseEntity<>("Local cache size can not be more than " + currentConfiguration.getSdfs().getMaxSdfsLocalCacheSize(), HttpStatus.BAD_REQUEST);
        }
        if (newConfiguration.getSystemProperties().getTaskHistoryTTS() < 0) {
            return new ResponseEntity<>("Task history TTS can not be less than 0", HttpStatus.BAD_REQUEST);
        }

        systemService.updateSystemConfiguration(newConfiguration);
        return new ResponseEntity<>("", HttpStatus.OK);
    }

    @RolesAllowed({"ROLE_ADMIN", "ROLE_USER"})
    @RequestMapping(value = "/backup", method = RequestMethod.GET)
    public ResponseEntity<SystemBackupDto> getConfiguration() {
        return new ResponseEntity<>(new SystemBackupDto(sdfsStateService.getBackupTime()), HttpStatus.OK);
    }

    @RolesAllowed({"ROLE_ADMIN", "ROLE_USER"})
    @RequestMapping(value = "/backup", method = RequestMethod.POST)
    public ResponseEntity<String> backupSystem() {
        systemService.backup();
        return new ResponseEntity<>("", HttpStatus.OK);
    }

    @RolesAllowed({"ROLE_ADMIN", "ROLE_USER"})
    @RequestMapping(value = "/mail/configuration/test", method = RequestMethod.POST)
    public ResponseEntity mailConfigurationTest(@RequestBody MailConfigurationTestDto dto) {
        mailService.testConfiguration(dto.getMailConfiguration(), dto.getTestEmail(), dto.getDomain());
        return new ResponseEntity<>(HttpStatus.OK);
    }

    private static class SystemBackupDto {
        public Long lastBackup;

        public SystemBackupDto(Long lastBackup) {
            this.lastBackup = lastBackup;
        }
    }

    private static class RemoveAppDTO {
        public String systemId;
        public boolean removeS3Bucket;
    }

    // iops per GB can not be less than 1 and more than 50
    private boolean checkIopsAreValid(SystemConfiguration.SystemProperties systemProperties) {
        boolean result = true;
        if (systemProperties.getRestoreVolumeIopsPerGb() > maxIopsPerGb || systemProperties.getTempVolumeIopsPerGb() > maxIopsPerGb) {
            result = false;
        }
        if (systemProperties.getRestoreVolumeIopsPerGb() < 1 || systemProperties.getTempVolumeIopsPerGb() < 1) {
            result = false;
        }
        return result;
    }
}
