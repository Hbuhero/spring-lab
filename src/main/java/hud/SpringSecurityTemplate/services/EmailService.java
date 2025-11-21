package hud.SpringSecurityTemplate.services;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Map;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private TemplateEngine templateEngine;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.name:ShambaBoraGateway}")
    private String appName;

    @Value("${app.url:http://localhost:8080}")
    private String appUrl;

    /**
     * Send a simple text email
     */
    public void sendSimpleEmail(String to, String subject, String text) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(text, false);
            
            mailSender.send(message);
            logger.info("Simple email sent successfully to: {}", to);
        } catch (MessagingException | MailException e) {
            logger.error("Failed to send simple email to: {}. Error: {}", to, e.getMessage());
        }
    }

    /**
     * Send HTML email using template
     */
    public void sendHtmlEmail(String to, String subject, String templateName, Map<String, Object> variables) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            // Add common variables
            variables.put("appName", appName);
            variables.put("appUrl", appUrl);
            variables.put("year", java.time.Year.now().getValue());
            
            Context context = new Context();
            context.setVariables(variables);
            
            String htmlContent = templateEngine.process(templateName, context);
            
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            
            mailSender.send(message);
            logger.info("HTML email sent successfully to: {} using template: {}", to, templateName);
        } catch (MessagingException | MailException e) {
            logger.error("Failed to send HTML email to: {} using template: {}. Error: {}", to, templateName, e.getMessage());
        }
    }

    /**
     * Send welcome email after successful registration
     */
    @Async
    public void sendWelcomeEmail(String to, String firstName, String lastName) {
        try {
            Map<String, Object> variables = Map.of(
                "firstName", firstName != null ? firstName : "",
                "lastName", lastName != null ? lastName : "",
                "fullName", (firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "").trim()
            );
            
             sendHtmlEmail(to, "Welcome to " + appName, "welcome-email", variables);
        } catch (Exception e) {
            logger.error("Failed to send welcome email to: {}. Error: {}", to, e.getMessage());
             sendSimpleEmail(to, "Welcome to " + appName,
                "Dear " + (firstName != null ? firstName : "User") + ",\n\n" +
                "Welcome to " + appName + "! Your account has been successfully created.\n\n" +
                "Best regards,\n" + appName + " Team");
        }
    }

    /**
     * Send password reset email
     */
    @Async
    public void sendPasswordResetEmail(String to, String firstName, String resetToken) {
        try {
            String resetUrl = appUrl + "/reset-password?token=" + resetToken;
            
            Map<String, Object> variables = Map.of(
                "firstName", firstName != null ? firstName : "User",
                "resetUrl", resetUrl,
                "resetToken", resetToken
            );
            
             sendHtmlEmail(to, "Password Reset Request - " + appName, "password-reset-email", variables);
        } catch (Exception e) {
            logger.error("Failed to send password reset email to: {}. Error: {}", to, e.getMessage());
            String resetUrl = appUrl + "/reset-password?token=" + resetToken;
             sendSimpleEmail(to, "Password Reset Request - " + appName,
                "Dear " + (firstName != null ? firstName : "User") + ",\n\n" +
                "You have requested to reset your password. Please click the link below to reset your password:\n\n" +
                resetUrl + "\n\n" +
                "This link will expire in 24 hours.\n\n" +
                "If you did not request this password reset, please ignore this email.\n\n" +
                "Best regards,\n" + appName + " Team");
        }
    }

    /**
     * Send password change confirmation email
     */
    @Async
    public void sendPasswordChangeConfirmationEmail(String to, String firstName) {
        try {
            Map<String, Object> variables = Map.of(
                "firstName", firstName != null ? firstName : "User"
            );
            
             sendHtmlEmail(to, "Password Changed Successfully - " + appName, "password-change-confirmation", variables);
        } catch (Exception e) {
            logger.error("Failed to send password change confirmation email to: {}. Error: {}", to, e.getMessage());
             sendSimpleEmail(to, "Password Changed Successfully - " + appName,
                "Dear " + (firstName != null ? firstName : "User") + ",\n\n" +
                "Your password has been successfully changed.\n\n" +
                "If you did not make this change, please contact our support team immediately.\n\n" +
                "Best regards,\n" + appName + " Team");
        }
    }

    /**
     * Send account status change notification
     */
    @Async
    public void sendAccountStatusEmail(String to, String firstName, String status, String reason) {
        try {
            Map<String, Object> variables = Map.of(
                "firstName", firstName != null ? firstName : "User",
                "status", status,
                "reason", reason != null ? reason : "Administrative decision"
            );
            
            sendHtmlEmail(to, "Account Status Update - " + appName, "account-status-email", variables);
        } catch (Exception e) {
            logger.error("Failed to send account status email to: {}. Error: {}", to, e.getMessage());
            sendSimpleEmail(to, "Account Status Update - " + appName,
                "Dear " + (firstName != null ? firstName : "User") + ",\n\n" +
                "Your account status has been updated to: " + status + "\n\n" +
                "Reason: " + (reason != null ? reason : "Administrative decision") + "\n\n" +
                "If you have any questions, please contact our support team.\n\n" +
                "Best regards,\n" + appName + " Team");
        }
    }

    /**
     * Send temporary password email (admin reset)
     */
    @Async
    public void sendTemporaryPasswordEmail(String to, String firstName, String temporaryPassword) {
        try {
            Map<String, Object> variables = Map.of(
                "firstName", firstName != null ? firstName : "User",
                "temporaryPassword", temporaryPassword,
                "loginUrl", appUrl + "/login"
            );
            
            sendHtmlEmail(to, "Temporary Password - " + appName, "temporary-password-email", variables);
        } catch (Exception e) {
            logger.error("Failed to send temporary password email to: {}. Error: {}", to, e.getMessage());
            sendSimpleEmail(to, "Temporary Password - " + appName,
                "Dear " + (firstName != null ? firstName : "User") + ",\n\n" +
                "Your password has been reset by an administrator.\n\n" +
                "Your temporary password is: " + temporaryPassword + "\n\n" +
                "Please log in and change your password immediately.\n\n" +
                "Login URL: " + appUrl + "/login\n\n" +
                "Best regards,\n" + appName + " Team");
        }
    }

    /**
     * Send verification email
     */
    @Async
    public void sendVerificationEmail(String to, String firstName, String verificationToken) {
        try {
            String verificationUrl = appUrl + "/verify-email?token=" + verificationToken;
            
            Map<String, Object> variables = Map.of(
                "firstName", firstName != null ? firstName : "User",
                "verificationUrl", verificationUrl,
                "verificationToken", verificationToken
            );
            
            sendHtmlEmail(to, "Email Verification - " + appName, "email-verification", variables);
        } catch (Exception e) {
            logger.error("Failed to send verification email to: {}. Error: {}", to, e.getMessage());
            String verificationUrl = appUrl + "/verify-email?token=" + verificationToken;
            sendSimpleEmail(to, "Email Verification - " + appName,
                "Dear " + (firstName != null ? firstName : "User") + ",\n\n" +
                "Please verify your email address by clicking the link below:\n\n" +
                verificationUrl + "\n\n" +
                "This link will expire in 24 hours.\n\n" +
                "Best regards,\n" + appName + " Team");
        }
    }

    /**
     * Send generic notification email
     */
    @Async
    public void sendNotificationEmail(String to, String subject, String message, String firstName) {
        try {
            Map<String, Object> variables = Map.of(
                "firstName", firstName != null ? firstName : "User",
                "message", message,
                "subject", subject
            );
            
            sendHtmlEmail(to, subject, "notification-email", variables);
        } catch (Exception e) {
            logger.error("Failed to send notification email to: {}. Error: {}", to, e.getMessage());
            sendSimpleEmail(to, subject,
                "Dear " + (firstName != null ? firstName : "User") + ",\n\n" +
                message + "\n\n" +
                "Best regards,\n" + appName + " Team");
        }
    }
}
