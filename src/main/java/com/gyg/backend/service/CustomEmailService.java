package com.gyg.backend.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;

@Service
public class CustomEmailService {

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
    }

    public boolean isConfigured() {
        return !host.isBlank() && port > 0 && port <= 65535 && !from.isBlank();
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
        try {
            html = StreamUtils.copyToString(
                            new ClassPathResource(templatePath).getInputStream(),
                            StandardCharsets.UTF_8)
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
}
