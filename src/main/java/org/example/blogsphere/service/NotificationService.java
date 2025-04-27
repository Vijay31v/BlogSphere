package org.example.blogsphere.service;

import org.example.blogsphere.entity.Notification;
import org.example.blogsphere.entity.Notification.NotificationType;
import org.example.blogsphere.entity.User;
import org.example.blogsphere.entity.UserPreference;
import org.example.blogsphere.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class NotificationService {
    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final EmailService emailService;
    private final SimpMessagingTemplate messagingTemplate;

    @Value("${app.base-url}")
    private String baseUrl;

    @Autowired
    public NotificationService(NotificationRepository notificationRepository,
                               EmailService emailService,
                               SimpMessagingTemplate messagingTemplate) {
        this.notificationRepository = notificationRepository;
        this.emailService = emailService;
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Creates a notification for a user and sends it through appropriate channels
     * based on user preferences
     */
    @Transactional
    public Notification createNotification(User recipient, NotificationType type,
                                           String message, String link) {
        // Create base notification

        Notification notification = new Notification();
        notification.setUser(recipient);
        notification.setType(type);
        notification.setMessage(message);
        notification.setLink(baseUrl+link);
        notification.setRead(false);
        notification.setCreatedAt(LocalDateTime.now());

        // Save to database
        notification = notificationRepository.save(notification);
        logger.info("Created {} notification for user {}: {}",
                type, recipient.getUsername(), message);

        // Send real-time notification if user is online
        sendRealTimeNotification(notification);

        // Check user preferences and send email if appropriate
        sendEmailBasedOnPreferences(recipient, notification);

        return notification;
    }

    /**
     * Send batch notifications to multiple users
     */
    @Transactional
    public void createBatchNotifications(List<User> recipients, NotificationType type,
                                         String message, String link) {
        for (User recipient : recipients) {
            createNotification(recipient, type, message, link);
        }
    }

    /**
     * Send a notification about a new comment
     */
    @Transactional
    public Notification createCommentNotification(User postAuthor, User commenter,
                                                  String postTitle, String commentContent,
                                                  String postUrl) {
        if (postAuthor.getId().equals(commenter.getId())) {
            // Don't notify users about their own comments
            return null;
        }

        String message = commenter.getUsername() + " commented on your post: " + postTitle;
        return createNotification(postAuthor, NotificationType.COMMENT, message, postUrl);
    }

    /**
     * Send a notification about a new like
     */
    @Transactional
    public Notification createLikeNotification(User postAuthor, User liker,
                                               String postTitle, String postUrl) {
        if (postAuthor.getId().equals(liker.getId())) {
            // Don't notify users about their own likes
            return null;
        }

        String message = liker.getUsername() + " liked your post: " + postTitle;
        return createNotification(postAuthor, NotificationType.LIKE, message, postUrl);
    }

    /**
     * Send a notification about a new follower
     */
    @Transactional
    public Notification createFollowNotification(User followed, User follower) {
        String message = follower.getUsername() + " started following you";
        String link = "/profile/" + follower.getUsername();
        return createNotification(followed, NotificationType.FOLLOW, message, link);
    }

    /**
     * Send a system notification to a user
     */
    @Transactional
    public Notification createSystemNotification(User recipient, String message, String link) {
        return createNotification(recipient, NotificationType.SYSTEM, message, link);
    }

    /**
     * Get paginated notifications for a user
     */
    public Page<Notification> getUserNotifications(Long userId, Pageable pageable) {
        return notificationRepository.findByUserId(userId, pageable);
    }
    public List<Notification> getRecentNotifications(Long userId, int limit) {
        PageRequest pageRequest = PageRequest.of(0, limit, Sort.by("createdAt").descending());
        return notificationRepository.findByUserId(userId, pageRequest).getContent();
    }
    /**
     * Get unread notifications for a user
     */
    public List<Notification> getUnreadNotifications(Long userId) {
        return notificationRepository.findByUserIdAndReadFalse(userId);
    }

    /**
     * Count unread notifications for a user
     */
    public int countUnreadNotifications(Long userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    /**
     * Mark notification as read
     */
    @Transactional
    public void markAsRead(Long notificationId) {
        Optional<Notification> notificationOpt = notificationRepository.findById(notificationId);
        notificationOpt.ifPresent(notification -> {
            notification.setRead(true);
            notificationRepository.save(notification);
        });
    }

    /**
     * Mark all notifications for a user as read
     */
    @Transactional
    public void markAllAsRead(Long userId) {
        List<Notification> unreadNotifications = notificationRepository.findByUserIdAndReadFalse(userId);
        for (Notification notification : unreadNotifications) {
            notification.setRead(true);
            notificationRepository.save(notification);
        }
    }

    @Transactional
    public void markAsUnread(Long notificationId) {
        Optional<Notification> notificationOpt = notificationRepository.findById(notificationId);
        notificationOpt.ifPresent(notification -> {
            notification.setRead(false);
            notificationRepository.save(notification);
        });
    }


    /**
     * Send real-time notification via WebSocket
     */
    private void sendRealTimeNotification(Notification notification) {
        System.out.println("Sending real-time notification to user " + notification.getUser().getId() + " ...");
        try {
            String destination = "/user/" + notification.getUser().getUsername() + "/queue/notifications";
            System.out.println("Sending real-time notification to destination " + destination);
            messagingTemplate.convertAndSendToUser(
                    notification.getUser().getUsername(),
                    "/queue/notifications",
                    createNotificationDTO(notification)
            );
            System.out.println("Sent real-time notification to destination " + destination);
            logger.debug("Sent real-time notification to user {}", notification.getUser().getId());
        } catch (Exception e) {
            logger.error("Failed to send real-time notification", e);
        }
    }

    /**
     * Create a simplified DTO for sending over WebSocket
     */
    private Map<String, Object> createNotificationDTO(Notification notification) {
        return Map.of(
                "id", notification.getId(),
                "type", notification.getType().toString(),
                "message", notification.getMessage(),
                "link", notification.getLink(),
                "createdAt", notification.getCreatedAt().toString()
        );
    }

    /**
     * Check user preferences and send email notification if enabled
     */
    private void sendEmailBasedOnPreferences(User recipient, Notification notification) {
        // Skip if email service is disabled
        if (!emailService.isEmailServiceEnabled()) {
            return;
        }

        UserPreference prefs = recipient.getPreferences();
        if (prefs == null) {
            // No preferences set, use defaults
            return;
        }

        // Default to not sending email
        boolean shouldSendEmail = false;

        // Check which type of notification it is and if email is enabled for that type
        switch (notification.getType()) {
            case COMMENT:
                shouldSendEmail = prefs.isEmailNewComment();
                if (shouldSendEmail) {
                    // Extract the commenter name and post title from the message
                    String message = notification.getMessage();
                    int commentedIndex = message.indexOf(" commented on");
                    int postIndex = message.indexOf(": ");

                    if (commentedIndex > 0 && postIndex > 0) {
                        String commenterName = message.substring(0, commentedIndex);
                        String postTitle = message.substring(postIndex + 2);

                        // Send email notification
                        emailService.sendCommentNotification(
                                recipient.getEmail(),
                                commenterName,
                                postTitle,
                                "", // We don't have the actual comment content here
                                notification.getLink()
                        );
                    }
                }
                break;

            case LIKE:
                shouldSendEmail = prefs.isEmailNewLike();
                // Implement email sending for likes if needed
                break;

            case FOLLOW:
                shouldSendEmail = prefs.isEmailNewFollower();
                if (shouldSendEmail) {
                    String message = notification.getMessage();
                    int followedIndex = message.indexOf(" started following you");

                    if (followedIndex > 0) {
                        String followerName = message.substring(0, followedIndex);
                        String profileUrl = notification.getLink();
                        emailService.sendFollowNotification(
                                recipient.getEmail(),
                                followerName,
                                profileUrl
                        );
                    }
                }
                break;
            case POST:
                shouldSendEmail = prefs.isEmailNewsletter();
                if (shouldSendEmail) {
                    // Parse notification message to extract author and post title
                    String message = notification.getMessage();
                    String[] parts = message.split(" published a new post: ");
                    if (parts.length == 2) {
                        String authorName = parts[0];
                        String postTitle = parts[1];
                        String postUrl = notification.getLink();

                        emailService.sendNewPostToFollower(
                                recipient.getEmail(),
                                recipient.getUsername(),
                                authorName,
                                postTitle,
                                postUrl
                        );
                    }
                }
                break;

            case SYSTEM:
                // System notifications are typically always sent
                shouldSendEmail = true;
                // Implement system notification email if needed
                break;
        }

        if (shouldSendEmail) {
            logger.info("Sending email notification to {} for {} notification",
                    recipient.getEmail(), notification.getType());
        }
    }

    /**
     * Delete a notification
     */
    @Transactional
    public void deleteNotification(Long notificationId) {
        notificationRepository.deleteById(notificationId);
    }

    /**
     * Delete all notifications for a user
     */
    @Transactional
    public void deleteAllUserNotifications(Long userId) {
        notificationRepository.deleteByUserId(userId);
    }
}
