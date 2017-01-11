package com.sungardas.enhancedsnapshots.service.impl;

import com.sungardas.enhancedsnapshots.aws.dynamodb.model.MailConfigurationDocument;
import com.sungardas.enhancedsnapshots.components.ConfigurationMediator;
import com.sungardas.enhancedsnapshots.dto.MailConfigurationDto;
import com.sungardas.enhancedsnapshots.exception.ConfigurationException;
import com.sungardas.enhancedsnapshots.exception.EmailNotificationException;
import com.sungardas.enhancedsnapshots.service.CryptoService;
import com.sungardas.enhancedsnapshots.service.SimpleNotificationService;
import freemarker.cache.TemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.util.*;

@Service
public class SimpleNotificationServiceImpl implements SimpleNotificationService {
    private static final Logger LOG = LogManager.getLogger(SimpleNotificationServiceImpl.class);

    @Autowired
    protected CryptoService cryptoService;

    @Autowired
    protected ConfigurationMediator configurationMediator;

    @Autowired
    protected ResourceLoader resourceLoader;

    @Value("${enhancedsnapshots.mail.test.message.subject}")
    protected String testSubject;

    @Value("${enhancedsnapshots.mail.test.message}")
    protected String testMessage;

    @Value("${enhancedsnapshots.mail.info.template.path}")
    protected String systemInformationTemplatePath;

    protected Template infoTemplate;

    protected Session session;

    @PostConstruct
    private void postConstruct() throws IOException {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_25);
        cfg.setTemplateLoader(getTemplateLoader());

        infoTemplate = cfg.getTemplate(systemInformationTemplatePath);
    }

    @Override
    public void testConfiguration(MailConfigurationDto config, String testEmail, String domain) {
        try {
            MailConfigurationDocument document = new MailConfigurationDocument();
            BeanUtils.copyProperties(config, document);
            Session session = getSession(document, false);
            Map<String, String> data = new HashMap<>();
            data.put("domain", domain);
            data.put("message", testMessage);

            Set<String> recipients = new HashSet<>();
            recipients.add(testEmail);

            notifyViaEmail(data, testSubject, infoTemplate, recipients, session, config.getFromMailAddress());
        } catch (Exception e) {
            throw new ConfigurationException("Invalid configuration", e);
        }
    }

    protected Session getSession(MailConfigurationDocument configuration, boolean decryptPassword) {
        if (configuration == null) {
            return null;
        }
        try {
            Properties props = new Properties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.host", configuration.getMailSMTPHost());

            props.put("mail.smtp.port", configuration.getMailSMTPPort());

            switch (configuration.getConnectionType()) {
                case TLS:
                    props.put("mail.smtp.starttls.enable", "true");
                    break;
                case SSL:
                    props.put("mail.smtp.socketFactory.port", configuration.getMailSMTPPort());
                    props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
                    break;
            }


            return Session.getInstance(props,
                    new javax.mail.Authenticator() {
                        protected PasswordAuthentication getPasswordAuthentication() {
                            if (decryptPassword) {
                                return new PasswordAuthentication(configuration.getUserName(),
                                        cryptoService.decrypt(configurationMediator.getConfigurationId(), configuration.getPassword()));
                            } else {
                                return new PasswordAuthentication(configuration.getUserName(), configuration.getPassword());
                            }
                        }
                    });
        } catch (RuntimeException e) {
            LOG.error(e);
            session = null;
            return null;
        }
    }

    protected void notifyViaEmail(Map data, String subject, Template template, Set<String> recipients, Session session, String senderEmail) {
        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(senderEmail));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(String.join(",", recipients)));
            message.setSubject(subject);

            StringWriter stringWriter = new StringWriter();
            template.process(data, stringWriter);

            message.setContent(stringWriter.toString(), "text/html; charset=utf-8");

            Transport.send(message);
        } catch (Exception e) {
            LOG.error(e);
            throw new EmailNotificationException(e);
        }
    }

    protected TemplateLoader getTemplateLoader() {
        return new TemplateLoader() {
            @Override
            public Object findTemplateSource(String name) throws IOException {
                Resource resource = resourceLoader.getResource(name);
                return resource.exists() ? resource : null;
            }

            @Override
            public long getLastModified(Object templateSource) {
                Resource resource = (Resource) templateSource;
                try {
                    return resource.lastModified();
                } catch (IOException e) {
                    return 0;
                }
            }

            @Override
            public Reader getReader(Object templateSource, String encoding) throws IOException {
                Resource resource = (Resource) templateSource;
                return new InputStreamReader(resource.getInputStream());
            }

            @Override
            public void closeTemplateSource(Object templateSource) throws IOException {
            }
        };
    }
}
