package org.example.blogsphere.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "user_preferences")
public class UserPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private boolean emailNewFollower = true;

    @Column(nullable = false)
    private boolean emailNewComment = true;

    @Column(nullable = false)
    private boolean emailNewLike = false;

    @Column(nullable = false)
    private boolean emailNewsletter = true;

    // Constructors
    public UserPreference() {
    }


    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public boolean isEmailNewFollower() {
        return emailNewFollower;
    }

    public void setEmailNewFollower(boolean emailNewFollower) {
        this.emailNewFollower = emailNewFollower;
    }

    public boolean isEmailNewComment() {
        return emailNewComment;
    }

    public void setEmailNewComment(boolean emailNewComment) {
        this.emailNewComment = emailNewComment;
    }

    public boolean isEmailNewLike() {
        return emailNewLike;
    }

    public void setEmailNewLike(boolean emailNewLike) {
        this.emailNewLike = emailNewLike;
    }

    public boolean isEmailNewsletter() {
        return emailNewsletter;
    }

    public void setEmailNewsletter(boolean emailNewsletter) {
        this.emailNewsletter = emailNewsletter;
    }

}
