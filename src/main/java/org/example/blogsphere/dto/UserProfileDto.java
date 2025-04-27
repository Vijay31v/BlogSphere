package org.example.blogsphere.dto;

import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

public class UserProfileDto {

    @Size(max = 100, message = "Full name must be less than 100 characters")
    private String fullName;

    @Size(max = 500, message = "Bio must be less than 500 characters")
    private String bio;

    @Size(max = 100, message = "Location must be less than 100 characters")
    private String location;

    @URL(message = "Website must be a valid URL")
    @Size(max = 255, message = "Website URL must be less than 255 characters")
    private String website;

    private String profileImage;

    @Size(max = 50, message = "Twitter username must be less than 50 characters")
    private String twitterUsername;

    // Getters and setters
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
}
