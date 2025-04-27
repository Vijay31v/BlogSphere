package org.example.blogsphere.service.impl;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.example.blogsphere.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailServiceImpl.class);

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final boolean emailEnabled;

    @Value("${spring.mail.username:}")
    private String senderEmail;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    @Value("classpath:static/img/logo.png")
    private Resource logoResource;


    @Autowired
    public EmailServiceImpl(JavaMailSender mailSender, TemplateEngine templateEngine,
                            @Value("${app.email.enabled:false}") boolean emailEnabled) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.emailEnabled = emailEnabled;
    }

    @Override
    public boolean isEmailServiceEnabled() {
        return emailEnabled;
    }

    @Async
    @Override
    public void sendPasswordResetEmail(String to, String token) {
        if (!emailEnabled) {
            logger.info("Email service disabled. Would have sent password reset to: {} with token: {}", to, token);
            return;
        }

        try {
            Context context = new Context();
            context.setVariable("token", token);
            context.setVariable("resetUrl", baseUrl + "/password/reset?token=" + token);
            context.setVariable("expiryTime", "24 hours");

            String emailContent = templateEngine.process("email/password-reset", context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(senderEmail);
            helper.setTo(to);
            helper.setSubject("BlogSphere - Password Reset Request");
            helper.setText(emailContent, true);
            helper.addInline("logo", logoResource, "image/png");


            try {
                mailSender.send(message);
                logger.info("Email sent to: {}", to);
            } catch (MailAuthenticationException e) {
                logger.error("Email authentication failed. Check your username/password", e);
            } catch (MailSendException e) {
                logger.error("Failed to connect to mail server. Check your connection settings", e);
            }

            logger.info("Password reset email sent to: {}", to);
        } catch (Exception e) {
            logger.error("Failed to send password reset email to: {}", to, e);
        }
    }

    @Async
    @Override
    public void sendWelcomeEmail(String to, String username) {
        if (!emailEnabled) {
            logger.info("Email service disabled. Would have sent welcome email to: {}", to);
            return;
        }

        try {
            Context context = new Context();
            context.setVariable("username", username);
            context.setVariable("loginUrl", baseUrl + "/login");

            String emailContent = templateEngine.process("email/welcome", context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(senderEmail);
            helper.setTo(to);
            helper.setSubject("Welcome to BlogSphere!");
            helper.setText(emailContent, true);
            helper.addInline("logo", logoResource, "image/png");


            mailSender.send(message);
            logger.info("Welcome email sent to: {}", to);
        } catch (MessagingException e) {
            logger.error("Failed to send welcome email to: {}", to, e);
        }
    }

    @Async
    @Override
    public void sendFollowNotification(String to, String followerName, String profileUrl) {
        if (!emailEnabled) {
            logger.info("Email service disabled. Would have sent follow notification to: {}", to);
            return;
        }

        try {
            Context context = new Context();
            context.setVariable("followerName", followerName);
            context.setVariable("profileUrl", profileUrl);
            context.setVariable("preferenceEditUrl", baseUrl+"/profile/edit");

            String emailContent = templateEngine.process("email/follow-notification", context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(senderEmail);
            helper.setTo(to);
            helper.setSubject(followerName + " started following you on BlogSphere");
            helper.setText(emailContent, true);
            helper.addInline("logo", logoResource, "image/png");

            mailSender.send(message);
            logger.info("Follow notification email sent to: {}", to);
        } catch (Exception e) {
            logger.error("Failed to send follow notification email to: {}", to, e);
        }
    }

    @Async
    @Override
    public void sendNewPostToFollower(String to, String followerName, String authorName, String postTitle, String postUrl) {
        if (!emailEnabled) {
            logger.info("Email service disabled. Would have sent new post notification to: {}", to);
            return;
        }

        try {
            Context context = new Context();
            context.setVariable("followerName", followerName);
            context.setVariable("authorName", authorName);
            context.setVariable("postTitle", postTitle);
            context.setVariable("postUrl", postUrl);
            context.setVariable("subscriptionUrl", baseUrl+"/subscriptions");

            String emailContent = templateEngine.process("email/new-post-notification", context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(senderEmail);
            helper.setTo(to);
            helper.setSubject(authorName + " published a new post: " + postTitle);
            helper.setText(emailContent, true);
            helper.addInline("logo", logoResource, "image/png");

            mailSender.send(message);
            logger.info("New post notification sent to follower: {}", to);
        } catch (Exception e) {
            logger.error("Failed to send new post notification to: {}", to, e);
        }
    }

    @Async
    @Override
    public void sendCommentNotification(String to, String commenterName, String postTitle,
                                        String commentContent, String postUrl) {
        if (!emailEnabled) {
            logger.info("Email service disabled. Would have sent comment notification to: {}", to);
            return;
        }

        try {
            Context context = new Context();
            context.setVariable("commenterName", commenterName);
            context.setVariable("postTitle", postTitle);
            context.setVariable("commentContent", commentContent);
            context.setVariable("postUrl", postUrl);

            String emailContent = templateEngine.process("email/comment-notification", context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(senderEmail);
            helper.setTo(to);
            helper.setSubject("New Comment on Your BlogSphere Post: " + postTitle);
            helper.setText(emailContent, true);
            helper.addInline("logo", logoResource, "image/png");


            mailSender.send(message);
            logger.info("Comment notification email sent to: {}", to);
        } catch (MessagingException e) {
            logger.error("Failed to send comment notification email to: {}", to, e);
        }
    }
}
