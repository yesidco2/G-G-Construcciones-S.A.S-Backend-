package com.gyg.backend.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.fail;

class SmtpConnectionDiagnosticTest {

    @Test
    @EnabledIfEnvironmentVariable(named = "APP_SMTP_DIAGNOSTIC", matches = "true")
    void shouldConnectAndAuthenticateWithoutSendingEmail() {
        String host = requiredSetting("SMTP_HOST");
        int port = parsePort(requiredSetting("SMTP_PORT"));
        String username = requiredSetting("SMTP_USERNAME");
        String password = requiredSetting("SMTP_PASSWORD");
        String from = requiredSetting("APP_MAIL_FROM");

        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(host);
        sender.setPort(port);
        sender.setUsername(username);
        sender.setPassword(CustomEmailService.normalizeGmailAppPassword(host, password));

        Properties mailProperties = new Properties();
        mailProperties.setProperty("mail.smtp.auth", requiredSetting("SMTP_AUTH"));
        mailProperties.setProperty(
                "mail.smtp.starttls.enable", requiredSetting("SMTP_STARTTLS_ENABLE"));
        mailProperties.setProperty(
                "mail.smtp.starttls.required", requiredSetting("SMTP_STARTTLS_REQUIRED"));
        mailProperties.setProperty("mail.smtp.connectiontimeout", "10000");
        mailProperties.setProperty("mail.smtp.timeout", "10000");
        mailProperties.setProperty("mail.smtp.writetimeout", "10000");
        sender.setJavaMailProperties(mailProperties);

        try {
            new CustomEmailService(sender, host, port, from).testConnection();
        } catch (EmailDeliveryException exception) {
            fail("SMTP diagnostic failed: " + SmtpFailureDiagnostics.describe(exception));
        }
    }

    private static String requiredSetting(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            fail("SMTP diagnostic is missing environment variable " + name + ".");
        }
        return value;
    }

    private static int parsePort(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            fail("SMTP_PORT must be numeric.");
            return -1;
        }
    }
}
