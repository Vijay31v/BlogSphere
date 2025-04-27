package org.example.blogsphere.dto;

public class UserPreferenceDto {

    private boolean emailNewFollower = true;
    private boolean emailNewComment = true;
    private boolean emailNewLike = false;
    private boolean emailNewsletter = true;

    // Getters and setters
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
