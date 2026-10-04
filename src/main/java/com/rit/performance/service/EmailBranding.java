package com.rit.performance.service;

import jakarta.mail.MessagingException;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.web.util.HtmlUtils;

/** Shared MIME branding for transactional emails and queued notifications. */
final class EmailBranding {
    private static final String LOGO = "<div style=\"background:#fff;padding:20px;\">"
            + "<img src=\"cid:rit-logo\" width=\"270\" alt=\"RailInfo Tech\" "
            + "style=\"display:block;width:270px;max-width:100%;height:auto;border:0;\"></div>";

    private EmailBranding() { }

    static void setContent(MimeMessageHelper message, String body, boolean html) throws MessagingException {
        String content = body == null ? "" : body;
        if (!html) {
            content = "<!doctype html><html lang=\"en\"><body style=\"font-family:Arial,sans-serif;color:#172033;\">"
                    + LOGO + "<div style=\"padding:20px;white-space:pre-wrap;line-height:1.6;\">"
                    + HtmlUtils.htmlEscape(content) + "</div></body></html>";
        } else if (!content.contains("cid:rit-logo")) {
            // Older queued HTML may predate the shared template header.
            var bodyTag = java.util.regex.Pattern.compile("<body\\b[^>]*>",
                    java.util.regex.Pattern.CASE_INSENSITIVE).matcher(content);
            content = bodyTag.find()
                    ? content.substring(0, bodyTag.end()) + LOGO + content.substring(bodyTag.end())
                    : LOGO + content;
        }
        message.setText(content, true);
        message.addInline("rit-logo", new ClassPathResource("email/rit-logo.png"), "image/png");
    }
}
