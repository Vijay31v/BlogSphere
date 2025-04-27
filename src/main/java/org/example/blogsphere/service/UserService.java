package org.example.blogsphere.service;

import jakarta.persistence.EntityNotFoundException;
import org.example.blogsphere.entity.*;
import org.example.blogsphere.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.time.LocalDateTime;
import java.util.*;


@Service
@Transactional
public class UserService {
    private final UserRepository userRepository;
    private final BlogPostRepository blogPostRepository;
    private final UserPreferenceRepository userPreferenceRepository;
    private final NotificationRepository notificationRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final NotificationService notificationService;
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    @Autowired
    public UserService(UserRepository userRepository,
                       BlogPostRepository blogPostRepository,
                       UserPreferenceRepository userPreferenceRepository,
                       NotificationRepository noficicationRepository,
                       PasswordResetTokenRepository tokenRepository,
                       EmailService emailService,
                       PasswordEncoder passwordEncoder,
                       NotificationService notificationService) {
        this.userRepository = userRepository;
        this.blogPostRepository = blogPostRepository;
        this.userPreferenceRepository = userPreferenceRepository;
        this.notificationRepository = noficicationRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.notificationService = notificationService;
    }

    public User registerUser(String username, String email, String password, Role role) {
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already exists");
        }

        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already exists");
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole(role);
        user.setCreatedAt(LocalDateTime.now());

        // Initialize user preferences
        UserPreference preferences = new UserPreference();
        preferences.setUser(user);
        user.setPreferences(preferences);

        return userRepository.save(user);
    }

    public User updateUser(User user) {
        // Validate user exists
        if (!userRepository.existsById(user.getId())) {
            throw new EntityNotFoundException("User not found with ID: " + user.getId());
        }

        // Save updated user
        return userRepository.save(user);
    }


    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
    }

    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
    }

    /**
     * Retrieves all users with pagination
     */
    public Page<User> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }
    public Long getUsersCount() {
        return userRepository.count();
    }
    public Long getUsersCreatedInLast30Days() {
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        return userRepository.countNewUsers(thirtyDaysAgo);
    }


    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
    }

    public boolean isUsernameAvailable(String username) {
        return !userRepository.existsByUsername(username);
    }

    public boolean isEmailAvailable(String email) {
        return !userRepository.existsByEmail(email);
    }

    public void updateUserProfile(Long userId, Map<String, String> profileData) {
        User user = getUserById(userId);

        if (profileData.containsKey("fullName")) {
            user.setFullName(profileData.get("fullName"));
        }

        if (profileData.containsKey("bio")) {
            user.setBio(profileData.get("bio"));
        }

        if (profileData.containsKey("location")) {
            user.setLocation(profileData.get("location"));
        }

        if (profileData.containsKey("website")) {
            user.setWebsite(profileData.get("website"));
        }

        if (profileData.containsKey("twitterUsername")) {
            user.setTwitterUsername(profileData.get("twitterUsername"));
        }

        if (profileData.containsKey("profileImage")) {
            user.setProfileImage(profileData.get("profileImage"));
        }

        userRepository.save(user);
    }
    public UserPreference getUserPreferences(Long userId) {
        return userPreferenceRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("User preferences not found"));
    }
    public void updateUserPreferences(Long userId, Map<String, Object> preferences) {
        User user = getUserById(userId);
        UserPreference userPreferences = user.getPreferences();

        if (userPreferences == null) {
            userPreferences = new UserPreference();
            userPreferences.setUser(user);
            user.setPreferences(userPreferences);
        }

        if (preferences.containsKey("emailNewFollower")) {
            userPreferences.setEmailNewFollower((Boolean) preferences.get("emailNewFollower"));
        }

        if (preferences.containsKey("emailNewComment")) {
            userPreferences.setEmailNewComment((Boolean) preferences.get("emailNewComment"));
        }

        if (preferences.containsKey("emailNewLike")) {
            userPreferences.setEmailNewLike((Boolean) preferences.get("emailNewLike"));
        }

        if (preferences.containsKey("emailNewsletter")) {
            userPreferences.setEmailNewsletter((Boolean) preferences.get("emailNewsletter"));
        }


        userPreferenceRepository.save(userPreferences);
    }

    public boolean changePassword(Long userId, String currentPassword, String newPassword) {
        User user = getUserById(userId);

        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            return false;
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        return true;
    }

    /**
     * Generates and stores a password reset token for a user
     */
    public void requestPasswordReset(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User with this email does not exist"));

        String token = generateResetToken();
        tokenRepository.findByUserId(user.getId()).ifPresent(tokenRepository::delete);
        LocalDateTime expirationDate = LocalDateTime.now().plusHours(24);

        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setToken(token);
        resetToken.setUserId(user.getId());
        resetToken.setExpirationDate(expirationDate);
        tokenRepository.save(resetToken);


        // If email service is enabled, send the email
        if (emailService.isEmailServiceEnabled()) {
            emailService.sendPasswordResetEmail(user.getEmail(), token);
        } else {
            // Fallback for development/testing
            logger.info("Password reset token for {}: {}", email, token);
        }
    }

    /**
            * Checks if a password reset token is valid
    */
    public boolean isValidPasswordResetToken(String token) {
        Optional<PasswordResetToken> tokenOpt = tokenRepository.findByToken(token);
        if (tokenOpt.isEmpty()) {
            return false;
        }

        PasswordResetToken resetToken = tokenOpt.get();
        return LocalDateTime.now().isBefore(resetToken.getExpirationDate());

    }

    /**
     * Resets a user's password using a valid token
     */
    public void resetPassword(String token, String newPassword) {
        if (!isValidPasswordResetToken(token)) {
            throw new IllegalArgumentException("Invalid or expired password reset token");
        }

        PasswordResetToken resetToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid token"));
        Long userId = resetToken.getUserId();

        // Delete the token so it can't be used again
        tokenRepository.delete(resetToken);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

    }

    /**
     * Generates a unique token for password reset
     */
    private String generateResetToken() {
        return UUID.randomUUID().toString();
    }

    public void followUser(Long followerId, Long followingId) {
        User follower = userRepository.findById(followerId)
                .orElseThrow(() -> new EntityNotFoundException("Follower not found"));

        User following = userRepository.findById(followingId)
                .orElseThrow(() -> new EntityNotFoundException("User to follow not found"));

        // Don't follow yourself
        if (followerId.equals(followingId)) {
            throw new IllegalArgumentException("You cannot follow yourself");
        }

        follower.follow(following);
        userRepository.save(follower);

        // Create notification for the followed user
        notificationService.createFollowNotification(following, follower);
    }

    public void unfollowUser(Long followerId, Long followingId) {
        User follower = userRepository.findById(followerId)
                .orElseThrow(() -> new EntityNotFoundException("Follower not found"));

        User following = userRepository.findById(followingId)
                .orElseThrow(() -> new EntityNotFoundException("User to unfollow not found"));

        follower.unfollow(following);
        userRepository.save(follower);
    }

    /**
     * Gets the count of unread notifications for a user
     */
    public long getUnreadNotificationsCount(Long userId) {
        // Count unread notifications for the user
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    public List<User> getTopBloggers(int count) {
        return userRepository.findTopBloggers(PageRequest.of(0, count));
    }

    // Get post count for a user
    public int getUserPostCount(Long userId) {
        return blogPostRepository.countByAuthorId(userId);
    }

    // Get follower count for a user
    public int getUserFollowerCount(Long userId) {
        return userRepository.countFollowersByUserId(userId);
    }
    public boolean isFollowing(Long followerId, Long followingId) {
        User follower = getUserById(followerId);
        User following = getUserById(followingId);

        return follower.getFollowing().contains(following);
    }

    public Set<User> getFollowers(Long userId) {
        return getUserById(userId).getFollowers();
    }
    public Set<User> getFollowing(Long userId) {
        return getUserById(userId).getFollowing();
    }

    public void recordLogin(String username) {
        User user = getUserByUsername(username);
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);
    }

    public void deactivateUser(Long userId) {
        User user = getUserById(userId);
        user.setActive(false);
        userRepository.save(user);
    }

    public void activateUser(Long userId) {
        User user = getUserById(userId);
        user.setActive(true);
        userRepository.save(user);
    }

    public Map<String, Object> getUserStats(int year, int month) {
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", userRepository.countByCreatedAtBetween(
                LocalDateTime.of(year, month, 1, 0, 0),
                LocalDateTime.of(year, month, 1, 0, 0).plusMonths(1)
        ));
        stats.put("active", userRepository.countActiveUsers());
        stats.put("newUsers", userRepository.countNewUsers(
                LocalDateTime.now().minusMonths(1)
        ));
        return stats;
    }

}
