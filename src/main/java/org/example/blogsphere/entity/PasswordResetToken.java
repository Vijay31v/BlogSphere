package org.example.blogsphere.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "password_reset_tokens")
public class PasswordResetToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String token;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private LocalDateTime expirationDate;

    // Constructors, getters, and setters
    // Default no-args constructor required by JPA
    public PasswordResetToken() {
    }

    // ID field
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    // Token field
    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    // Expiration date field
    public LocalDateTime getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(LocalDateTime expirationDate) {
        this.expirationDate = expirationDate;
    }

    // Token validity check method
    public boolean isTokenValid() {
        return LocalDateTime.now().isBefore(this.expirationDate);
    }


}


