package com.gyg.backend.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

@Service
public class CustomEmailService {

    private static final Logger logger = LoggerFactory.getLogger(CustomEmailService.class);

    private final JavaMailSender mailSender;
    private final String host;
    private final int port;
    private final String from;

    public CustomEmailService(
            JavaMailSender mailSender,
            @Value("${spring.mail.host:}") String host,
            @Value("${spring.mail.port:587}") int port,
            @Value("${app.mail.from:}") String from) {
        this.mailSender = mailSender;
        this.host = host;
        this.port = port;
        this.from = from;
        normalizeGmailPassword();
        logLoadedConfiguration();
    }

    public boolean isConfigured() {
        List<String> issues = configurationIssues();
        if (!issues.isEmpty()) {
            logger.warn("SMTP configuration is incomplete or inconsistent: {}.", String.join(", ", issues));
        }
        return issues.isEmpty();
    }

    public void testConnection() {
        if (!isConfigured()) {
            throw new EmailDeliveryException(new IllegalStateException("SMTP configuration is incomplete."));
        }
        if (!(mailSender instanceof JavaMailSenderImpl implementation)) {
            throw new EmailDeliveryException(
                    new IllegalStateException("SMTP connection diagnostics require JavaMailSenderImpl."));
        }
        try {
            implementation.testConnection();
        } catch (MessagingException | MailException exception) {
            throw new EmailDeliveryException(exception);
        }
    }

    public void sendPasswordReset(String email, String displayName, String actionLink) {
        send(
                email,
                "Restablece tu contraseña | G&G Construcciones",
                "templates/email/password-reset.html",
                "Restablece tu contraseña usando este enlace: ",
                displayName,
                actionLink);
    }

    public void sendEmailVerification(String email, String displayName, String actionLink) {
        send(
                email,
                "Verifica tu correo electrónico | G&G Construcciones",
                "templates/email/verify-email.html",
                "Verifica tu correo electrónico usando este enlace: ",
                displayName,
                actionLink);
    }

    private void send(
            String email,
            String subject,
            String templatePath,
            String textPrefix,
            String displayName,
            String actionLink) {
        if (!isConfigured()) {
            throw new EmailDeliveryException();
        }
        String safeLink = validateActionLink(actionLink);
        String safeName = displayName == null || displayName.isBlank()
                ? "Hola"
                : htmlEscape(displayName);
        String html;
        try (InputStream template = new ClassPathResource(templatePath).getInputStream()) {
            html = StreamUtils.copyToString(template, StandardCharsets.UTF_8)
                    .replace("{{DISPLAY_NAME}}", safeName)
                    .replace("{{ACTION_URL}}", htmlEscape(safeLink));
        } catch (IOException exception) {
            throw new EmailDeliveryException(exception);
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            helper.setFrom(from);
            helper.setTo(email);
            helper.setSubject(subject);
            helper.setText(textPrefix + safeLink, html);
            mailSender.send(message);
        } catch (MessagingException | MailException exception) {
            throw new EmailDeliveryException(exception);
        }
    }

    private static String validateActionLink(String actionLink) {
        if (actionLink == null) {
            throw new EmailDeliveryException();
        }
        try {
            URI uri = URI.create(actionLink);
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null) {
                throw new EmailDeliveryException();
            }
            return actionLink;
        } catch (IllegalArgumentException exception) {
            throw new EmailDeliveryException(exception);
        }
    }

    private static String htmlEscape(String value) {
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    static String normalizeGmailAppPassword(String smtpHost, String password) {
        if (password == null || !"smtp.gmail.com".equalsIgnoreCase(smtpHost)) {
            return password;
        }
        return password.replaceAll("\\s+", "");
    }

    private void normalizeGmailPassword() {
        if (!(mailSender instanceof JavaMailSenderImpl implementation)) {
            return;
        }
        String password = implementation.getPassword();
        String normalized = normalizeGmailAppPassword(host, password);
        if (password != null && !password.equals(normalized)) {
            implementation.setPassword(normalized);
            logger.info("Removed formatting whitespace from the configured Gmail app password.");
        }
    }

    private List<String> configurationIssues() {
        List<String> issues = new ArrayList<>();
        if (host.isBlank()) {
            issues.add("spring.mail.host");
        }
        if (port <= 0 || port > 65535) {
            issues.add("spring.mail.port");
        }
        if (from.isBlank()) {
            issues.add("app.mail.from");
        }
        if (mailSender instanceof JavaMailSenderImpl implementation) {
            Properties properties = implementation.getJavaMailProperties();
            boolean authenticationEnabled = propertyEnabled(properties, "mail.smtp.auth");
            if (authenticationEnabled && isBlank(implementation.getUsername())) {
                issues.add("spring.mail.username");
            }
            if (authenticationEnabled && isBlank(implementation.getPassword())) {
                issues.add("spring.mail.password");
            }
            if ("smtp.gmail.com".equalsIgnoreCase(host) && port == 587
                    && (!authenticationEnabled
                    || !propertyEnabled(properties, "mail.smtp.starttls.enable")
                    || !propertyEnabled(properties, "mail.smtp.starttls.required"))) {
                issues.add("Gmail SMTP port 587 requires AUTH and mandatory STARTTLS");
            }
        }
        return issues;
    }

    private void logLoadedConfiguration() {
        if (mailSender instanceof JavaMailSenderImpl implementation) {
            Properties properties = implementation.getJavaMailProperties();
            logger.info(
                    "SMTP configuration loaded: hostPresent={}, gmailHost={}, port={}, usernamePresent={}, "
                            + "passwordPresent={}, senderAddressPresent={}, authEnabled={}, "
                            + "starttlsEnabled={}, starttlsRequired={}",
                    !host.isBlank(),
                    "smtp.gmail.com".equalsIgnoreCase(host),
                    port,
                    !isBlank(implementation.getUsername()),
                    !isBlank(implementation.getPassword()),
                    !from.isBlank(),
                    propertyEnabled(properties, "mail.smtp.auth"),
                    propertyEnabled(properties, "mail.smtp.starttls.enable"),
                    propertyEnabled(properties, "mail.smtp.starttls.required"));
        } else {
            logger.info(
                    "SMTP configuration loaded: hostPresent={}, gmailHost={}, port={}, senderAddressPresent={}, "
                            + "senderImplementation={}",
                    !host.isBlank(),
                    "smtp.gmail.com".equalsIgnoreCase(host),
                    port,
                    !from.isBlank(),
                    mailSender.getClass().getSimpleName());
        }
    }

    private static boolean propertyEnabled(Properties properties, String name) {
        return Boolean.parseBoolean(properties.getProperty(name, "false"));
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
