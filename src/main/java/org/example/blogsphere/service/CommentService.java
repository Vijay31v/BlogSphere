package org.example.blogsphere.service;

import jakarta.persistence.EntityNotFoundException;
import org.example.blogsphere.entity.*;
import org.example.blogsphere.repository.BlogPostRepository;
import org.example.blogsphere.repository.CommentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class CommentService {
    private final CommentRepository commentRepository;
    private final BlogPostRepository blogPostRepository;
    private final NotificationService notificationService;

    @Autowired
    public CommentService(CommentRepository commentRepository, BlogPostRepository blogPostRepository, NotificationService notificationService) {
        this.commentRepository = commentRepository;
        this.blogPostRepository = blogPostRepository;
        this.notificationService = notificationService;
    }

    public List<Comment> getCommentsByPost(Long blogPostId) {
        return commentRepository.findByBlogPostId(blogPostId);
    }

    public Page<Comment> getPaginatedComments(Long blogPostId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return commentRepository.findByBlogPostId(blogPostId, pageable);
    }

    public Comment addComment(Long blogPostId, User user, String commentText) {
        BlogPost blogPost = blogPostRepository.findById(blogPostId)
                .orElseThrow(() -> new EntityNotFoundException("Blog post not found"));

        // Check if comments are allowed
        if (!blogPost.isAllowComments()) {
            throw new IllegalStateException("Comments are disabled for this post");
        }

        Comment comment = new Comment();
        comment.setBlogPost(blogPost);
        comment.setUser(user);
        comment.setCommentText(commentText);
        comment.setCreatedAt(LocalDateTime.now());

        commentRepository.save(comment);
        notificationService.createCommentNotification(
                    blogPost.getAuthor(), user, blogPost.getTitle(),
                    user.getUsername() + " commented on your post: " + blogPost.getTitle(),
                    "/blogs/" + blogPost.getId()
            );
        return comment;
    }

    public void deleteComment(Long commentId, User currentUser) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new EntityNotFoundException("Comment not found"));

        // Allow deletion if user is the comment author, the blog post author, or an admin
        boolean isCommentAuthor = comment.getUser().getId().equals(currentUser.getId());
        boolean isBlogAuthor = comment.getBlogPost().getAuthor().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRole() == Role.ROLE_ADMIN;

        if (!isCommentAuthor && !isBlogAuthor && !isAdmin) {
            throw new AccessDeniedException("You do not have permission to delete this comment");
        }

        commentRepository.delete(comment);
    }

    public List<Comment> getCommentsByUser(Long userId) {
        return commentRepository.findByUserId(userId);
    }

    public Comment getCommentById(Long commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new EntityNotFoundException("Comment not found"));
    }

    /**
     * Gets recent comments on posts authored by a specific user
     */
    public List<Comment> getRecentCommentsOnUserPosts(Long userId, int limit) {
        // Fetch all blog posts by the user
        List<BlogPost> userPosts = blogPostRepository.findByAuthorId(userId);

        // If user has no posts, return empty list
        if (userPosts.isEmpty()) {
            return Collections.emptyList();
        }

        // Extract blog post IDs
        List<Long> postIds = userPosts.stream()
                .map(BlogPost::getId)
                .collect(Collectors.toList());

        // Fetch comments for these posts, sorted by creation date
        return commentRepository.findByBlogPostIdInOrderByCreatedAtDesc(postIds, PageRequest.of(0, limit));
    }

    public Long getBlogPostIdByCommentId(Long commentId) {
        return commentRepository.findBlogPostIdByCommentId(commentId);
    }

    public Page<Comment> getLatestComments(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return commentRepository.findLatest(pageable);
    }

    public Long getCommentCount() {
        return commentRepository.count();
    }

    public Map<String, Object> getCommentStats(int year, int month) {
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", commentRepository.countByCreatedAtBetween(
                LocalDateTime.of(year, month, 1, 0, 0),
                LocalDateTime.of(year, month, 1, 0, 0).plusMonths(1)
        ));
        stats.put("activeThreads", blogPostRepository.countPostsWithComments());
        return stats;
    }

}
