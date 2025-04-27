package org.example.blogsphere.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "VARCHAR(50)")
    private Role role;

    // Profile fields
    private String fullName;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String bio;

    private String location;

    private String website;

    private String profileImage;

    private String twitterUsername;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime lastLoginAt;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private UserPreference preferences;

    // Bidirectional relationship with BlogPost
    @OneToMany(mappedBy = "author", cascade = CascadeType.ALL)
    private List<BlogPost> posts = new ArrayList<>();

    // Following relationship
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "user_follows",
            joinColumns = @JoinColumn(name = "follower_id"),
            inverseJoinColumns = @JoinColumn(name = "following_id")
    )
    private Set<User> following = new HashSet<>();

    // Followers relationship
    @ManyToMany(mappedBy = "following", fetch = FetchType.LAZY)
    private Set<User> followers = new HashSet<>();

    // Notifications
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Notification> notifications = new ArrayList<>();

    public User() {
    }

    public User(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public List<BlogPost> getPosts() {
        return posts;
    }

    public void setPosts(List<BlogPost> posts) {
        this.posts = posts;
    }

    public Set<User> getFollowing() {
        return following;
    }

    public void setFollowing(Set<User> following) {
        this.following = following;
    }

    public Set<User> getFollowers() {
        return followers;
    }

    public void setFollowers(Set<User> followers) {
        this.followers = followers;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public String getProfileImage() {
        return profileImage;
    }

    public void setProfileImage(String profileImage) {
        this.profileImage = profileImage;
    }

    public String getTwitterUsername() {
        return twitterUsername;
    }

    public void setTwitterUsername(String twitterUsername) {
        this.twitterUsername = twitterUsername;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(LocalDateTime lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }

    public UserPreference getPreferences() {
        return preferences;
    }

    public void setPreferences(UserPreference preferences) {
        this.preferences = preferences;
    }

    public List<Notification> getNotifications() {
        return notifications;
    }

    public void setNotifications(List<Notification> notifications) {
        this.notifications = notifications;
    }

    // Convenience methods
    public void follow(User userToFollow) {
        if (this != userToFollow) {
            userToFollow.getFollowers().add(this);
            this.getFollowing().add(userToFollow);
        }
    }

    public void unfollow(User userToUnfollow) {
        userToUnfollow.getFollowers().remove(this);
        this.getFollowing().remove(userToUnfollow);
    }

    public void initializePreferences() {
        if (this.preferences == null) {
            UserPreference newPreferences = new UserPreference();
            newPreferences.setUser(this);
            this.preferences = newPreferences;
        }
    }
}
