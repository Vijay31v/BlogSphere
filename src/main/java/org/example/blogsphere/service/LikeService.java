package org.example.blogsphere.service;

import jakarta.persistence.EntityNotFoundException;
import org.example.blogsphere.entity.BlogPost;
import org.example.blogsphere.entity.Like;
import org.example.blogsphere.entity.User;
import org.example.blogsphere.repository.BlogPostRepository;
import org.example.blogsphere.repository.LikeRepository;
import org.example.blogsphere.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class LikeService {
    private final LikeRepository likeRepository;
    private final BlogPostRepository blogPostRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Autowired
    public LikeService(LikeRepository likeRepository, BlogPostRepository blogPostRepository, UserRepository userRepository, NotificationService notificationService) {
        this.likeRepository = likeRepository;
        this.blogPostRepository = blogPostRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    public void likePost(Long postId, Long userId) {
        if (likeRepository.existsByBlogPostIdAndUserId(postId, userId)) {
            throw new IllegalArgumentException("You have already liked this post!");
        }

        BlogPost blogPost = blogPostRepository.findById(postId)
                .orElseThrow(() -> new EntityNotFoundException("Blog post not found"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        Like like = new Like();
        like.setBlogPost(blogPost);
        like.setUser(user);

        likeRepository.save(like);
        notificationService.createLikeNotification(
                blogPost.getAuthor(), user,
                blogPost.getTitle(),
                "/blogs/" + blogPost.getId()
        );
    }

    public void unlikePost(Long postId, Long userId) {
        if (!likeRepository.existsByBlogPostIdAndUserId(postId, userId)) {
            throw new IllegalArgumentException("You have not liked this post!");
        }

        likeRepository.deleteByBlogPostIdAndUserId(postId, userId);
    }

    public boolean hasUserLikedPost(Long postId, Long userId) {
        return likeRepository.existsByBlogPostIdAndUserId(postId, userId);
    }

    public long getLikeCount(Long postId) {
        return likeRepository.countByBlogPostId(postId);
    }

    public List<Like> getLikesByUser(Long userId) {
        return likeRepository.findByUserId(userId);
    }

    public List<Like> getLikesByPost(Long postId) {
        return likeRepository.findByBlogPostId(postId);
    }

    public Map<Long, Long> getLikeCountsForPosts(List<Long> postIds) {
        return postIds.stream()
                .collect(Collectors.toMap(
                        postId -> postId,
                        postId -> likeRepository.countByBlogPostId(postId)
                ));
    }
}
