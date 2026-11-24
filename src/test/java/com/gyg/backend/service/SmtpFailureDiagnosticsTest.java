package com.gyg.backend.service;

import jakarta.mail.MessagingException;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailSendException;

import javax.net.ssl.SSLHandshakeException;
import java.net.ConnectException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SmtpFailureDiagnosticsTest {

    @Test
    void shouldReportNestedAuthenticationFailureAndSmtpCodeWithoutSensitiveText() {
        MessagingException smtpResponse = new MessagingException(
                "535-5.7.8 Authentication rejected for private@example.com secret=do-not-log "
                        + "https://example.com/reset?token=private");
        MailAuthenticationException authenticationFailure =
                new MailAuthenticationException("SMTP authentication failed", smtpResponse);

        String diagnostics = SmtpFailureDiagnostics.describe(
                new EmailDeliveryException(authenticationFailure));

        assertTrue(diagnostics.contains("category=AUTHENTICATION"));
        assertTrue(diagnostics.contains("smtpCode=535"));
        assertTrue(diagnostics.contains("MessagingException"));
        assertFalse(diagnostics.contains("private@example.com"));
        assertFalse(diagnostics.contains("do-not-log"));
        assertFalse(diagnostics.contains("https://"));
    }

    @Test
    void shouldDistinguishTlsAndConnectionFailures() {
        String tls = SmtpFailureDiagnostics.describe(
                new EmailDeliveryException(new SSLHandshakeException("certificate validation failed")));
        String connection = SmtpFailureDiagnostics.describe(
                new EmailDeliveryException(new MailSendException(
                        "Sending failed", new ConnectException("connection refused"))));

        assertTrue(tls.contains("category=TLS"));
        assertTrue(connection.contains("category=CONNECTION"));
    }
}
