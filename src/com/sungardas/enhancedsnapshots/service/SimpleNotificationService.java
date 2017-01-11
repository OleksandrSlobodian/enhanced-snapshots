package com.sungardas.enhancedsnapshots.service;

import com.sungardas.enhancedsnapshots.dto.MailConfigurationDto;

public interface SimpleNotificationService {
    /**
     * Test mail configuration (send test mail)
     * @param config mail configuration
     * @param testEmail test mail address
     * @param domain instance domain
     */
    void testConfiguration(MailConfigurationDto config, String testEmail, String domain);
}
