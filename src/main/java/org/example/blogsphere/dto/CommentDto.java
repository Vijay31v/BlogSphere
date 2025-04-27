package org.example.blogsphere.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CommentDto {

    private Long id;

    @NotBlank(message = "Comment text is required")
    @Size(min = 1, max = 1000, message = "Comment must be between 1 and 1000 characters")
    private String commentText;

    private Long blogPostId;

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCommentText() {
        return commentText;
    }

    public void setCommentText(String commentText) {
        this.commentText = commentText;
    }

    public Long getBlogPostId() {
        return blogPostId;
    }

    public void setBlogPostId(Long blogPostId) {
        this.blogPostId = blogPostId;
    }
}
