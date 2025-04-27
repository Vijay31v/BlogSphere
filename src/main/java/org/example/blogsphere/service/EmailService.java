package org.example.blogsphere.service;

public interface EmailService {
    void sendPasswordResetEmail(String to, String token);
    void sendWelcomeEmail(String to, String username);
    void sendCommentNotification(String to, String commenterName, String postTitle, String commentContent, String postUrl);
    void sendFollowNotification(String to, String followerName, String profileUrl);
    void sendNewPostToFollower(String to, String followerName, String authorName, String postTitle, String postUrl);
    boolean isEmailServiceEnabled();
}
