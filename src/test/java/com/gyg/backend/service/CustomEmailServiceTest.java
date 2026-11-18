package com.gyg.backend.service;

import jakarta.mail.Session;
import jakarta.mail.Part;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.Multipart;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class CustomEmailServiceTest {

    @Test
    void shouldSendResponsiveHtmlWithEscapedContentAndFirebaseLink() throws Exception {
        JavaMailSender sender = mock(JavaMailSender.class);
        when(sender.createMimeMessage())
                .thenReturn(new MimeMessage(Session.getInstance(new Properties())));
        CustomEmailService service = new CustomEmailService(
                sender, "smtp.example.com", 587, "no-reply@example.com");

        service.sendPasswordReset(
                "owner@example.com",
                "\"><img src=x onerror=alert(1)>",
                "https://example.firebaseapp.com/__/auth/action?mode=reset&continueUrl=https%3A%2F%2Fapp.example.com");

        var messageCaptor = org.mockito.ArgumentCaptor.forClass(MimeMessage.class);
        verify(sender).send(messageCaptor.capture());
        MimeMessage message = messageCaptor.getValue();
        message.saveChanges();
        assertTrue(message.getSubject().contains("Restablece tu contraseña"));
        String html = findHtml(message);
        assertTrue(html.contains("&quot;&gt;&lt;img src=x onerror=alert(1)&gt;"), html);
        assertTrue(html.contains("href=\"https://example.firebaseapp.com/__/auth/action?mode=reset&amp;"));
        assertTrue(html.contains("background:#102a43"));
        assertTrue(html.contains("background:#ed7d20"));
    }

    @Test
    void shouldRejectNonHttpsActionLinks() {
        CustomEmailService service = new CustomEmailService(
                mock(JavaMailSender.class), "smtp.example.com", 587, "no-reply@example.com");

        assertThrows(EmailDeliveryException.class,
                () -> service.sendEmailVerification("owner@example.com", null, "javascript:alert(1)"));
    }

    @Test
    void shouldRequireSmtpAndSenderConfiguration() {
        CustomEmailService service = new CustomEmailService(
                mock(JavaMailSender.class), "", 587, "");

        assertThrows(EmailDeliveryException.class,
                () -> service.sendPasswordReset("owner@example.com", null, "https://example.com/action"));
    }

    @Test
    void shouldRemoveOnlyGmailAppPasswordFormattingSpaces() {
        JavaMailSenderImpl gmailSender = new JavaMailSenderImpl();
        gmailSender.setPassword("abcd efgh ijkl mnop");
        new CustomEmailService(gmailSender, "smtp.gmail.com", 587, "sender@example.com");

        assertEquals("abcdefghijklmnop", gmailSender.getPassword());

        JavaMailSenderImpl otherSender = new JavaMailSenderImpl();
        otherSender.setPassword("password with spaces");
        new CustomEmailService(otherSender, "smtp.example.com", 587, "sender@example.com");

        assertEquals("password with spaces", otherSender.getPassword());
    }

    private static String findHtml(Part part) throws Exception {
        if (part.isMimeType("text/html")) {
            try (InputStream input = part.getInputStream()) {
                return new String(input.readAllBytes(), StandardCharsets.UTF_8);
            }
        }
        Object content = part.getContent();
        if (content instanceof Multipart multipart) {
            for (int index = 0; index < multipart.getCount(); index++) {
                String html = findHtml(multipart.getBodyPart(index));
                if (!html.isEmpty()) {
                    return html;
                }
            }
        }
        return "";
    }
}
