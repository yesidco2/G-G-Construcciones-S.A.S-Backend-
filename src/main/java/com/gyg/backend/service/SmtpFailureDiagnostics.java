package com.gyg.backend.service;

import jakarta.mail.MessagingException;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSendException;

import javax.net.ssl.SSLException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SmtpFailureDiagnostics {

    private static final int MAX_CAUSES = 12;
    private static final Pattern SMTP_STATUS_CODE =
            Pattern.compile("^\\s*([45]\\d\\d)(?=[\\s-])");
    private static final Pattern SMTP_EXCEPTION_STATUS_CODE =
            Pattern.compile("(?<!\\d)([45]\\d\\d)(?=[\\s:-]|$)");

    private SmtpFailureDiagnostics() {
    }

    public static String describe(Throwable failure) {
        List<Throwable> causes = collectCauses(failure);
        String names = causes.stream()
                .map(cause -> cause.getClass().getSimpleName())
                .reduce((first, second) -> first + " -> " + second)
                .orElse("none");
        return "category=" + category(causes)
                + ", smtpCode=" + smtpCode(causes)
                + ", causes=[" + names + "]";
    }

    private static List<Throwable> collectCauses(Throwable failure) {
        if (failure == null) {
            return List.of();
        }
        List<Throwable> causes = new ArrayList<>();
        Deque<Throwable> pending = new ArrayDeque<>();
        Set<Throwable> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        pending.add(failure);

        while (!pending.isEmpty() && causes.size() < MAX_CAUSES) {
            Throwable current = pending.removeFirst();
            if (!seen.add(current)) {
                continue;
            }
            causes.add(current);
            addIfPresent(pending, current.getCause());
            for (Throwable suppressed : current.getSuppressed()) {
                addIfPresent(pending, suppressed);
            }
            if (current instanceof MessagingException messagingException) {
                addIfPresent(pending, messagingException.getNextException());
            }
        }
        return causes;
    }

    private static void addIfPresent(Deque<Throwable> pending, Throwable exception) {
        if (exception != null) {
            pending.addLast(exception);
        }
    }

    private static String category(List<Throwable> causes) {
        if (causes.stream().anyMatch(cause ->
                cause instanceof SSLException
                        || cause.getClass().getSimpleName().toUpperCase(Locale.ROOT).contains("TLS"))) {
            return "TLS";
        }
        if (causes.stream().anyMatch(cause ->
                cause instanceof MailAuthenticationException
                        || cause.getClass().getSimpleName().contains("AuthenticationFailed"))) {
            return "AUTHENTICATION";
        }
        if (causes.stream().anyMatch(cause ->
                cause instanceof ConnectException
                        || cause instanceof SocketTimeoutException
                        || cause instanceof UnknownHostException
                        || cause.getClass().getSimpleName().contains("MailConnect"))) {
            return "CONNECTION";
        }
        if (causes.stream().anyMatch(cause ->
                cause instanceof MailSendException || cause instanceof MessagingException)) {
            return "SEND";
        }
        if (causes.stream().anyMatch(cause -> cause instanceof MailException)) {
            return "MAIL";
        }
        if (causes.stream().anyMatch(cause ->
                cause instanceof IllegalArgumentException || cause instanceof IllegalStateException)) {
            return "CONFIGURATION";
        }
        return "OTHER";
    }

    private static String smtpCode(List<Throwable> causes) {
        for (Throwable cause : causes) {
            String message = cause.getMessage();
            if (message == null) {
                continue;
            }
            Pattern pattern = cause.getClass().getSimpleName().startsWith("SMTP")
                    ? SMTP_EXCEPTION_STATUS_CODE
                    : SMTP_STATUS_CODE;
            Matcher matcher = pattern.matcher(message);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        return "none";
    }
}
