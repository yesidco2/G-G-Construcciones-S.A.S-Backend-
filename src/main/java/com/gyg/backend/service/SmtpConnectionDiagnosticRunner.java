package com.gyg.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("dev")
@ConditionalOnProperty(name = "app.mail.test-connection", havingValue = "true")
public class SmtpConnectionDiagnosticRunner implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(SmtpConnectionDiagnosticRunner.class);

    private final CustomEmailService emailService;

    public SmtpConnectionDiagnosticRunner(CustomEmailService emailService) {
        this.emailService = emailService;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            emailService.testConnection();
            logger.info("Optional SMTP connection and authentication test succeeded; no email was sent.");
        } catch (EmailDeliveryException exception) {
            logger.error("Optional SMTP connection test failed: {}",
                    SmtpFailureDiagnostics.describe(exception));
        }
    }
}
