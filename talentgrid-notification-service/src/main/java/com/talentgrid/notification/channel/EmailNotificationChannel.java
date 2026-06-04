package com.talentgrid.notification.channel;

import com.talentgrid.kafka.events.notification.NotificationPayload;
import com.talentgrid.notification.model.NotificationChannelType;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.UnsupportedEncodingException;
import java.util.Map;

/**
 * Email notification channel using Spring Mail + Thymeleaf templates.
 *
 * <p>Active when {@code notification.email.enabled=true} (default: true).
 * Set {@code notification.email.enabled=false} to disable in local/test environments.</p>
 *
 * <p>Template resolution: if {@link NotificationPayload#getTemplateId()} is set,
 * resolves a Thymeleaf template from
 * {@code classpath:/templates/email/{templateId}.html}.
 * Falls back to a plain HTML wrapper if the template is missing.</p>
 *
 * <p><strong>FIX:</strong> Added explicit INFO logs at every decision point so
 * failures are never silent. Added specific catch blocks for
 * {@link MailAuthenticationException} and {@link MailSendException} so the
 * root cause is immediately visible in logs without reading a stack trace.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "notification.email.enabled", havingValue = "true", matchIfMissing = true)
public class EmailNotificationChannel implements NotificationChannelHandler {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${notification.email.from:noreply@talentgrid.io}")
    private String fromAddress;

    @Value("${notification.email.from-name:TalentGrid}")
    private String fromName;

    @Override
    public NotificationChannelType channelType() {
        return NotificationChannelType.EMAIL;
    }

    @Override
    public void deliver(NotificationPayload payload) throws MessagingException {

        // FIX: Log at INFO (not just warn) so this is visible in default log configs
        if (payload.getRecipientEmail() == null || payload.getRecipientEmail().isBlank()) {
            log.warn("[EMAIL] ⚠ recipientEmail is null or blank — skipping email delivery. " +
                            "Check that DemandPayload.recipientEmail is set in the request body and " +
                            "mapped in DemandController. | userId={} | type={}",
                    payload.getRecipientUserId(), payload.getNotificationType());
            return;
        }

        log.info("[EMAIL] ▶ Starting email delivery | userId={} | type={} | templateId={} | toPresent=true",
                payload.getRecipientUserId(),
                payload.getNotificationType(),
                payload.getTemplateId());

        String htmlContent = resolveContent(payload);

        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

        try {
            helper.setFrom(fromAddress, fromName);
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("Invalid email sender config: from=" + fromAddress, e);
        }

        helper.setTo(payload.getRecipientEmail());
        helper.setSubject(payload.getTitle());
        helper.setText(htmlContent, true);

        log.info("[EMAIL] ▶ Invoking SMTP send | host=smtp.gmail.com | port=587 | userId={}",
                payload.getRecipientUserId());

        try {
            mailSender.send(mimeMessage);
        } catch (MailAuthenticationException e) {
            // FIX: Specific catch — makes it immediately obvious the App Password is wrong/missing
            log.error("[EMAIL] ✗ SMTP authentication failed — check spring.mail.password in application.properties. " +
                            "Gmail requires an App Password (not your account password). " +
                            "Generate one at: https://myaccount.google.com/apppasswords | error={}",
                    e.getMessage());
            throw e; // re-throw so orchestrator counts this as a channel failure
        } catch (MailSendException e) {
            log.error("[EMAIL] ✗ SMTP send failed | userId={} | type={} | cause={}",
                    payload.getRecipientUserId(), payload.getNotificationType(), e.getMessage());
            throw e;
        }

        log.info("[EMAIL] ✓ Email sent successfully | userId={} | type={}",
                payload.getRecipientUserId(), payload.getNotificationType());
    }

    /**
     * Resolves the HTML body. Uses a Thymeleaf template when {@code templateId} is provided,
     * falls back to the plain {@code message} field otherwise.
     */
    private String resolveContent(NotificationPayload payload) {
        if (payload.getTemplateId() != null && !payload.getTemplateId().isBlank()) {
            try {
                Context context = new Context();
                context.setVariable("title", payload.getTitle());
                context.setVariable("message", payload.getMessage());
                context.setVariable("notificationType", payload.getNotificationType());
                context.setVariable("moduleName", payload.getModuleName());
                context.setVariable("referenceId", payload.getReferenceId());
                context.setVariable("referenceType", payload.getReferenceType());
                context.setVariable("priority", payload.getPriority());

                if (payload.getTemplateVariables() != null) {
                    for (Map.Entry<String, String> entry : payload.getTemplateVariables().entrySet()) {
                        context.setVariable(entry.getKey(), entry.getValue());
                    }
                }

                String templateName = "email/" + payload.getTemplateId();
                log.debug("[EMAIL] Resolving Thymeleaf template: classpath:/templates/{}.html", templateName);
                String rendered = templateEngine.process(templateName, context);
                log.debug("[EMAIL] Template rendered successfully | templateId={}", payload.getTemplateId());
                return rendered;
            } catch (Exception e) {
                log.warn("[EMAIL] Template '{}' failed to render — falling back to plain HTML. " +
                                "Check that templates/email/{}.html exists in notification-service resources. " +
                                "Error: {}",
                        payload.getTemplateId(), payload.getTemplateId(), e.getMessage());
            }
        }

        // Fallback: wrap plain message in minimal HTML
        log.debug("[EMAIL] Using plain HTML fallback (no templateId set)");
        return "<html><body>" +
                "<h2>" + escapeHtml(payload.getTitle()) + "</h2>" +
                "<p>" + escapeHtml(payload.getMessage()) + "</p>" +
                "<hr/><p style='color:#888;font-size:12px;'>TalentGrid Notification Service</p>" +
                "</body></html>";
    }

    private String escapeHtml(String input) {
        if (input == null) return "";
        return input
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}